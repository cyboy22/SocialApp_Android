import org.json.JSONObject

public data class RequestData(val requestCode: ClientRequestType,
                       val requestInfo: JSONObject,
                       var responseCode: ClientResponseType,
                       var responseInfo: JSONObject? = null)
