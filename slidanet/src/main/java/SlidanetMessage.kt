import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

internal class SlidanetMessage {

    private val TAG = "SlidanetMessage"
    private var writeBody = ByteArrayOutputStream()
    private var readBody: ByteArrayInputStream? = null
    private var messageType = SlidanetMessageType.AuthenticateConnectionRequest
    private var messageSize = 0

    constructor(messageType: SlidanetMessageType) {
        this.messageType = messageType
    }

    constructor(data: ByteArray) {
        readBody = ByteArrayInputStream(data)
    }

    init {
    }

    fun putString(value: String) {

        value.toByteArray(Charset.forName("UTF-8")).apply {
            writeBody.write(this, 0, this.size)
            messageSize += this.size
        }
    }

    fun putInteger(width: Int, value: Int) {

        String.format("%" + width.toString() + "d", value).apply {
            this.toByteArray(Charset.forName("UTF-8")).let {
                writeBody.write(it, 0, it.size)
                messageSize += it.size
            }
        }
    }

    fun putFloat(value: Float) {

        String.format(Slidanet.locale, "-%10.3f", value).apply {
            this.toByteArray(Charset.forName("UTF-8")).let {
                writeBody.write(it, 0, it.size)
                messageSize += it.size
            }
        }
    }

    fun putContent(value: ByteArray) {

        writeBody.write(value, 0, value.size)
        messageSize += value.size
    }

    @Throws(NumberFormatException::class,
        NullPointerException::class,
        IndexOutOfBoundsException::class,
        java.lang.NumberFormatException::class)
    internal fun getInteger(width: Int): Int? {

        var result: Int?

        val outputBuffer = ByteArray(width)
        if (readBody?.read(outputBuffer, 0, width) == width) {
            String(outputBuffer, Charset.forName("UTF-8")).apply {
                result = this.trim().toInt()
                print("Hello")
            }
        } else {
            result = null
        }

        return result
    }

    @Throws(NumberFormatException::class,
        NullPointerException::class,
        IndexOutOfBoundsException::class)
    internal fun getString(length: Int): String? {

        val result: String?

        val outputBuffer = ByteArray(length)
        if (readBody?.read(outputBuffer, 0, length) == length) {
            result = String(outputBuffer, Charset.forName("UTF-8"))
        } else {
            result = null
        }

        return result
    }

    @Throws(NumberFormatException::class)
    internal fun getFloat() : Float? {

        var result: Float?

        val outputBuffer = ByteArray(Constants.floatWidth)
        if (readBody?.read(outputBuffer, 0, Constants.floatWidth) == Constants.floatWidth) {
            String(outputBuffer, Charset.forName("UTF-8")).apply {
                result = this.toFloat()
            }
        } else {
            result = null
        }

        return result
    }

    internal fun getContent(length: Int): ByteArray {

        val outputBuffer = ByteArray(length)

        if (readBody?.read(outputBuffer, 0, length) != length) {
            //throw error
        }
        return outputBuffer
    }

    fun printReadMessage() {

        Log.d(TAG, readBody.toString())
    }

    fun printWriteMessage() {

        Log.d(TAG, writeBody.toString())
    }

    fun printWriteMessage(streamMessage: ByteArrayOutputStream) {

        Log.d(TAG, streamMessage.toByteArray().toString())
    }

    fun send() {

        val message = ByteArrayOutputStream()

        String.format("%" + Constants.networkMessageSizeLength.toString() + "d", messageSize).apply {
            this.toByteArray(Charset.forName("UTF-8")).let {
                message.write(it, 0, it.size)
            }
        }

        String.format("%" + Constants.networkMessageTypeLength.toString() + "d", messageType.ordinal).apply {
            this.toByteArray(Charset.forName("UTF-8")).let {
                message.write(it, 0, it.size)
            }
        }

        writeBody.toByteArray().let {
            message.write(it, 0, it.size)
        }

        Slidanet.server.send(message.toByteArray())
    }
}