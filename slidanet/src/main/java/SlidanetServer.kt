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

        Slidanet.mainHandler?.post { Slidanet.server.authenticateConnection(Slidanet.requestId,
                                                                            Slidanet.platformName,
                                                                            Slidanet.platformPassword,
                                                                            Slidanet.userId) }

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
                    SlidanetMessageType.values()[messageType], messageBody) }
            }
        }

        print("Exiting Server Read Process")
    }

    fun send(message: ByteArray) {

        Slidanet.sendMessageHandler.post{
            outputStream?.write(message)
            outputStream?.flush() }
    }

    override fun authenticateConnection(requestId: Int,
                                        platformName: String,
                                        platformPassword: String,
                                        userId: String
                                        ) {

        SlidanetMessage(SlidanetMessageType.AuthenticateConnectionRequest_).apply {
            putInteger(Constants.integerWidth, requestId)
            putInteger(Constants.nameWidth, platformName.length)
            putString(platformName)
            putString(platformPassword)
            putString(userId)
        }.send()
    }
}