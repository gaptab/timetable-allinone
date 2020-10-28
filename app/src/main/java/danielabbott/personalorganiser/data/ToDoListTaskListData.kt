package danielabbott.personalorganiser.data

class ToDoListTaskListData(
    val id: Long,
    var name: String,
    var dateTime: Long?,
    var hasTime: Boolean,
    var colour: Int?,
    val hasNotes: Boolean
)