package danielabbott.personalorganiser.ui.timers

import android.content.Context
import android.os.*
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import danielabbott.personalorganiser.Notifications
import danielabbott.personalorganiser.R
import danielabbott.personalorganiser.data.DB.context
import danielabbott.personalorganiser.data.Settings

class AlarmActivity : AppCompatActivity() {

    private lateinit var vibrationRepeat: Runnable
    private var stopVibration = false
    private var vibrationCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)

        } else {

            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        setContentView(R.layout.activity_alarm)

        findViewById<ImageButton>(R.id.back).setOnClickListener {
            stopVibration = true
            finish()
        }

        var timerName = "";
        if (intent != null && intent.extras != null) {
            timerName = intent.extras!!.getString("timerName") ?: ""
            findViewById<TextView>(R.id.timerName).text = timerName
        }

        val mHandler = Handler()


        var alarmDisabled =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    android.provider.Settings.Global.getInt(contentResolver, "zen_mode") == 2


        if (!alarmDisabled && Settings.getAlarmVibrationEnabled(this)) {
            val vibrator =
                context.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            vibrationCounter = 10
            vibrationRepeat = Runnable {
                if (!stopVibration && vibrationCounter > 0) {
                    vibrationCounter -= 1
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                1000,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        vibrator.vibrate(1000)
                    }
                    mHandler.postDelayed(vibrationRepeat, 2000)
                } else {
                    finish()
                }
            }
            mHandler.post(vibrationRepeat)
        } else {
            mHandler.postDelayed({
                finish()
            }, 7000)
        }

        Notifications.showNotification(
            context.applicationContext,
            "Timer",
            timerName,
            Notifications.Channel.TIMER,
            null
        )
    }
}
