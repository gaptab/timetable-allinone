package danielabbott.personalorganiser

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import danielabbott.personalorganiser.data.DB

class NotificationBroadcastReceiver : BroadcastReceiver() {

    // Called by AlarmManager
    // Shows the notification that was scheduled
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) {
            return
        }

        val reqCode = intent!!.extras!!.getInt("reqCode")


        DB.init(context)

        DB.getNotificationsToShow(reqCode).forEach { n ->

            // Intent runs when notification is tapped
            var pendingIntent: PendingIntent? = null
            if ((n.channel == Notifications.Channel.TODOLIST || n.channel == Notifications.Channel.TIMETABLE) && n.taskOrEventId >= 0) {
                val intent2 = Intent(context.applicationContext, NotificationTapped::class.java)
                intent2.putExtra("taskoreventid", n.taskOrEventId)
                intent2.putExtra("channel", n.channel.id_int)

                // Request code = current time
                // https://stackoverflow.com/a/21204851/11498001

                pendingIntent = PendingIntent.getBroadcast(
                    context.applicationContext,
                    System.currentTimeMillis().toInt(),
                    intent2,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            Notifications.showNotification(
                context,
                "Reminder",
                n.content,
                n.channel,
                pendingIntent
            )
        }
    }


}