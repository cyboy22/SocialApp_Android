import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.net.Uri
import android.opengl.*
import android.view.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.GestureDetectorCompat
import org.json.JSONObject
import java.io.File
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.*

internal class SlidanetContentAddress (private val contentAddress: String,
                                       private var contentType: SlidanetContentType = SlidanetContentType.Image,
                                       private var contentPath: String = "",
                                       private var videoStartTime: Float = 0.0F,
                                       private var editorEnabled: Boolean = false,
                                       private var doubleTapEditingEnabled: Boolean = true) : TextureView(Slidanet.applicationContext),
                                                                                              SlidanetObject,
                                                                                              SlidanetVideoManager,
                                                                                              TextureView.SurfaceTextureListener,
                                                                                              GestureDetector.OnGestureListener,
                                                                                              GestureDetector.OnDoubleTapListener,
                                                                                              SurfaceTexture.OnFrameAvailableListener {

    private lateinit var bmp: Bitmap
    private var updateDuringEdit = false
    private var startTime: Float = 0.0F
    private var takenStatus = false
    private var displayNeedsUpdate = false
    private var textureViewReady = false
    private var moveRequestCount = 0
    internal var lastMoveTimestamp = Date()
    private var editingScale = Constants.defaultSlideEditingScale
    private var contentFilter: SlidanetContentFilterType = SlidanetContentFilterType.Default
    internal var videoPlayStatus: Boolean = false
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private var viewers = mutableListOf<JSONObject>()
    private var visibilityPreference: SlidanetVisibilityPreferenceType = SlidanetVisibilityPreferenceType.Neutral
    private var gestureDetector = GestureDetectorCompat(context, this)
    private var videoIsRunning = false
    private var editorContentAddress: String = ""
    private lateinit var contentAddressOwner: String
    private lateinit var visibilityPreferences: MutableMap<String, Boolean>
    private lateinit var takers: MutableMap<String, String>
    private var savedShareMode = ShareModeType.SlideXYZ
    private var savedNormalizedTranslationX = 1f
    private var savedNormalizedTranslationY = 1f
    private var savedNormalizedTranslationZ = 1f
    private var savedBoxBeginX = 0f
    private var savedBoxBeginY = 0f
    private var savedBoxEndX = 0f
    private var savedBoxEndY = 0f
    private var savedPixPercentage = 0f
    private var moveCount = 0
    private var contentWasTaken = false

    @Volatile private var giveEnabled = false
    @Volatile private var hideEnabled = false
    @Volatile internal var giveWasTaken = false
    @Volatile internal var normalizedTranslationX = .5f
    @Volatile internal var normalizedTranslationY = .5f
    @Volatile internal var normalizedTranslationZ = 1f
    @Volatile internal var pixelWidth = Constants.defaultPixelWidth
    @Volatile internal var dynamicPixelWidth = 1
    @Volatile internal var boxBeginX = 0f
    @Volatile internal var boxBeginY = 0f
    @Volatile internal var boxEndX = 0f
    @Volatile internal var boxEndY = 0f
    @Volatile internal var rotationAngle = Constants.noRotation
    @Volatile internal var shareMode = ShareModeType.SlideXYZ
    @Volatile internal var pixPercentage = 0f
    @Volatile internal var slideEnabled = false
    @Volatile internal var peekEnabled = false
    @Volatile internal var pixEnabled = false
    @Volatile internal var fadeBarrier = 0.1F
    @Volatile internal var snapThreshold = 0.1F
    @Volatile internal var redMaskColor = 1f
    @Volatile internal var contentAlpha = 1f
    @Volatile internal var greenMaskColor = 1f
    @Volatile internal var blueMaskColor = 1f
    @Volatile internal var alphaMaskColor = 1f
    @Volatile internal var flipTexture = false
    @Volatile internal var textureHandles: IntArray = IntArray(1)
    @Volatile internal var videoSurfaceTextureId = 0
    @Volatile internal var scale: Float = 1.0F
    @Volatile internal var textureId = 0
    @Volatile internal lateinit var windowSurface: EGLSurface
    @Volatile internal var shaderName = "DefaultShader"
    @Volatile internal lateinit var textureTransformationBuffer: FloatBuffer
    @Volatile internal var textureTransformMatrix: FloatArray = floatArrayOf()
    @Volatile internal var textureWidth = 0
    @Volatile internal var textureHeight = 0
    @Volatile internal lateinit var videoSurfaceTexture: SurfaceTexture
    @Volatile internal lateinit var videoSurface: Surface
    @Volatile internal lateinit var indicesBuffer: ShortBuffer
    @Volatile internal lateinit var vertexBuffer: FloatBuffer
    @Volatile internal lateinit var sTexture: SurfaceTexture
    private lateinit var videoPlayer: SlidanetVideoPlayer

    private var initialized: Boolean = false

    init {

        if (!editorEnabled) {

            gestureDetector.setOnDoubleTapListener(this)

            when (contentType) {

                SlidanetContentType.Image -> initializeImage()
                SlidanetContentType.StaticVideo -> initializeVideo()
                else -> {}
            }

            initializeTexture()
        }
    }

    override fun initializeImage() {

        BitmapFactory.decodeFile(Uri.fromFile(File(contentPath)).toString())?.let {

            bmp = it
            bitmapWidth = it.width
            bitmapHeight = it.height
        }
    }

    override fun initializeVideo() {

        getVideoFrame(videoStartTime, Uri.fromFile(File(contentPath)))?.let {

            bmp = it
            bitmapWidth = it.width
            bitmapHeight = it.height
        }

        videoPlayer = SlidanetVideoPlayer(videoSurface,true, this)
    }

    override fun initializeTexture() {

        textureTransformMatrix = FloatArray(16)
        Matrix.setIdentityM(textureTransformMatrix,0)
        surfaceTextureListener = this
        initializeIndices()
    }

    override fun getEditingScale(): Float {
        return editingScale
    }

    override fun getEditorContentAddress(): String {
        return editorContentAddress
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

    override fun setMoveCount(count: Int) {
        moveCount = count
    }

    override fun getMoveCount() : Int {
        return moveCount
    }

    override fun incrementMoveCount() {
        moveCount++
    }

    override fun getContentFilter() : SlidanetContentFilterType {
        return contentFilter
    }

    override fun setContentFilter(contentFilter: SlidanetContentFilterType) {
        this.contentFilter = contentFilter
    }

    override fun getVideoPlayer() : SlidanetVideoPlayer {
        return videoPlayer
    }

    override fun distributeMove() {

        when (shareMode) {

            ShareModeType.SlideXYZ -> distributeTranslation()

            ShareModeType.SlidePeekDefine,
            ShareModeType.SlidePeekSlide,
            ShareModeType.SlidePixDefine -> distributeMaskBox()

            ShareModeType.SlidePixSlide -> distributePixelWidth()

            else -> {}
        }
    }

    override fun logEditingRequest() {




        moveCount = 0
    }


    override fun getDoubleTapEditingEnabled() : Boolean {
        return doubleTapEditingEnabled
    }

    override fun setDoubleTapEditingEnabled(editingState: Boolean) {
        doubleTapEditingEnabled = editingState
    }

    override fun commitEditing() {

        (Slidanet.editorContent?.parent as ViewManager).removeView(Slidanet.editorContent)

        Slidanet.ownerEditingInProgress = false

        moveCount = Slidanet.contentInEditor?.getMoveCount()!!

        Slidanet.server.logRequest(contentAddress,SlidanetLoggingRequestType.move, moveCount)

        moveCount = 1

        Slidanet.editingState = SlidanetEditingStateType.InActive

        Slidanet.editorContentAddress = ""


        distributeTranslation()
    }

    override fun cancelEditing() {

        (Slidanet.editorContent?.parent as ViewManager).removeView(Slidanet.editorContent)

        Slidanet.ownerEditingInProgress = false

        Slidanet.editorContentAddress = ""

        Slidanet.editingState = SlidanetEditingStateType.InActive

        restore()
    }

    override fun setupEditor(initiator: SlidanetEditingInitiatorType) {

        Slidanet.editorContentAddress = contentAddress

        if (contentAddressOwner != Slidanet.slidaName) {

            if (giveEnabled) {

                Slidanet.followerTakeInProgress = true
                initializeEditor(contentAddress, initiator)

            }

        } else {

            if (updateDuringEdit) {
                saveParameters()
            }
            Slidanet.ownerEditingInProgress = true
            Slidanet.contentInEditor?.let {

                it.setContentAddressOwner(contentAddressOwner)
                it.initializeEditor(contentAddress, initiator)
                it.setMoveCount(0)
            }
        }
    }

    override fun onDoubleTap(p0: MotionEvent?): Boolean {

        if (doubleTapEditingEnabled) {

            if (Slidanet.editingState == SlidanetEditingStateType.InActive) {

                setupEditor(SlidanetEditingInitiatorType.DoubleTap)
            }
        }

        return true
    }

    private fun saveParameters() {

        savedShareMode = shareMode
        savedNormalizedTranslationX = normalizedTranslationX
        savedNormalizedTranslationY = normalizedTranslationY
        savedNormalizedTranslationZ = normalizedTranslationZ
        savedPixPercentage = pixPercentage
        savedBoxBeginX = boxBeginX
        savedBoxBeginY = boxBeginY
        savedBoxEndX = boxEndX
        savedBoxEndY = boxEndY
    }

    override fun onDoubleTapEvent(p0: MotionEvent?): Boolean {

        return true
    }

    override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {

        val runnable = Runnable {
            /*
            sTexture.detachFromGLContext()
            sTexture.release()
            deleteTextures()
            */

            initializeSurfaceTexture(p0, p1, p2)
        }

        Slidanet.rendererHandler.post(runnable)
    }

    private fun initializeSurfaceTexture(surfaceTexture: SurfaceTexture, w: Int, h: Int) {

        sTexture = surfaceTexture
        textureWidth = w
        textureHeight = h
        windowSurface = Slidanet.renderer.createWindowSurface(sTexture)

        if (textureId == 0) {

            createTexture()
        }

        this.initializeVertices(normalizedTranslationX, normalizedTranslationY)

        if (contentType == SlidanetContentType.StaticVideo) {

            val videoSurfaceTextureHandle = intArrayOf(1)
            GLES20.glGenTextures(videoSurfaceTextureHandle.size, videoSurfaceTextureHandle, 0)
            videoSurfaceTextureId = videoSurfaceTextureHandle[0]
            videoSurfaceTexture = SurfaceTexture(videoSurfaceTextureId)
            videoSurfaceTexture.setDefaultBufferSize(textureWidth, textureHeight)
            videoSurfaceTexture.setOnFrameAvailableListener(this, Slidanet.rendererHandler)
            videoSurfaceTexture.attachToGLContext(videoSurfaceTextureId)
            videoSurface = Surface(videoSurfaceTexture)
        }

        textureViewReady = true
        displayNeedsUpdate = true
    }

    override fun initializeVertices(translationX: Float, translationY: Float) {

        var vertexCoordinates = floatArrayOf()

        when (rotationAngle) {

            Constants.noRotation -> {
                when (shareMode) {

                    /*
                    ShareModeType.SlideRightToLeft -> {

                        vertexCoordinates = floatArrayOf((2 * translationX)/editingScale,        1f, 0f, 0f, 1f,// top left
                                                         (2 * translationX)/editingScale,       -1f, 0f, 0f, 0f,// bottom left
                                                         (-2 * translationX)/editingScale + 1f, -1f, 0f, 1f, 0f,//bottom right
                                                         (-2 * translationX)/editingScale + 1f,  1f, 0f, 1f, 1f) // top right
                    }

                    ShareModeType.SlideLeftToRight -> {

                        vertexCoordinates = floatArrayOf((2 * translationX)/editingScale - 1f,  1f, 0f, 0f, 1f,// top left
                                                         (2 * translationX)/editingScale - 1f, -1f, 0f, 0f, 0f,// bottom left
                                                         (-2 * translationX)/editingScale,     -1f, 0f, 1f, 0f,//bottom right
                                                         (-2 * translationX)/editingScale,      1f, 0f, 1f, 1f) // top right
                    }

                    ShareModeType.SlideTopToBottom -> {

                        vertexCoordinates = floatArrayOf( -1f, (2 * translationY)/editingScale + 1f,  0f, 0f, 1f,// top left
                                                          -1f, (2 * translationY)/editingScale,       0f, 0f, 0f,// bottom left
                                                           1f,  (2 * translationY)/editingScale,      0f, 1f, 0f,//bottom right
                                                           1f,  (2 * translationY)/editingScale + 1f, 0f, 1f, 1f) // top right


                    }

                    ShareModeType.SlideBottomToTop -> {
                        vertexCoordinates = floatArrayOf( -1f, (2 * translationY)/editingScale,      0f, 0f, 1f,// top left
                                                          -1f, (2 * translationY)/editingScale - 1f, 0f, 0f, 0f,// bottom left
                                                           1f, (2 * translationY)/editingScale - 1f, 0f, 1f, 0f,//bottom right
                                                           1f, (2 * translationY)/editingScale,      0f, 1f, 1f) // top right
                    }
                    */
                    ShareModeType.SlideXYZ -> {

                        vertexCoordinates = floatArrayOf((-1f + 2 * translationX)/editingScale, (1f - 2 * translationY)/editingScale,  0f, 0f, 1f,// top left
                                                         (-1f + 2 * translationX)/editingScale, (-1f - 2 * translationY)/editingScale, 0f, 0f, 0f,// bottom left
                                                         (1f + 2 * translationX)/editingScale,  (-1f - 2 * translationY)/editingScale, 0f, 1f, 0f,//bottom right
                                                         (1f + 2 * translationX)/editingScale,  (1f - 2 * translationY)/editingScale,  0f, 1f, 1f) // top right
                    }

                    else -> {}
                }
            }

            Constants.rotateLeft -> {

                vertexCoordinates = floatArrayOf((-1f + 2 * translationX)/editingScale, (1f - 2 * translationY)/editingScale, 0f, 1f, 1f,// top left
                                                 (-1f + 2 * translationX)/editingScale, (-1f - 2 * translationY)/editingScale, 0f, 0f, 1f,// bottom left
                                                 (1f + 2 * translationX)/editingScale, (-1f - 2 * translationY)/editingScale,  0f, 0f, 0f,//bottom right
                                                 (1f + 2 * translationX)/editingScale,  (1f - 2 * translationY)/editingScale,  0f, 1f, 0f) // top right
            }

            Constants.rotateUpsideDown -> {

                vertexCoordinates = floatArrayOf((-1f + 2 * translationX)/editingScale, (1f - 2 * translationY)/editingScale, 0f, 1f, 0f,// top left
                                                 (-1f + 2 * translationX)/editingScale, (-1f - 2 * translationY)/editingScale, 0f, 1f, 1f,// bottom left
                                                 (1f + 2 * translationX)/editingScale, (-1f - 2 * translationY)/editingScale,  0f, 0f, 1f,//bottom right
                                                 (1f + 2 * translationX)/editingScale,  (1f - 2 * translationY)/editingScale,  0f, 0f, 0f) // top right
            }

            Constants.rotateRight -> {

                vertexCoordinates = floatArrayOf((-1f + 2 * translationX)/editingScale, (1f - 2 * translationY)/editingScale, 0f, 0f, 0f,// top left
                                                 (-1f + 2 * translationX)/editingScale, (-1f - 2 * translationY)/editingScale, 0f, 1f, 0f,// bottom left
                                                 (1f + 2 * translationX)/editingScale, (-1f - 2 * translationY)/editingScale,  0f, 1f, 1f,//bottom right
                                                 (1f + 2 * translationX)/editingScale,  (1f - 2 * translationY)/editingScale,  0f, 0f, 1f) // top right
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

        val runnable = Runnable {

            sTexture = p0
            textureWidth = p1
            textureHeight = p2

            Slidanet.renderer.destroyWindowSurface(windowSurface)
            deleteTextures()
            Slidanet.renderer.destroyWindowSurface(windowSurface)

            initializeSurfaceTexture(p0, p1, p2)
        }

        Slidanet.rendererHandler.post(runnable)
    }

    override fun deleteTextures() {

        GLES20.glDeleteTextures(1, textureHandles, 0)

        if (contentType  == SlidanetContentType.StaticVideo) {

            val videoTextureHandles = intArrayOf(1)
            videoTextureHandles[0] = videoSurfaceTextureId
            GLES20.glDeleteTextures(1, videoTextureHandles, 0)
        }
    }

    override fun detachSurfaceTexture() {

        sTexture.detachFromGLContext()

    }

    override fun releaseSurfaceTexture() {

        sTexture.release()

    }

    override fun copyOwnerParametersToEditor(contentAddress: String) {

        Slidanet.rendererHandler.post {

            Slidanet.slidanetContentAddresses[contentAddress]?.let {

                shareMode = it.getShareMode()
                contentAddressOwner = it.getContentAddressOwner()
                updateDuringEdit = it.getUpdateDuringEdit()
                rotationAngle = it.getRotationAngle()
                moveRequestCount = 0
                contentPath = it.getContentPath()
                contentType = it.getContentType()
                shaderName = it.getShaderName()
                editingScale = it.getEditingScale()
                editorContentAddress = it.getEditorContentAddress()
                fadeBarrier = it.getFadeBarrier()
                snapThreshold = it.getSnapThreshold()
                flipTexture = it.getFlipTexture()
                redMaskColor = it.getRedMaskColor()
                greenMaskColor = it.getGreenMaskColor()
                blueMaskColor = it.getBlueMaskColor()
                alphaMaskColor = it.getAlphaMaskColor()
                startTime = it.getStartTime()
                hideEnabled = it.getHideEnabled()
                doubleTapEditingEnabled = it.getDoubleTapEditingEnabled()
                redMaskColor = it.getRedMaskColor()
                greenMaskColor = it.getGreenMaskColor()
                blueMaskColor = it.getBlueMaskColor()
                alphaMaskColor = it.getAlphaMaskColor()

                when (shareMode) {

                    ShareModeType.SlideXYZ -> {

                        normalizedTranslationX = it.getNormalizedTranslationX()
                        normalizedTranslationY = it.getNormalizedTranslationY()
                        normalizedTranslationZ = it.getNormalizedTranslationZ()
                    }

                    ShareModeType.SlidePeekDefine,
                    ShareModeType.SlidePeekSlide-> {

                        boxBeginX = it.getBoxBeginX()
                        boxBeginY = it.getBoxBeginY()
                        boxEndX = it.getBoxEndX()
                        boxEndY = it.getBoxEndY()
                    }

                    ShareModeType.SlidePixDefine,
                    ShareModeType.SlidePixSlide-> {

                        pixPercentage = it.getPixPercentage()
                    }

                    else -> {}
                }
            }

            Slidanet.mainHandler?.post {

                when (contentType) {

                    SlidanetContentType.Image -> {

                        initializeImage()
                    }

                    SlidanetContentType.StaticVideo -> {

                        initializeVideo()
                    }

                    else -> { }
                }

                initializeTexture()
            }
        }
    }

    override fun setEditorContentAddress(address: String) {

        editorContentAddress = address
    }

    override fun setPixelWidth(pixelWidth: Int) {

        this.pixelWidth = pixelWidth
    }

    override fun distributePixelWidth() {

        Slidanet.rendererHandler.post {

            Slidanet.server.distributePixelWidth(contentAddress,
                shareMode,
                pixelWidth)
        }
    }

    override fun setFlipTexture(state: Boolean) {

        flipTexture = state
    }

    override fun setRedMaskColor(color: Float) {

        redMaskColor = color
    }

    override fun setGreenMaskColor(color: Float) {

        greenMaskColor = color
    }

    override fun setBlueMaskColor(color: Float) {

        blueMaskColor = color
    }

    override fun setAlphaMaskColor(color: Float) {

        alphaMaskColor = color
    }

    override fun setStartTime(startTime: Float) {

        this.startTime = startTime
    }

    override fun setEditingScale(scale: Float) {

        editingScale = scale
    }

    override fun setContentType(cType: SlidanetContentType) {

        contentType = cType
    }

    override fun setContentPath(path: String) {

        contentPath = path
    }


    override fun setMoveRequestCount(count: Int) {

        setMoveRequestCount(moveRequestCount)
    }


    override fun setUpdateDuringEdit(state: Boolean) {

        updateDuringEdit = state
    }

    override fun setRotationAngle(angle: Int) {

        rotationAngle = angle
    }

    override fun restore() {

        Slidanet.rendererHandler.post {

            shareMode = savedShareMode
            normalizedTranslationX = savedNormalizedTranslationX
            normalizedTranslationY = savedNormalizedTranslationY
            normalizedTranslationZ = savedNormalizedTranslationZ
            boxBeginX = savedBoxBeginX
            boxBeginY = savedBoxBeginY
            boxEndX = savedBoxEndX
            pixPercentage = savedPixPercentage
            displayNeedsUpdate = true

        }
    }

    override fun initializeEditor(contentAddress: String, initiator: SlidanetEditingInitiatorType) {

        var editorView: ConstraintLayout = Slidanet.editorContent!!
        Slidanet.editorControl?.initialize()

        if (Slidanet.slidaName == contentAddressOwner) {

            copyOwnerParametersToEditor(contentAddress)
            initializeEditorLayoutParams()

        } else if (giveEnabled) {

            editorView = Slidanet.editorControl!!

        } else {

            return
        }

        Slidanet.rendererHandler.post { Slidanet.editorControl?.initializeAvailableMovement(contentAddress, contentAddressOwner) }

        if (initiator == SlidanetEditingInitiatorType.DoubleTap || initiator == SlidanetEditingInitiatorType.LocalEditing) {

            val request = JSONObject()
            request.put(SlidanetConstants.content_address, editorContentAddress)
            val response = SlidanetResponseData(requestCode = SlidanetRequestType.EditContent,
                                                requestInfo = request,
                                                responseCode = SlidanetResponseType.EditingContent,
                                                editorView = editorView)

            Slidanet.mainHandler?.post { Slidanet.slidanetResponseHandler.slidanetResponse(response) }
        }

        if (Slidanet.editingState == SlidanetEditingStateType.InActive) {

            Slidanet.editingState = SlidanetEditingStateType.Active
        }
    }

    override fun setNormalizedTranslationX(x: Float) {

        normalizedTranslationX = x
    }

    override fun setNormalizedTranslationY(y: Float) {

        normalizedTranslationY = y
    }

    override fun setNormalizedTranslationZ(z: Float) {

        normalizedTranslationZ = z
    }

    private fun initializeEditorLayoutParams() {

        val aspectRatio = textureWidth.toFloat() / textureHeight.toFloat()

        var nw = 0
        var nh = 0
        var i = 1

        while (nh < Slidanet.screenHeightInPixels && nw < Slidanet.screenWidthInPixels) {

            nw = i
            nh = (nw.toFloat() / aspectRatio).toInt()
            i++
        }

        var editingScale = 1f

        when (shareMode) {

            ShareModeType.SlideXYZ -> {

                editingScale = 3f
            }

            ShareModeType.SlidePeekDefine,
            ShareModeType.SlidePeekSlide,
            ShareModeType.SlidePixDefine,
            ShareModeType.SlidePixSlide -> {

                editingScale = 1f
            }

            else -> {}
        }

        Slidanet.rendererHandler.post { this.editingScale = editingScale }

        this.layoutParams = ConstraintLayout.LayoutParams(

            (nw.toFloat() / editingScale).toInt(),
            (nh.toFloat() / editingScale).toInt()
        )

        if (Slidanet.editingState == SlidanetEditingStateType.InActive) {

            Slidanet.editorContent?.removeAllViews()
        }

        val constraintSet = ConstraintSet()
        constraintSet.clone(Slidanet.editorContent)

        Slidanet.editorContent?.let {

            constraintSet.connect(this.id, ConstraintSet.TOP, it.id, ConstraintSet.TOP, 0)
            constraintSet.connect(this.id, ConstraintSet.LEFT, it.id, ConstraintSet.LEFT, 0)
            constraintSet.connect(this.id, ConstraintSet.BOTTOM, it.id, ConstraintSet.BOTTOM, 0)
            constraintSet.connect(this.id, ConstraintSet.RIGHT, it.id, ConstraintSet.RIGHT, 0)
        }

        /*
        if (shareMode == ShareModeType.SlideRightToLeft ||
            shareMode == ShareModeType.SlideLeftToRight ||
            shareMode == ShareModeType.SlideTopToBottom ||
            shareMode == ShareModeType.SlideBottomToTop ||
            shareMode == ShareModeType.SlideUnrestricted) {

            Slidanet.referenceView.layoutParams = ConstraintLayout.LayoutParams( (nw.toFloat()/editingScale).toInt(),
                                                                                  (nh.toFloat()/editingScale).toInt())
        }
        */

        when (shareMode) {

            ShareModeType.SlideXYZ -> {

                Slidanet.editorContent?.let {

                    constraintSet.connect(Slidanet.referenceView.id, ConstraintSet.TOP, it.id, ConstraintSet.TOP, 0)
                    constraintSet.connect(Slidanet.referenceView.id, ConstraintSet.LEFT, it.id, ConstraintSet.LEFT, 0)
                    constraintSet.connect(Slidanet.referenceView.id, ConstraintSet.BOTTOM, it.id, ConstraintSet.BOTTOM, 0)
                    constraintSet.connect(Slidanet.referenceView.id, ConstraintSet.RIGHT, it.id, ConstraintSet.RIGHT, 0)

                }
            }

            /*
            ShareModeType.SlideRightToLeft -> {
                constraintSet.connect(Slidanet.referenceView.id, ConstraintSet.TOP, this.id, ConstraintSet.TOP, 0)
                constraintSet.connect(Slidanet.referenceView.id, ConstraintSet.LEFT, this.id, ConstraintSet.LEFT, 0)
            }

            ShareModeType.SlideLeftToRight -> {
                constraintSet.connect(Slidanet.referenceView.id, ConstraintSet.TOP, this.id, ConstraintSet.TOP, 0)
                constraintSet.connect(Slidanet.referenceView.id, ConstraintSet.RIGHT, this.id, ConstraintSet.RIGHT, 0)

            }
            ShareModeType.SlideTopToBottom -> {
                constraintSet.connect(Slidanet.referenceView.id, ConstraintSet.LEFT, this.id, ConstraintSet.LEFT, 0)
                constraintSet.connect(Slidanet.referenceView.id, ConstraintSet.BOTTOM, this.id, ConstraintSet.BOTTOM, 0)
            }

            ShareModeType.SlideBottomToTop-> {
                constraintSet.connect(Slidanet.referenceView.id, ConstraintSet.TOP, this.id, ConstraintSet.TOP, 0)
                constraintSet.connect(Slidanet.referenceView.id, ConstraintSet.LEFT, this.id, ConstraintSet.LEFT, 0)
            }
            */

            else -> {}
        }

        constraintSet.applyTo(Slidanet.editorContent)

        if (Slidanet.editingState == SlidanetEditingStateType.InActive) {

            Slidanet.editorContent?.addView(this)

            if (shareMode != ShareModeType.SlidePeekSlide &&
                shareMode != ShareModeType.SlidePeekDefine) {

                Slidanet.editorContent?.addView(Slidanet.referenceView)
            }

            Slidanet.editorContent?.addView(Slidanet.editorControl)
        }
    }

    override fun getNormalizedTranslationX(): Float {

        return normalizedTranslationX
    }

    override fun getNormalizedTranslationY(): Float {

        return normalizedTranslationY
    }

    override fun getNormalizedTranslationZ(): Float {

        return normalizedTranslationZ
    }

    override fun setBoxBeginX(beginX: Float) {

        boxBeginX = beginX
    }

    override fun setBoxBeginY(beginY: Float) {

        boxBeginY = beginY
    }

    override fun setBoxEndX(endX: Float) {

        boxEndX = endX
    }

    override fun setBoxEndY(endY: Float) {

        boxEndY = endY
    }

    override fun getBoxBeginX(): Float {

        return boxBeginX
    }

    override fun getBoxBeginY(): Float {

        return boxBeginY
    }

    override fun getBoxEndX(): Float {

        return boxEndX
    }

    override fun getBoxEndY(): Float {

        return boxEndY
    }

    override fun getPixelWidth(): Int {

        return pixelWidth
    }

    override fun getDynamicPixelWidth(): Int {

        return dynamicPixelWidth
    }

    override fun getRotationAngle(): Int {

        return rotationAngle
    }

    override fun getScale(): Float {

        return scale
    }

    override fun getRedMaskColor(): Float {

        return redMaskColor
    }

    override fun getGreenMaskColor(): Float {

        return greenMaskColor
    }

    override fun getBlueMaskColor(): Float {

        return blueMaskColor
    }

    override fun getAlphaMaskColor(): Float {

        return alphaMaskColor
    }

    override fun getFlipTexture(): Boolean {

        return flipTexture
    }

    override fun getContentPath(): String {

        return contentPath
    }

    override fun getContentType(): SlidanetContentType {

        return contentType
    }

    override fun getContentAddressOwner(): String {

        return contentAddressOwner
    }

    override fun getUpdateDuringEdit(): Boolean {

        return updateDuringEdit
    }

    override fun getStartTime(): Float {

        return startTime
    }

    override fun getTakenStatus(): Boolean {

        return takenStatus
    }

    override fun getTextureWidth(): Int {

        return textureWidth
    }

    override fun getTextureHeight(): Int {

        return textureHeight
    }

    override fun getContentAddress(): String {

        return contentAddress
    }

    override fun distributeTranslation() {

        Slidanet.rendererHandler.post {

            Slidanet.server.distributeTranslation(contentAddress,
                                                  shareMode,
                                                  normalizedTranslationX,
                                                  normalizedTranslationY,
                                                  normalizedTranslationZ)
        }
    }

    override fun distributeMaskBox() {

        Slidanet.rendererHandler.post {

            Slidanet.server.distributeMaskBox(contentAddress,
                                              shareMode,
                                              boxBeginX,
                                              boxBeginY,
                                              boxEndX,
                                              boxEndY)
        }
    }

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {

        Slidanet.rendererHandler.post { sTexture.release() }

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

    override fun getViewPosition() : IntArray {

        val array: IntArray = intArrayOf(0, 0)
        this.getLocationInWindow(array)
        return array
    }


    override fun initializeBoxDimensions(bx: Float, by: Float, ex: Float, ey: Float) {

        boxBeginX = bx
        boxBeginY = by
        boxEndX = ex
        boxEndY = ey

    }

    private fun initializePixelWidth() : Int {

        return pixelWidth
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

            if (contentType == SlidanetContentType.StaticVideo) {
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
                                             alpha = contentAlpha))

            drawElements( indicesBuffer, 6)

            swapBuffers(windowSurface)

            displayNeedsUpdate = false
        }
    }

    override fun getVisibilityPreference() : SlidanetVisibilityPreferenceType {

        return visibilityPreference
    }

    override fun setVisibilityPreference(perference: SlidanetVisibilityPreferenceType) {

        visibilityPreference = perference
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

    override fun setGiveEnabled(give: Boolean) {

        giveEnabled = give
    }

    override fun setHideEnabled(hide: Boolean) {

        hideEnabled = hide
    }

    override fun getGiveEnabled(): Boolean {

        return giveEnabled
    }

    override fun getHideEnabled(): Boolean {

        return hideEnabled
    }

    override fun setShareMode(mode: ShareModeType) {

        shareMode = mode
    }

    override fun getShareMode(): ShareModeType {

        return shareMode
    }

    override fun setContentAlpha(alpha: Float) {
        contentAlpha = alpha
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

    override fun setContentAddressOwner(contentAddressOwner: String) {

        this.contentAddressOwner = contentAddressOwner
    }

    override fun getContentWasTaken() : Boolean {

        return contentWasTaken
    }

    override fun setContentWasTaken(state: Boolean) {

        contentWasTaken = state
    }

    override fun onFrameAvailable(p0: SurfaceTexture?) {

        Slidanet.rendererHandler.post {

            surfaceTexture?.updateTexImage()
            surfaceTexture?.getTransformMatrix(textureTransformMatrix)
            displayNeedsUpdate = true
        }
    }

    override fun onVideoPrepared() {

        videoPlayer.play()
    }

    override fun onVideoStarted() {

        Slidanet.rendererHandler.post { videoIsRunning = true }
    }

    override fun onVideoFinished() {

        Slidanet.rendererHandler.post { videoIsRunning = false }
    }

    override fun updateTexture() {

        Slidanet.rendererHandler.post {

            videoSurfaceTexture.updateTexImage()
            val textureTransformationArray = floatArrayOf()
            videoSurfaceTexture.getTransformMatrix(textureTransformationArray)
            textureTransformationBuffer = createFloatBuffer(textureTransformationArray)
            displayNeedsUpdate = true
        }
    }

    override fun onFrameAvailable(presentationTime: Long): Boolean {

        return true
    }
}