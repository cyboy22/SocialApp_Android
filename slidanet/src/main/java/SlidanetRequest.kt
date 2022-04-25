import android.view.ViewDebug

internal interface SlidanetRequest {

    fun authenticateConnection(requestId: Int,
                               applicationName: String,
                               applicationPassword: String,
                               slidaName: String)
    fun disconnectFromNetwork(requestId: Int)
    fun connectContent(requestId: Int, contentAddress: String)
    fun distributeTranslation(contentAddress: String,
                              shareMode: SlidanetSharingStyleType,
                              x: Float,
                              y: Float,
                              z: Float)
    fun distributeMaskBox(contentAddress: String,
                          shareMode: SlidanetSharingStyleType,
                          boxBeginX: Float,
                          boxBeginY: Float,
                          boxEndX: Float,
                          boxEndY: Float)
    fun distributePixelWidth(contentAddress: String,
                             shareMode: SlidanetSharingStyleType,
                             pixelWidth: Int)
    fun setShareModePix(requestId: Int,
                        contentAddress: String,
                        shareMode: SlidanetSharingStyleType,
                        boxBeginX: Float,
                        boxBeginY: Float,
                        boxEndX: Float,
                        boxEndY: Float,
                        pixWidth: Int)
    fun setShareModePeek(requestId: Int,
                         contentAddress: String,
                         shareMode: SlidanetSharingStyleType,
                         boxBeginX: Float,
                         boxBeginY: Float,
                         boxEndX: Float,
                         boxEndY: Float)
    fun setShareModeSlide(requestId: Int,
                          contentAddress: String,
                          shareMode: SlidanetSharingStyleType,
                          x: Float,
                          y: Float,
                          z: Float)
    fun giveContentAddress(requestId: Int,
                           contentAddress: String)
    fun takeContentAddress(requestId: Int,
                           contentAddress: String)
    fun setVisibilityPreference(requestId: Int,
                                contentAddress: String,
                                preference: Int)
    fun setContentFilter(requestId: Int,
                         contentAddress: String,
                         filter: Int)
    fun logRequest(contentAddress: String,
                   loggingType: SlidanetLoggingRequestType,
                   requestCount: Int)
    fun setHideState(requestId: Int, contentAddress: String, state: Boolean)
    fun disconnectContent(requestId: Int, contentAddress: String)
    fun disconnectAllContent(requestId: Int)

}