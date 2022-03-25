import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.net.Uri
import android.opengl.*
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import androidx.core.view.GestureDetectorCompat
import org.json.JSONObject
import java.io.File
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.*

internal class SlidanetView (private val contentAddress: String,
                             private val contentType: SlidanetContentType = SlidanetContentType.KImage,
                             private val contentPath: String = "",
                             private var videoStartTime: Float = 0.0F,
                             private val applicationContext: Context) : TextureView(applicationContext),
                                                                        SlidanetObject,
                                                                        SlidanetVideoManager,
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
    private var hideEnabled = false
    private var muteEnabled = false
    private var freezeEnabled = false
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
    private var viewers = mutableListOf<JSONObject>()
    private lateinit var visibilityPreference: VisibilityPreferenceType
    private var moreOutstanding: Boolean = false
    private var gestureDetector = GestureDetectorCompat(context, this)
    private var videoIsRunning = false
    private var editInvokedFromDoubleTap = false
    private lateinit var visibilityPreferences: MutableMap<String, Boolean>
    private lateinit var takers: MutableMap<String, String>

    @Volatile internal var normalizedTranslationX = 1.0F
    @Volatile internal var normalizedTranslationY = 0.0F
    @Volatile internal var normalizedTranslationZ = 1.0F
    @Volatile internal var pixelWidth = Constants.defaultPixelWidth
    @Volatile internal var dynamicPixelWidth = 1
    @Volatile internal var boxBeginX = 0f
    @Volatile internal var boxBeginY = 0f
    @Volatile internal var boxEndX = 0f
    @Volatile internal var boxEndY = 0f
    @Volatile internal var rotationAngle = Constants.noRotation
    @Volatile internal var shareMode = ShareModeType.Slide
    @Volatile internal var peekMode = PeekModeType.PeekNone
    @Volatile internal var pixMode = PixModeType.PixNone
    @Volatile internal var slideMode = SlideModeType.SlideDefinition
    @Volatile internal var pixPercentage = 0.0F
    @Volatile internal var slideEnabled = false
    @Volatile internal var peekEnabled = false
    @Volatile internal var pixEnabled = false
    @Volatile internal var fadeBarrier = 0.1F
    @Volatile internal var snapThreshold = 0.1F
    @Volatile internal var redMaskColor = 1f
    @Volatile internal var greenMaskColor = 1f
    @Volatile internal var blueMaskColor = 1f
    @Volatile internal var alphaMaskColor = 1f
    @Volatile internal var flipTexture = false
    @Volatile internal var textureHandles: IntArray = IntArray(1)
    @Volatile internal var videoSurfaceTextureId = 0
    @Volatile internal var scale: Float = 1.0F
    @Volatile internal var textureId = 0
    @Volatile internal var backgroundTextureId = 0
    @Volatile internal lateinit var windowSurface: EGLSurface
    @Volatile internal var textureAvailable = false
    @Volatile internal var shaderName = "DefaultShader"
    @Volatile internal lateinit var textureTransformationBuffer: FloatBuffer
    @Volatile internal var textureTransformMatrix: FloatArray = floatArrayOf()
    @Volatile internal var textureWidth = 0
    @Volatile internal var textureHeight = 0
    @Volatile internal lateinit var videoSurfaceTexture: SurfaceTexture
    @Volatile internal lateinit var videoSurface: Surface
    @Volatile internal lateinit var backgroundVertexBuffer: FloatBuffer
    @Volatile internal lateinit var indicesBuffer: ShortBuffer
    @Volatile internal lateinit var vertexBuffer: FloatBuffer
    @Volatile internal lateinit var sTexture: SurfaceTexture
    private lateinit var videoPlayer: SlidanetVideoPlayer

    private var initialized: Boolean = false

    init {

        gestureDetector.setOnDoubleTapListener(this)

        when (contentType) {

            SlidanetContentType.KImage -> initializeImage()
            SlidanetContentType.KVideo -> initializeVideo()
        }

        initializeTexture()
    }

    private fun updateNormalizedTranslation(x: Float, y: Float) {

    }


    private fun initializeImage() {

        BitmapFactory.decodeFile(Uri.fromFile(File(contentPath)).toString())?.let {

            bmp = it
            bitmapWidth = it.width
            bitmapHeight = it.height
        }
    }

    private fun initializeVideo() {

        getVideoFrame(videoStartTime, Uri.fromFile(File(contentPath)))?.let {

            bmp = it
            bitmapWidth = it.width
            bitmapHeight = it.height
        }

        videoPlayer = SlidanetVideoPlayer(videoSurface,true, this)
    }

    private fun initializeTexture() {

        textureTransformMatrix = FloatArray(16)
        Matrix.setIdentityM(textureTransformMatrix,0)
        surfaceTextureListener = this
        initializeIndices()
    }

    private fun initializeIndices() {

        indicesBuffer = createShortBuffer(shortArrayOf(0, 1, 2,
                                                       2, 3, 0))
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

        val runnable = Runnable {

            sTexture.detachFromGLContext()
            sTexture.release()
            deleteTextures()

            initializeSurfaceTexture(p0, p1, p2)
        }

        Slidanet.rendererHandler.post(runnable)
    }

    private fun initializeSurfaceTexture(surfaceTexture: SurfaceTexture, w: Int, h: Int) {

        sTexture = surfaceTexture
        textureWidth = w
        textureHeight = h
        this.windowSurface = Slidanet.renderer.createWindowSurface(sTexture)
        Slidanet.renderer.makeCurrent(windowSurface)
        Slidanet.renderer.setViewport(textureWidth, textureHeight)

        if (textureId == 0) {
            createTexture()
        }

        this.initializeVertices(normalizedTranslationX, normalizedTranslationY)

        if (contentType == SlidanetContentType.KVideo) {

            val videoSurfaceTextureHandle = intArrayOf(1)
            GLES20.glGenTextures(videoSurfaceTextureHandle.size, videoSurfaceTextureHandle, 0)
            videoSurfaceTextureId = videoSurfaceTextureHandle[0]
            videoSurfaceTexture = SurfaceTexture(videoSurfaceTextureId)
            videoSurfaceTexture.setDefaultBufferSize(textureWidth, textureHeight)
            videoSurfaceTexture.setOnFrameAvailableListener(this, Slidanet.rendererHandler)
            videoSurfaceTexture.attachToGLContext(videoSurfaceTextureId)
            videoSurface = Surface(videoSurfaceTexture)
        }

        textureAvailable = true
        textureViewReady = true
        displayNeedsUpdate = true
    }

    private fun initializeVertices(translationX: Float, translationY: Float) {

        var vertexCoordinates = floatArrayOf()

        when (rotationAngle) {

            Constants.noRotation -> {

                vertexCoordinates = floatArrayOf((-1f + 2 * translationX)/scale, (1f - 2 * translationY)/scale, 0f, 0f, 1f,// top left
                    (-1f + 2 * translationX)/scale, (-1f - 2 * translationY)/scale, 0f, 0f, 0f,// bottom left
                    (1f + 2 * translationX)/scale, (-1f - 2 * translationY)/scale,  0f, 1f, 0f,//bottom right
                    (1f + 2 * translationX)/scale,  (1f - 2 * translationY)/scale,  0f, 1f, 1f) // top right

            }

            Constants.rotateLeft -> {

                vertexCoordinates = floatArrayOf((-1f + 2 * translationX)/scale, (1f - 2 * translationY)/scale, 0f, 1f, 1f,// top left
                    (-1f + 2 * translationX)/scale, (-1f - 2 * translationY)/scale, 0f, 0f, 1f,// bottom left
                    (1f + 2 * translationX)/scale, (-1f - 2 * translationY)/scale,  0f, 0f, 0f,//bottom right
                    (1f + 2 * translationX)/scale,  (1f - 2 * translationY)/scale,  0f, 1f, 0f) // top right

            }

            Constants.rotateUpsideDown -> {

                vertexCoordinates = floatArrayOf((-1f + 2 * translationX)/scale, (1f - 2 * translationY)/scale, 0f, 1f, 0f,// top left
                    (-1f + 2 * translationX)/scale, (-1f - 2 * translationY)/scale, 0f, 1f, 1f,// bottom left
                    (1f + 2 * translationX)/scale, (-1f - 2 * translationY)/scale,  0f, 0f, 1f,//bottom right
                    (1f + 2 * translationX)/scale,  (1f - 2 * translationY)/scale,  0f, 0f, 0f) // top right

            }

            Constants.rotateRight -> {

                vertexCoordinates = floatArrayOf((-1f + 2 * translationX)/scale, (1f - 2 * translationY)/scale, 0f, 0f, 0f,// top left
                    (-1f + 2 * translationX)/scale, (-1f - 2 * translationY)/scale, 0f, 1f, 0f,// bottom left
                    (1f + 2 * translationX)/scale, (-1f - 2 * translationY)/scale,  0f, 1f, 1f,//bottom right
                    (1f + 2 * translationX)/scale,  (1f - 2 * translationY)/scale,  0f, 0f, 1f) // top right

            }

            else -> {
            }
        }

        vertexBuffer = createFloatBuffer(vertexCoordinates)
    }

    private fun createTexture() {

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)

        GLES20.glGenTextures(textureHandles.size, textureHandles, 0)

        textureId = textureHandles[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR.toFloat())
        Slidanet.renderer.checkGlError("MIN Texture Filter")

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR.toFloat())
        Slidanet.renderer.checkGlError("MAG Texture Filter")

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE.toFloat())
        Slidanet.renderer.checkGlError("Texture Clamp To Edge S")

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE.toFloat())
        Slidanet.renderer.checkGlError("Texture Clamp To Edge T")

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        Slidanet.renderer.checkGlError("Texture Creation")

        bmp.recycle()

    }

    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

        textureAvailable = true
        sTexture = p0
        textureWidth = p1
        textureHeight = p2

        val runnable = Runnable {

                Slidanet.renderer.destroyWindowSurface(windowSurface)
                deleteTextures()
                Slidanet.renderer.destroyWindowSurface(windowSurface)

                initializeSurfaceTexture(p0, p1, p2)
        }

        Slidanet.rendererHandler.post(runnable)


    }

    private fun deleteTextures() {

        GLES20.glDeleteTextures(1, textureHandles,0)

        if (contentType  == SlidanetContentType.KVideo) {

            val videoTextureHandles = intArrayOf(1)
            videoTextureHandles[0] = videoSurfaceTextureId
            GLES20.glDeleteTextures(1, videoTextureHandles,0)
        }
    }

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {

        sTexture.release()
        return true
    }

    override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
    }

    override fun getDisplayNeedsUpdate(): Boolean {
        return displayNeedsUpdate
    }

    override fun getTextureViewReady(): Boolean {
        return textureViewReady
    }

    private fun initializePixelWidth() : Int {

        var px : Int = pixelWidth

        if (pixMode == PixModeType.PixDynamic) {

            px = dynamicPixelWidth
        }

        return px
    }
    override fun render() {

        Slidanet.renderer.apply {

            makeCurrent(windowSurface)
            this.clearSurface()
            this.setViewport(textureWidth, textureHeight)
            /*
            if (viewType == SlidaViewType.Text) {
                    activateTexture(GLES20.GL_TEXTURE_2D, textureView.getBackgroundTextureId())

                    loadShader(ShaderContext(_shader = textureView.getShaderType(),
                        _verticesBuffer = textureView.getBackgroundVertexBuffer(),
                        _textureTransformMatrix = textureView.GetTextureTransformMatrix(),
                        _textureWidth = textureView.getTextureWidth(),
                        _textureHeight = textureView.getTextureHeight(),
                        _boxBeginX = textureView.getBoxBeginX(),
                        _boxBeginY = textureView.getBoxBeginY(),
                        _boxEndX = textureView.getBoxEndX(),
                        _boxEndY = textureView.getBoxEndY(),
                        _viewType = viewType))

                    drawElements(textureView.getIndicesBuffer(), 6)
                }
            */

            if (contentType == SlidanetContentType.KVideo) {
                /*
                if (!textureView.getVideoIsRunning()) {
                        activateTexture(GLES20.GL_TEXTURE_2D, textureView.getTextureId())
                    } else {
                        activateTexture(
                            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                            textureView.videoSurfaceTextureId
                        )
                    }
                 */
            } else {

                activateTexture(GLES20.GL_TEXTURE_2D, textureId)
            }

            loadShader(SlidanetShaderContext(shaderName = shaderName,
                                             verticesBuffer = vertexBuffer,
                                             boxBeginX = boxBeginX,
                                             boxBeginY = boxBeginY,
                                             boxEndX = boxEndX,
                                             boxEndY = boxEndY,
                                             viewType = contentType,
                                             flipTexture = flipTexture,
                                             peekItEnabled = peekEnabled,
                                             pixItEnabled = pixEnabled,
                                             pixelWidth = initializePixelWidth(),
                                             pixelHeight = initializePixelWidth(),
                                             maskRedValue = redMaskColor,
                                             maskGreenValue = greenMaskColor,
                                             maskBlueValue = blueMaskColor,
                                             maskAlphaValue = alphaMaskColor,
                                             textureWidth = textureWidth,
                                             textureHeight = textureHeight,
                                             alpha = 1f))

                drawElements( indicesBuffer, 6)

                swapBuffers(windowSurface)

                displayNeedsUpdate = false
            }
    }

    override fun setVisibilityPreferences(preferences: MutableMap<String, Boolean>) {

        visibilityPreferences = preferences
    }

    override fun setTakers(takerList: MutableMap<String, String>) {

        takers = takerList
    }

    override fun setShareTranslationParameters(x: Float, y: Float, z: Float) {

        normalizedTranslationX = x
        normalizedTranslationY = y
        normalizedTranslationZ = z
    }

    override fun setShareBoxParameters(beginX: Float, beginY: Float, endX: Float, endY: Float) {

        boxBeginX = beginX
        boxBeginY = beginY
        boxEndX = endX
        boxEndY = endY
    }

    override fun setVisibilityPreference(preference: VisibilityPreferenceType) {

        visibilityPreference = preference
    }

    override fun setGiveEnabled(give: Boolean) {

        giveEnabled = give
    }

    override fun setHideEnabled(hide: Boolean) {

        hideEnabled = hide
    }

    override fun setMuteEnabled(mute: Boolean) {

        muteEnabled = mute
    }

    override fun setFreezeEnabled(give: Boolean) {

        freezeEnabled = give
    }

    override fun getGiveEnabled(): Boolean {

        return giveEnabled
    }

    override fun getHideEnabled(): Boolean {

        return hideEnabled
    }

    override fun getMuteEnabled(): Boolean {

        return muteEnabled
    }

    override fun getFreezeEnabled(): Boolean {

        return freezeEnabled
    }

    override fun setShareMode(mode: ShareModeType) {

        shareMode = mode
    }

    override fun setSlideMode(mode: SlideModeType) {

        slideMode = mode
    }

    override fun setPeekMode(mode: PeekModeType) {

        peekMode = mode
    }

    override fun setPixMode(mode: PixModeType) {

        pixMode = mode
    }

    override fun getShareMode(): ShareModeType {

        return shareMode
    }

    override fun getSlideMode(): SlideModeType {

        return slideMode
    }

    override fun getPeekMode(): PeekModeType {

        return peekMode
    }

    override fun getPixMode(): PixModeType {

        return pixMode
    }

    override fun setPixPercentage(value: Float) {

        pixPercentage = value
    }

    override fun getPixPercentage(): Float {

        return pixPercentage
    }

    override fun setShaderName(name: String) {

        shaderName = name
    }

    override fun getShaderName(): String {

        return shaderName
    }

    override fun setSlideEnabled(value: Boolean) {

        slideEnabled = value
    }

    override fun getSlideEnabled(): Boolean {

        return slideEnabled
    }

    override fun setPixEnabled(value: Boolean) {

        pixEnabled = value
    }

    override fun getPixEnabled(): Boolean {

        return pixEnabled
    }

    override fun setPeekEnabled(value: Boolean) {

        peekEnabled = value
    }

    override fun getPeekEnabled(): Boolean {

        return peekEnabled
    }

    override fun setFadeBarrier(barrier: Float) {

        fadeBarrier = barrier
    }

    override fun getFadeBarrier(): Float {

        return fadeBarrier
    }

    override fun setSnapThreshold(threshold: Float) {

        snapThreshold = threshold
    }

    override fun getSnapThreshold(): Float {

        return snapThreshold
    }

    override fun setDisplayNeedsUpdate(state: Boolean) {

        displayNeedsUpdate = state
    }

    override fun onFrameAvailable(p0: SurfaceTexture?) {

    }

    override fun onVideoPrepared() {
    }

    override fun onVideoStarted() {
    }

    override fun onVideoFinished() {
    }

    override fun updateTexture() {
    }

    override fun onFrameAvailable(presentationTime: Long): Boolean {
        return true
    }
}