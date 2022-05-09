import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class SlidanetConnectivityChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent) {

        if (intent.action.equals("android.net.conn.CONNECTIVITY_CHANGE", ignoreCase = true)) {

            Toast.makeText(context, "Connection changed", Toast.LENGTH_SHORT).show()
        }

        if (!Slidanet.isConnected()) {

            Slidanet.disconnect()

        } else {

            Slidanet.clearData()

        }
    }
}