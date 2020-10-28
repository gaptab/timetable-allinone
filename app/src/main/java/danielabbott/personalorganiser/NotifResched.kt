package danielabbott.personalorganiser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import danielabbott.personalorganiser.data.DB

class NotifResched : BroadcastReceiver() {

    // Called once every 6 days (unless the app is run during the 6 days - resets the timer)
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            DB.init(context)
            Notifications.scheduleAllNotifications(context)
        }
    }
}