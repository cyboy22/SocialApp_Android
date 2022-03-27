import android.content.ContentResolver
import android.content.Context
import android.graphics.*
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
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.*

object Slidanet {

    private val requests = mutableMapOf<Int, SlidanetResponseData>()
    private val slidanetViews = mutableMapOf<String, SlidanetObject>()
    private val slidanetLayouts = mutableMapOf<String, ConstraintLayout>()
    private var serverReadThread = HandlerThread(createUUID(),
                                                 Process.THREAD_PRIORITY_BACKGROUND)
    private var serverWriteThread = HandlerThread(createUUID(),
                                                  Process.THREAD_PRIORITY_BACKGROUND)
    private var rendererThread = HandlerThread(createUUID(), Process.THREAD_PRIORITY_BACKGROUND)
    private var receiveMessageHandler: Handler
    private var connectedToSlidanet: Boolean = false
    private var rendererInitialized = false
    private val mime: MimeTypeMap = MimeTypeMap.getSingleton()
    internal val locale: Locale = Locale.ENGLISH
    internal var rendererHandler: Handler
    internal var requestId = 0
    internal var mainHandler: Handler? = Handler(Looper.getMainLooper())
    internal var sendMessageHandler: Handler
    internal var server = SlidanetServer()
    internal lateinit var slidaName: String
    internal lateinit var applicationName: String
    internal lateinit var applicationPassword: String
    private lateinit var slidanetResponseHandler: SlidanetResponseHandler
    private lateinit var contentResolver: ContentResolver
    internal lateinit var renderer: SlidanetRenderer
    internal lateinit var applicationContext: Context
    internal lateinit var editingContent: SlidanetObject

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
                appContext: Context,
                slidaName: String,
                responseHandler: SlidanetResponseHandler) : SlidanetResponseType {

        try {

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

            applicationContext = appContext
            contentResolver = applicationContext.contentResolver

            slidanetResponseHandler = responseHandler
            this.applicationName = applicationName
            this.applicationPassword = applicationPassword
            this.slidaName = slidaName

            val request = JSONObject()
            request.put(SlidanetConstants.application_name, applicationName)
            request.put(SlidanetConstants.application_password, applicationPassword)
            request.put(SlidanetConstants.slida_name, slidaName)

            requestId++
            requests[requestId] = SlidanetResponseData(SlidanetRequestType.Connect,
                                                       request,
                                                       SlidanetResponseType.Undefined)
            if (!connectedToSlidanet) {

                if (!rendererInitialized) {
                    rendererHandler.post { renderer = SlidanetRenderer() }
                }
                receiveMessageHandler.post { server.connect(requestId, ipAddress, ipPort) }
            }
        } catch (e: JSONException) {
            return SlidanetResponseType.InternalErrorOccurred
        }

        return SlidanetResponseType.RequestSubmitted
    }

    fun disconnect() : SlidanetResponseType {

        if (isExistingRequest(SlidanetRequestType.Disconnect)) return SlidanetResponseType.OutstandingRequestExists

        val request = JSONObject()
        request.put(SlidanetConstants.slida_name, slidaName)

        requestId++
        requests[requestId] = SlidanetResponseData(SlidanetRequestType.Disconnect,
                                                   request,
                                                   SlidanetResponseType.Undefined)

        server.disconnectFromNetwork(requestId)

        return SlidanetResponseType.RequestSubmitted
    }

    fun connectContent(slidanetContentAddress: String,
                       appContentPath: String = "",
                       videoStartTime: Float = 0.0F,
                       updateDuringEditing: Boolean = true) :SlidanetResponseType {

        if (isConnected()) {

            var contentType = SlidanetContentType.Image

            var objectWidth = 0
            var objectHeight = 0

            if (isExistingRequest(SlidanetRequestType.ConnectContent, slidanetContentAddress)) return SlidanetResponseType.OutstandingRequestExists

            if (slidanetContentAddress.length != Constants.uuidWidth || !isLettersOrDigits(slidanetContentAddress)) {

                return SlidanetResponseType.InvalidSlidanetContentAddress
            }

            if (videoStartTime < 0.0) {

                return SlidanetResponseType.InvalidAppVideoStartTime
            }

            val file = File(appContentPath)

            if (file.exists()) {

                val uri = Uri.fromFile(file)

                if (uri.scheme.equals(ContentResolver.SCHEME_CONTENT)) {

                    mime.getMimeTypeFromExtension(contentResolver.getType(uri))?.let {

                        if (Constants.supportImageFileTypes.contains(it)) {

                            contentType = SlidanetContentType.Image

                        } else if (Constants.supportVideoFileTypes.contains(it)) {

                            contentType = SlidanetContentType.Video

                        } else {

                            return SlidanetResponseType.UnsupportedContentType
                        }
                    }
                }

                when (contentType) {


                    SlidanetContentType.Image -> {

                        BitmapFactory.decodeFile(uri.toString())?.let {

                            objectWidth = it.width
                            objectHeight = it.height

                        } ?: kotlin.run {

                            return SlidanetResponseType.UnableToDecodeAppContentFile
                        }
                    }

                    SlidanetContentType.Video -> {

                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(uri.path)

                        retriever.getFrameAtTime(floatTimeToMicroseconds(videoStartTime), MediaMetadataRetriever.OPTION_CLOSEST)?.let {

                            objectWidth = it.width
                            objectHeight = it.height

                        } ?: kotlin.run {

                            return SlidanetResponseType.UnableToLoadInitialVideoFrame
                        }
                    }
                }
            } else {

                return SlidanetResponseType.AppContentFileNotFound
            }

            val request = JSONObject()
            request.put(SlidanetConstants.slida_name, slidaName)
            request.put(SlidanetConstants.content_address, slidanetContentAddress)
            request.put(SlidanetConstants.update_during_editing, updateDuringEditing.toInt())
            request.put(SlidanetConstants.object_width, objectWidth)
            request.put(SlidanetConstants.object_height, objectHeight)

            when (contentType) {

                SlidanetContentType.Image -> request.put(Constants.contentTypeLiteral, 0)
                SlidanetContentType.Video -> {
                    request.put(Constants.contentTypeLiteral, 1)
                    request.put(Constants.videoStartTimeLiteral, videoStartTime )
                }
            }

            request.put(Constants.contentPathLiteral, appContentPath)

            requestId++
            requests[requestId] = SlidanetResponseData(requestCode = SlidanetRequestType.ConnectContent,
                                                       requestInfo = request,
                                                       responseCode =SlidanetResponseType.Undefined,
                                                       applicationContext = applicationContext)

            server.connectContent(requestId = requestId,
                                  slidanetContentAddress = slidanetContentAddress)




        } else {

            return SlidanetResponseType.NotConnectedToSlidanet
        }

        return SlidanetResponseType.RequestSubmitted
    }

    fun disconnectContent(slidanetContentAddress: String) : SlidanetResponseType {

        if (isExistingRequest(SlidanetRequestType.DisconnectContent)) return SlidanetResponseType.OutstandingRequestExists

        return SlidanetResponseType.RequestSubmitted
    }

    fun disconnectAllContent() : SlidanetResponseType {

        if (isExistingRequest(SlidanetRequestType.DisconnectAllContent)) return SlidanetResponseType.OutstandingRequestExists

        val request = JSONObject()
        request.put(SlidanetConstants.slida_name, slidaName)
        requestId++
        requests[requestId] = SlidanetResponseData(SlidanetRequestType.DisconnectAllContent,
            request,
            SlidanetResponseType.Undefined)

        server.disconnectAllContent(requestId)


        return SlidanetResponseType.RequestSubmitted
    }

    fun editContent(slidanetContentAddress: String,
                    commitWhilstEditing: Boolean = false) : SlidanetResponseType {

        return SlidanetResponseType.RequestSubmitted
    }

    fun commitContent(slidanetContentAddress: String) : SlidanetResponseType {

        return SlidanetResponseType.RequestSubmitted
    }

    fun requestMoreContent(slidanetContentAddress: String) : SlidanetResponseType {

        return SlidanetResponseType.RequestSubmitted
    }

    fun requestLessContent(slidanetContentAddress: String) : SlidanetResponseType {

        return SlidanetResponseType.RequestSubmitted
    }

    fun giveContent(slidanetContentAddress: String) : SlidanetResponseType {

        return SlidanetResponseType.RequestSubmitted
    }

    fun takeContent(slidanetContentAddress: String) : SlidanetResponseType {

        return SlidanetResponseType.RequestSubmitted
    }

    fun pauseContentAudio(slidanetContentAddress: String) : SlidanetResponseType {

        return SlidanetResponseType.RequestSubmitted
    }

    fun resumeContentAudio(slidanetContentAddress: String) : SlidanetResponseType {

        return SlidanetResponseType.RequestSubmitted
    }

    fun setContentFilter(slidanetContentAddress: String,
                         filterType: SlidanetFilterType = SlidanetFilterType.Default) : SlidanetResponseType {

        return SlidanetResponseType.RequestSubmitted
    }

    internal fun processServerMessage(messageType: SlidanetMessageType, message: ByteArray) {

        try {
            when (messageType) {

                SlidanetMessageType.AuthenticateConnectionResponse -> processAuthenticateMemberResponse(message)
                SlidanetMessageType.ConnectContentResponse -> processConnectContentResponse(message)
                SlidanetMessageType.UpdateContentContextResponse -> processUpdateContentContextResponse(message)
                SlidanetMessageType.DisconnectAllContentResponse -> processDisconnectAllContentResponse(message)
                SlidanetMessageType.DisconnectResponse -> processDisconnectResponse(message)

                else -> {}
            }
        } catch (e: Exception) {

        }
    }

    private fun processAuthenticateMemberResponse(message: ByteArray) {

        try {
            var responseCode: SlidanetResponseType

            SlidanetMessage(message).apply {

                requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->

                    if (rc_it < 0 || rc_it >= SlidanetResponseType.MaxValue.ordinal) {

                        handleInternalError()
                    }

                    responseCode = SlidanetResponseType.values()[rc_it]

                    if (responseCode == SlidanetResponseType.ConnectionAuthenticated) {

                        rendererHandler.post { renderer.setRenderingState(true) }
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

    private fun processDisconnectResponse(message: ByteArray) {

        try {

            var responseCode: SlidanetResponseType

            SlidanetMessage(message).apply {

                requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->

                    if (rc_it >= 0 && rc_it < SlidanetResponseType.MaxValue.ordinal) {

                        responseCode = SlidanetResponseType.values()[rc_it]
                        val requestId = requireNotNull(this.getInteger(Constants.integerWidth))

                        if (requestId >= 0 && requestId < requests.size) {

                            requests[requestId]?.apply {

                                this.responseCode = responseCode
                                mainHandler?.post { slidanetResponseHandler.slidanetResponse(this) }
                            }

                            requests.clear()
                            removeSlidanetViews()
                            connectedToSlidanet = false
                            rendererHandler.post { renderer.setRenderingState(false) }

                        }
                    }
                }
            }
        } catch (e: IllegalArgumentException) {

            handleInternalError()
        }
    }

    private fun processConnectContentResponse(message:ByteArray) {

        try {

            var responseCode: SlidanetResponseType

            SlidanetMessage(message).let { msg ->

                requireNotNull(msg.getInteger(Constants.shortWidth)).let { rc_it ->

                    if (rc_it >= 0 && rc_it < SlidanetResponseType.MaxValue.ordinal) {

                        responseCode = SlidanetResponseType.values()[rc_it]
                        val requestId = requireNotNull(msg.getInteger(Constants.integerWidth))

                        if (requestId < 0 || requestId >= requests.size) {

                            handleInternalError()
                        }

                        requests[requestId]?.let { req ->

                            req.responseCode = responseCode

                            when (responseCode) {

                                SlidanetResponseType.AppContentConnectedToSlidanetAddress -> {

                                    req.requestInfo.getString(Constants.slidanetContentAddressLiteral).let { slidanetContentAddress ->

                                        val contentAddressOwner = requireNotNull(msg.getString(Constants.uuidWidth))

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

                                                    slidanetViews[slidanetContentAddress]?.setVisibilityPreferences(visibilityPreferences)

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

                                                    slidanetViews[slidanetContentAddress]?.setTakers(takers)

                                                } catch (e: JSONException) {

                                                    handleInternalError()
                                                }
                                            }
                                        } else {

                                            val viewTaken = requireNotNull(msg.getInteger(Constants.flagWidth)).bool

                                            if (viewTaken) {

                                                val shareMode = requireNotNull(msg.getInteger(Constants.nameWidth))

                                                if (shareMode < 0 || shareMode >= ShareModeType.MaxValue.ordinal) {

                                                    handleInternalError()
                                                }

                                                val takeTranslationX = requireNotNull(msg.getFloat())
                                                val takeTranslationY = requireNotNull(msg.getFloat())
                                                val takeTranslationZ = requireNotNull(msg.getFloat())
                                                val takeBoxBeginX = requireNotNull(msg.getFloat())
                                                val takeBoxBeginY= requireNotNull(msg.getFloat())
                                                val takeBoxEndX = requireNotNull(msg.getFloat())
                                                val takeBoxEndY = requireNotNull(msg.getFloat())

                                                slidanetViews[slidanetContentAddress]?.let {

                                                    rendererHandler.post { it.setShareTranslationParameters(takeTranslationX,
                                                                                                            takeTranslationY,
                                                                                                            takeTranslationZ)
                                                    it.setShareBoxParameters(takeBoxBeginX,
                                                                             takeBoxBeginY,
                                                                             takeBoxEndX,
                                                                             takeBoxEndY) }

                                                }

                                                val visibilityPreference = requireNotNull(msg.getInteger(Constants.nameWidth))
                                                var pref = VisibilityPreferenceType.Neutral

                                                when (visibilityPreference) {

                                                    -1 -> pref = VisibilityPreferenceType.RequestLess
                                                     0 -> pref = VisibilityPreferenceType.Neutral
                                                     1 -> pref = VisibilityPreferenceType.RequestMore
                                                }

                                                slidanetViews[slidanetContentAddress]?.setVisibilityPreference(pref)

                                            }
                                        }

                                        createSlidanetView(requestId)?.let { slidaView ->

                                            slidanetViews[slidanetContentAddress] = slidaView
                                            rendererHandler.post { renderer.addRenderingObject(slidanetContentAddress, slidaView) }
                                            slidaView.id = View.generateViewId()
                                            req.slidanetView = createClientLayout(slidaView, this.applicationContext)
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

    private fun processDisconnectAllContentResponse(message: ByteArray) {

        try {

            var responseCode: SlidanetResponseType

            SlidanetMessage(message).apply {

                requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->

                    if (rc_it < 0 || rc_it >= SlidanetResponseType.MaxValue.ordinal) {

                        handleInternalError()
                    }

                    responseCode = SlidanetResponseType.values()[rc_it]

                    if (responseCode == SlidanetResponseType.DisconnectedFromAllContent) {

                        requests.clear()
                        removeSlidanetViews()
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

    private fun processUpdateContentContextResponse(message: ByteArray) {

        try {

            SlidanetMessage(message).apply {

                val slidanetContentAddress = requireNotNull(this.getString(Constants.uuidWidth))

                slidanetViews[slidanetContentAddress]?.let {

                    val contentAddressOwner = requireNotNull(this.getString(Constants.uuidWidth))

                    val giveEnabled = requireNotNull(this.getInteger(Constants.flagWidth)).bool
                    val hideEnabled = requireNotNull(this.getInteger(Constants.flagWidth)).bool
                    val muteEnabled = requireNotNull(this.getInteger(Constants.flagWidth)).bool
                    val freezeEnabled = requireNotNull(this.getInteger(Constants.flagWidth)).bool

                    val shareMode = requireNotNull(this.getInteger(Constants.nameWidth))
                    if (shareMode < 0 || shareMode >= ShareModeType.MaxValue.ordinal) handleInternalError()
                    val shareModeType = ShareModeType.values()[shareMode]

                    val slideMode = requireNotNull(this.getInteger(Constants.nameWidth))
                    if (slideMode < 0 || slideMode >= SlideModeType.MaxValue.ordinal) handleInternalError()
                    val slideModeType = SlideModeType.values()[slideMode]

                    val peekMode = requireNotNull(this.getInteger(Constants.nameWidth))
                    if (peekMode < 0 || peekMode >= PeekModeType.MaxValue.ordinal) handleInternalError()
                    val peekModeType = PeekModeType.values()[peekMode]

                    val pixMode = requireNotNull(this.getInteger(Constants.nameWidth))
                    if (pixMode < 0 || pixMode >= PixModeType.MaxValue.ordinal) handleInternalError()
                    val pixModeType = PixModeType.values()[pixMode]

                    val translationX = requireNotNull(this.getFloat())
                    val translationY = requireNotNull(this.getFloat())
                    val translationZ = requireNotNull(this.getFloat())

                    val fadeBarrier = requireNotNull(this.getFloat())
                    val snapThreshold = requireNotNull(this.getFloat())

                    val boxBeginX = requireNotNull(this.getFloat())
                    val boxBeginY = requireNotNull(this.getFloat())
                    val boxEndX = requireNotNull(this.getFloat())
                    val boxEndY = requireNotNull(this.getFloat())

                    val pixPercentage = requireNotNull(this.getFloat())

                    val shaderNameLength = requireNotNull(this.getInteger(Constants.nameWidth))
                    val shaderName = requireNotNull(this.getString(shaderNameLength))

                    val slideEnabled = requireNotNull(this.getInteger(Constants.flagWidth)).bool
                    val peekEnabled = requireNotNull(this.getInteger(Constants.flagWidth)).bool
                    val pixEnabled = requireNotNull(this.getInteger(Constants.flagWidth)).bool

                    rendererHandler.post {  it.setGiveEnabled(giveEnabled)
                                            it.setHideEnabled(hideEnabled)
                                            it.setMuteEnabled(muteEnabled)
                                            it.setFreezeEnabled(freezeEnabled)
                                            it.setShareMode(shareModeType)
                                            it.setSlideMode(slideModeType)
                                            it.setPeekMode(peekModeType)
                                            it.setPixMode(pixModeType)
                                            it.setShareTranslationParameters(translationX, translationY, translationZ)
                                            it.setFadeBarrier(fadeBarrier)
                                            it.setSnapThreshold(snapThreshold)
                                            it.setShareBoxParameters(boxBeginX, boxBeginY, boxEndX, boxEndY)
                                            it.setPixPercentage(pixPercentage)
                                            it.setShaderName(shaderName)
                                            it.setSlideEnabled(slideEnabled)
                                            it.setPeekEnabled(peekEnabled)
                                            it.setPixEnabled(pixEnabled)

                                            if (it.getTextureViewReady()) it.setDisplayNeedsUpdate(true)

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

    private fun createClientLayout(slidanetView: SlidanetView, applicationContext: Context): ConstraintLayout {

        val v = ConstraintLayout(applicationContext)
        v.id = View.generateViewId()
        val constraintLayoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT,
                                                                   ConstraintLayout.LayoutParams.WRAP_CONTENT)
        v.layoutParams = constraintLayoutParams
        v.addView(slidanetView)
        val constraintSet = ConstraintSet()
        constraintSet.clone(v)
        constraintSet.connect(slidanetView.id, ConstraintSet.TOP, v.id, ConstraintSet.TOP, 0)
        constraintSet.connect(slidanetView.id, ConstraintSet.LEFT, v.id, ConstraintSet.LEFT, 0)
        constraintSet.connect(slidanetView.id, ConstraintSet.BOTTOM, v.id, ConstraintSet.BOTTOM, 0)
        constraintSet.connect(slidanetView.id, ConstraintSet.RIGHT, v.id, ConstraintSet.RIGHT, 0)
        constraintSet.applyTo(v);

        return v
    }

    private fun createSlidanetView(requestId: Int): SlidanetView? {

        var slidanetView: SlidanetView? = null

        requests[requestId]?.let {

            val requestData: JSONObject = it.requestInfo
            val slidanetContentAddress = requestData.getString(Constants.slidanetContentAddressLiteral)
            val contentPath = requestData.getString(Constants.contentPathLiteral)

            when (requestData.getInt(Constants.contentTypeLiteral)) {

                0 -> slidanetView = SlidanetView(contentAddress = slidanetContentAddress,
                                                 contentType = SlidanetContentType.Image,
                                                 contentPath = contentPath,
                                                 applicationContext = it.applicationContext!!)
                1 -> { val videoStartTime = requestData.getDouble(Constants.videoStartTimeLiteral)
                       slidanetView = SlidanetView(contentAddress = slidanetContentAddress,
                                                   contentType = SlidanetContentType.Video,
                                                   contentPath = contentPath,
                                                   videoStartTime = videoStartTime.toFloat(),
                                                   applicationContext = it.applicationContext!!)
                }
            }
        }

        return slidanetView
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

    private fun removeSlidanetViews() {

        for ((_, element) in slidanetViews) {
            val e = element as SlidanetView
            (e.parent as ViewGroup).removeView(e)
        }

        for ((_, element) in slidanetLayouts) {
            (element.parent as ViewGroup).removeView(element)
        }

        slidanetLayouts.clear()
        slidanetViews.clear()

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