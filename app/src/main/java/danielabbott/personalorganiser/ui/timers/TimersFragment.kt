package danielabbott.personalorganiser.ui.timers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.math.MathUtils
import androidx.core.math.MathUtils.clamp
import androidx.core.view.get
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import danielabbott.personalorganiser.EventSchedule
import danielabbott.personalorganiser.MainActivity
import danielabbott.personalorganiser.R
import danielabbott.personalorganiser.data.DB
import danielabbott.personalorganiser.data.Settings
import danielabbott.personalorganiser.data.Timer

class TimersFragment : Fragment() {

    inner class TimerUI(
        val id: Long?,
        var name: String?,
        val layout: LinearLayout,
        val timer: TextView,
        val tvTimerName: TextView,
        var time: Int,
        var originalTime: Int, // time == originalTime when stopped=true
        var paused: Boolean = false, // play/pause
        var stopped: Boolean = true, // if stopped is true then playing is also set to false

        // for keeping time accurate
        // either both are null or both are set
        var lastPlayTime: Long? = null, // time in millis when timer was last played/resumed
        var lastPlayTimeValue: Int? = null // Time value at ^
    )

    private val timers: ArrayList<TimerUI> = ArrayList<TimerUI>(20)
    private var mHandler: Handler? = null
    private lateinit var timerChanger: Runnable
    private var stopTimerChanger = false
    private lateinit var linearLayout: LinearLayout

    private fun setTime(tv: TextView, time: Int) {
        val h = ((time / 60) / 60).toString().padStart(2, '0')
        val m = ((time / 60) % 60).toString().padStart(2, '0')
        val s = (time % 60).toString().padStart(2, '0')
        tv.text = "$h:$m:$s"
    }

    private fun sign(x: Int): Int {
        return if (x < 0) -1 else if (x == 0) 0 else 1
    }

    private fun updateTimers() {
        timers.forEach { timer ->
            if (!timer.stopped && !timer.paused) {
                val direction = if (timer.originalTime == 0) 1 else -1
                timer.time += direction

                if (timer.time < 0) {
                    timer.time = 0
                }

                if (timer.time >= 0) {
                    // Correct timer if fallen behind/ahead by a few seconds
                    if (timer.lastPlayTime != null && timer.lastPlayTimeValue != null) {
                        val trueTime =
                            clamp(
                                timer.lastPlayTimeValue!! + ((System.currentTimeMillis() - timer.lastPlayTime!!) / 1000L).toInt() * direction,
                                0,
                                99 * 60 * 60
                            )

                        if (kotlin.math.abs(trueTime - timer.time) > 3) {
                            //  More than 3 seconds out, skip a second
                            timer.time += sign(trueTime - timer.time)
                        }
                    }

                    setTime(timer.timer, timer.time)
                }
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mHandler = Handler()
        timerChanger = Runnable {
            if (!stopTimerChanger) {
                updateTimers()
                mHandler!!.postDelayed(timerChanger, 1000)
            }
        }

        val root = inflater.inflate(R.layout.fragment_timers_list, container, false)

        linearLayout = root.findViewById<LinearLayout>(R.id.linlayout)



        root.findViewById<FloatingActionButton>(R.id.fab_new).setOnClickListener {
            DialogNewTimer(true, null, null) { name, hours, minutes, seconds ->
                val t = hours * 60 * 60 + minutes * 60 + seconds
                addTimerUI(null, name, t, t, true, false)

                val sv = root.findViewById(R.id.scroll) as ScrollView
                sv.postDelayed({
                    sv.scrollTo(0, 999999)
                }, 100)
            }.show(fragmentManager!!, null)


        }

        (activity as MainActivity).setToolbarTitle("Timers")
        return root
    }

    private fun addTimerUI(
        id: Long?,
        name: String?,
        initialTime: Int,
        t: Int,
        stopped: Boolean,
        paused: Boolean
    ) {

        val layout =
            LayoutInflater.from(context)
                .inflate(R.layout.timer_list_item, linearLayout, false) as LinearLayout
        linearLayout.addView(layout)

        val timerObj =
            TimerUI(
                id,
                name,
                layout,
                layout.findViewById<TextView>(R.id.timer),
                layout.findViewById<TextView>(R.id.timerName),
                t,
                initialTime,
                paused,
                stopped
            )
        timers.add(timerObj)

        if (!stopped && !paused) {
            timerObj.lastPlayTime = System.currentTimeMillis()
            timerObj.lastPlayTimeValue = t
        }

        val timerName: TextView = layout.findViewById(R.id.timerName)
        val timer: TextView = layout.findViewById(R.id.timer)
        val bPlayPause: ImageView = layout.findViewById(R.id.bPlayPause)
        val bStop: ImageView = layout.findViewById(R.id.bStop)
        val bDelete: ImageView = layout.findViewById(R.id.bDelete)

        timerName.text = name
        setTime(timer, timerObj.time)

        bStop.isEnabled = !stopped
        bStop.alpha = if (stopped) 0.3f else 1.0f

        bDelete.isEnabled = stopped
        bDelete.alpha = if (stopped) 1.0f else 0.3f

        if (paused) {
            bPlayPause.setImageResource(R.drawable.ic_pause)
        }

        bDelete.setOnClickListener {
            timers.remove(timerObj)
            linearLayout.removeView(layout)


            if (timerObj.id != null) {
                DB.deleteTimer(timerObj.id)
            }
        }

        bStop.setOnClickListener {
            timerObj.paused = false
            timerObj.stopped = true
            timerObj.time = timerObj.originalTime
            setTime(timer, timerObj.time)
            bStop.isEnabled = false
            bStop.alpha = 0.3f
            bPlayPause.setImageResource(R.drawable.ic_play)
            bDelete.isEnabled = true
            bDelete.alpha = 1.0f
            timer.setTextColor(0xff606060.toInt())
            timerName.setTextColor(0xff606060.toInt())
        }

        bPlayPause.setOnClickListener {
            if (timerObj.stopped || timerObj.paused) {
                timerObj.lastPlayTime = System.currentTimeMillis()
                timerObj.lastPlayTimeValue = timerObj.time
                bDelete.isEnabled = false
                bDelete.alpha = 0.3f
                timer.setTextColor(0xff000000.toInt())
                timerName.setTextColor(0xff000000.toInt())
            } else {
                timer.setTextColor(0xff606060.toInt())
                timerName.setTextColor(0xff606060.toInt())
            }

            if (timerObj.stopped) {
                timerObj.paused = false
                timerObj.stopped = false
                bStop.isEnabled = true
                bStop.alpha = 1.0f
            } else {
                timerObj.paused = !timerObj.paused
            }

            if (timerObj.paused) {
                bPlayPause.setImageResource(R.drawable.ic_pause)
            } else {
                bPlayPause.setImageResource(R.drawable.ic_play)
            }

        }

        layout.setOnClickListener {
            if (timerObj.stopped) {
                DialogNewTimer(
                    false,
                    timerObj.name,
                    timerObj.originalTime
                ) { name, hours, minutes, seconds ->
                    val t = hours * 60 * 60 + minutes * 60 + seconds
                    timerObj.originalTime = t
                    timerObj.time = t
                    setTime(timerObj.timer, t)
                    timerObj.name = name
                    timerObj.tvTimerName.text = name
                }.show(fragmentManager!!, null)
            }
        }
    }

    override fun onPause() {
        stopTimerChanger = true
        timers.forEach {
            DB.saveOrUpdateTimer(
                Timer(
                    it.id,
                    it.name,
                    if (it.stopped) null else it.time,
                    it.originalTime,
                    System.currentTimeMillis(),
                    it.paused
                )
            )
        }
        scheduleAlarms()
        timers.clear()
        super.onPause()
    }

    override fun onResume() {
        timers.clear()
        clearPendingAlarms(context!!)
        linearLayout.removeAllViews()
        DB.getTimers().forEach {
            val stopped = it.time == null
            val time =
                if (stopped) it.initialTime else {
                    if (it.isPaused) {
                        it.time!!
                    } else {
                        val direction = if (it.initialTime == 0) 1 else -1
                        MathUtils.clamp(
                            it.time!! + ((System.currentTimeMillis() - it.time_saved) / 1000L).toInt() * direction,
                            0,
                            99 * 60 * 60
                        )
                    }
                }

            addTimerUI(it.id, it.name, it.initialTime, time, stopped, it.isPaused)
        }


        stopTimerChanger = false
        timerChanger.run()
        super.onResume()
    }


    companion object {
        fun clearPendingAlarms(context: Context) {
            var i = -2
            var minReqCode = Settings.lowestAlarmID(context.applicationContext)
            while (i >= minReqCode) {
                val intent =
                    Intent(context.applicationContext, AlarmActivity::class.java)
                val intent2 =
                    PendingIntent.getActivity(context, i, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                EventSchedule.clearEvent(context.applicationContext, intent2)
                i -= 1
            }
            Settings.resetLowestAlarmID(context)
        }
    }

    private fun scheduleAlarms() {
        if (!Settings.getAlarmsEnabled(context!!)) {
            return
        }

        clearPendingAlarms(context!!)

        timers.forEach { timer ->
            if (!timer.stopped && !timer.paused && timer.originalTime > 0 && timer.time > 0) {
                val intent = Intent(context!!.applicationContext, AlarmActivity::class.java)
                intent.putExtra("timerName", timer.name)

                val intent2 =
                    PendingIntent.getActivity(
                        context!!,
                        Settings.getAlarmID(context!!.applicationContext),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )

                EventSchedule.scheduleEvent(
                    context!!,
                    System.currentTimeMillis() + timer.time * 1000,
                    intent2
                )
            }
        }


    }
}
