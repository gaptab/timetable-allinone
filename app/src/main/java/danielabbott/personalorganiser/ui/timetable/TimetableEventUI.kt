package danielabbott.personalorganiser.ui.timetable

import danielabbott.personalorganiser.data.TimetableEvent

class TimetableEventUI(
    var colour: Int,
    val e: TimetableEvent,
    val day: Int,
    val hasNotes: Boolean
) {
    var ui_x: Float = 0.0f
    var ui_y: Float = 0.0f
    var ui_w: Float = 0.0f
    var ui_h: Float = 0.0f
}
