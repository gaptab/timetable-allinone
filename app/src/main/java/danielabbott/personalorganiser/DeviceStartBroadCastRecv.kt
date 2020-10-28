package danielabbott.personalorganiser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import danielabbott.personalorganiser.data.DB

class DeviceStartBroadCastRecv : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent?.action == "android.intent.action.BOOT_COMPLETED") {
            // Device has just (re)started
            // All scheduled notifications are lost, reschedule them
            DB.init(context)
            Notifications.scheduleAllNotifications(context)
        }
    }
}