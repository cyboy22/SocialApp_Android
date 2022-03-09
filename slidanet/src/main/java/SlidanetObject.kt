internal interface SlidanetObject {

    fun getDisplayNeedsUpdate() : Boolean
    fun getTextureViewReady() : Boolean
    fun render()
}