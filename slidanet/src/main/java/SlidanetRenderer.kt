import Slidanet.getRawResource
import android.graphics.SurfaceTexture
import android.opengl.*
import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glUniform1i
import android.opengl.GLES20.glDrawElements
import android.util.Log
import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import android.view.Surface
import com.example.slidanet.R
import org.json.JSONObject
import java.lang.IllegalStateException
import java.nio.ShortBuffer


internal class SlidanetRenderer {

    companion object {

        const val POSITION_COORDS_PER_VERTEX = 3
        const val TEXTURE_COORDS_PER_VERTEX = 2
        const val POSITION_OFFSET = 0
        const val TEXCOORD_OFFSET = 3
        const val vertexStride = (POSITION_COORDS_PER_VERTEX + TEXTURE_COORDS_PER_VERTEX) * 4
        private const val DEFAULT_SURFACE_WIDTH = 10
        private const val DEFAULT_SURFACE_HEIGHT = 10
    }

    private val DEFAULT_VERTEX_SHADER = "attribute mediump vec3 a_position;\n" +
            "attribute mediump vec2 a_texcoord;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main()\n" +
            "{\n" +
            "   gl_Position = vec4(a_position, 1.0);\n" +
            "   v_texcoord = a_texcoord;\n" +
            "}\n"

    private val DEFAULT_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;" +
            "uniform sampler2D s_texture;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main() {" +
            "   vec4 textureColor = texture2D(s_texture, v_texcoord);\n" +
            "   gl_FragColor = vec4(textureColor.r, textureColor.g, textureColor.b, textureColor.a);" +
            "}"

    private val TAG = "Slidanet Renderer"
    private val frameCallback: Choreographer.FrameCallback = Choreographer.FrameCallback { updateSlidaObjects() }
    private var eglDisplay: EGLDisplay? = EGL14.EGL_NO_DISPLAY
    private var eglContext = EGL14.EGL_NO_CONTEXT
    private lateinit var eglConfig: EGLConfig
    private val shaders = mutableMapOf<SlidanetContentFilterType, Int>()

    private lateinit var defaultSurface: EGLSurface
    private var positionHandle = 0
    private var textureCoordinatesHandle = 0
    internal val slidaObjects = mutableMapOf<String, SlidanetObject>()
    private var editorObject: SlidanetObject? = null
    private var enableRendering = false

    init {

        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {

            throw RuntimeException("EGL already set up")
        }

        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay === EGL14.EGL_NO_DISPLAY) {

            throw RuntimeException("unable to get EGL14 display")
        }

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {

            eglDisplay = null
            throw RuntimeException("unable to initialize EGL14")
        }

        checkGlError("eglInitialize")

        if (eglContext === EGL14.EGL_NO_CONTEXT) {

            val config = getConfig() ?: throw RuntimeException("Unable to find a suitable EGLConfig")
            val attrib2_list = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)

            val context = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, attrib2_list, 0)
            checkGlError("eglCreateContext")
            eglConfig = config
            eglContext = context
        }

        createDefaultSurface()
        initializeGLParameters()
        initializeShaders()

        if (enableRendering) {
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }

        Slidanet.mainHandler?.post { Slidanet.setRendererInitialized(true) }

    }

    internal fun addRenderingObject(viewId: String, slidaView: SlidanetObject) {

        slidaObjects += Pair(
            viewId,
            slidaView
        )
    }

    internal fun removeRenderingObject(viewId: String) {

        slidaObjects.remove(viewId)
    }

    fun setEditorObject(editorSubject: SlidanetObject) {

        editorObject = editorSubject
    }

    fun setContentBackgroundColor(r: Float,
                                  b: Float,
                                  g: Float,
                                  a: Float) {

        GLES20.glClearColor(r ,g, b, a)
    }

    fun clearSurface() {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
    }

    internal fun clearRenderingObjects() {

        slidaObjects.clear()
    }

    private fun initializeShader(shaderType: Int, source: String): Int {

        var shader = GLES20.glCreateShader(shaderType)
        checkGlError("glCreateShader type=$shaderType")
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        checkGlError("glCompileShader type=$shaderType")
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)

        if (compiled[0] == 0) {

            Log.e(TAG, "Could not compile shader $shaderType:")
            val check = GLES20.glGetShaderInfoLog(shader)
            Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            shader = 0
        }
        return shader
    }

    internal fun setRenderingState(state: Boolean) {

        enableRendering = state

        if (state) {

            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {

        val vertexShader = initializeShader(GLES20.GL_VERTEX_SHADER, vertexSource)

        if (vertexShader == 0) {
            return 0
        }

        val fragmentShader = initializeShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (fragmentShader == 0) {
            return 0
        }

        var program = GLES20.glCreateProgram()
        checkGlError("glCreateProgram")
        if (program == 0) {
            Log.e(TAG, "Could not create program")
        }
        GLES20.glAttachShader(program, vertexShader)
        checkGlError("glAttachShader")
        GLES20.glAttachShader(program, fragmentShader)
        checkGlError("glAttachShader")

        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {

            Log.e(TAG, "Could not link program: ")
            Log.e(TAG, GLES20.glGetProgramInfoLog(program))
            GLES20.glDeleteProgram(program)
            program = 0
        }
        return program
    }

    private fun initializeShaders() {

        //val defaultVertexShader = DEFAULT_VERTEX_SHADER
        //val defaultFragmentShader = DEFAULT_FRAGMENT_SHADER


        val defaultVertexShader = getRawResource(R.raw.default_vertex_shader)
        val defaultFragmentShader = getRawResource(R.raw.default_fragment_shader)

        //val shaderProgram = createProgram(defaultVertexShader, defaultFragmentShader)

        val shaderProgram = createProgram(defaultVertexShader!!, defaultFragmentShader!!)
        //if (shaderProgram > 0) {  shaders += Pair("DefaultShader", shaderProgram) }
        if (shaderProgram > 0) {  shaders += Pair(SlidanetContentFilterType.Default, shaderProgram) }

    }

    private fun createOffscreenSurface(): EGLSurface {

        val surfaceAttribs = intArrayOf(
            EGL14.EGL_WIDTH, DEFAULT_SURFACE_WIDTH,
            EGL14.EGL_HEIGHT, DEFAULT_SURFACE_HEIGHT,
            EGL14.EGL_NONE
        )
        val eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, surfaceAttribs, 0
        )
        checkGlError("eglCreatePbufferSurface")
        if (eglSurface == null) {
            throw java.lang.RuntimeException("surface was null")
        }
        return eglSurface
    }

    private fun createDefaultSurface() {

        defaultSurface = createOffscreenSurface()
        makeCurrent(defaultSurface)
    }

    internal fun makeCurrent(eglSurface: android.opengl.EGLSurface) {

        if (eglDisplay === EGL14.EGL_NO_DISPLAY) {

            Log.d(TAG, "NOTE: makeCurrent w/o display")
        }
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {

            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    private fun initializeGLParameters() {

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glFrontFace(GLES20.GL_CCW)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun updateSlidaObjects() {

        for (v in slidaObjects.entries) {

            val slidaView = v.value
            if (slidaView.getDisplayNeedsUpdate())

                if (slidaView.getTextureViewReady()) {

                    slidaView.render()
                    slidaView.setDisplayNeedsUpdate(false)
                }
        }

        editorObject?.let {

            if (it.getDisplayNeedsUpdate())

                if (it.getTextureViewReady())
                {
                    it.render()
                    it.setDisplayNeedsUpdate(false)
                }

        }

        if (enableRendering) {

            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    internal fun checkGlError(op: String) {

        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {

            when (error) {

                GLES20.GL_INVALID_ENUM ->  {
                    print("invalid enum")
                }

                GLES20.GL_INVALID_VALUE -> {
                    print("invalid value")
                }

                GLES20.GL_INVALID_OPERATION -> {
                    print("Invalid Operation")
                }

                GLES20.GL_INVALID_FRAMEBUFFER_OPERATION -> {
                    print("Invalid Frame Buffer Operation")
                }

                GLES20.GL_OUT_OF_MEMORY -> {
                    print("Out Of Memory")
                }

                else -> { // Note the block
                    print("x is neither 1 nor 2")
                }
            }

            val msg = op + ": glError 0x" + Integer.toHexString(error)
            Log.e(TAG, msg)
            throw RuntimeException(msg)
        }
    }

    private fun getConfig(): EGLConfig? {

        val attribList = intArrayOf(

            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 0,
            EGL14.EGL_STENCIL_SIZE, 0,
            EGL14.EGL_CONFORMANT, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE)

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(

                eglDisplay, attribList, 0, configs, 0, configs.size,
                numConfigs, 0
            )
        ) {
            Log.w(TAG, "unable to find RGB8888 / 2 EGLConfig")
            return null
        }
        return configs[0]
    }

    fun swapBuffers(eglSurface: EGLSurface): Boolean {

        return EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    internal fun createWindowSurface(surface: Any): android.opengl.EGLSurface {

        if (surface !is Surface && surface !is SurfaceTexture) {

            throw RuntimeException("invalid surface: $surface")
        }

        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        return EGL14.eglCreateWindowSurface(eglDisplay,
                                            eglConfig,
                                            surface,
                                            surfaceAttribs,
                                           0)
    }

    internal fun destroyWindowSurface(surface: EGLSurface): Boolean {

        return EGL14.eglDestroySurface(eglDisplay, surface)
    }

    fun activateTexture(textureUnit: Int, textureId: Int) {

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(textureUnit, textureId)
        checkGlError("Activate Texture Id " + String.format("%d", textureId))
    }

    fun drawElements(indexBuffer: ShortBuffer, indexCount: Int) {

        //glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
        glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        checkGlError("glDrawElements ")

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(textureCoordinatesHandle)
    }

    fun setViewport(width: Int, height: Int) {

        GLES20.glViewport(0, 0, width, height)
    }

    fun loadShader(shaderContext: SlidanetShaderContext) {

        shaders[shaderContext.contentFilter]?.let {

            GLES20.glUseProgram(it)
            checkGlError("glUseProgram")

            if (shaderContext.videoIsRunning) {

                val textureTransformMatrixLocation = glGetUniformLocation(it, "texture_transform")
                GLES20.glUniformMatrix4fv(textureTransformMatrixLocation,
                                          1,
                                          false,
                                          shaderContext.textureTransformMatrix, 0)
            }

            shaderContext.verticesBuffer.position(POSITION_OFFSET)
            positionHandle = GLES20.glGetAttribLocation(it, "a_position")
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle,
                                         POSITION_COORDS_PER_VERTEX,
                                         GLES20.GL_FLOAT,
                                        false,
                                         vertexStride,
                                         shaderContext.verticesBuffer)

            shaderContext.verticesBuffer.position(TEXCOORD_OFFSET)
            textureCoordinatesHandle = GLES20.glGetAttribLocation(it, "a_texcoord")
            GLES20.glEnableVertexAttribArray(textureCoordinatesHandle)
            GLES20.glVertexAttribPointer(textureCoordinatesHandle,
                                         TEXTURE_COORDS_PER_VERTEX,
                                         GLES20.GL_FLOAT,
                                        false,
                                         vertexStride,
                                         shaderContext.verticesBuffer)


            val videoRunningLocation = glGetUniformLocation(it, "video_running")
            glUniform1i(videoRunningLocation, shaderContext.videoIsRunning.toInt())

            val peekItEnabledLocation = glGetUniformLocation(it, "peek_active")
            glUniform1i(peekItEnabledLocation, shaderContext.peekItEnabled.toInt())

            val textureWidthLocation = glGetUniformLocation(it, "texture_width")
            GLES20.glUniform1f(textureWidthLocation, shaderContext.textureWidth.toFloat())

            val textureHeightLocation = glGetUniformLocation(it, "texture_height")
            GLES20.glUniform1f(textureHeightLocation, shaderContext.textureHeight.toFloat())

            val alphaLocation = glGetUniformLocation(it, "alpha")
            GLES20.glUniform1f(alphaLocation, shaderContext.alpha)

            val boxXBeginLocation = glGetUniformLocation(it, "box_x_begin")
            GLES20.glUniform1f(boxXBeginLocation, shaderContext.boxBeginX)

            val boxYBeginLocation = glGetUniformLocation(it, "box_y_begin")
            GLES20.glUniform1f(boxYBeginLocation, shaderContext.boxBeginY)

            val boxXEndLocation = glGetUniformLocation(it, "box_x_end")
            GLES20.glUniform1f(boxXEndLocation, shaderContext.boxEndX)

            val boxYEndLocation = glGetUniformLocation(it, "box_y_end")
            GLES20.glUniform1f(boxYEndLocation, shaderContext.boxEndY)

            val peekRedColorLocation = glGetUniformLocation(it, "peek_it_mask_r_value")
            GLES20.glUniform1f(peekRedColorLocation, shaderContext.maskRedValue)

            val peekGreenColorLocation = glGetUniformLocation(it, "peek_it_mask_g_value")
            GLES20.glUniform1f(peekGreenColorLocation, shaderContext.maskGreenValue)

            val peekBlueColorLocation = glGetUniformLocation(it, "peek_it_mask_b_value")
            GLES20.glUniform1f(peekBlueColorLocation, shaderContext.maskBlueValue)

            val peekAlphaColorLocation = glGetUniformLocation(it, "peek_it_mask_a_value")
            GLES20.glUniform1f(peekAlphaColorLocation, shaderContext.maskAlphaValue)

            val flipTextureLocation = glGetUniformLocation(it, "flip_texture")
            glUniform1i(flipTextureLocation, shaderContext.flipTexture.toInt())

            var textureSamplerLocation = 0

            when (shaderContext.viewType) {

                SlidanetContentType.Image -> {
                    textureSamplerLocation = glGetUniformLocation(it, "s_texture")
                }

                SlidanetContentType.StaticVideo -> {

                    if (shaderContext.videoIsRunning) {

                        textureSamplerLocation = glGetUniformLocation(it, "external_texture")

                    } else {

                        textureSamplerLocation = glGetUniformLocation(it, "s_texture")
                    }
                }

                else -> {}
            }

            glUniform1i(textureSamplerLocation, 0)

        } ?: kotlin.run {

            val request = JSONObject()
            val response = SlidanetResponseData(requestCode = SlidanetRequestType.APIMessage,
                                                requestInfo = request,
                                                responseCode = SlidanetResponseType.InternalErrorOccurred)
            Slidanet.mainHandler?.post { Slidanet.slidanetResponseHandler.slidanetResponse(response) }
        }
    }
}