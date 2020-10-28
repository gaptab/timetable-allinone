package danielabbott.personalorganiser.data

class Note(
    var id: Long,
    var contents: String,
    var tags: ArrayList<Tag>
)