import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.UnknownServiceException
import java.nio.charset.StandardCharsets.UTF_8
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

    const val platformName = "platform_name"
    const val platformId = "platform_id"
    const val platformPassword = "platform_password"
    const val userId = "user_id"
    const val requestData = "request_data"
    const val responseCode = "response_code"
    const val responseData = "response_data"
    const val ipAddressLiteral = "ip_address"
    const val ipPortLiteral = "ip_port"
    const val directoryCacheURL = "http://192.168.0.7:50007/designatedService"
    const val keystorePassword = "slidanet"
    const val responseCodeLiteral = "response_code"
    const val successLiteral = "success"
    const val requestInfoLiteral = "request_info"
}

enum class ClientRequestType {
    ConnectToNetwork
}

internal enum class SlidanetMessageType { SetRouterInfoRequest,
                                          SetServerInfoRequest_,
                                          GetRouterListRequest_,
                                          GetRouterListResponse_,
                                          CreatePlatformRequest_,
                                          CreatePlatformResponse_,
                                          CreateUserRequest_,
                                          CreateUserResponse_,
                                          DeleteUserRequest_,
                                          DeleteUserResponse_,
                                          CreateViewRequest_,
                                          CreateViewResponse_,
                                          DeleteViewRequest_,
                                          DeleteViewResponse_,
                                          GetViewContextFromConnectionRequest_,
                                          GetViewContextFromConnectionResponse_,
                                          GetViewContextFromDatabaseRequest_,
                                          RouteToViewersRequest_,
                                          RouteToConnectionRequest_,
                                          RouteToOwnersRequest_,
                                          ClientHeartbeatRequest_,
                                          ClientHeartbeatResponse_,
                                          ServerHeartbeatResponse_,
                                          RouterHeartbeatRequest_,
                                          RouterHeartbeatResponse_,
                                          AuthenticateConnectionRequest_,
                                          AuthenticateConnectionResponse_,
                                          ConnectToViewRequest_,
                                          ConnectToViewResponse_,
                                          AddViewInterestRequest_,
                                          ViewerJoinedNotificationRequest_,
                                          MoveRequest_,
                                          MoveResponse_,
                                          SetBoxRequest_,
                                          SetBoxResponse_,
                                          GiveRequest_,
                                          GiveResponse_,
                                          TakeRequest_,
                                          TakeResponse_,
                                          TakeNotificationRequest_,
                                          GetViewersRequest_,
                                          GetViewersResponse_,
                                          GetTakersRequest_,
                                          GetTakersResponse_,
                                          GetVisibilityPreferenceSubscribersRequest_,
                                          GetVisibilityPreferenceSubscribersResponse_,
                                          GetVisibilityPreferenceRequest_,
                                          GetVisibilityPreferenceResponse_,
                                          SetVisibilityPreferenceRequest_,
                                          SetVisibilityPreferenceResponse_,
                                          VisibilityNotificationRequest_,
                                          SetShareModeDefinitionRequest_,
                                          SetShareModeDefinitionResponse_,
                                          SetMuteRequest_,
                                          SetMuteResponse_,
                                          SetHideRequest_,
                                          SetHideResponse_,
                                          SetFreezeRequest_,
                                          SetFreezeResponse_,
                                          DisconnectFromViewRequest_,
                                          DisconnectFromViewResponse_,
                                          RemoveViewInterestRequest_,
                                          ViewerLeftNotificationRequest_,
                                          LogUsageRequest_,
                                          AddPlatformRequest_,
                                          AddPlatformResponse_,
                                          PlatformAuthenticateRequest_,
                                          PlatformAuthenticateResponse_,
                                          PlatformAddUserRequest_,
                                          PlatformAddUserResponse_,
                                          PlatformAddViewRequest_,
                                          PlatformAddViewResponse_,
                                          PlatformGetConnectInfoRequest,
                                          PlatformGetConnectInfoResponse_,
                                          DisconnectRequest_}

enum class SlidanetResponseType { KrequestSubmitted,
                                  KplatformNameNotAlphanumeric,
                                  KplatformIdNotAlphanumeric,
                                  KPlatformIdLengthGreaterThan32,
                                  KplatformPasswordNotAlphanumeric,
                                  KPlatformPasswordLengthGreaterThan32,
                                  KslidanetUserIdNotAlphanumeric,
                                  KslidanetUserIdLengthGreaterThan32,
                                  KInternalErrorOccurred,
                                  kBadContentURL
}

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

enum class SlidanetContentType { Image, Video, Text }

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
    private val requests = mutableMapOf<Int, RequestData>()
    internal var server = SlidanetServer()
    private var serverReadThread = HandlerThread(createUUID(),
                                                 Process.THREAD_PRIORITY_BACKGROUND)
    private var serverWriteThread = HandlerThread(createUUID(),
                                                  Process.THREAD_PRIORITY_BACKGROUND)
    internal var mainHandler: Handler? = Handler(Looper.getMainLooper())
    internal lateinit var applicationContext: Context
    internal var sendMessageHandler: Handler
    private var receiveMessageHandler: Handler
    private var connectedToSlidanet: Boolean = false
    internal val locale: Locale = Locale.ENGLISH

    init {
        serverReadThread.start()
        receiveMessageHandler = Handler(serverReadThread.looper)
        serverWriteThread.start()
        sendMessageHandler = Handler(serverWriteThread.looper)
    }

    fun isConnected() : Boolean {
        return connectedToSlidanet
    }

    fun connectToNetwork(platformName: String,
                         platformId: String,
                         platformPassword: String,
                         userId: String,
                         appContext: Context,
                         responseHandler: SlidanetResponseHandler) : SlidanetResponseType {

        try {
            if (!isLettersOrDigits(platformName)) return SlidanetResponseType.KplatformNameNotAlphanumeric
            if (platformName.length > getFieldCapacity(Constants.nameWidth)) return SlidanetResponseType.KplatformNameNotAlphanumeric
            if (!isLettersOrDigits(platformId)) return SlidanetResponseType.KplatformIdNotAlphanumeric
            if (platformId.length > Constants.uuidWidth) return SlidanetResponseType.KPlatformIdLengthGreaterThan32
            if (!isLettersOrDigits(platformPassword)) return SlidanetResponseType.KplatformPasswordNotAlphanumeric
            if (platformPassword.length > Constants.uuidWidth) return SlidanetResponseType.KPlatformPasswordLengthGreaterThan32
            if (!isLettersOrDigits(userId)) return SlidanetResponseType.KslidanetUserIdNotAlphanumeric
            if (userId.length > Constants.uuidWidth) return SlidanetResponseType.KslidanetUserIdLengthGreaterThan32

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

            val requestData = RequestData(ClientRequestType.ConnectToNetwork, request, ClientResponseType.Undefined_)
            requestId++
            requests[requestId] = requestData

            val platformDetails = JSONObject()
            platformDetails.put(Constants.platformName, platformName)
            platformDetails.put(Constants.platformId, platformId)
            val url = URL(Constants.directoryCacheURL)
            val conn = url.openConnection() as HttpURLConnection
            conn.apply {
                this.requestMethod = "GET"
                this.setRequestProperty("Content-Type", "application/json; utf-8")
                this.doOutput = true
                connect()

                this.outputStream.write(platformDetails.toString().toByteArray(UTF_8),
                    0,
                    platformDetails.toString().toByteArray(UTF_8).size)


                if (this.responseCode in 200..299) {
                    val responseContent = BufferedInputStream(conn.inputStream).bufferedReader().use { it.readText() }
                    receiveMessageHandler.post { server.connect(requestId,
                        JSONObject(responseContent).get(Constants.ipAddressLiteral) as String,
                        JSONObject(responseContent).get(Constants.ipPortLiteral) as Int) }
                }
            }
            conn.disconnect()

        } catch (e: JSONException) {
            return SlidanetResponseType.KInternalErrorOccurred
        } catch (e: MalformedURLException) {
            return SlidanetResponseType.KInternalErrorOccurred
        } catch (e: IOException) {
            val error = e.toString()
            return SlidanetResponseType.KInternalErrorOccurred
        } catch (e: UnknownServiceException) {
            return SlidanetResponseType.KInternalErrorOccurred
        }

        return SlidanetResponseType.KrequestSubmitted
    }

    fun connectNotification(requestId: Int) {

        connectedToSlidanet = true
        requests[requestId]?.apply {
            this.responseCode = ClientResponseType.ConnectionAuthenticated_
            slidanetResponseHandler.slidanetResponse(this)
        }
    }

    fun disconnectFromNetwork() : SlidanetResponseType {

        return SlidanetResponseType.KrequestSubmitted
    }

    fun connectToView(contentType: SlidanetContentType,
                      contentUrl: URL,
                      contentBackgroundColor: Array<Int>,
                      contentParentType: SlidanetViewParentType,
                      contentParent: Any,
                      contentLayoutParams: Any,
                      applicationContext: Context,
                      slidanetViewId: String,
    ) :SlidanetResponseType {

        return SlidanetResponseType.KrequestSubmitted

    }

    fun disconnectFromView(slidanetViewId: String) : SlidanetResponseType {

        return SlidanetResponseType.KrequestSubmitted
    }

    fun editView(slidanetViewId: String,
                 updateWhilstEditing: Boolean = false) : SlidanetResponseType {

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

    internal fun processServerMessage(messageType: SlidanetMessageType, message: ByteArray) {

        try {
            when (messageType) {

                SlidanetMessageType.AuthenticateConnectionResponse_ -> processAuthenticateMemberResponse(message)

                else -> {}
            }
        } catch (e: Exception) {

        }
    }

    private fun processAuthenticateMemberResponse(message: ByteArray) {

        var responseCode = ClientResponseType.Undefined_

        SlidanetMessage(message).apply {
            requireNotNull(this.getInteger(Constants.shortWidth)).let { rc_it ->
                responseCode = ClientResponseType.values()[rc_it]
            }
            val requestId = requireNotNull(this.getInteger(Constants.shortWidth))
            requests[requestId]?.apply {
                this.responseCode = responseCode
                mainHandler?.post { slidanetResponseHandler.slidanetResponse(this) }
            }
        }
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

fun createUUID() : String {
    return UUID.randomUUID().toString()
}