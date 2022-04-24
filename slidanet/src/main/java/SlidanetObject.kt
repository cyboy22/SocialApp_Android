import org.json.JSONObject
import java.nio.FloatBuffer
import java.nio.ShortBuffer

internal interface SlidanetObject {

    fun getDisplayNeedsUpdate(): Boolean
    fun getTextureViewReady(): Boolean
    fun render()
    fun setVisibilityPreferences(preferences: MutableMap<String, Boolean>)
    fun setTakers(takersList: MutableMap<String, String>)
    fun setShareTranslationParameters(x: Float, y: Float, z: Float)
    fun setShareBoxParameters(beginX: Float, beginY: Float, endX: Float, endY: Float)
    fun setVisibilityPreference(preference: SlidanetVisibilityPreferenceType)
    fun getVisibilityPreference() : SlidanetVisibilityPreferenceType
    fun setGiveEnabled(give: Boolean)
    fun setHideEnabled(give: Boolean)
    fun getGiveEnabled(): Boolean
    fun getHideEnabled(): Boolean
    fun setShareMode(mode: ShareModeType)
    fun getShareMode(): ShareModeType
    fun setPixPercentage(value: Float)
    fun getPixPercentage(): Float
    fun setShaderName(name: String)
    fun getShaderName(): String
    fun setSlideEnabled(value: Boolean)
    fun getSlideEnabled(): Boolean
    fun setPixEnabled(value: Boolean)
    fun getPixEnabled(): Boolean
    fun setPeekEnabled(value: Boolean)
    fun getPeekEnabled(): Boolean
    fun setFadeBarrier(barrier: Float)
    fun getFadeBarrier(): Float
    fun setSnapThreshold(barrier: Float)
    fun getSnapThreshold(): Float
    fun setDisplayNeedsUpdate(state: Boolean)
    fun setContentAddressOwner(contentAddressOwner: String)
    fun deleteTextures()
    fun detachSurfaceTexture()
    fun releaseSurfaceTexture()
    fun copyOwnerParametersToEditor(contentAddress: String)
    fun initializeEditor(contentAddress: String, initiator: SlidanetEditingInitiatorType)
    fun setNormalizedTranslationX(x: Float)
    fun setNormalizedTranslationY(y: Float)
    fun setNormalizedTranslationZ(z: Float)
    fun getNormalizedTranslationX(): Float
    fun getNormalizedTranslationY(): Float
    fun getNormalizedTranslationZ(): Float
    fun setBoxBeginX(beginX: Float)
    fun setBoxBeginY(beginY: Float)
    fun setBoxEndX(endX: Float)
    fun setBoxEndY(endY: Float)
    fun getBoxBeginX(): Float
    fun getBoxBeginY(): Float
    fun getBoxEndX(): Float
    fun getBoxEndY(): Float
    fun getPixelWidth(): Int
    fun getDynamicPixelWidth(): Int
    fun getRotationAngle(): Int
    fun getScale(): Float
    fun getRedMaskColor(): Float
    fun getGreenMaskColor(): Float
    fun getBlueMaskColor(): Float
    fun getAlphaMaskColor(): Float
    fun getFlipTexture(): Boolean
    fun getContentPath(): String
    fun getContentType(): SlidanetContentType
    fun getContentAddressOwner(): String
    fun getUpdateDuringEdit(): Boolean
    fun getStartTime(): Float
    fun getTakenStatus(): Boolean
    fun getTextureWidth(): Int
    fun getTextureHeight(): Int
    fun setUpdateDuringEdit(state: Boolean)
    fun setRotationAngle(angle: Int)
    fun setMoveRequestCount(count: Int)
    fun setContentPath(path: String)
    fun setContentType(cType: SlidanetContentType)
    fun setEditingScale(scale: Float)
    fun setEditorContentAddress(address: String)
    fun setFlipTexture(state: Boolean)
    fun setRedMaskColor(color: Float)
    fun setGreenMaskColor(color: Float)
    fun setBlueMaskColor(color: Float)
    fun setAlphaMaskColor(color: Float)
    fun setStartTime(startTime: Float)
    fun initializeImage()
    fun initializeBackground()
    fun initializeVideo()
    fun initializeTexture()
    fun getEditingScale(): Float
    fun getEditorContentAddress(): String
    fun initializeBoxDimensions(bx: Float, by: Float, ex: Float, ey: Float)
    fun getViewPosition() : IntArray
    fun initializeVertices(normalizedTranslationX: Float, normalizedTranslationY: Float)
    fun getContentAddress() : String
    fun distributeTranslation()
    fun distributeMaskBox()
    fun distributePixelWidth()
    fun setMoveCount(count: Int)
    fun getMoveCount() : Int
    fun incrementMoveCount()
    fun setPixelWidth(pixelWidth: Int)
    fun getContentWasTaken() : Boolean
    fun setContentWasTaken(state: Boolean)
    fun setupEditor(initiator: SlidanetEditingInitiatorType)
    fun commitEditing()
    fun cancelEditing()
    fun getDoubleTapEditingEnabled() : Boolean
    fun setDoubleTapEditingEnabled(editingState: Boolean)
    fun distributeMove()
    fun logEditingRequest()
    fun setContentFilter(contentFilter: SlidanetContentFilterType)
    fun getContentFilter() : SlidanetContentFilterType
    fun getVideoPlayer() : SlidanetVideoPlayer
    fun setContentAlpha(alpha: Float)
    fun getIndicesBuffer() : ShortBuffer
    fun getBackgroundAlphaColor(): Float
    fun getBackgroundVertexBuffer() : FloatBuffer
    fun getEditorBackgroundVertexBuffer() : FloatBuffer
    fun restore()
}