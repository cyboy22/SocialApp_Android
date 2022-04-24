import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout

internal class SlidanetEditorContent(val applicationContext: Context) : ConstraintLayout(applicationContext) {

    init {

        this.layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        this.setBackgroundColor(Color.MAGENTA)
    }
}
