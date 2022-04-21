import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import org.json.JSONObject

public data class SlidanetResponseData(val requestCode: SlidanetRequestType,
                                       val requestInfo: JSONObject,
                                       var responseCode: SlidanetResponseType,
                                       val responseInfo: JSONObject? = null,
                                       val applicationContext: Context? = null,
                                       var slidanetContentContainer: ConstraintLayout? = null,
                                       val editorView: ConstraintLayout? = null)
