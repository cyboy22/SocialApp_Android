import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout

internal class SlidanetEditorContent(val applicationContext: Context) : ConstraintLayout(applicationContext) {

    init {
        this.layoutParams = ConstraintLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

}
