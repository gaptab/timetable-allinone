package danielabbott.personalorganiser

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import danielabbott.personalorganiser.data.Settings
import java.util.*

object EventSchedule {

    // Runs the intent at the given time
    // Works fine after the app is closed but not after the device is restarted
    // Time is the time in milliseconds since the epoch
    // If requireAccurate is true then accurate timing will be used regardless of the accurate notifications setting
    fun scheduleEvent(
        context: Context,
        time: Long,
        intent: PendingIntent,
        requireAccurate: Boolean = true
    ) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && (requireAccurate || Settings.getAccurateNotificationsEnabled(
                context
            ))
        ) {
            alarmMgr.setExact(
                AlarmManager.RTC_WAKEUP,
                time,
                intent
            )
        } else {
            // set() works on android versions < 19 as setExact does on API levels >= 19
            alarmMgr.set(
                AlarmManager.RTC_WAKEUP,
                time,
                intent
            )
        }

    }

    // day: Day of week
    // time: Minutes since midnight
    fun getTime(day: Int, time: Int): Long {
        Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            // Java day values start with sunday=1
            var jDay = day + 2
            if (day == 8) {
                jDay = 1
            }
            set(Calendar.DAY_OF_WEEK, jDay)
            set(Calendar.HOUR_OF_DAY, time / 60)
            set(Calendar.MINUTE, time % 60)
            set(Calendar.SECOND, 0)
            return timeInMillis
        }
    }

    fun clearEvent(context: Context, intent: PendingIntent?) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmMgr.cancel(intent)
    }

}