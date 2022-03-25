internal interface SlidanetRequest {

    fun authenticateConnection(requestId: Int,
                               applicationName: String,
                               applicationPassword: String,
                               slidaName: String)
    fun disconnectFromNetwork(requestId: Int)
    fun connectContent(requestId: Int, viewId: String)
    fun disconnectAllContent(requestId: Int)
}