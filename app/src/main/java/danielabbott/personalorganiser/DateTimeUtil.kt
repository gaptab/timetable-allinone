package danielabbott.personalorganiser

import android.content.Context
import java.util.*

object DateTimeUtil {

    // Takes time value as milliseconds since epoch
    fun getHoursAndMinutes(dateTime: Long): Pair<Int, Int> {
        Calendar.getInstance().apply {
            timeInMillis = dateTime
            return Pair<Int, Int>(get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE))
        }
    }


    fun getDaySinceEpoch(dateTime: Long): Int {
        Calendar.getInstance().apply {
            timeInMillis = dateTime
            val year = get(Calendar.YEAR)
            var day: Int = 0 // days since jan 1 1970
            var yr: Int = 1970
            while (yr < year) {
                day += if (yr % 4 == 0 && (yr % 100 != 0 || yr % 400 == 0)) 366 else 365
                day += if (yr % 4 != 0) {
                    355
                } else if (yr % 100 != 0) {
                    366
                } else if (yr % 400 != 0) {
                    355
                } else {
                    366
                }
                yr += 1
            }

            return day + get(Calendar.DAY_OF_YEAR)

        }
    }

    // Takes time value as minutes since midnight
    fun getHoursAndMinutes(time: Int): Pair<Int, Int> {
        return Pair<Int, Int>(time / 60, time % 60)
    }

    fun getDateTimeMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            return timeInMillis
        }
    }

    // Takes time value as milliseconds since epoch
    fun getYearMonthDay(dateTime: Long): Triple<Int, Int, Int> {
        Calendar.getInstance().apply {
            timeInMillis = dateTime
            return Triple<Int, Int, Int>(
                get(Calendar.YEAR),
                get(Calendar.MONTH) + 1,
                get(Calendar.DAY_OF_MONTH)
            )
        }
    }

    fun getDateString(context: Context, dateTime: Long): String {

        val date = Date(dateTime)
        val format = android.text.format.DateFormat.getDateFormat(context.applicationContext)

        return format.format(date)
    }

    fun getDateString(context: Context, year: Int, month: Int, day: Int): String {
        return getDateString(context, getDateTimeMillis(year, month, day, 6, 0))
    }

    fun getTimeString(dateTime: Long): String {
        val hm = getHoursAndMinutes(dateTime)
        val h = hm.first.toString().padStart(2, '0')
        val m = hm.second.toString().padStart(2, '0')
        return "$h:$m"
    }

    fun getTimeString(time: Int): String {
        val hm = getHoursAndMinutes(time)
        val h = hm.first.toString().padStart(2, '0')
        val m = hm.second.toString().padStart(2, '0')
        return "$h:$m"
    }

    fun getDateTimeString(context: Context, dateTime: Long): String {
        return "${getDateString(context, dateTime)}  ${getTimeString(dateTime)}"
    }

    fun getStartOfDay(dateTime: Long): Long {
        Calendar.getInstance().apply {
            timeInMillis = dateTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)

            return timeInMillis
        }
    }

    fun getTimeOfDay(day: Long, time: Int): Long {
        Calendar.getInstance().apply {
            timeInMillis = day
            set(Calendar.HOUR_OF_DAY, time / 60)
            set(Calendar.MINUTE, time % 60)
            set(Calendar.SECOND, 0)
            return timeInMillis
        }
    }

    fun timeOfDayMillis(dateTime: Long): Long {
        Calendar.getInstance().apply {
            timeInMillis = dateTime
            return get(Calendar.HOUR_OF_DAY).toLong() * 60L * 60L * 1000L + get(Calendar.MINUTE).toLong() * 60L * 1000L
        }
    }

    fun stripSeconds(dateTime: Long): Long {
        Calendar.getInstance().apply {
            timeInMillis = dateTime
            set(Calendar.SECOND, 0)
            return timeInMillis
        }
    }

    fun getDayOfMonth(dateTime: Long): Int {
        Calendar.getInstance().apply {
            timeInMillis = dateTime
            return get(Calendar.DAY_OF_MONTH)
        }
    }


}