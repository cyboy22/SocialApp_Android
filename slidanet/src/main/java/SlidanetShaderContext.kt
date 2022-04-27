import java.nio.FloatBuffer

internal data class SlidanetShaderContext(val contentFilter: SlidanetContentFilterType = SlidanetContentFilterType.Default,
                                          val peekItEnabled: Boolean = false,
                                          val pixItEnabled: Boolean = false,
                                          val viewType: SlidanetContentType = SlidanetContentType.Image,
                                          val pixelWidth: Int = Constants.defaultPixelWidth,
                                          val pixelHeight: Int = Constants.defaultPixelWidth,
                                          val boxBeginX: Float = .0f,
                                          val boxBeginY: Float = .0f,
                                          val boxEndX: Float = .0f,
                                          val boxEndY: Float = .0f,
                                          val maskRedValue: Float = .0f,
                                          val maskGreenValue: Float = .0f,
                                          val maskBlueValue: Float = .0f,
                                          val maskAlphaValue: Float = 1f,
                                          val verticesBuffer: FloatBuffer,
                                          val textureTransformMatrix: FloatArray = floatArrayOf(0.0f),
                                          val flipTexture: Boolean = false,
                                          val videoIsRunning: Boolean = false,
                                          val textureWidth: Int = 0,
                                          val textureHeight: Int = 0,
                                          val alpha: Float = 1f) {

    override fun equals(other: Any?): Boolean {

        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SlidanetShaderContext

        if (!textureTransformMatrix.contentEquals(other.textureTransformMatrix)) return false

        return true
    }

    override fun hashCode(): Int {

        return textureTransformMatrix.contentHashCode()
    }
}
