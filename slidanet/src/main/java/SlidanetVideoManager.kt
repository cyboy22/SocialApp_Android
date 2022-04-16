internal interface SlidanetVideoManager {

    fun onVideoPrepared()
    fun onVideoStarted()
    fun onVideoFinished()
    fun updateTexture()
    fun onFrameAvailable(presentationTime: Long) : Boolean
}