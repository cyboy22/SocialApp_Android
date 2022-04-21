import android.content.ContentValues.TAG
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.slidanet.R
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.lang.Exception
import java.net.UnknownHostException
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory

internal class SlidanetServer(): SlidanetRequest {

    private val TAG: String = "SocialServer"
    private var inputStream: DataInputStream? = null
    private var outputStream: DataOutputStream? = null
    private var mainHandler: Handler? = Handler(Looper.getMainLooper())
    private var sslSocket: SSLSocket? = null
    private var connected = false

    fun connect(requestId: Int,
                ipAddress: String,
                ipPort: Int) {

        try {

            initialize(requestId, ipAddress, ipPort)
            processSession()

        } catch (e: Exception) {

            sslSocket?.close()
            e.message?.let { Log.d(TAG, it) }
        }
    }

    @Throws(
        UnknownHostException::class,
        IOException::class,
        EOFException::class,
        SecurityException::class,
        IllegalArgumentException:: class,
        IndexOutOfBoundsException::class,
        NullPointerException::class)
    internal fun initialize(requestId: Int,
                            ipAddress: String,
                            ipPort: Int) {

        val keystore = KeyStore.getInstance("BKS")
        val keystoreInputStream = Slidanet.applicationContext.resources?.openRawResource(R.raw.slidanet)
        keystore.load(keystoreInputStream, Constants.keystorePassword.toCharArray())
        keystoreInputStream?.close()

        val algorithm = KeyManagerFactory.getDefaultAlgorithm()
        val keyManagerFactory = KeyManagerFactory.getInstance(algorithm)
        keyManagerFactory.init(keystore, Constants.keystorePassword.toCharArray())

        val tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        tmf.init(keystore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.keyManagers, tmf.trustManagers, SecureRandom())

        sslSocket = sslContext.socketFactory.createSocket(ipAddress, ipPort) as SSLSocket
        sslSocket?.startHandshake()
        inputStream = DataInputStream(sslSocket?.inputStream)
        outputStream = DataOutputStream(sslSocket?.outputStream)

        connected = true

        authenticateConnection(Slidanet.requestId,
                               Slidanet.applicationName,
                               Slidanet.applicationPassword,
                               Slidanet.slidaName)
    }

    @Throws(UnknownHostException::class,
        IOException::class,
        EOFException::class,
        SecurityException::class,
        IllegalArgumentException:: class,
        NullPointerException::class)
    private fun processSession() {

        while (connected) {

            val messageHeader = ByteArray(Constants.networkMessageHeaderLength)

            inputStream?.readFully(messageHeader)
            SlidanetMessage(messageHeader).let {

                val messageLength = requireNotNull(it.getInteger(Constants.integerWidth))
                val messageType = requireNotNull(it.getInteger(Constants.shortWidth))
                val messageBody = ByteArray(messageLength)
                inputStream?.readFully(messageBody)
                mainHandler?.post { Slidanet.processServerMessage(

                    SlidanetMessageType.values()[messageType], messageBody)
                }
            }
        }

        print("Exiting Server Read Process")
    }

    fun send(message: ByteArray) {

        Slidanet.sendMessageHandler.post {

            outputStream?.write(message)
            outputStream?.flush()
        }
    }

    override fun authenticateConnection(requestId: Int,
                                        applicationName: String,
                                        applicationPassword: String,
                                        slidaName: String) {

        SlidanetMessage(SlidanetMessageType.AuthenticateConnectionRequest).apply {

            putInteger(Constants.integerWidth, requestId)
            putInteger(Constants.nameWidth, applicationName.length)
            putString(applicationName)
            putInteger(Constants.nameWidth, applicationPassword.length)
            putString(applicationPassword)
            putInteger(Constants.nameWidth, slidaName.length)
            putString(slidaName)

        }.send()
    }

    override fun disconnectFromNetwork(requestId: Int) {

        SlidanetMessage(SlidanetMessageType.DisconnectRequest).apply {

            putInteger(Constants.integerWidth, requestId)

        }.send()
    }

    override fun connectContent(requestId: Int, contentAddress: String) {

        SlidanetMessage(SlidanetMessageType.ConnectContentRequest).apply {

            putInteger(Constants.integerWidth, requestId)
            putInteger(Constants.nameWidth, contentAddress.length)
            putString(contentAddress)

        }.send()
    }

    override fun disconnectContent(requestId: Int, contentAddress: String) {

        SlidanetMessage(SlidanetMessageType.DisconnectContentRequest).apply {

            putInteger(Constants.integerWidth, requestId)
            putInteger(Constants.nameWidth, contentAddress.length)
            putString(contentAddress)

        }.send()
    }

    override fun disconnectAllContent(requestId: Int) {

        SlidanetMessage(SlidanetMessageType.DisconnectAllContentRequest).apply {

            putInteger(Constants.integerWidth, requestId)

        }.send()
    }

    override fun distributeTranslation(contentAddress: String,
                                       shareMode: ShareModeType,
                                       x: Float,
                                       y: Float,
                                       z: Float) {

        SlidanetMessage(SlidanetMessageType.MoveContentRequest).apply {

            putInteger(Constants.nameWidth, contentAddress.length)
            putString(contentAddress)
            putInteger(Constants.nameWidth, shareMode.ordinal)
            putFloat(x)
            putFloat(y)
            putFloat(z)

        }.send()
    }

    override fun distributeMaskBox(contentAddress: String,
                                   shareMode: ShareModeType,
                                   boxBeginX: Float,
                                   boxBeginY: Float,
                                   boxEndX: Float,
                                   boxEndY: Float) {

        SlidanetMessage(SlidanetMessageType.MoveContentRequest).apply {

            putInteger(Constants.nameWidth, contentAddress.length)
            putString(contentAddress)
            putInteger(Constants.nameWidth, shareMode.ordinal)
            putFloat(boxBeginX)
            putFloat(boxBeginY)
            putFloat(boxEndX)
            putFloat(boxEndY)

        }.send()
    }

    override fun distributePixelWidth(contentAddress: String,
                                      shareMode: ShareModeType,
                                      pixelWidth: Int) {

        SlidanetMessage(SlidanetMessageType.MoveContentRequest).apply {

            putInteger(Constants.nameWidth, contentAddress.length)
            putString(contentAddress)
            putInteger(Constants.nameWidth, shareMode.ordinal)
            putInteger(Constants.nameWidth, pixelWidth)

        }.send()
    }

    override fun logRequest(contentAddress: String, loggingType: SlidanetLoggingRequestType, requestCount: Int) {

    }

    override fun setShareModePix(requestId: Int,
                                 contentAddress: String,
                                 shareMode: ShareModeType,
                                 boxBeginX: Float,
                                 boxBeginY: Float,
                                 boxEndX: Float,
                                 boxEndY: Float,
                                 pixWidth: Int) {

        SlidanetMessage(SlidanetMessageType.SetContentShareModeRequest).apply {

            putInteger(Constants.integerWidth, requestId)
            putInteger(Constants.nameWidth, contentAddress.length)
            putString(contentAddress)
            putInteger(Constants.nameWidth, shareMode.ordinal)
            putFloat(boxBeginX)
            putFloat(boxBeginY)
            putFloat(boxEndX)
            putFloat(boxEndY)
            putInteger(Constants.nameWidth, pixWidth)

        }.send()
    }

    override fun setShareModePeek(requestId: Int,
                                  contentAddress: String,
                                  shareMode: ShareModeType,
                                  boxBeginX: Float,
                                  boxBeginY: Float,
                                  boxEndX: Float,
                                  boxEndY: Float) {

        SlidanetMessage(SlidanetMessageType.SetContentShareModeRequest).apply {

            putInteger(Constants.integerWidth, requestId)
            putInteger(Constants.nameWidth, contentAddress.length)
            putString(contentAddress)
            putInteger(Constants.nameWidth, shareMode.ordinal)
            putFloat(boxBeginX)
            putFloat(boxBeginY)
            putFloat(boxEndX)
            putFloat(boxEndY)

        }.send()
    }

    override fun setShareModeSlide(requestId: Int,
                                   contentAddress: String,
                                   shareMode: ShareModeType,
                                   x: Float,
                                   y: Float,
                                   z: Float) {

        SlidanetMessage(SlidanetMessageType.SetContentShareModeRequest).apply {

            putInteger(Constants.integerWidth, requestId)
            putInteger(Constants.nameWidth, contentAddress.length)
            putString(contentAddress)
            putInteger(Constants.nameWidth, shareMode.ordinal)
            putFloat(x)
            putFloat(y)
            putFloat(z)

        }.send()
    }

    override fun giveContentAddress(requestId: Int, contentAddress: String) {

        SlidanetMessage(SlidanetMessageType.GiveContentRequest).apply {

            putInteger(Constants.integerWidth, requestId)
            putInteger(Constants.nameWidth, contentAddress.length)
            putString(contentAddress)

        }.send()
    }

    override fun takeContentAddress(requestId: Int, contentAddress: String) {

        SlidanetMessage(SlidanetMessageType.TakeContentRequest).apply {

            putInteger(Constants.integerWidth, requestId)
            putInteger(Constants.nameWidth, contentAddress.length)
            putString(contentAddress)

        }.send()
    }

    override fun setVisibilityPreference(requestId: Int, contentAddress: String, preference: Int) {

        SlidanetMessage(SlidanetMessageType.SetContentVisibilityPreferenceRequest).apply {

            putInteger(Constants.integerWidth, requestId)
            putInteger(Constants.nameWidth, contentAddress.length)
            putString(contentAddress)
            putInteger(Constants.nameWidth, preference)

        }.send()
    }

    override fun setContentFilter(requestId: Int,
                                  contentAddress: String,
                                  filter: Int) {

        SlidanetMessage(SlidanetMessageType.SetContentFilterRequest).apply {

            putInteger(Constants.integerWidth, requestId)
            putInteger(Constants.nameWidth, contentAddress.length)
            putString(contentAddress)
            putInteger(Constants.shortWidth, filter)

        }.send()
    }

    override fun setHideState(requestId: Int, contentAddress: String, state: Boolean) {

        SlidanetMessage(SlidanetMessageType.SetHideContentRequest).apply {

            putInteger(Constants.integerWidth, requestId)
            putInteger(Constants.nameWidth, contentAddress.length)
            putString(contentAddress)
            putInteger(Constants.flagWidth, state.toInt())

        }.send()
    }
}