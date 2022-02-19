import org.json.JSONObject

interface SlidanetResponseHandler {
    fun slidanetResponse(responseData: RequestData)
}