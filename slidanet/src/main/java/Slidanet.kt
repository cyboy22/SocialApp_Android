import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.view.Gravity
import android.view.View
import android.widget.TextView
import com.example.slidanet.R
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.AccessController.getContext
import java.util.*


internal object Constants {

    internal const val networkMessageSizeLength: Int = 10
    internal const val networkMessageTypeLength: Int = 5
    internal const val networkMessageHeaderLength = networkMessageSizeLength + networkMessageTypeLength
    const val floatWidth: Int = 10
    const val integerWidth: Int = 10
    const val shortWidth: Int = 5
    const val nameWidth = 2
    const val flagWidth = 1
    const val uuidWidth = 32
    const val defaultFontSize = 14.0F
    const val defaultPixelWidth = 30
    const val platformName = "platform_name"
    const val platformId = "platform_id"
    const val platformPassword = "platform_password"
    const val userId = "user_id"
    const val requestData = "request_data"
    const val responseCode = "response_code"
    const val responseData = "response_data"
    const val ipAddressLiteral = "ip_address"
    const val ipPortLiteral = "ip_port"
    const val keystorePassword = "slidanet"
    const val responseCodeLiteral = "response_code"
    const val successLiteral = "success"
    const val requestInfoLiteral = "request_info"
    const val defaultSlideEditingScale = 3.0F
    const val noRotation = 0
}

enum class SlidanetRequestType {
    KConnectToNetwork,
    KDisconnectFromNetwork,
    KDisconnectAllViews
}

internal enum class ShaderType { DefaultShader
}

internal enum class ShareModeType { Slide,
                                    Peek,
                                    Pix }

internal enum class SlideModeType { SlideNone,
                                    SlideDefinition }

internal enum class PeekModeType { PeekNone,
                                   PeekDefinition,
                                   PeekSlide }

internal enum class PixModeType { PixNone,
                                  PixDefinition,
                                  PixSlide,
                                  PixDynamic
}

internal enum class SlidanetMessageType { AuthenticateConnectionRequest,
                                          AuthenticateConnectionResponse,
                                          ConnectToViewRequest,
                                          ConnectToViewResponse,
                                          MoveRequest,
                                          MoveResponse,
                                          SetBoxRequest,
                                          SetBoxResponse,
                                          DisconnectFromViewRequest,
                                          DisconnectFromViewResponse,
                                          DisconnectAllViewsRequest,
                                          DisconnectAllViewsResponse,
                                          DisconnectRequest,
                                          DisconnectResponse,
                                          MaxValue }

enum class SlidanetResponseType { KrequestSubmitted,
                                  KplatformNameNotAlphanumeric,
                                  KplatformIdNotAlphanumeric,
                                  KPlatformIdLengthGreaterThan32,
                                  KplatformPasswordNotAlphanumeric,
                                  KPlatformPasswordLengthGreaterThan32,
                                  KslidanetUserIdNotAlphanumeric,
                                  KslidanetUserIdLengthGreaterThan32,
                                  KBadContentURL,
                                  KBadContentPath,
                                  KInvalidIpAddressFormat,
                                  KInvalidPortNumber,
                                  KInternalErrorOccurred,
                                  KConnectionAuthenticated,
                                  KDisconnectedFromAllViews,
                                  KDisconnected_,
                                  KUndefined_}

/*
enum class ClientResponseType { Ok_,
                                ConnectionAuthenticated_,
                                InvalidPlatformPassword_,
                                AuthenticationFailed_,
                                CreatedUser_,
                                CreatedView_,
                                ConnectedToView_,
                                ViewNotOnPlatform_,
                                ViewNotFound_,
                                ViewGiven_,
                                ViewTaken_,
                                VisibilityPreferenceSet_,
                                ShareModeDefinitionSet_,
                                MuteSet_,
                                HideSet_,
                                FreezeSet_,
                                InternalError_,
                                Undefined_}
*/
enum class SlidanetContentType { KImage, KVideo }

enum class SlidanetFilterType { Default }

enum class SlidanetViewParentType { KConstraintLayout,
                                    KRelativeLayout,
                                    KLinearLayout,
                                    KFrameLayout }

object Slidanet {

    internal var requestId = 0
    internal lateinit var userId: String
    internal lateinit var platformName: String
    private lateinit var platformId: String
    internal lateinit var platformPassword: String
    private lateinit var slidanetResponseHandler: SlidanetResponseHandler
    private val requests = mutableMapOf<Int, SlidanetResponseData>()
    private val slidanetViews = mutableMapOf<String, SlidanetObject>()
    internal var server = SlidanetServer()
    private var serverReadThread = HandlerThread(createUUID(),
                                                 Process.THREAD_PRIORITY_BACKGROUND)
    private var serverWriteThread = HandlerThread(createUUID(),
                                                  Process.THREAD_PRIORITY_BACKGROUND)
    private var rendererThread = HandlerThread(createUUID(), Process.THREAD_PRIORITY_BACKGROUND)

    private var mainHandler: Handler? = Handler(Looper.getMainLooper())
    internal lateinit var applicationContext: Context
    internal var sendMessageHandler: Handler
    private var receiveMessageHandler: Handler
    private var rendererHandler: Handler
    private var connectedToSlidanet: Boolean = false
    private lateinit var renderer: SlidanetRenderer
    internal val locale: Locale = Locale.ENGLISH
    private var rendererInitialized = false

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

    fun connectToNetwork(platformName: String,
                         platformId: String,
                         platformPassword: String,
                         userId: String,
                         ipAddress: String,
                         ipPort: Int,
                         appContext: Context,
                         responseHandler: SlidanetResponseHandler) : SlidanetResponseType {

        try {

            val str = platformName.filter { !it.isWhitespace()}
            if (!isLettersOrDigits(str)) return SlidanetResponseType.KplatformNameNotAlphanumeric
            if (platformName.length > getFieldCapacity(Constants.nameWidth)) return SlidanetResponseType.KplatformNameNotAlphanumeric
            if (!isLettersOrDigits(platformId)) return SlidanetResponseType.KplatformIdNotAlphanumeric
            if (platformId.length > Constants.uuidWidth) return SlidanetResponseType.KPlatformIdLengthGreaterThan32
            if (!isLettersOrDigits(platformPassword)) return SlidanetResponseType.KplatformPasswordNotAlphanumeric
            if (platformPassword.length > Constants.uuidWidth) return SlidanetResponseType.KPlatformPasswordLengthGreaterThan32
            if (!isLettersOrDigits(userId)) return SlidanetResponseType.KslidanetUserIdNotAlphanumeric
            if (userId.length > Constants.uuidWidth) return SlidanetResponseType.KslidanetUserIdLengthGreaterThan32
            if (!isValidIpAddress(ipAddress)) return SlidanetResponseType.KInvalidIpAddressFormat
            if (!isValidPortNumber(ipPort)) return SlidanetResponseType.KInvalidPortNumber

            applicationContext = appContext
            slidanetResponseHandler = responseHandler
            this.platformName = platformName
            this.platformId = platformId
            this.platformPassword = platformPassword
            this.userId = userId

            val request = JSONObject()
            val requestArray = JSONArray()
            val arrayDetails = JSONObject()
            arrayDetails.put(Constants.platformName, platformName)
            arrayDetails.put(Constants.platformId, platformId)
            arrayDetails.put(Constants.platformPassword, platformPassword)
            arrayDetails.put(Constants.userId, userId)
            requestArray.put(arrayDetails)
            request.put(Constants.requestInfoLiteral, requestArray)
            requestId++
            requests[requestId] = SlidanetResponseData(SlidanetRequestType.KConnectToNetwork,
                                                       request,
                                                       SlidanetResponseType.KUndefined_)
            if (!connectedToSlidanet) {

                rendererHandler.post { renderer = SlidanetRenderer() }
                receiveMessageHandler.post { server.connect(requestId,
                                                            ipAddress,
                                                            ipPort) }
            }
        } catch (e: JSONException) {
            return SlidanetResponseType.KInternalErrorOccurred
        }

        return SlidanetResponseType.KrequestSubmitted
    }

    fun connectNotification(requestId: Int) {

        connectedToSlidanet = true
        requests[requestId]?.apply {
            this.responseCode = SlidanetResponseType.KConnectionAuthenticated
            slidanetResponseHandler.slidanetResponse(this)
        }
    }

    fun disconnectFromNetwork() : SlidanetResponseType {

        val request = JSONObject()
        val requestArray = JSONArray()
        val arrayDetails = JSONObject()
        arrayDetails.put(Constants.userId, userId)
        requestArray.put(arrayDetails)
        request.put(Constants.requestInfoLiteral, requestArray)
        requestId++
        requests[requestId] = SlidanetResponseData(SlidanetRequestType.KDisconnectFromNetwork,
                                                   request,
                                                   SlidanetResponseType.KUndefined_)

        server.disconnectFromNetwork(requestId)

        return SlidanetResponseType.KrequestSubmitted
    }

    fun connectToView(viewId: String,
                      contentType: SlidanetContentType = SlidanetContentType.KImage,
                      contentPath: String = "",
                      applicationContext: Context) :SlidanetResponseType {

        return SlidanetResponseType.KrequestSubmitted
    }

    fun disconnectFromView(slidanetViewId: String) : SlidanetResponseType {

        return SlidanetResponseType.KrequestSubmitted
    }

    fun disconnectAllViews() : SlidanetResponseType {

        val request = JSONObject()
        val requestArray = JSONArray()
        val arrayDetails = JSONObject()
        arrayDetails.put(Constants.userId, userId)
        requestArray.put(arrayDetails)
        request.put(Constants.requestInfoLiteral, requestArray)
        requestId++
        requests[requestId] = SlidanetResponseData(SlidanetRequestType.KDisconnectAllViews,
            request,
            SlidanetResponseType.KUndefined_)

        server.disconnectAllViews(requestId)


        return SlidanetResponseType.KrequestSubmitted
    }

    fun editView(slidanetViewId: String,
                 commitWhilstEditing: Boolean = false) : SlidanetResponseType {

        return SlidanetResponseType.KrequestSubmitted
    }

    fun commitView(slidanetViewId: String) : SlidanetResponseType {

        return SlidanetResponseType.KrequestSubmitted
    }

    fun requestMore(slidanetViewId: String) : SlidanetResponseType {

        return SlidanetResponseType.KrequestSubmitted
    }

    fun requestLess(slidanetViewId: String) : SlidanetResponseType {

        return SlidanetResponseType.KrequestSubmitted
    }

    fun giveView(slidanetViewId: String) : SlidanetResponseType {

        return SlidanetResponseType.KrequestSubmitted
    }

    fun takeView(slidanetViewId: String) : SlidanetResponseType {

        return SlidanetResponseType.KrequestSubmitted
    }

    fun pauseAudio(slidanetViewId: String) : SlidanetResponseType {

        return SlidanetResponseType.KrequestSubmitted
    }

    fun resumeAudio(slidanetViewId: String) : SlidanetResponseType {

        return SlidanetResponseType.KrequestSubmitted
    }

    fun setFilter(slidanetViewId: String,
                  filterType: SlidanetFilterType = SlidanetFilterType.Default) : SlidanetResponseType {

        return SlidanetResponseType.KrequestSubmitted
    }

    internal fun processServerMessage(messageType: SlidanetMessageType, message: ByteArray) {

        try {
            when (messageType) {

                SlidanetMessageType.AuthenticateConnectionResponse -> processAuthenticateMemberResponse(message)
                SlidanetMessageType.DisconnectResponse -> processDisconnectResponse(message)
                SlidanetMessageType.DisconnectAllViewsResponse -> processDisconnectAllViewsResponse(message)

                else -> {}
            }
        } catch (e: Exception) {

        }
    }

    private fun processAuthenticateMemberResponse(message: ByteArray) {

        var responseCode = SlidanetResponseType.KUndefined_

        SlidanetMessage(message).apply {
            requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->
                responseCode = SlidanetResponseType.values()[rc_it]
            }

            if (responseCode == SlidanetResponseType.KConnectionAuthenticated) {
                // start renderer
            }

            val requestId = requireNotNull(this.getInteger(Constants.integerWidth))
            requests[requestId]?.apply {
                this.responseCode = responseCode
                mainHandler?.post { slidanetResponseHandler.slidanetResponse(this) }
            }
        }
    }

    private fun processDisconnectResponse(message: ByteArray) {

        var responseCode = SlidanetResponseType.KUndefined_

        SlidanetMessage(message).apply {
            requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->
                responseCode = SlidanetResponseType.values()[rc_it]
            }
            val requestId = requireNotNull(this.getInteger(Constants.integerWidth))
            requests[requestId]?.apply {
                this.responseCode = responseCode
                mainHandler?.post { slidanetResponseHandler.slidanetResponse(this) }
            }
        }
    }

    private fun processDisconnectAllViewsResponse(message: ByteArray) {

        var responseCode = SlidanetResponseType.KUndefined_

        SlidanetMessage(message).apply {
            requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->
                responseCode = SlidanetResponseType.values()[rc_it]
            }
            if (responseCode == SlidanetResponseType.KDisconnectedFromAllViews) {
                slidanetViews.clear()
                // all other cleanup
            }
            val requestId = requireNotNull(this.getInteger(Constants.integerWidth))
            requests[requestId]?.apply {
                this.responseCode = responseCode
                mainHandler?.post { slidanetResponseHandler.slidanetResponse(this) }
            }
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
        //flip.postScale(1f, -1f)

        val paintBg = Paint()
        paintBg.color = textBackgroundColor
        //canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)

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
}

fun isLettersOrDigits(chars: String): Boolean {
    for (c in chars)
    {
        if (c !in 'A'..'Z' && c !in 'a'..'z' && c !in '0'..'9') {
            return false
        }
    }
    return true
}

fun getFieldCapacity(value: Int) : Int {

    var result = 0

    when (value) {
        1 -> result = 9
        2 -> result = 99
        3 -> result = 999
        4 -> result = 9999
        5 -> result = 99999
        6 -> result = 999999
        7 -> result = 9999999
        8 -> result = 99999999
        9 -> result = 999999999
        else -> result = 999999999
    }

    return result
}

fun isValidIpAddress(ip: String?): Boolean {

    try {
        if (ip == null || ip.isEmpty()) {
            return false
        }
        val parts = ip.split("\\.".toRegex()).toTypedArray()
        if (parts.size != 4) {
            return false
        }

        for (s in parts) {
            val i = s.toInt()
            if (i < 0 || i > 255) {
                return false
            }
        }

        if (ip.endsWith(".")) {
            return false
        }

        return true

    } catch (nfe: NumberFormatException) {
        return false
    }
}

fun isValidPortNumber(port: Int) : Boolean {

    return port in 1..0xFFFF
}

internal fun createUUID() : String {
    return UUID.randomUUID().toString()
}

internal fun booleanToInt(b: Boolean): Int {
    return if (b) 1 else 0
}

internal fun Boolean.toInt() = if (this) 1 else 0