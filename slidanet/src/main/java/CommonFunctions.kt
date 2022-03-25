import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.nio.charset.Charset
import java.util.*

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

fun intToBoolean(value: Int) : Boolean {
    return value > 0
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

internal fun floatTimeToMicroseconds(floatTime: Float) : Long {
    return (floatTime * 1000000L).toLong()
}

val Int.bool:Boolean get() = this == 1

internal fun getVideoFrame(atTime: Float, uri: Uri) : Bitmap? {

    var bitmap: Bitmap? = null

    try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(requireNotNull(uri.path))
        bitmap = retriever.getFrameAtTime(floatTimeToMicroseconds(atTime), MediaMetadataRetriever.OPTION_CLOSEST)
    } catch (e: IllegalArgumentException) {
        return bitmap
    } catch (e: SecurityException) {
        return bitmap
    }

    return bitmap
}

internal fun createFloatBuffer(coords: FloatArray): FloatBuffer {
    val bb = ByteBuffer.allocateDirect(coords.size * Constants.sizeOfFloat)
    bb.order(ByteOrder.nativeOrder())
    val fb = bb.asFloatBuffer()
    fb.put(coords)
    fb.position(0)
    return fb
}

internal fun createShortBuffer(coords: ShortArray): ShortBuffer {
    val bb = ByteBuffer.allocateDirect(coords.size * 4)
    bb.order(ByteOrder.nativeOrder())
    val sb = bb.asShortBuffer()
    sb.put(coords)
    sb.position(0)
    return sb
}

fun isPureAscii(v: String?): Boolean {
    return Charset.forName("US-ASCII").newEncoder().canEncode(v)
    // or "ISO-8859-1" for ISO Latin 1
    // or StandardCharsets.US_ASCII with JDK1.7+
}
