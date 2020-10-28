package danielabbott.personalorganiser.data

class TimetableEvent(
    val id: Long,
    val timetable_id: Long,
    var startTime: Int,
    var duration: Int, // In minutes
    var days: Int,
    var name: String,
    var notes: String?,
    var remind30Mins: Boolean,
    var remind1Hr: Boolean,
    var remind2Hrs: Boolean,
    var remindMorning: Boolean,
    var goal_id: Long?,

    // Set when data is loaded from database, ignored when storing in database
    var goal_colour: Int? = null
)
