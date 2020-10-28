package danielabbott.personalorganiser.data

import danielabbott.personalorganiser.Notifications

class NotificationData(
    var content: String,
    var channel: Notifications.Channel,
    var taskOrEventId: Long,
    var time: Long,
    var reqCode: Int // for android alarm api
) {
}