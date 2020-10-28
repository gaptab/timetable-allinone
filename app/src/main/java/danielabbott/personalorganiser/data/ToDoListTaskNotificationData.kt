package danielabbott.personalorganiser.data

class ToDoListTaskNotificationData(
    var id: Long,
    var dateTime: Long?,
    var hasTime: Boolean,
    var name: String,
    var remind30Mins: Boolean,
    var remind1Hr: Boolean,
    var remind2Hrs: Boolean,
    var remindMorning: Boolean,
    var repeat: Repeat
)