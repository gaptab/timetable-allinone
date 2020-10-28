package danielabbott.personalorganiser.data

class Timer(
    val id: Long?,
    val name: String?,
    // Times in seconds.
    val time: Int?,
    val initialTime: Int, // 0 = timer goes up
    val time_saved: Long, // milliseconds since epoch
    val isPaused: Boolean
)