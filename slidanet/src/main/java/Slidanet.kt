import android.content.ContentResolver
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.math.absoluteValue


object Slidanet {

    internal val requests = mutableMapOf<Int, SlidanetResponseData>()
    internal val slidanetContentAddresses = mutableMapOf<String, SlidanetObject>()
    private val slidanetLayouts = mutableMapOf<String, ConstraintLayout>()
    private var serverReadThread = HandlerThread(createUUID(), Process.THREAD_PRIORITY_BACKGROUND)
    private var serverWriteThread = HandlerThread(createUUID(), Process.THREAD_PRIORITY_BACKGROUND)
    private var rendererThread = HandlerThread(createUUID(), Process.THREAD_PRIORITY_BACKGROUND)
    private var receiveMessageHandler: Handler
    private var connectedToSlidanet: Boolean = false
    private var rendererInitialized = false
    internal var screenWidthInPixels: Int = 0
    internal var screenHeightInPixels: Int = 0
    internal var screenDensity: Float = 0f
    private val mime: MimeTypeMap = MimeTypeMap.getSingleton()
    internal val locale: Locale = Locale.ENGLISH
    internal var rendererHandler: Handler
    internal var requestId = 0
    internal var mainHandler: Handler? = Handler(Looper.getMainLooper())
    internal var sendMessageHandler: Handler
    internal var server = SlidanetServer()
    internal var contentInEditor: SlidanetObject? = null
    internal lateinit var slidaName: String
    internal lateinit var applicationName: String
    internal lateinit var applicationPassword: String
    internal lateinit var slidanetResponseHandler: SlidanetResponseHandler
    private lateinit var contentResolver: ContentResolver
    internal lateinit var renderer: SlidanetRenderer
    internal lateinit var applicationContext: Context
    internal var editorContent: SlidanetEditorContent? = null
    internal var editorControl: SlidanetContentControl? = null
    internal var relativeLayout: RelativeLayout? = null
    internal lateinit var referenceView: SlidanetImage
    private lateinit var border: SlidanetBorder
    internal var editingContent = false
    internal var editorContentAddress = ""
    internal var editingState: SlidanetEditingStateType = SlidanetEditingStateType.InActive
    internal var followerTakeInProgress = false
    internal var ownerEditingInProgress = false

    init {

        serverReadThread.start()
        receiveMessageHandler = Handler(serverReadThread.looper)
        serverWriteThread.start()
        sendMessageHandler = Handler(serverWriteThread.looper)
        rendererThread.start()
        rendererHandler = Handler(rendererThread.looper)
    }

    fun isConnected() : Boolean {

        return connectedToSlidanet
    }

    fun connect(ipAddress: String,
                ipPort: Int,
                applicationName: String,
                applicationPassword: String,
                applicationContext: Context,
                slidaName: String,
                screenWidthInPixels: Int,
                screenHeightInPixels: Int,
                screenDensity: Float,
                responseHandler: SlidanetResponseHandler) : SlidanetResponseType {

        try {

            if (!connectedToSlidanet) {

                if (isExistingRequest(SlidanetRequestType.Connect)) return SlidanetResponseType.OutstandingRequestExists
                if (!isPureAscii(applicationName)) return SlidanetResponseType.ApplicationNameContainsNonASCIICharacters
                if (applicationName.length > getFieldCapacity(Constants.nameWidth)) return SlidanetResponseType.ApplicationNameTooLong
                if (!isPureAscii(applicationPassword)) return SlidanetResponseType.ApplicationPasswordContainsNonASCIICharacters
                if (applicationPassword.length > Constants.uuidWidth) return SlidanetResponseType.ApplicationPasswordLengthGreaterThan32
                if (applicationPassword.length > getFieldCapacity(Constants.nameWidth)) return SlidanetResponseType.ApplicationPasswordTooLong
                if (!isLettersOrDigits(slidaName)) return SlidanetResponseType.SlidaNameContainsNonASCIICharacters
                if (slidaName.length > Constants.uuidWidth) return SlidanetResponseType.SlidaNameTooLong
                if (!isValidIpAddress(ipAddress)) return SlidanetResponseType.InvalidIpAddressFormat
                if (!isValidPortNumber(ipPort)) return SlidanetResponseType.InvalidPortNumber
                if (screenWidthInPixels <= 0) return SlidanetResponseType.ScreenWidthMustBeGreaterThanZero
                if (screenHeightInPixels <= 0) return SlidanetResponseType.ScreenHeightMustBeGreaterThanZero
                if (screenWidthInPixels > screenHeightInPixels) return SlidanetResponseType.ScreenHeightMustBeGreaterThanWidth

                this.screenWidthInPixels = screenWidthInPixels
                this.screenHeightInPixels = screenHeightInPixels
                this.screenDensity = screenDensity
                this.applicationContext = applicationContext

                if (editorContent == null) {

                    contentInEditor = SlidanetContentAddress(contentAddress = "ABC",
                                                             contentPath = "XYZ",
                                                             editorEnabled = true)
                    referenceView = SlidanetImage(applicationContext)
                    referenceView.id = View.generateViewId()
                    border = SlidanetBorder(Constants.defaultBorderColor, Constants.defaultBorderWidth)
                    border.setDottedLine()
                    referenceView.background = border

                    editorContent = SlidanetEditorContent(applicationContext)
                    editorContent!!.id = View.generateViewId()
                    relativeLayout = RelativeLayout(applicationContext)
                    relativeLayout?.id = View.generateViewId()
                    relativeLayout?.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                                                                               RelativeLayout.LayoutParams.MATCH_PARENT )
                    val border = GradientDrawable()
                    border.setColor(Color.argb(0,1,1,1))
                    border.setStroke(2, -0x1000000) //black border with full opacity
                    referenceView.background  = border

                }

                if (editorControl == null) {

                    editorControl = SlidanetContentControl(applicationContext)
                    editorControl!!.id = View.generateViewId()
                }

                this.applicationContext = applicationContext
                contentResolver = applicationContext.contentResolver
                slidanetResponseHandler = responseHandler
                this.applicationName = applicationName
                this.applicationPassword = applicationPassword
                this.slidaName = slidaName

                val request = JSONObject()
                request.put(SlidanetConstants.slidanet_application_name, applicationName)
                request.put(SlidanetConstants.slidanet_application_password, applicationPassword)
                request.put(SlidanetConstants.slidanet_name, slidaName)
                requestId++
                requests[requestId] = SlidanetResponseData(SlidanetRequestType.Connect,
                                                           request,
                                                           SlidanetResponseType.Undefined)

                if (!rendererInitialized) {

                        rendererHandler.post { renderer = SlidanetRenderer()
                        renderer.setEditorObject(contentInEditor!!)
                    }
                }

                receiveMessageHandler.post { server.connect(requestId = requestId,
                                                            ipAddress = ipAddress,
                                                            ipPort = ipPort) }

            } else {

                return SlidanetResponseType.AlreadyConnectedToSlidanet
            }

        } catch (e: JSONException) {

            return SlidanetResponseType.InternalErrorOccurred
        }

        return SlidanetResponseType.RequestSubmitted
    }

    fun disconnect() : SlidanetResponseType {

        if (connectedToSlidanet) {

            if (isExistingRequest(SlidanetRequestType.Disconnect)) return SlidanetResponseType.OutstandingRequestExists

            val request = JSONObject()
            request.put(SlidanetConstants.slidanet_name, slidaName)
            requestId++
            requests[requestId] = SlidanetResponseData(SlidanetRequestType.Disconnect,
                                                       request,
                                                       SlidanetResponseType.Undefined)
            server.disconnectFromNetwork(requestId = requestId)

        } else {

            return SlidanetResponseType.NotConnectedToSlidanet
        }

        return SlidanetResponseType.RequestSubmitted
    }

    fun connectContent(slidanetContentAddress: String,
                       appContentPath: String = "",
                       videoStartTime: Float = 0f,
                       updateDuringEditing: Boolean = false,
                       ) : SlidanetResponseType {

        try {
            if (isConnected()) {

                slidanetContentAddresses[slidanetContentAddress]?.let {

                    SlidanetResponseType.AlreadyConnectedToContent

                } ?: kotlin.run {

                    var contentType = SlidanetContentType.Image
                    var objectWidth = 0
                    var objectHeight = 0

                    if (isExistingRequest(SlidanetRequestType.ConnectContent, slidanetContentAddress)) return SlidanetResponseType.OutstandingRequestExists

                    if (slidanetContentAddress.length != Constants.uuidWidth || !isLettersOrDigits(slidanetContentAddress)) {

                        return SlidanetResponseType.InvalidSlidanetContentAddress
                    }

                    val file = File(appContentPath)

                    if (file.exists()) {

                        val uri = Uri.fromFile(file)

                        if (uri.scheme.equals(ContentResolver.SCHEME_CONTENT)) {

                            mime.getMimeTypeFromExtension(contentResolver.getType(uri))?.let {

                                if (Constants.supportImageFileTypes.contains(it)) {

                                    contentType = SlidanetContentType.Image

                                } else if (Constants.supportVideoFileTypes.contains(it)) {

                                    contentType = SlidanetContentType.StaticVideo

                                } else {

                                    return SlidanetResponseType.UnsupportedContentType
                                }
                            }
                        }

                        when (contentType) {

                            SlidanetContentType.Image -> {

                                BitmapFactory.decodeFile(appContentPath)?.let {

                                    objectWidth = it.width
                                    objectHeight = it.height

                                } ?: kotlin.run {

                                    return SlidanetResponseType.UnableToDecodeAppContentFile
                                }
                            }

                            SlidanetContentType.StaticVideo -> {

                                val retriever = MediaMetadataRetriever()
                                retriever.setDataSource(uri.path)

                                retriever.getFrameAtTime(floatTimeToMicroseconds(videoStartTime), MediaMetadataRetriever.OPTION_CLOSEST)?.let {

                                    objectWidth = it.width
                                    objectHeight = it.height

                                } ?: kotlin.run {

                                    return SlidanetResponseType.UnableToLoadInitialVideoFrame
                                }
                            }
                            else -> {}
                        }

                    } else {

                        return SlidanetResponseType.AppContentFileNotFound
                    }

                    val request = JSONObject()
                    request.put(SlidanetConstants.slidanet_name, slidaName)
                    request.put(SlidanetConstants.app_content_path, appContentPath)
                    request.put(SlidanetConstants.slidanet_content_type, contentType.ordinal)
                    request.put(SlidanetConstants.slidanet_content_address, slidanetContentAddress)
                    request.put(SlidanetConstants.update_during_editing, updateDuringEditing.toInt())
                    request.put(SlidanetConstants.object_width, objectWidth)
                    request.put(SlidanetConstants.object_height, objectHeight)

                    when (contentType) {

                        SlidanetContentType.Image -> request.put(Constants.contentTypeLiteral, 0)

                        SlidanetContentType.StaticVideo -> {
                            request.put(Constants.contentTypeLiteral, 1)
                        }

                        else -> {}
                    }

                    request.put(Constants.contentPathLiteral, appContentPath)
                    requestId++
                    requests[requestId] = SlidanetResponseData(requestCode = SlidanetRequestType.ConnectContent,
                                                               requestInfo = request,
                                                               responseCode =SlidanetResponseType.Undefined,
                                                               applicationContext = applicationContext)
                    server.connectContent(requestId = requestId,
                                          contentAddress = slidanetContentAddress)
                }

            } else {

                return SlidanetResponseType.NotConnectedToSlidanet
            }

        } catch (e: Exception) {

            val text1 = e.message
            val text2 = e.localizedMessage
            val text3 = e.cause
            handleInternalError()
        }

        return SlidanetResponseType.RequestSubmitted
    }

    fun commitContentEditing(slidanetContentAddress: String) : SlidanetResponseType {

        if (connectedToSlidanet) {

            if (followerTakeInProgress || ownerEditingInProgress) {

                slidanetContentAddresses[slidanetContentAddress]?.let {

                    if (editorContentAddress == slidanetContentAddress) {

                        if (it.getContentAddressOwner() == slidaName) {

                            if (!it.getUpdateDuringEdit()) {

                                val request = JSONObject()
                                requestId++
                                request.put(SlidanetConstants.slidanet_content_address, slidanetContentAddress)
                                val response = SlidanetResponseData(requestCode = SlidanetRequestType.CommitContentEditing,
                                                                    requestInfo = request,
                                                                    responseCode = SlidanetResponseType.CommittedContentEdits)
                                mainHandler?.post { slidanetResponseHandler.slidanetResponse(response) }

                                it.commitEditing()

                            } else {

                                return SlidanetResponseType.UpdatesImmediatelyDistributed
                            }

                        } else {

                            return SlidanetResponseType.NotContentAddressOwner
                        }

                    } else {

                        return SlidanetResponseType.AddressNotMatchingEditedContent
                    }

                } ?: kotlin.run {

                    return SlidanetResponseType.InvalidSlidanetContentAddress
                }

            } else {

                return SlidanetResponseType.NotEditingContent
            }

        } else {

            return SlidanetResponseType.NotConnectedToSlidanet
        }

        return SlidanetResponseType.RequestSubmitted
    }

    fun cancelContentEditing(slidanetContentAddress: String) : SlidanetResponseType {

        if (connectedToSlidanet) {

            if (ownerEditingInProgress) {

                slidanetContentAddresses[slidanetContentAddress]?.let {

                    if (editorContentAddress == slidanetContentAddress) {

                        if (it.getContentAddressOwner() == slidaName) {

                            if (!it.getUpdateDuringEdit()) {

                                val request = JSONObject()
                                requestId++
                                request.put(SlidanetConstants.slidanet_content_address, slidanetContentAddress)
                                val response = SlidanetResponseData(requestCode = SlidanetRequestType.CancelContentEditing,
                                                                    requestInfo = request,
                                                                    responseCode = SlidanetResponseType.CancelledContentEdits)
                                mainHandler?.post { slidanetResponseHandler.slidanetResponse(response) }

                                it.cancelEditing()

                            } else {

                                return SlidanetResponseType.CancelNotAllowedInThisState
                            }

                        } else {

                            return SlidanetResponseType.NotContentAddressOwner
                        }

                    } else {

                        return SlidanetResponseType.AddressNotMatchingEditedContent
                    }

                } ?: kotlin.run {

                    return SlidanetResponseType.InvalidSlidanetContentAddress
                }

            } else {

                return SlidanetResponseType.NotEditingContent
            }

        } else {

            return SlidanetResponseType.NotConnectedToSlidanet
        }

        return SlidanetResponseType.RequestSubmitted
    }

    fun setSharingStyle(slidanetContentAddress: String,
                        slidanetSharingStyle: SlidanetSharingStyleType,
                        x: Float = 1f,
                        y: Float = 0f,
                        //z: Float = 0f,
                        boxBeginX: Float = 0f,
                        boxBeginY: Float = 0f,
                        boxEndX: Float = 0f,
                        boxEndY: Float = 0f) : SlidanetResponseType {

        if (connectedToSlidanet) {

            slidanetContentAddresses[slidanetContentAddress]?.let {

                if (it.getContentAddressOwner() != slidaName) {

                    return SlidanetResponseType.NotContentAddressOwner
                }

                when (slidanetSharingStyle) {

                    SlidanetSharingStyleType.SlideLeftAndRight,
                    SlidanetSharingStyleType.SlideUpAndDown,
                    SlidanetSharingStyleType.SlideAllDirections -> {

                        if (it.getShareMode() == slidanetSharingStyle) {

                            return SlidanetResponseType.AlreadyInSlideMode
                        }

                        if (x.absoluteValue > 1f || y.absoluteValue > 1f) return SlidanetResponseType.InvalidSlideParameters

                        val request = JSONObject()
                        request.put(SlidanetConstants.slidanet_content_address, slidanetContentAddress)
                        request.put(SlidanetConstants.slidanet_share_style, Constants.slide)
                        requestId++
                        requests[requestId] = SlidanetResponseData(requestCode = SlidanetRequestType.SetContentShareStyle,
                                                                   requestInfo = request,
                                                                   responseCode = SlidanetResponseType.ShareModeDefinitionSet,
                                                                   sharingStyle = slidanetSharingStyle)

                        rendererHandler.post {

                            it.setShareMode(slidanetSharingStyle)
                            it.setShareTranslationParameters(x, y, 1f)
                            it.setShareBoxParameters(0f, 0f, 0f, 0f)
                            it.initializeVertices(x, y)

                            if (editingState == SlidanetEditingStateType.Active) {

                                mainHandler?.post { it.setupEditor(SlidanetEditingInitiatorType.LocalShareStyleUpdate) }
                            }
                        }

                        server.setShareModeSlide(requestId = requestId,
                                                 contentAddress = slidanetContentAddress,
                                                 shareMode = slidanetSharingStyle,
                                                 x = x,
                                                 y = y,
                                                 z = 1f)
                    }

                    SlidanetSharingStyleType.PeekDefine,
                    SlidanetSharingStyleType.PeekSlide -> {

                        if (it.getShareMode() == slidanetSharingStyle && it.getShareMode() == SlidanetSharingStyleType.PeekDefine) {

                            return SlidanetResponseType.AlreadyInPeekDefineMode

                        }

                        if (it.getShareMode() == slidanetSharingStyle && it.getShareMode() == SlidanetSharingStyleType.PeekSlide) {

                            return SlidanetResponseType.AlreadyInPeekSlideMode

                        }

                        if (boxBeginX.absoluteValue > 1f || boxEndX.absoluteValue > 1f) return SlidanetResponseType.InvalidSlideParameters
                        if (boxBeginY.absoluteValue > 1f || boxEndY.absoluteValue > 1f) return SlidanetResponseType.InvalidSlideParameters
                        if (boxEndX <= boxBeginX || boxEndY <= boxBeginY) return SlidanetResponseType.InvalidSlideParameters

                        val request = JSONObject()
                        request.put(SlidanetConstants.slidanet_content_address, slidanetContentAddress)
                        request.put(SlidanetConstants.slidanet_share_style, Constants.peek)
                        requestId++
                        requests[requestId] = SlidanetResponseData(requestCode = SlidanetRequestType.SetContentShareStyle,
                                                                   requestInfo = request,
                                                                   responseCode = SlidanetResponseType.ShareModeDefinitionSet,
                                                                   sharingStyle = slidanetSharingStyle)
                        rendererHandler.post {

                            it.setShareMode(slidanetSharingStyle)
                            it.setShareTranslationParameters(0f,0f,0f)
                            it.initializeVertices(0f,0f)
                            it.setShareBoxParameters(boxBeginX, boxBeginY, boxEndX, boxEndY)

                            if (editingState == SlidanetEditingStateType.Active) {

                                    mainHandler?.post { it.setupEditor(SlidanetEditingInitiatorType.LocalShareStyleUpdate) }
                            }
                        }

                        server.setShareModePeek(requestId = requestId,
                                                contentAddress = it.getContentAddress(),
                                                shareMode = it.getShareMode(),
                                                boxBeginX = boxBeginX,
                                                boxBeginY = boxBeginY,
                                                boxEndX = boxEndX,
                                                boxEndY = boxEndY)

                    }
                }
            } ?: kotlin.run {

                return SlidanetResponseType.InvalidSlidanetContentAddress
            }

        } else {

            return SlidanetResponseType.NotConnectedToSlidanet
        }

        return SlidanetResponseType.RequestSubmitted
    }

    fun disconnectContent(slidanetContentAddress: String) : SlidanetResponseType {

        if (connectedToSlidanet) {

            slidanetContentAddresses[slidanetContentAddress]?.let {

                if (isExistingRequest(SlidanetRequestType.DisconnectContent)) return SlidanetResponseType.OutstandingRequestExists

                requestId++
                val request = JSONObject()
                request.put(SlidanetConstants.slidanet_name, slidaName)
                request.put(SlidanetConstants.slidanet_content_address, slidanetContentAddress)
                requests[requestId] = SlidanetResponseData(requestCode = SlidanetRequestType.DisconnectContent,
                                                           requestInfo = request,
                                                           responseCode = SlidanetResponseType.Undefined,
                                                           applicationContext = applicationContext
                )

                server.disconnectContent(requestId = requestId,
                                         contentAddress = slidanetContentAddress)

            } ?: kotlin.run {

                return SlidanetResponseType.NotConnectedToContent
            }

        } else {

            return SlidanetResponseType.NotConnectedToSlidanet
        }

        return SlidanetResponseType.RequestSubmitted
    }

    fun disconnectAllContent() : SlidanetResponseType {

        if (connectedToSlidanet) {

            if (isExistingRequest(SlidanetRequestType.DisconnectAllContent)) return SlidanetResponseType.OutstandingRequestExists

            val request = JSONObject()
            request.put(SlidanetConstants.slidanet_name, slidaName)
            requestId++
            requests[requestId] = SlidanetResponseData(SlidanetRequestType.DisconnectAllContent,
                                                       request,
                                                       SlidanetResponseType.Undefined)

            server.disconnectAllContent(requestId = requestId)

        } else {

            return SlidanetResponseType.NotConnectedToSlidanet
        }

        return SlidanetResponseType.RequestSubmitted

    }

    fun editContent(slidanetContentAddress: String,
                    commitWhilstEditing: Boolean = false,
                    doubleTapEditingEnabled: Boolean = true) : SlidanetResponseType {

        if (connectedToSlidanet) {

            if (followerTakeInProgress || ownerEditingInProgress) {

                return SlidanetResponseType.EditingInProgress

            } else {

                slidanetContentAddresses[slidanetContentAddress]?.let {

                    if (editingState == SlidanetEditingStateType.InActive) {

                        it.setDoubleTapEditingEnabled(doubleTapEditingEnabled)
                        it.setUpdateDuringEdit(commitWhilstEditing)
                        it.setupEditor(SlidanetEditingInitiatorType.LocalEditing)

                        val request = JSONObject()
                        request.put(SlidanetConstants.slidanet_content_address, slidanetContentAddress)
                        val response = SlidanetResponseData(requestCode = SlidanetRequestType.EditContent,
                                                            requestInfo = request,
                                                            responseCode = SlidanetResponseType.EditingContent)
                        mainHandler?.post { slidanetResponseHandler.slidanetResponse(response) }
                    }

                } ?: kotlin.run {

                    return SlidanetResponseType.InvalidSlidanetContentAddress
                }
            }

        } else {

            return SlidanetResponseType.NotConnectedToSlidanet

        }

        return SlidanetResponseType.RequestSubmitted
    }

    fun hideContent(slidanetContentAddress: String) : SlidanetResponseType {

        if (connectedToSlidanet) {

            slidanetContentAddresses[slidanetContentAddress]?.let {

                if (!it.getHideEnabled()) {

                    val request = JSONObject()
                    request.put(SlidanetConstants.slidanet_content_address, slidanetContentAddress)
                    requestId++
                    requests[requestId] = SlidanetResponseData(SlidanetRequestType.HideContent,
                                                               request,
                                                               SlidanetResponseType.Undefined)

                    server.setHideState(requestId = requestId,
                                        contentAddress = slidanetContentAddress,
                                        state = true)

                } else {

                    return SlidanetResponseType.ContentAlreadyHidden
                }

            } ?: kotlin.run {

                return SlidanetResponseType.InvalidSlidanetContentAddress
            }

        } else {

            return SlidanetResponseType.NotConnectedToSlidanet
        }

        return SlidanetResponseType.RequestSubmitted
    }

    fun unHideContent(slidanetContentAddress: String) : SlidanetResponseType {

        if (connectedToSlidanet) {

            slidanetContentAddresses[slidanetContentAddress]?.let {

                if (it.getHideEnabled()) {

                    val request = JSONObject()
                    request.put(SlidanetConstants.slidanet_content_address, slidanetContentAddress)
                    requestId++
                    requests[requestId] = SlidanetResponseData(SlidanetRequestType.UnhideContent,
                                                               request,
                                                               SlidanetResponseType.Undefined)
                    server.setHideState(requestId = requestId,
                                        contentAddress = slidanetContentAddress,
                                        state = false)

                } else {

                    return SlidanetResponseType.ContentNotHidden
                }

            } ?: kotlin.run {

                return SlidanetResponseType.InvalidSlidanetContentAddress
            }

        } else {

            return SlidanetResponseType.NotConnectedToSlidanet
        }

        return SlidanetResponseType.RequestSubmitted

    }

    fun commitContent(slidanetContentAddress: String) : SlidanetResponseType {

        if (connectedToSlidanet) {

            if (ownerEditingInProgress) {


                slidanetContentAddresses[slidanetContentAddress]?.let {

                    if (editorContentAddress == slidanetContentAddress) {

                        if (it.getContentAddressOwner() == slidaName) {

                            if (!it.getUpdateDuringEdit()) {

                                val request = JSONObject()
                                request.put(SlidanetConstants.slidanet_content_address, slidanetContentAddress)
                                val response = SlidanetResponseData(requestCode = SlidanetRequestType.CancelContentEditing,
                                                                    requestInfo = request,
                                                                    responseCode = SlidanetResponseType.CancelledContentEdits)
                                mainHandler?.post { slidanetResponseHandler.slidanetResponse(response) }

                                it.commitEditing()

                            } else {

                                return SlidanetResponseType.UpdatesImmediatelyDistributed
                            }

                        } else {

                            return SlidanetResponseType.NotContentAddressOwner
                        }

                    } else {

                        return SlidanetResponseType.AddressNotMatchingEditedContent
                    }

                } ?: kotlin.run {

                    return SlidanetResponseType.InvalidSlidanetContentAddress
                }

            } else {

                return SlidanetResponseType.NotEditingContent
            }

        } else {

            return SlidanetResponseType.NotConnectedToSlidanet
        }

        return SlidanetResponseType.RequestSubmitted
    }

    fun setContentVisibilityPreference(slidanetContentAddress: String,
                                       visibilityPreference: SlidanetVisibilityPreferenceType) : SlidanetResponseType {

        slidanetContentAddresses[slidanetContentAddress]?.let {

            if (it.getContentAddressOwner() != slidaName) {

                if (it.getVisibilityPreference() == visibilityPreference) {
                    return SlidanetResponseType.PreferenceMatchesCurrent
                }

                val request = JSONObject()
                request.put(SlidanetConstants.slidanet_content_address, slidanetContentAddress)
                requestId++
                requests[requestId] = SlidanetResponseData(SlidanetRequestType.SetContentVisibilityPreference,
                                                           request,
                                                           SlidanetResponseType.Undefined)
                server.setVisibilityPreference(requestId = requestId,
                                               contentAddress = slidanetContentAddress,
                                               preference = visibilityPreference.ordinal)

            } else {

                return SlidanetResponseType.AlreadyContentAddressOwner
            }

        } ?: kotlin.run {

            return SlidanetResponseType.InvalidSlidanetContentAddress
        }

        return SlidanetResponseType.RequestSubmitted
    }

    fun giveContent(slidanetContentAddress: String) : SlidanetResponseType {

        slidanetContentAddresses[slidanetContentAddress]?.let {

            if (it.getContentAddressOwner() == slidaName) {

                if (!it.getGiveEnabled()) {

                    val request = JSONObject()
                    request.put(SlidanetConstants.slidanet_content_address, slidanetContentAddress)
                    requestId++
                    requests[requestId] = SlidanetResponseData(SlidanetRequestType.GiveContent,
                                                               request,
                                                               SlidanetResponseType.Undefined)

                    it.setGiveEnabled(true)
                    server.giveContentAddress(requestId = requestId,
                                              contentAddress =  slidanetContentAddress)

                } else {

                    return SlidanetResponseType.GiveAlreadyEnabled
                }

            } else {

                return SlidanetResponseType.NotContentAddressOwner
            }

        } ?: kotlin.run {

            return SlidanetResponseType.InvalidSlidanetContentAddress
        }

        return SlidanetResponseType.RequestSubmitted
    }

    fun takeContent(slidanetContentAddress: String) : SlidanetResponseType {

        if (connectedToSlidanet) {

            slidanetContentAddresses[slidanetContentAddress]?.let {

                if (it.getContentAddressOwner() == slidaName) {

                    if (it.getGiveEnabled()) {

                        val request = JSONObject()
                        request.put(SlidanetConstants.slidanet_content_address, slidanetContentAddress)
                        requestId++
                        requests[requestId] = SlidanetResponseData(SlidanetRequestType.TakeContent,
                                                                   request,
                                                                   SlidanetResponseType.Undefined)

                        it.setGiveEnabled(false)
                        server.takeContentAddress(requestId = requestId,
                                                  contentAddress =  slidanetContentAddress)

                    } else {

                        return SlidanetResponseType.GiveNotEnabled
                    }

                } else {

                    return SlidanetResponseType.NotContentAddressOwner
                }

            } ?: kotlin.run {

                return SlidanetResponseType.InvalidSlidanetContentAddress
            }

        } else {

            SlidanetResponseType.NotConnectedToSlidanet
        }

        return SlidanetResponseType.RequestSubmitted
    }

    fun pauseContent(slidanetContentAddress: String) : SlidanetResponseType {

        if (connectedToSlidanet) {

            slidanetContentAddresses[slidanetContentAddress]?.let {

                if (it.getContentType() == SlidanetContentType.StaticVideo) {

                    it.getVideoPlayer().pause()
                    val request = JSONObject()
                    request.put(SlidanetConstants.slidanet_content_address, slidanetContentAddress)
                    val response = SlidanetResponseData(SlidanetRequestType.PauseContent,
                                                               request,
                                                               SlidanetResponseType.Undefined)
                    mainHandler?.post { slidanetResponseHandler.slidanetResponse(response) }


                } else {

                    SlidanetResponseType.VideoContentTypeRequired

                }

            } ?: kotlin.run {

                return SlidanetResponseType.InvalidSlidanetContentAddress
            }

        } else {

            SlidanetResponseType.NotConnectedToSlidanet
        }

        return SlidanetResponseType.RequestSubmitted
    }

    fun playContent(slidanetContentAddress: String) : SlidanetResponseType {

        if (connectedToSlidanet) {

            slidanetContentAddresses[slidanetContentAddress]?.let {

                if (it.getContentType() == SlidanetContentType.StaticVideo) {

                    it.getVideoPlayer().play()
                    val request = JSONObject()
                    request.put(SlidanetConstants.slidanet_content_address, slidanetContentAddress)
                    val response = SlidanetResponseData(SlidanetRequestType.PlayContent,
                                                        request,
                                                        SlidanetResponseType.Undefined)
                    mainHandler?.post { slidanetResponseHandler.slidanetResponse(response) }

                } else {

                    SlidanetResponseType.VideoContentTypeRequired

                }

            } ?: kotlin.run {

                return SlidanetResponseType.InvalidSlidanetContentAddress
            }

        } else {

            SlidanetResponseType.NotConnectedToSlidanet
        }

        return SlidanetResponseType.RequestSubmitted
    }

    fun setContentFilter(slidanetContentAddress: String,
                         slidanetContentFilter: SlidanetContentFilterType) : SlidanetResponseType {

        if (connectedToSlidanet) {

            slidanetContentAddresses[slidanetContentAddress]?.let {

                if (slidanetContentFilter != it.getContentFilter()) {

                    val request = JSONObject()
                    request.put(SlidanetConstants.slidanet_content_address, slidanetContentAddress)
                    request.put(SlidanetConstants.slidanet_content_filter, slidanetContentFilter.ordinal)
                    requestId++
                    requests[requestId] = SlidanetResponseData(SlidanetRequestType.SetContentFilter,
                                                               request,
                                                               SlidanetResponseType.Undefined)
                    server.setContentFilter(requestId = requestId,
                                            contentAddress = slidanetContentAddress,
                                            filter =  slidanetContentFilter.ordinal )

                } else {

                    return SlidanetResponseType.ContentFilterAlreadySet
                }

            } ?: kotlin.run {

                return SlidanetResponseType.InvalidSlidanetContentAddress
            }

        } else {

            SlidanetResponseType.NotConnectedToSlidanet
        }

        return SlidanetResponseType.RequestSubmitted
    }

    internal fun processServerMessage(messageType: SlidanetMessageType, message: ByteArray) {

        try {

            when (messageType) {

                SlidanetMessageType.AuthenticateConnectionResponse -> processAuthenticateMemberResponse(message)
                SlidanetMessageType.ConnectContentResponse -> processConnectContentResponse(message)
                SlidanetMessageType.UpdateContentContextRequest -> processUpdateContentContextRequest(message)
                SlidanetMessageType.MoveContentRequest -> processMoveContentRequest(message)
                SlidanetMessageType.SetContentShareModeRequest -> processSetContentShareModeRequest(message)
                SlidanetMessageType.SetContentShareModeResponse -> processSetContentShareModeResponse(message)
                SlidanetMessageType.DisconnectContentResponse -> processDisconnectContentResponse(message)
                SlidanetMessageType.GiveContentRequest -> processGiveContentRequest(message)
                SlidanetMessageType.GiveContentResponse -> processGiveContentResponse(message)
                SlidanetMessageType.TakeContentRequest -> processTakeContentRequest(message)
                SlidanetMessageType.TakeContentResponse -> processTakeContentResponse(message)
                SlidanetMessageType.SetContentVisibilityPreferenceResponse -> processSetContentVisibilityPreferenceResponse(message)
                SlidanetMessageType.SetHideContentResponse -> processHideContentResponse(message)
                SlidanetMessageType.SetContentFilterRequest -> processSetContentFilterResponse(message)
                SlidanetMessageType.DisconnectAllContentResponse -> processDisconnectAllContentResponse(message)
                SlidanetMessageType.DisconnectResponse -> processDisconnectResponse(message)

                else -> {}
            }
        } catch (e: Exception) {

            val text = e.message
            val text2 = e.localizedMessage
            val text3 = e.cause
            e.printStackTrace()
        }
    }

    private fun processAuthenticateMemberResponse(message: ByteArray) {

        try {

            var responseCode: SlidanetResponseType

            SlidanetMessage(message).apply {

                val requestId = requireNotNull(this.getInteger(Constants.integerWidth))

                if (requestId < 0 || requestId > requests.size) {

                    handleInternalError()
                }

                requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->

                    if (rc_it < 0 || rc_it >= SlidanetResponseType.MaxValue.ordinal) {

                        handleInternalError()
                    }

                    responseCode = SlidanetResponseType.values()[rc_it]

                    if (responseCode == SlidanetResponseType.ConnectionAuthenticated) {

                        rendererHandler.post { renderer.setRenderingState(true) }
                        connectedToSlidanet = true
                    }

                    requests[requestId]?.apply {

                        this.responseCode = responseCode
                        slidanetResponseHandler.slidanetResponse(this)
                    }

                    requests.remove(requestId)
                }
            }
        } catch (e: IllegalArgumentException) {

            handleInternalError()
        }
    }
    private fun processDisconnectResponse(message: ByteArray) {

        try {

            var responseCode: SlidanetResponseType

            SlidanetMessage(message).apply {

                val requestId = requireNotNull(this.getInteger(Constants.integerWidth))

                if (requestId < 0 || requestId >= requests.size) {

                    throw IllegalAccessException("")
                }

                requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->

                    if (rc_it >= 0 && rc_it < SlidanetResponseType.MaxValue.ordinal) {

                        responseCode = SlidanetResponseType.values()[rc_it]

                        requests[requestId]?.apply {

                            this.responseCode = responseCode
                            mainHandler?.post { slidanetResponseHandler.slidanetResponse(this) }

                        }

                        requests.clear()
                        removeSlidanetContentAddresses()
                        connectedToSlidanet = false
                        rendererHandler.post { renderer.setRenderingState(false) }

                    } else {

                        throw IllegalAccessException("")
                    }
                }
            }
        } catch (e: IllegalArgumentException) {

            requests.clear()
            removeSlidanetContentAddresses()
            connectedToSlidanet = false
            rendererHandler.post { renderer.setRenderingState(false) }
            val request = JSONObject()
            request.put(SlidanetConstants.slidanet_name, slidaName)

            SlidanetResponseData(SlidanetRequestType.Disconnect,
                                 request,
                                 SlidanetResponseType.Undefined)
        }
    }

    private fun processConnectContentResponse(message:ByteArray) {

        try {

            var responseCode: SlidanetResponseType

            SlidanetMessage(message).let { msg ->

                val requestId = requireNotNull(msg.getInteger(Constants.integerWidth))

                requireNotNull(msg.getInteger(Constants.shortWidth)).let { rc_it ->

                    if (rc_it >= 0 && rc_it < SlidanetResponseType.MaxValue.ordinal) {

                        responseCode = SlidanetResponseType.values()[rc_it]

                        requests[requestId]?.let { req ->

                            req.responseCode = responseCode

                            when (responseCode) {

                                SlidanetResponseType.AppContentConnectedToSlidanetAddress -> {

                                    req.requestInfo.getString(SlidanetConstants.slidanet_content_address).let { slidanetContentAddress ->

                                        val contentAddressOwnerLength = requireNotNull(msg.getInteger(Constants.nameWidth))
                                        val contentAddressOwner = requireNotNull(msg.getString(contentAddressOwnerLength))

                                        if (contentAddressOwner == slidaName) {

                                            val visibilityPreferenceCount = requireNotNull(msg.getInteger(Constants.integerWidth))

                                            if (visibilityPreferenceCount > 0) {

                                                val visibilityPreferences = mutableMapOf<String, Boolean>()

                                                try {

                                                    for (i in 0 until visibilityPreferenceCount-1) {

                                                        val subscriber = requireNotNull(msg.getString(Constants.uuidWidth))
                                                        val preference = requireNotNull(msg.getInteger(Constants.nameWidth)).bool
                                                        visibilityPreferences[subscriber] = preference
                                                    }

                                                    slidanetContentAddresses[slidanetContentAddress]?.setVisibilityPreferences(visibilityPreferences)

                                                } catch (e: JSONException) {

                                                    handleInternalError()
                                                }
                                            }

                                            val give = requireNotNull(msg.getInteger(Constants.flagWidth)).bool

                                            if (give) {

                                                val takersCount = requireNotNull(msg.getInteger(Constants.integerWidth))

                                                val takers = mutableMapOf<String, String>()

                                                try {

                                                    for (i in 0 until takersCount-1) {

                                                        val taker = requireNotNull(msg.getString(Constants.uuidWidth))
                                                        val timestampLength = requireNotNull(msg.getInteger(Constants.nameWidth))
                                                        val preference = requireNotNull(msg.getString(timestampLength))
                                                        takers[taker] = preference
                                                    }

                                                    slidanetContentAddresses[slidanetContentAddress]?.setTakers(takers)

                                                } catch (e: JSONException) {

                                                    handleInternalError()
                                                }
                                            }
                                        } else {

                                            val viewTaken = requireNotNull(msg.getInteger(Constants.flagWidth)).bool

                                            if (viewTaken) {

                                                val shareMode = requireNotNull(msg.getInteger(Constants.nameWidth))

                                                if (shareMode < 0 || shareMode >= SlidanetSharingStyleType.MaxValue.ordinal) {

                                                    handleInternalError()
                                                }

                                                val takeTranslationX = requireNotNull(msg.getFloat())
                                                val takeTranslationY = requireNotNull(msg.getFloat())
                                                val takeTranslationZ = requireNotNull(msg.getFloat())
                                                val takeBoxBeginX = requireNotNull(msg.getFloat())
                                                val takeBoxBeginY= requireNotNull(msg.getFloat())
                                                val takeBoxEndX = requireNotNull(msg.getFloat())
                                                val takeBoxEndY = requireNotNull(msg.getFloat())

                                                slidanetContentAddresses[slidanetContentAddress]?.let {

                                                    it.setContentAddressOwner(contentAddressOwner)

                                                    rendererHandler.post { it.setShareTranslationParameters(takeTranslationX,
                                                                                                            takeTranslationY,
                                                                                                            takeTranslationZ)
                                                    it.setShareBoxParameters(takeBoxBeginX,
                                                                             takeBoxBeginY,
                                                                             takeBoxEndX,
                                                                             takeBoxEndY) }
                                                    it.setUpdateDuringEdit(false)
                                                }

                                                val visibilityPreference = requireNotNull(msg.getInteger(Constants.nameWidth))
                                                var pref = SlidanetVisibilityPreferenceType.Neutral

                                                when (visibilityPreference) {

                                                    0 -> pref = SlidanetVisibilityPreferenceType.RequestLess
                                                    1-> pref = SlidanetVisibilityPreferenceType.Neutral
                                                    2-> pref = SlidanetVisibilityPreferenceType.RequestMore
                                                }

                                                slidanetContentAddresses[slidanetContentAddress]?.setVisibilityPreference(pref)

                                            }
                                        }

                                        createSlidanetContentAddress(requestId)?.let { slidaContentAddress ->

                                            slidanetContentAddresses[slidanetContentAddress] = slidaContentAddress
                                            rendererHandler.post { renderer.addRenderingObject(slidanetContentAddress, slidaContentAddress) }
                                            slidaContentAddress.id = View.generateViewId()
                                            req.slidanetContentContainer = createClientLayout(slidaContentAddress, this.applicationContext)
                                        }
                                    }
                                }

                                else -> {

                                }
                            }

                            mainHandler?.post { slidanetResponseHandler.slidanetResponse(req) }
                        }

                        requests.remove(requestId)

                    } else {

                        handleInternalError()
                    }
                }
            }

        } catch (e: IllegalArgumentException) {

            handleInternalError()
        }
    }

    private fun processGiveContentResponse(message: ByteArray) {

        try {

            var responseCode: SlidanetResponseType

            SlidanetMessage(message).apply {

                val requestId = requireNotNull(this.getInteger(Constants.shortWidth))

                if (requestId < 0 || requestId >= requests.size) {

                    throw IllegalAccessException("")
                }

                requests[requestId]?.let {

                    requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->

                        if (rc_it < 0 || rc_it >= SlidanetResponseType.MaxValue.ordinal) {

                            handleInternalError()
                        }
                        responseCode = SlidanetResponseType.values()[rc_it]

                        if (responseCode == SlidanetResponseType.ContentGiven) {

                            val contentAddress = it.requestInfo.getString(SlidanetConstants.slidanet_content_address)

                            if (contentAddress.isNotEmpty()) {

                                slidanetContentAddresses[contentAddress]?.setGiveEnabled(true)
                                it.responseCode = responseCode
                                mainHandler?.post { slidanetResponseHandler.slidanetResponse(it) }
                                requests.remove(requestId)
                            }
                        }
                    }
                }
            }

        } catch (e: java.lang.IllegalArgumentException) {

            handleInternalError()
        }
    }

    private fun processSetContentVisibilityPreferenceResponse(message: ByteArray) {

        var responseCode: SlidanetResponseType

        try {

            SlidanetMessage(message).apply {

                val requestId = requireNotNull(this.getInteger(Constants.shortWidth))

                if (requestId < 0 || requestId >= requests.size) {

                    throw IllegalAccessException("")
                }

                requests[requestId]?.let {

                    requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->

                        if (rc_it < 0 || rc_it >= SlidanetResponseType.MaxValue.ordinal) {

                            handleInternalError()
                        }
                        responseCode = SlidanetResponseType.values()[rc_it]

                        if (responseCode == SlidanetResponseType.VisibilityPreferenceSet) {

                            val visibilityPreference = requireNotNull(this.getInteger(Constants.nameWidth))

                            if (visibilityPreference < 0 || visibilityPreference >= SlidanetVisibilityPreferenceType.MaxValue.ordinal) {

                                handleInternalError()
                            }

                            val contentAddress = it.requestInfo.getString(SlidanetConstants.slidanet_content_address)

                            if (contentAddress.isNotEmpty()) {

                                slidanetContentAddresses[contentAddress]?.setVisibilityPreference(SlidanetVisibilityPreferenceType.values()[visibilityPreference])
                                it.responseCode = responseCode
                                mainHandler?.post { slidanetResponseHandler.slidanetResponse(it) }
                                requests.remove(requestId)
                            }
                        }
                    }
                }
            }

        } catch (e: java.lang.IllegalArgumentException) {

            handleInternalError()
        }
    }

    private fun processTakeContentResponse(message: ByteArray) {

        var responseCode: SlidanetResponseType

        try {

            SlidanetMessage(message).apply {

                val requestId = requireNotNull(this.getInteger(Constants.shortWidth))

                if (requestId < 0 || requestId >= requests.size) {

                    throw IllegalAccessException("")
                }

                requests[requestId]?.let {

                    requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->

                        if (rc_it < 0 || rc_it >= SlidanetResponseType.MaxValue.ordinal) {

                            handleInternalError()
                        }
                        responseCode = SlidanetResponseType.values()[rc_it]

                        if (responseCode == SlidanetResponseType.ContentTaken) {

                            val contentAddress = it.requestInfo.getString(SlidanetConstants.slidanet_content_address)

                            if (contentAddress.isNotEmpty()) {

                                slidanetContentAddresses[contentAddress]?.setGiveEnabled(false)

                                it.responseCode = responseCode
                                mainHandler?.post { slidanetResponseHandler.slidanetResponse(it) }
                                requests.remove(requestId)
                            }
                        }
                    }
                }
            }

        } catch (e: java.lang.IllegalArgumentException) {

            handleInternalError()
        }
    }

    private fun processHideContentResponse(message: ByteArray) {

        var responseCode: SlidanetResponseType

        try {

            SlidanetMessage(message).apply {

                val requestId = requireNotNull(this.getInteger(Constants.shortWidth))

                if (requestId < 0 || requestId >= requests.size) {

                    throw IllegalAccessException("")
                }

                requests[requestId]?.let {

                    requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->

                        if (rc_it < 0 || rc_it >= SlidanetResponseType.MaxValue.ordinal) {

                            handleInternalError()
                        }
                        responseCode = SlidanetResponseType.values()[rc_it]

                        slidanetContentAddresses[it.requestInfo.getString(SlidanetConstants.slidanet_content_address)]?.let { contentAddress ->

                            when (responseCode) {

                                SlidanetResponseType.ContentHidden -> {
                                    rendererHandler.post { contentAddress.setHideEnabled(true)
                                                           contentAddress.setContentAlpha(0f)
                                                           contentAddress.setDisplayNeedsUpdate(true)
                                    }
                                }

                                SlidanetResponseType.ContentUnhidden -> {
                                    rendererHandler.post { contentAddress.setHideEnabled(false)
                                                           contentAddress.setContentAlpha(1f)
                                                           contentAddress.setDisplayNeedsUpdate(true)
                                    }
                                }

                                else -> { }
                            }

                            it.responseCode = responseCode
                            mainHandler?.post { slidanetResponseHandler.slidanetResponse(it) }
                            requests.remove(requestId)
                        }
                    }
                }
            }

        } catch (e: java.lang.IllegalArgumentException) {

            handleInternalError()
        }
    }

    private fun processSetContentFilterResponse(message: ByteArray) {

        var responseCode: SlidanetResponseType

        try {

            SlidanetMessage(message).apply {

                val requestId = requireNotNull(this.getInteger(Constants.shortWidth))

                if (requestId < 0 || requestId >= requests.size) {

                    throw IllegalAccessException("")
                }

                requests[requestId]?.let {

                    requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->

                        if (rc_it < 0 || rc_it >= SlidanetResponseType.MaxValue.ordinal) {

                            handleInternalError()
                        }
                        responseCode = SlidanetResponseType.values()[rc_it]

                        slidanetContentAddresses[it.requestInfo.getString(SlidanetConstants.slidanet_content_address)]?.let { contentAddress ->

                            if (responseCode == SlidanetResponseType.FilterSet) {

                                rendererHandler.post {

                                    contentAddress.setContentFilter(SlidanetContentFilterType.values()[it.requestInfo.getInt(SlidanetConstants.slidanet_content_filter)])
                                    contentAddress.setDisplayNeedsUpdate(true)
                                }
                            }

                            it.responseCode = responseCode
                            mainHandler?.post { slidanetResponseHandler.slidanetResponse(it) }
                            requests.remove(requestId)
                        }
                    }
                }
            }

        } catch (e: java.lang.IllegalArgumentException) {

            handleInternalError()
        }
    }

    private fun processGiveContentRequest(message: ByteArray) {

        try {

            SlidanetMessage(message).apply {

                val contentAddressLength = requireNotNull(this.getInteger(Constants.shortWidth))
                val contentAddress = requireNotNull(this.getString(contentAddressLength))

                slidanetContentAddresses[contentAddress]?.let {
                    it.setGiveEnabled(true)

                    val request = JSONObject()
                    request.put(SlidanetConstants.slidanet_content_address, it.getContentAddress())
                    val response = SlidanetResponseData(SlidanetRequestType.SlidanetUpdate,
                                                        request,
                                                        SlidanetResponseType.ContentGiven)
                    mainHandler?.post { slidanetResponseHandler.slidanetResponse(response) }
                }
            }

        } catch (e: java.lang.IllegalArgumentException) {

            handleInternalError()
        }
    }

    private fun processTakeContentRequest(message: ByteArray) {

        try {

            SlidanetMessage(message).apply {

                val contentAddressLength = requireNotNull(this.getInteger(Constants.shortWidth))
                val contentAddress = requireNotNull(this.getString(contentAddressLength))

                slidanetContentAddresses[contentAddress]?.let {
                    it.setGiveEnabled(false)

                    val request = JSONObject()
                    request.put(SlidanetConstants.slidanet_content_address, it.getContentAddress())
                    val response = SlidanetResponseData(SlidanetRequestType.SlidanetUpdate,
                                                        request,
                                                        SlidanetResponseType.ContentTaken)
                    mainHandler?.post { slidanetResponseHandler.slidanetResponse(response) }
                }
            }

        } catch (e: java.lang.IllegalArgumentException) {

            handleInternalError()
        }
    }

    private fun processDisconnectContentResponse(message: ByteArray) {

        try {

            var responseCode: SlidanetResponseType

            SlidanetMessage(message).apply {

                val requestId = requireNotNull(this.getInteger(Constants.shortWidth))

                if (requestId < 0 || requestId >= requests.size) {

                    handleInternalError()
                }

                requests[requestId]?.let {

                    requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->

                        if (rc_it < 0 || rc_it >= SlidanetResponseType.MaxValue.ordinal) {

                            handleInternalError()
                        }
                        responseCode = SlidanetResponseType.values()[rc_it]

                        if (responseCode == SlidanetResponseType.DisconnectedFromContent) {

                            val contentAddress = it.requestInfo.getString(SlidanetConstants.slidanet_content_address)

                            if (contentAddress.isNotEmpty()) {

                                removeSlidanetContentAddress(contentAddress)

                                it.responseCode = responseCode
                                mainHandler?.post { slidanetResponseHandler.slidanetResponse(it) }

                                requests.remove(requestId)
                            }
                        }
                    }
                }
            }
        } catch (e: IllegalArgumentException) {

            handleInternalError()
        }
    }

    private fun processMoveContentRequest(message: ByteArray) {

        try {

            var responseCode: SlidanetResponseType

            SlidanetMessage(message).apply {

                val contentAddressLength = requireNotNull(this.getInteger(Constants.nameWidth))
                val contentAddress = requireNotNull(this.getString(contentAddressLength))
                requireNotNull(this.getInteger(Constants.nameWidth)).let { rc_it ->

                    if (rc_it < 0 || rc_it >= SlidanetSharingStyleType.MaxValue.ordinal) {

                        handleInternalError()
                    }

                    when (SlidanetSharingStyleType.values()[rc_it]) {

                        SlidanetSharingStyleType.SlideLeftAndRight,
                        SlidanetSharingStyleType.SlideUpAndDown,
                        SlidanetSharingStyleType.SlideAllDirections-> {

                            val x = requireNotNull(this.getFloat())
                            val y = requireNotNull(this.getFloat())
                            val z = requireNotNull(this.getFloat())

                            slidanetContentAddresses[contentAddress]?.let {

                                if (contentAddress == editorContentAddress) {

                                    if (it.getContentAddressOwner() == slidaName) {

                                        contentInEditor?.let { contentEditor ->

                                            rendererHandler.post {

                                                contentEditor.setShareTranslationParameters(x,y,z)
                                                contentEditor.initializeVertices(x,y)

                                                if (it.getUpdateDuringEdit()) {

                                                    it.setShareTranslationParameters(x,y,z)
                                                    it.initializeVertices(x,y)
                                                    it.setDisplayNeedsUpdate(true)
                                                }
                                            }
                                        }

                                    } else {

                                        rendererHandler.post {

                                            it.setShareTranslationParameters(x,y,z)
                                            it.initializeVertices(x,y)
                                            it.setDisplayNeedsUpdate(true)
                                        }
                                    }

                                } else {

                                    rendererHandler.post {

                                        it.setShareTranslationParameters(x,y,z)
                                        it.initializeVertices(x, y)
                                        it.setDisplayNeedsUpdate(true)
                                    }
                                }

                            } ?: kotlin.run {

                                handleInternalError()
                            }
                        }

                        SlidanetSharingStyleType.PeekDefine,
                        SlidanetSharingStyleType.PeekSlide -> {

                            val boxBeginX = requireNotNull(this.getFloat())
                            val boxBeginY = requireNotNull(this.getFloat())
                            val boxEndX = requireNotNull(this.getFloat())
                            val boxEndY = requireNotNull(this.getFloat())

                            slidanetContentAddresses[contentAddress]?.let {

                                if (contentAddress == editorContentAddress) {

                                    if (it.getContentAddressOwner() == slidaName) {

                                        contentInEditor?.let { contentEditor ->

                                            rendererHandler.post {

                                                contentEditor.setShareBoxParameters(boxBeginX, boxBeginY, boxEndX, boxEndY)

                                                if (it.getUpdateDuringEdit()) {

                                                    it.setShareBoxParameters(boxBeginX, boxBeginY, boxEndX, boxEndY)
                                                    it.setDisplayNeedsUpdate(true)
                                                }
                                            }
                                        }

                                    } else {

                                        rendererHandler.post {

                                            it.setShareBoxParameters(boxBeginX, boxBeginY, boxEndX, boxEndY)
                                            it.setDisplayNeedsUpdate(true)
                                        }
                                    }
                                } else {

                                    rendererHandler.post {

                                        it.setShareBoxParameters(boxBeginX, boxBeginY, boxEndX, boxEndY)
                                        it.setDisplayNeedsUpdate(true)
                                    }
                                }
                            }
                        }

                        else -> {}
                    }
                }
            }
        } catch (e: IllegalArgumentException) {

            handleInternalError()
        }
    }

    private fun processDisconnectAllContentResponse(message: ByteArray) {

        try {

            var responseCode: SlidanetResponseType

            SlidanetMessage(message).apply {

                requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->

                    if (rc_it < 0 || rc_it >= SlidanetResponseType.MaxValue.ordinal) {

                        throw java.lang.IllegalArgumentException("")
                    }

                    responseCode = SlidanetResponseType.values()[rc_it]

                    if (responseCode == SlidanetResponseType.DisconnectedFromAllContent) {

                        requests.clear()
                        removeSlidanetContentAddresses()

                    }

                    val requestId = requireNotNull(this.getInteger(Constants.integerWidth))

                    if (requestId < 0 || requestId >= requests.size) {

                        handleInternalError()
                    }

                    requests[requestId]?.apply {

                        this.responseCode = responseCode
                        mainHandler?.post { slidanetResponseHandler.slidanetResponse(this) }
                    }

                    requests.remove(requestId)
                }
            }
        } catch (e: IllegalArgumentException) {

            handleInternalError()
        }
    }

    private fun processSetContentShareModeRequest(message: ByteArray) {

        try {

            SlidanetMessage(message).apply {

                val contentAddressLength = requireNotNull(this.getInteger(Constants.nameWidth))
                val contentAddress = requireNotNull(this.getString(contentAddressLength))

                requireNotNull(this.getInteger(Constants.nameWidth)).let { rc_it ->

                    if (rc_it < 0 || rc_it >= SlidanetSharingStyleType.MaxValue.ordinal) {

                        handleInternalError()
                    }

                    when (SlidanetSharingStyleType.values()[rc_it]) {

                        SlidanetSharingStyleType.SlideLeftAndRight,
                        SlidanetSharingStyleType.SlideUpAndDown,
                        SlidanetSharingStyleType.SlideAllDirections -> {

                            val x = requireNotNull(getFloat())
                            val y = requireNotNull(getFloat())
                            val z = requireNotNull(getFloat())

                            slidanetContentAddresses[contentAddress]?.let {

                                if (contentAddress == editorContentAddress) {

                                    if (it.getContentAddressOwner() == slidaName) {

                                        contentInEditor?.let { contentEditor ->

                                            rendererHandler.post {

                                                contentEditor.setShareTranslationParameters(x, y, z)
                                                contentEditor.initializeVertices(x, y)
                                                contentEditor.setPeekEnabled(false)
                                                contentEditor.setDisplayNeedsUpdate(true)

                                                if (it.getUpdateDuringEdit()) {

                                                    it.setShareTranslationParameters(x,y, z)
                                                    it.initializeVertices(x, y)
                                                    it.setPeekEnabled(false)
                                                    it.setDisplayNeedsUpdate(true)

                                                }
                                            }
                                        }

                                    } else {

                                        rendererHandler.post {

                                            it.setShareTranslationParameters(x, y, z)
                                            it.initializeVertices(x, y)
                                            it.setPeekEnabled(false)
                                            it.setDisplayNeedsUpdate(true)

                                        }
                                    }

                                } else {

                                    rendererHandler.post {

                                        it.setShareTranslationParameters(x, y, z)
                                        it.initializeVertices(x, y)
                                        it.setPeekEnabled(false)
                                        it.setDisplayNeedsUpdate(true)

                                    }
                                }

                            } ?: kotlin.run {

                                handleInternalError()
                            }
                        }

                        SlidanetSharingStyleType.PeekDefine,
                        SlidanetSharingStyleType.PeekSlide -> {

                            val boxBeginX = requireNotNull(getFloat())
                            val boxBeginY = requireNotNull(getFloat())
                            val boxEndX = requireNotNull(getFloat())
                            val boxEndY = requireNotNull(getFloat())

                            slidanetContentAddresses[contentAddress]?.let {

                                if (contentAddress == editorContentAddress) {

                                    if (it.getContentAddressOwner() == slidaName) {

                                        contentInEditor?.let { contentEditor ->

                                            rendererHandler.post {

                                                contentEditor.setShareTranslationParameters(0f,0f,1f)
                                                contentEditor.initializeVertices(0f,0f)
                                                contentEditor.setShareBoxParameters(boxBeginX, boxBeginY, boxEndX, boxEndY)
                                                contentEditor.setDisplayNeedsUpdate(true)

                                                if (it.getUpdateDuringEdit()) {

                                                    it.initializeVertices(0f,0f)
                                                    it.setShareBoxParameters(boxBeginX, boxBeginY, boxEndX, boxEndY)
                                                    it.setDisplayNeedsUpdate(true)

                                                }
                                            }
                                        }

                                    } else {

                                        rendererHandler.post {

                                            it.setShareTranslationParameters(0f,0f,1f)
                                            it.initializeVertices(0f,0f)
                                            it.setShareBoxParameters(boxBeginX, boxBeginY, boxEndX, boxEndY)
                                            it.setDisplayNeedsUpdate(true)

                                        }
                                    }
                                } else {

                                    rendererHandler.post {

                                        it.setShareTranslationParameters(0f,0f,1f)
                                        it.initializeVertices(0f,0f)
                                        it.setShareBoxParameters(boxBeginX, boxBeginY, boxEndX, boxEndY)
                                        it.setDisplayNeedsUpdate(true)

                                    }
                                }
                            }
                        }

                        else -> {}
                    }
                }
            }
        } catch (e: IllegalArgumentException) {

            handleInternalError()
        }
    }

    private fun processSetContentShareModeResponse(message: ByteArray) {

        try {

            var responseCode: SlidanetResponseType

            SlidanetMessage(message).apply {


                val requestId = requireNotNull(this.getInteger(Constants.integerWidth))

                if (requestId < 0 || requestId >= requests.size) {

                    throw IllegalArgumentException("")
                }

                requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->

                    if (rc_it < 0 || rc_it >= SlidanetResponseType.MaxValue.ordinal) {

                        throw IllegalArgumentException("")
                    }

                    responseCode = SlidanetResponseType.values()[rc_it]
                    if (responseCode != SlidanetResponseType.ShareModeDefinitionSet) {

                        throw IllegalArgumentException("")
                    }

                    requests[requestId]?.apply {

                        this.responseCode = responseCode
                        mainHandler?.post { slidanetResponseHandler.slidanetResponse(this) }
                    }

                    requests.remove(requestId)
                }
            }
        } catch (e: IllegalArgumentException) {

            handleInternalError()
        }
    }

    private fun processUpdateContentContextRequest(message: ByteArray) {

        try {

            SlidanetMessage(message).apply {

                val slidanetContentAddressLength = requireNotNull(this.getInteger(Constants.nameWidth))
                val slidanetContentAddress = requireNotNull(this.getString(slidanetContentAddressLength))

                slidanetContentAddresses[slidanetContentAddress]?.let {

                    val contentAddressOwnerLength = requireNotNull(this.getInteger(Constants.nameWidth))
                    val contentAddressOwner = requireNotNull(this.getString(contentAddressOwnerLength))
                    val giveEnabled = requireNotNull(this.getInteger(Constants.flagWidth)).bool
                    val hideEnabled = requireNotNull(this.getInteger(Constants.flagWidth)).bool
                    val shareMode = requireNotNull(this.getInteger(Constants.nameWidth))
                    if (shareMode < 0 || shareMode >= SlidanetSharingStyleType.MaxValue.ordinal) handleInternalError()
                    val shareModeType = SlidanetSharingStyleType.values()[shareMode]
                    val translationX = requireNotNull(this.getFloat())
                    val translationY = requireNotNull(this.getFloat())
                    val translationZ = requireNotNull(this.getFloat())
                    val boxBeginX = requireNotNull(this.getFloat())
                    val boxBeginY = requireNotNull(this.getFloat())
                    val boxEndX = requireNotNull(this.getFloat())
                    val boxEndY = requireNotNull(this.getFloat())
                    val contentFilter = requireNotNull(this.getInteger(Constants.shortWidth))
                    val redMaskColor = requireNotNull(this.getFloat())
                    val blueMaskColor = requireNotNull(this.getFloat())
                    val greenMaskColor = requireNotNull(this.getFloat())
                    if (contentFilter < 0 || contentFilter >= SlidanetContentFilterType.MaxValue.ordinal) handleInternalError()
                    val contentFilterType = SlidanetContentFilterType.values()[contentFilter]

                    rendererHandler.post {

                        when (shareModeType) {

                            SlidanetSharingStyleType.PeekSlide,
                            SlidanetSharingStyleType.PeekDefine -> it.setPeekEnabled(true)

                            else -> {}
                        }

                        it.setGiveEnabled(giveEnabled)
                        it.setHideEnabled(hideEnabled)
                        it.setShareMode(shareModeType)
                        it.setShareTranslationParameters(translationX, translationY, translationZ)
                        it.setShareBoxParameters(boxBeginX, boxBeginY, boxEndX, boxEndY)
                        it.setContentFilter(contentFilterType)
                        it.setContentAddressOwner(contentAddressOwner)
                        it.setRedMaskColor(redMaskColor)
                        it.setBlueMaskColor(blueMaskColor)
                        it.setGreenMaskColor(greenMaskColor)
                        it.initializeVertices(translationX, translationY)

                        if (slidaName == contentAddressOwner) {

                        }

                        if (it.getTextureViewReady()) {
                            it.setDisplayNeedsUpdate(true)
                        }
                    }
                }
            }
        } catch (e: IllegalArgumentException) {

            handleInternalError()
        }
    }

    fun textToImage(text : CharSequence,
                    typeface: Typeface,
                    fontSize: Float,
                    textColor: Int,
                    textBackgroundColor: Int,
                    screenDensity: Float,
                    textIndent: Int,
                    context: Context) : Bitmap {

        val textView = TextView(context)

        textView.gravity = Gravity.START
        textView.text = text
        textView.textSize = fontSize
        textView.typeface = typeface
        textView.setTextColor(textColor)
        textView.setBackgroundColor(textBackgroundColor)

        textView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

        val width = textView.measuredWidth + (textIndent * screenDensity.toInt())
        val height = textView.measuredHeight + (textIndent * screenDensity.toInt())

        val image: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        textView.layout(0,0,width,height)
        textView.draw(canvas)

        val flip = Matrix()

        val paintBg = Paint()
        paintBg.color = textBackgroundColor

        return Bitmap.createBitmap(image,0,0,image.width, image.height, flip,true)
    }

    fun loadBitmapFromView(v: View): Bitmap? {

        val b = Bitmap.createBitmap(
            v.layoutParams.width,
            v.layoutParams.height,
            Bitmap.Config.ARGB_8888
        )
        val c = Canvas(b)
        v.layout(v.left, v.top, v.right, v.bottom)
        v.draw(c)
        return b
    }

    private fun createClientLayout(slidanetContentAddress: SlidanetContentAddress, applicationContext: Context): ConstraintLayout {

        val v = ConstraintLayout(applicationContext)
        v.id = View.generateViewId()

        val constraintLayoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT,
                                                                   ConstraintLayout.LayoutParams.MATCH_PARENT)
        slidanetContentAddress.layoutParams = constraintLayoutParams
        v.addView(slidanetContentAddress)

        return v
    }

    private fun createSlidanetContentAddress(requestId: Int): SlidanetContentAddress? {

        var ca: SlidanetContentAddress? = null

        requests[requestId]?.let {

            val requestData: JSONObject = it.requestInfo
            val slidanetContentAddress = requestData.getString(SlidanetConstants.slidanet_content_address)
            val contentPath = requestData.getString(SlidanetConstants.app_content_path)

            when (requestData.getInt(SlidanetConstants.slidanet_content_type)) {

                0 -> ca = SlidanetContentAddress(contentAddress = slidanetContentAddress,
                                                 contentType = SlidanetContentType.Image,
                                                 contentPath = contentPath)

                1 -> { val videoStartTime = requestData.getDouble(Constants.videoStartTimeLiteral)
                       ca = SlidanetContentAddress(contentAddress = slidanetContentAddress,
                                                   contentType = SlidanetContentType.StaticVideo,
                                                   contentPath = contentPath)
                }
            }
        }

        return ca
    }

    internal fun getRawResource(resource: Int): String? {

        var res: String? = null

        try {
            val input: InputStream = applicationContext.resources.openRawResource(resource)
            val baos = ByteArrayOutputStream()
            val b = ByteArray(1)

            while (input.read(b) != -1) {
                baos.write(b)
            }
            res = baos.toString()
            input.close()
            baos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return res
    }

    private fun handleInternalError() {

        disconnect()
    }

    private fun removeSlidanetContentAddress(contentAddress: String) {

        slidanetContentAddresses[contentAddress]?.let {

            val e = it as SlidanetContentAddress
            (e.parent as ViewGroup).removeView(e)
            slidanetLayouts[contentAddress]?.let { containerLayout ->
                (containerLayout.parent as ViewGroup).removeView(containerLayout)
            }
            rendererHandler.post { it.detachSurfaceTexture()
                                   it.releaseSurfaceTexture()
                                   it.deleteTextures()
                                   renderer.slidaObjects.remove(it.getContentAddress())
            }

            slidanetContentAddresses.remove(it.getContentAddress())

        }
    }

    private fun removeSlidanetContentAddresses() {

        for ((key, element) in slidanetContentAddresses) {

            removeSlidanetContentAddress(key)
        }

        rendererHandler.post { contentInEditor?.deleteTextures() }
        slidanetLayouts.clear()
        slidanetContentAddresses.clear()

        rendererHandler.post { renderer.clearRenderingObjects()
                               renderer.setRenderingState(false) }
    }

    private fun isExistingRequest(requestType: SlidanetRequestType, slidanetContentAddress: String = "") : Boolean {

        var found = false

        for ((_, element) in requests) {

            if (element.requestCode == requestType && slidanetContentAddress == "") {

                found = true
                break

            } else if (slidanetContentAddress.isNotEmpty()) {
                val json = element.requestInfo
                val vId = json.getString(Constants.slidanetContentAddressLiteral)
                if (vId == slidanetContentAddress) {

                    found = true
                    break
                }
            }
        }

        return found
    }

    internal fun setRendererInitialized(state: Boolean) {

        rendererInitialized = state
    }
}