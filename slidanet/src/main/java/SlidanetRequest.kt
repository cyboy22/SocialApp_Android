internal interface SlidanetRequest {

    fun authenticateConnection(requestId: Int,
                               platformName: String,
                               platformPassword: String,
                               userId: String)
    fun disconnectFromNetwork(requestId: Int)
    fun disconnectAllViews(requestId: Int)
}