package danielabbott.personalorganiser.data

import android.content.Context

object Settings {

    private fun getInt(context: Context, name: String, default: Int): Int {
        val sharedPref = context.applicationContext.getSharedPreferences(".", Context.MODE_PRIVATE)
            ?: return default
        return sharedPref.getInt(name, default)
    }

    private fun setInt(context: Context, name: String, value: Int) {
        val sharedPref =
            context.applicationContext.getSharedPreferences(".", Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putInt(name, value)
            commit()
        }
    }


    private fun getFloat(context: Context, name: String, default: Float): Float {
        val sharedPref = context.applicationContext.getSharedPreferences(".", Context.MODE_PRIVATE)
            ?: return default
        return sharedPref.getFloat(name, default)
    }

    private fun setFloat(context: Context, name: String, value: Float) {
        if (!java.lang.Float.isNaN(value)) {
            val sharedPref =
                context.applicationContext.getSharedPreferences(".", Context.MODE_PRIVATE) ?: return
            with(sharedPref.edit()) {
                putFloat(name, value)
                commit()
            }
        }
    }

    private fun getBool(context: Context, name: String, default: Boolean): Boolean {
        val sharedPref = context.applicationContext.getSharedPreferences(".", Context.MODE_PRIVATE)
            ?: return default
        return sharedPref.getBoolean(name, default)
    }

    private fun setBool(context: Context, name: String, value: Boolean) {
        val sharedPref =
            context.applicationContext.getSharedPreferences(".", Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putBoolean(name, value)
            commit()
        }
    }

    private fun getLong(context: Context, name: String, default: Long): Long {
        val sharedPref = context.applicationContext.getSharedPreferences(".", Context.MODE_PRIVATE)
            ?: return default
        return sharedPref.getLong(name, default)
    }

    private fun setLong(context: Context, name: String, value: Long) {
        val sharedPref =
            context.applicationContext.getSharedPreferences(".", Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putLong(name, value)
            commit()
        }
    }

    private fun getString(context: Context, name: String, default: String): String {
        val sharedPref = context.applicationContext.getSharedPreferences(".", Context.MODE_PRIVATE)
            ?: return default
        return sharedPref.getString(name, default) ?: default
    }

    private fun setString(context: Context, name: String, value: String) {
        val sharedPref =
            context.applicationContext.getSharedPreferences(".", Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString(name, value)
            commit()
        }
    }

    fun setActiveTimetable(id: Long, context: Context) {
        setLong(context, "active_timetable", id)
    }

    // Returns -1 if there is no active timetable
    fun getActiveTimetable(context: Context): Long {
        return getLong(context, "active_timetable", -1)
    }

    //  AlarmManager ids
    // 0+ = notifications
    // -1 = weekly reschedule (for when app has not been opened)
    // -2.. = alarms

    // Notification ID is used for scheduling notifications

    fun getNotificationID(context: Context): Int {
        val id = getInt(context, "notifId", -1) + 1
        setInt(context, "notifId", id)
        return id
    }

    fun getAlarmID(context: Context): Int {
        val id = getInt(context, "alarmId", -1) - 1
        setInt(context, "alarmId", id)
        return id
    }

    fun lowestAlarmID(context: Context): Int {
        return getInt(context, "alarmId", -2)
    }

    fun resetLowestAlarmID(context: Context) {
        setInt(context, "alarmId", -2)
    }


    fun getMorningReminderTime(context: Context): Int {
        return getInt(context, "morningTime", 7 * 60)
    }

    fun setMorningTime(context: Context, time: Int) {
        setInt(context, "morningTime", time)
    }

    fun setTimetableStartHour(context: Context, hr: Int) {
        setInt(context, "timetableStartHr", hr)
    }

    fun getTimetableStartHour(context: Context): Int {
        return getInt(context, "timetableStartHr", 7)
    }


    fun setTimetableEndHour(context: Context, hr: Int) {
        setInt(context, "timetableEndHr", hr)
    }

    fun getTimetableEndHour(context: Context): Int {
        return getInt(context, "timetableEndHr", 22)
    }


    fun setTimetablePosition(context: Context, x: Float, y: Float) {
        setFloat(context, "timetablePosX", x)
        setFloat(context, "timetablePosY", y)
    }

    fun getTimetablePosition(context: Context): Pair<Float, Float> {
        return Pair<Float, Float>(
            getFloat(context, "timetablePosX", 0.0f),
            getFloat(context, "timetablePosY", 0.0f)
        )
    }


    fun setTimetableZoom(context: Context, x: Float, y: Float) {
        setFloat(context, "timetableZoomX", x)
        setFloat(context, "timetableZoomY", y)
    }

    fun getTimetableZoom(context: Context): Pair<Float, Float> {
        return Pair(
            getFloat(context, "timetableZoomX", 1.0f),
            getFloat(context, "timetableZoomY", 1.0f)
        )
    }

    // 0 = To Do List
    // 1 = Timetable
    // 2 = Goals
    // 3 = Timers
    fun setLastPage(context: Context, p: Int) {
        setInt(context, "lastPage", p)
    }

    fun getLastPage(context: Context): Int {
        return getInt(context, "lastPage", 0)
    }

    fun getTimetableFontSize(context: Context): Float {
        return getFloat(context, "timetableFontSize", 18.0f)
    }

    fun setTimetableFontSize(context: Context, sz: Float) {
        setFloat(context, "timetableFontSize", sz)
    }

    fun getAlarmsEnabled(context: Context): Boolean {
        return getBool(context, "alarmsEnabled", true)
    }

    fun setAlarmsEnabled(context: Context, e: Boolean) {
        setBool(context, "alarmsEnabled", e)
    }

    fun getAlarmVibrationEnabled(context: Context): Boolean {
        return getBool(context, "alarmVibrationEnabled", true)
    }

    fun setAlarmVibrationEnabled(context: Context, e: Boolean) {
        setBool(context, "alarmVibrationEnabled", e)
    }


    fun getAccurateNotificationsEnabled(context: Context): Boolean {
        return getBool(context, "accurateNotificationsEnabled", true)
    }

    fun setAccurateNotificationsEnabled(context: Context, e: Boolean) {
        setBool(context, "accurateNotificationsEnabled", e)
    }


    fun getSelectedTagID(context: Context): Long {
        return getLong(context, "selectedTagID", -1)
    }

    fun setSelectedTagID(context: Context, id: Long) {
        setLong(context, "selectedTagID", id)
    }


    // Quick Insert Text
    fun getQIT(context: Context): String {
        // Defaults to medium white square emoji
        return getString(context, "qit", 0x25FB.toChar().toString() + 0xFE0F.toChar().toString())
    }

    fun setQIT(context: Context, e: String) {
        setString(context, "qit", e)
    }

    fun getMostRecentFindString(context: Context): String {
        return getString(context, "find", "")
    }

    fun setMostRecentFindString(context: Context, s: String) {
        setString(context, "find", s)
    }

    fun getMostRecentReplaceString(context: Context): String {
        return getString(context, "replace", "")
    }

    fun setMostRecentReplaceString(context: Context, s: String) {
        setString(context, "replace", s)
    }

    fun appStarted(context: Context) : Long {
        val lastTime = getLong(context, "appLastStarted", 0)
        setLong(context, "appLastStarted", System.currentTimeMillis())
        return lastTime
    }

}