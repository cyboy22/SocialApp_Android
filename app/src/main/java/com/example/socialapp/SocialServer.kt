package com.example.socialapp

import android.os.Handler
import android.os.Looper
import android.util.Log
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.net.UnknownHostException
import javax.net.ssl.*
import java.io.*
import java.lang.Exception
import java.net.InetSocketAddress
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.Executors
import javax.net.ssl.SSLSocket
import kotlin.coroutines.CoroutineContext

class SocialServer {

    private val TAG: String = "SocialServer"
    private var inputStream: DataInputStream? = null
    private var outputStream: DataOutputStream? = null
    private var mainHandler: Handler? = Handler(Looper.getMainLooper())
    private var sslSocket: SSLSocket? = null
    private var connected = false

    init {
    }

    fun connect() {

        try {

            initialize()
            processSession()

        } catch (e: Exception) {

            sslSocket?.close()
            e.message?.let { Log.d(TAG, it) }
        }
    }

    fun disconnect() {

        sslSocket?.close()
        SocialApp.mainHandler?.post {

            SocialApp.connectedToServer = false
        }
    }

    @Throws(UnknownHostException::class,
            IOException::class,
            EOFException::class,
            SecurityException::class,
            IllegalArgumentException:: class,
            IndexOutOfBoundsException::class,
            NullPointerException::class)
    private fun initialize() {

        val keystore = KeyStore.getInstance("BKS")
        val keystoreInputStream = SocialApp.applicationContext.resources?.openRawResource(R.raw.slidanet)
        keystore.load(keystoreInputStream, Constants.keystorePassword.toCharArray())
        keystoreInputStream?.close()

        val algorithm = KeyManagerFactory.getDefaultAlgorithm()
        val keyManagerFactory = KeyManagerFactory.getInstance(algorithm)
        keyManagerFactory.init(keystore, Constants.keystorePassword.toCharArray())

        val tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        tmf.init(keystore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.keyManagers, tmf.trustManagers, SecureRandom())

        sslSocket = sslContext.socketFactory.createSocket(Constants.serverIpAddress,
                                                          Constants.serverIpPort) as SSLSocket
        sslSocket?.startHandshake()
        inputStream = DataInputStream(sslSocket?.inputStream)
        outputStream = DataOutputStream(sslSocket?.outputStream)

        connected = true

        SocialApp.mainHandler?.post {

            SocialApp.connectedToServer = connected
            SocialApp.socialServer.authenticateMemberRequest()
        }
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

            SocialAppMessage(messageHeader).let {

                val messageLength = requireNotNull(it.getInteger(Constants.integerWidth))
                val messageType = requireNotNull(it.getInteger(Constants.shortWidth))
                val messageBody = ByteArray(messageLength)
                inputStream?.readFully(messageBody)
                mainHandler?.post { SocialApp.processServerMessage(
                                    MessageType.values()[messageType], messageBody) }
            }
        }

        print("Exiting Server Read Process")
    }

    fun addMemberRequest(memberName: String) {

        SocialAppMessage(MessageType.AddMemberRequest).apply {

            putInteger(Constants.nameWidth, memberName.length)
            putString(memberName)

        }.send()
    }

    fun followMemberRequest(memberName: String) {

        SocialAppMessage(MessageType.FollowMemberRequest).apply {

            putInteger(Constants.nameWidth, memberName.length)
            putString(memberName)

        }.send()
    }

    fun authenticateMemberRequest() {

        SocialAppMessage(MessageType.AuthenticateMemberRequest).apply {

            putInteger(Constants.nameWidth, SocialApp.memberName.length)
            putString(SocialApp.memberName)
            putString(SocialApp.memberId)
            putInteger(Constants.flagWidth, SocialApp.booleanToInt(SocialApp.slidanetModeActive))

        }.send()
    }

    fun getContentListingRequest() {

        SocialAppMessage(MessageType.GetContentListingRequest).apply {

            putInteger(Constants.nameWidth, SocialApp.memberName.length)
            putString(SocialApp.memberName)

        }.send()
    }

    fun getContentRequest(contentId: String) {

        SocialAppMessage(MessageType.GetContentRequest).apply {

            putString(contentId)

        }.send()
    }

    fun addContentRequest(content: Content) {

        SocialAppMessage(MessageType.AddContentRequest).apply {

            putInteger(Constants.nameWidth, content.contentType.ordinal)
            when (content.contentType) {

                ContentType.Text -> { putInteger(Constants.integerWidth, content.text.length)
                                      putString(content.text) }
                else -> {}
            }

        }.send()
    }

    fun send(message: ByteArray) {

        SocialApp.sendMessageHandler.post{

            outputStream?.write(message)
            outputStream?.flush()
        }
    }
}