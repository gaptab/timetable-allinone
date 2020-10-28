package danielabbott.personalorganiser.data


class Milestone(
    var id: Long,
    var name: String,
    var deadline: Long?, // ms since epoch
    var goal_id: Long
)