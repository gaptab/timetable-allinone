package danielabbott.personalorganiser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.core.content.ContextCompat.startActivity

class NotificationTapped : BroadcastReceiver() {

    // Called when notification is tapped, loads edit page for timetable event / task
    override fun onReceive(context: Context?, intent: Intent?) {

        if (context != null && intent != null && intent.extras != null) {
            val id = intent.extras!!.getLong("taskoreventid")
            val channelInt = intent.extras!!.getInt("channel")
            val channel = Notifications.Channel.fromInt(channelInt)


            val newIntent = Intent(context, MainActivity::class.java).apply {
                flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
                if (channel == Notifications.Channel.TODOLIST) {
                    putExtra("WHAT_LOAD", "TODO")
                } else {
                    putExtra("WHAT_LOAD", "TIMETABLE")
                }
                putExtra("TASK_EVENT_ID", id)
            }

            startActivity(context, newIntent, null)

        }
    }
}