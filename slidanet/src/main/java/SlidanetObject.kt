import org.json.JSONObject

internal interface SlidanetObject {

    fun getDisplayNeedsUpdate() : Boolean
    fun getTextureViewReady() : Boolean
    fun render()
    fun setVisibilityPreferences(preferences: MutableMap<String, Boolean>)
    fun setTakers(takersList: MutableMap<String, String>)
    fun setShareTranslationParameters(x: Float, y: Float, z: Float)
    fun setShareBoxParameters(beginX: Float, beginY: Float, endX: Float, endY: Float)
    fun setVisibilityPreference(preference: VisibilityPreferenceType)
    fun setGiveEnabled(give: Boolean)
    fun setHideEnabled(give: Boolean)
    fun setMuteEnabled(give: Boolean)
    fun setFreezeEnabled(give: Boolean)
    fun getGiveEnabled() : Boolean
    fun getHideEnabled() : Boolean
    fun getMuteEnabled() : Boolean
    fun getFreezeEnabled() : Boolean
    fun setShareMode(mode: ShareModeType)
    fun setSlideMode(mode: SlideModeType)
    fun setPeekMode(mode: PeekModeType)
    fun setPixMode(mode: PixModeType)
    fun getShareMode() : ShareModeType
    fun getSlideMode() : SlideModeType
    fun getPeekMode() : PeekModeType
    fun getPixMode() : PixModeType
    fun setPixPercentage(value: Float)
    fun getPixPercentage() : Float
    fun setShaderName(name: String)
    fun getShaderName() : String
    fun setSlideEnabled(value: Boolean)
    fun getSlideEnabled() : Boolean
    fun setPixEnabled(value: Boolean)
    fun getPixEnabled() : Boolean
    fun setPeekEnabled(value: Boolean)
    fun getPeekEnabled() : Boolean
    fun setFadeBarrier(barrier: Float)
    fun getFadeBarrier() : Float
    fun setSnapThreshold(barrier: Float)
    fun getSnapThreshold() : Float
    fun setDisplayNeedsUpdate(state: Boolean)
}