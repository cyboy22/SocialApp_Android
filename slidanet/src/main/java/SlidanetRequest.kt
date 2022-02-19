internal interface SlidanetRequest {

    fun authenticateConnection(requestId: Int,
                               platformName: String,
                               platformPassword: String,
                               userId: String)
}