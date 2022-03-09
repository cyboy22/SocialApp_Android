import org.json.JSONObject

public data class SlidanetResponseData(val requestCode: SlidanetRequestType,
                                       val requestInfo: JSONObject,
                                       var responseCode: SlidanetResponseType,
                                       var responseInfo: JSONObject? = null)
