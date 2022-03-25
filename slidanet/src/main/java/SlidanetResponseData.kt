import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import org.json.JSONObject

public data class SlidanetResponseData(val requestCode: SlidanetRequestType,
                                       val requestInfo: JSONObject,
                                       var responseCode: SlidanetResponseType,
                                       var responseInfo: JSONObject? = null,
                                       val applicationContext: Context? = null,
                                       var slidanetView: ConstraintLayout? = null)
