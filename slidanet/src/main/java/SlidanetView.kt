import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import androidx.core.view.GestureDetectorCompat
import org.json.JSONObject
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLSurface
import javax.microedition.khronos.opengles.GL10

internal class SlidanetView (private val viewId: String,
                             private val contentType: SlidanetContentType = SlidanetContentType.KImage,
                             private val contentPath: String = "",
                             private val applicationContext: Context) : TextureView(applicationContext),
                                                                        SlidanetObject,
                                                                        TextureView.SurfaceTextureListener,
                                                                        GestureDetector.OnGestureListener,
                                                                        GestureDetector.OnDoubleTapListener,
                                                                        SurfaceTexture.OnFrameAvailableListener {

    private lateinit var bmp: Bitmap
    private lateinit var viewOwnerId: String
    private var updateDuringEdit = false
    private var startTime: Float = 0.0F
    private var takenStatus = false
    private var giveEnabled = false
    private var displayNeedsUpdate = false
    private var textureViewReady = false
    private var moveRequestCount = 0
    private var viewMoved = false
    private var lastMoveTimestamp = Date()
    private var slideReferenceViewWidth = 0
    private var slideReferenceViewHeight = 0
    private var slideEditingScale = Constants.defaultSlideEditingScale
    private var videoPlayStatus: Boolean = false
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private var takers = mutableListOf<JSONObject>()
    private var viewers = mutableListOf<JSONObject>()
    private var lessOutstanding: Boolean = false
    private var moreOutstanding: Boolean = false
    private var gestureDetector = GestureDetectorCompat(context, this)
    private var videoIsRunning = false

    @Volatile private var normalizedTranslationX = 1.0F
    @Volatile private var normalizedTranslationY = 0.0F
    @Volatile private var pixelWidth = Constants.defaultPixelWidth
    @Volatile private var boxBeginX = 0f
    @Volatile private var boxBeginY = 0f
    @Volatile private var boxEndX = 0f
    @Volatile private var boxEndY = 0f
    @Volatile private var rotationAngle = Constants.noRotation
    @Volatile private var peekMode = PeekModeType.PeekNone
    @Volatile private var pixMode = PixModeType.PixNone
    @Volatile private var slideMode = SlideModeType.SlideDefinition
    @Volatile private var redMaskColor = 1f
    @Volatile private var greenMaskColor = 1f
    @Volatile private var blueMaskColor = 1f
    @Volatile private var alphaMaskColor = 1f
    @Volatile private var flipTexture = false
    @Volatile private var textureHandles: IntArray = IntArray(0)
    @Volatile private var videoSurfaceTextureId = 0
    @Volatile private var scale: Float = 1.0F
    @Volatile private var textureId = 0
    @Volatile private var backgroundTextureId = 0
    @Volatile private lateinit var windowSurface: EGLSurface
    @Volatile private var textureAvailable = false
    @Volatile private var shaderType = ShaderType.DefaultShader
    @Volatile private lateinit var textureTransformationBuffer: FloatBuffer
    @Volatile private var textureTransformMatrix: FloatArray = floatArrayOf()
    @Volatile private var textureWidth = 0
    @Volatile private var textureHeight = 0
    @Volatile private lateinit var videoSurfaceTexture: SurfaceTexture
    @Volatile private lateinit var videoSurface: Surface
    @Volatile private lateinit var backgroundVertexBuffer: FloatBuffer
    @Volatile private lateinit var indicesBuffer: ShortBuffer
    @Volatile private lateinit var vertexBuffer: FloatBuffer
    @Volatile private lateinit var sTexture: SurfaceTexture
    //private lateinit var videoPlayer: SlidanetVideoPlayer

    private var initialized: Boolean = false

    init {

        when (contentType) {

            SlidanetContentType.KImage -> initializeImage()

            SlidanetContentType.KVideo -> initializeVideo()

            else -> {}
        }

        initializeTexture()
    }

    private fun updateNormalizedTranslation(x: Float, y: Float) {

    }

    private fun initializeImage() {

    }

    private fun initializeVideo() {

    }

    private fun initializeTexture() {

    }

    override fun onDown(p0: MotionEvent?): Boolean {
        return true
    }

    override fun onShowPress(p0: MotionEvent?) {
    
    }

    override fun onSingleTapUp(p0: MotionEvent?): Boolean {
        return true
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        return true
    }

    override fun onLongPress(p0: MotionEvent?) {
        return
    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        return true
    }

    override fun onSingleTapConfirmed(p0: MotionEvent?): Boolean {
        return true
    }

    override fun onDoubleTap(p0: MotionEvent?): Boolean {
        return true
    }

    override fun onDoubleTapEvent(p0: MotionEvent?): Boolean {
        return true
    }

    override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {

    }

    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
        // should get here after layout params have been assigned and view added to hierarchy

    }

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
        return true
    }

    override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
        TODO("Not yet implemented")
    }

    override fun getDisplayNeedsUpdate(): Boolean {
        return displayNeedsUpdate
    }

    override fun getTextureViewReady(): Boolean {
        return textureViewReady
    }

    override fun render() {

    }

    override fun onFrameAvailable(p0: SurfaceTexture?) {

    }
}