
import android.graphics.*
import android.graphics.drawable.Drawable

internal class SlidanetBorder(color: Int, width: Int) : Drawable() {

    private var paint = Paint()
    private var boundsRect: Rect? = null

    init {

        paint.color = color
        paint.strokeWidth = width.toFloat()
        paint.style = Paint.Style.STROKE
    }

    override fun draw(p0: Canvas) {

        boundsRect?.let {  p0.drawRect(it, paint) }
    }

    override fun setAlpha(p0: Int) {
    }

    override fun setColorFilter(p0: ColorFilter?) {
    }

    override fun getOpacity(): Int {

        return PixelFormat.OPAQUE
    }

    fun setDottedLine() {

        paint.pathEffect = DashPathEffect(floatArrayOf(5f, 10f), 0.0F)
    }
}