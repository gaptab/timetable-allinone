package danielabbott.personalorganiser

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import danielabbott.personalorganiser.data.*

object Notifications {

    enum class Channel(var id: String, var id_int: Int) {
        TIMETABLE("Timetable", 0),
        TODOLIST("To Do List", 1),
        GOALS("Goals", 2),
        TIMER("Timer", 3);

        companion object {
            fun fromInt(value: Int) = values().first { it.id_int == value }
        }
    }

    // Incremented with each notification
    // Provides the ID value that is passed to Android
    // App doesn't keep track of notifications once they are shown
    private var notificationId: Int = 0

    // Android 8.0 and up requires that notifications be sorted into channels
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = Channel.values()
            channels.forEach {
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(it.id, it.id, importance)
                val notificationManager: NotificationManager =
                    context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        content: String,
        channel: Channel,
        pendingIntent: PendingIntent?
    ) {
        var builder = NotificationCompat.Builder(context.applicationContext, channel.id)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent).setAutoCancel(true)
        }



        with(NotificationManagerCompat.from(context.applicationContext)) {
            notify(notificationId, builder.build())
            notificationId++
        }
    }

    private fun scheduleNotification(
        context: Context,
        timeMs: Long,
        content: String,
        channel: Channel,
        id: Long
    ) {
        if (timeMs < System.currentTimeMillis()) {
            return
        }

        val reqCode = Settings.getNotificationID(context.applicationContext)
        DB.addNotification(NotificationData(content, channel, id, timeMs, reqCode))

        val intent = Intent(context.applicationContext, NotificationBroadcastReceiver::class.java)
        val intent2 = PendingIntent.getBroadcast(
            context.applicationContext,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        EventSchedule.scheduleEvent(context.applicationContext, timeMs, intent2, false)
    }

    private fun clearPendingNotification(context: Context, reqCode: Int) {
        val intent =
            Intent(context.applicationContext, NotificationBroadcastReceiver::class.java)
        intent.putExtra("reqCode", reqCode)
        val intent2 =
            PendingIntent.getBroadcast(
                context.applicationContext,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        EventSchedule.clearEvent(context.applicationContext, intent2)
    }

    // Tell android to cancel every scheduled alarm
    // ^ except for the weeklyish reschedule alarm (request code -1) and timer alarms
    fun clearPendingNotifications(context: Context) {
        DB.getActiveAlarmReqCodes().forEach {c->
            clearPendingNotification(context.applicationContext, c)
        }
        DB.clearNotifications()
    }

    private fun scheduleAllNotificationsForTimetableEvents(context: Context) {
        val activeTimetable = Settings.getActiveTimetable(context.applicationContext)
        if (activeTimetable < 0) {
            return
        }
        DB.getTimetableEvents(activeTimetable).forEach {
            scheduleAllNotificationsForTimetableEvent(context, it)
        }
    }

    private fun scheduleAllNotificationsForTimetableEvent(context: Context, it: TimetableEvent) {
        // Minutes before, -1 = morning of
        val times = listOf<Int>(30, 60, 120, -1)
        times.forEach { timeBefore ->
            if ((timeBefore == 30 && it.remind30Mins) ||
                (timeBefore == 60 && it.remind1Hr) ||
                (timeBefore == 120 && it.remind2Hrs) ||
                (timeBefore == -1 && it.remindMorning)
            ) {

                var time: Int

                if (timeBefore == -1) {
                    time = Settings.getMorningReminderTime(context.applicationContext)
                } else {
                    time = it.startTime - timeBefore
                }


                for (day in 0..6) {
                    if ((it.days and (1 shl day)) != 0) {
                        var j = 0
                        while (j < 2) { // This/next week
                            scheduleNotification(
                                context.applicationContext,
                                EventSchedule.getTime(day, time) + j * 7 * 24 * 60 * 60 * 1000,
                                it.name,
                                Channel.TIMETABLE,
                                it.id
                            )
                            j += 1
                        }

                    }
                }
            }
        }
    }

    private fun scheduleAllNotificationsForToDoListTasks(context: Context) {
        DB.getToDoListTasksNotificationData().forEach {
            scheduleAllNotificationsForToDoListTask(context, it)
        }
    }

    private fun scheduleAllNotificationsForToDoListTask(context: Context, it: ToDoListTaskNotificationData) {
        val dayMillis = 24 * 60 * 60 * 1000
        val taskDateTime = if (it.repeat == Repeat.DAILY) it.dateTime
            ?: System.currentTimeMillis() - dayMillis else it.dateTime
        if (taskDateTime != null) {
            val times = listOf<Int>(30, 60, 120, -1)
            times.forEach { timeBefore ->
                if ((timeBefore == 30 && it.hasTime && it.remind30Mins) ||
                    (timeBefore == 60 && it.hasTime && it.remind1Hr) ||
                    (timeBefore == 120 && it.hasTime && it.remind2Hrs) ||
                    (timeBefore == -1 && it.remindMorning)
                ) {

                    var dateTime: Long

                    if (timeBefore == -1) {
                        dateTime = DateTimeUtil.getStartOfDay(taskDateTime)
                        dateTime += Settings.getMorningReminderTime(context.applicationContext) * 60 * 1000
                    } else {
                        dateTime =
                            DateTimeUtil.stripSeconds(taskDateTime) - timeBefore * 60 * 1000
                    }

                    // Notifications for repeat events

                    fun schedule(dateTime: Long) {
                        scheduleNotification(
                            context.applicationContext,
                            dateTime,
                            it.name,
                            Channel.TODOLIST,
                            it.id
                        )
                    }

                    val time = DateTimeUtil.timeOfDayMillis(dateTime)

                    if (it.repeat == Repeat.DAILY || it.repeat == Repeat.WEEKLY) {
                        // Schedule notifications for next 7 days (daily) or for in 1 weeks time (weekly)

                        val mul = if (it.repeat == Repeat.WEEKLY) 7 else 1
                        val iterations = if (it.repeat == Repeat.WEEKLY) 1 else 7

                        for (i in 0 until iterations) {
                            val dt = DateTimeUtil.getTimeOfDay(
                                System.currentTimeMillis() + i * dayMillis * mul,
                                0
                            ) + time
                            if (dt >= dateTime - 60000) {
                                schedule(dt)
                            }
                        }
                    } else if (it.repeat == Repeat.EVERY_OTHER_DAY) {
                        val dayMod2 = DateTimeUtil.getDaySinceEpoch(dateTime) % 2
                        val todayMod2 =
                            DateTimeUtil.getDaySinceEpoch(System.currentTimeMillis()) % 2
                        var i = 0
                        if (dayMod2 != todayMod2) {
                            // No reminder today
                            i = 1
                        }
                        while (i < 7) {
                            val dt = DateTimeUtil.getTimeOfDay(
                                System.currentTimeMillis() + i * dayMillis,
                                0
                            ) + time
                            if (dt >= dateTime - 60000) {
                                schedule(dt)
                            }
                            i += 2
                        }
                    } else if (it.repeat == Repeat.MONTHLY) {
                        val timeMinutes =
                            ((DateTimeUtil.timeOfDayMillis(dateTime) / 1000) / 60).toInt()
                        val dayOfMonth = DateTimeUtil.getDayOfMonth(dateTime)
                        val now = DateTimeUtil.getYearMonthDay(System.currentTimeMillis())

                        // This month
                        schedule(
                            DateTimeUtil.getDateTimeMillis(
                                now.first,
                                now.second,
                                dayOfMonth,
                                timeMinutes / 60,
                                timeMinutes % 60
                            )
                        )

                        // Next month
                        var month = now.second
                        var year = now.first
                        month += 1
                        if (month > 12) {
                            month = 0
                            year += 1
                        }
                        schedule(
                            DateTimeUtil.getDateTimeMillis(
                                year,
                                month,
                                dayOfMonth,
                                timeMinutes / 60,
                                timeMinutes % 60
                            )
                        )

                    } else {
                        schedule(dateTime)
                    }
                }
            }
        }
    }

    fun scheduleAllNotifications(context: Context) {
        clearPendingNotifications(context.applicationContext)
        scheduleAllNotificationsForTimetableEvents(context.applicationContext)
        scheduleAllNotificationsForToDoListTasks(context.applicationContext)
    }

    fun unscheduleNotificationsForTask(context: Context, taskId: Long) {
        DB.getActiveAlarmReqCodesForTaskAndRemove(taskId).forEach {c->
            clearPendingNotification(context, c)
        }
    }

    fun unscheduleNotificationsForTTEvent(context: Context, eventId: Long) {
        DB.getActiveAlarmReqCodesForTTEventAndRemove(eventId).forEach {c->
            clearPendingNotification(context, c)
        }
    }

    fun scheduleForTask(context: Context, task: ToDoListTask, newTask: Boolean) {
        if(!newTask) {
            unscheduleNotificationsForTask(context, task.id)
        }

        scheduleAllNotificationsForToDoListTask(context, ToDoListTaskNotificationData(
            task.id,
            task.dateTime,
            task.hasTime,
            task.name,
            task.remind30Mins,
            task.remind1Hr,
            task.remind2Hrs,
            task.remindMorning,
            task.repeat
        ))
    }

    fun scheduleForTTEvent(context: Context, event: TimetableEvent, newEvent: Boolean) {
        if(!newEvent) {
            unscheduleNotificationsForTTEvent(context, event.id)
        }

        scheduleAllNotificationsForTimetableEvent(context, event)
    }

}