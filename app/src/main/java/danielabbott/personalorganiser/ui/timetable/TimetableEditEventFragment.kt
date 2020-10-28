package danielabbott.personalorganiser.ui.timetable

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import danielabbott.personalorganiser.MainActivity
import danielabbott.personalorganiser.Notifications
import danielabbott.personalorganiser.R
import danielabbott.personalorganiser.data.DB
import danielabbott.personalorganiser.data.TimetableEvent
import danielabbott.personalorganiser.ui.DataEntryFragment
import danielabbott.personalorganiser.ui.SpinnerChangeDetector
import danielabbott.personalorganiser.ui.TimeSelectView


class TimetableEditEventFragment(
    private val timetableId: Long,
    private val eventId: Long?,

    // These values are only used if eventId == null
    private val startTime_: Int = 0,
    private val endTime_: Int = 0,
    private val days_: Int = 0
) : DataEntryFragment() {

    val dayCheckboxes = ArrayList<SwitchCompat>(7)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        DB.init(context!!)
        var root = inflater.inflate(R.layout.fragment_timetable_edit_event, container, false)
        (activity!! as MainActivity).setToolbarTitle("Edit Timetable Event")

        val notes = root.findViewById<EditText>(R.id.notes)
        val name = root.findViewById<EditText>(R.id.name)

        dayCheckboxes.clear()
        dayCheckboxes.add(root.findViewById<SwitchCompat>(R.id.monday))
        dayCheckboxes.add(root.findViewById<SwitchCompat>(R.id.tuesday))
        dayCheckboxes.add(root.findViewById<SwitchCompat>(R.id.wednesday))
        dayCheckboxes.add(root.findViewById<SwitchCompat>(R.id.thursday))
        dayCheckboxes.add(root.findViewById<SwitchCompat>(R.id.friday))
        dayCheckboxes.add(root.findViewById<SwitchCompat>(R.id.saturday))
        dayCheckboxes.add(root.findViewById<SwitchCompat>(R.id.sunday))


        val r30 = root.findViewById<SwitchCompat>(R.id.r30)
        val r1 = root.findViewById<SwitchCompat>(R.id.r1)
        val r2 = root.findViewById<SwitchCompat>(R.id.r2)
        val rMorn = root.findViewById<SwitchCompat>(R.id.rMorn)
        val tvStart = root.findViewById<TimeSelectView>(R.id.tvStart)
        val tvEnd = root.findViewById<TimeSelectView>(R.id.tvEnd)
        val goal = root.findViewById<Spinner>(R.id.goal)

        picturePreviewsView = root.findViewById<LinearLayout>(R.id.PicturePreviews)

        super.init(root)
        super.initGoals(goal)




        var startTime: Int
        var endTime: Int

        var originalEventData: TimetableEvent? = null
        if (eventId != null) {
            // Fill widgets with data in database

            try {
                originalEventData = DB.getTimetableEvent(eventId)
            } catch (e: Exception) {
                val fragmentTransaction = fragmentManager!!.beginTransaction()
                fragmentTransaction.replace(R.id.fragmentView, TimetableFragment())
                fragmentTransaction.commit()
                return root
            }

            setDayCheckBoxes(originalEventData.days)

            notes.setText(if (originalEventData.notes == null) "" else originalEventData.notes!!)
            name.setText(originalEventData.name)
            r30.isChecked = originalEventData.remind30Mins
            r1.isChecked = originalEventData.remind1Hr
            r2.isChecked = originalEventData.remind2Hrs
            rMorn.isChecked = originalEventData.remindMorning

            super.setGoalSpinner(goal, originalEventData.goal_id)

            startTime = originalEventData.startTime
            endTime = originalEventData.startTime + originalEventData.duration


            // Get images in background
            object : Thread() {
                override fun run() {
                    DB.getTimetableEventPhotos(eventId).forEach {
                        addImage(Uri.parse(it))
                    }
                }
            }.start()
        } else {
            // Use values passed as arguments to constructor
            startTime = startTime_
            endTime = endTime_
            setDayCheckBoxes(days_)
        }

        // Save button
        val fab: FloatingActionButton = root.findViewById(R.id.fab_save)
        fab.setOnClickListener { _ ->
            if (!tvStart.timeSelected) {
                AlertDialog.Builder(context)
                    .setTitle("Invalid data")
                    .setMessage("No start time selected")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Okay", null)
                    .show()
            } else if (!tvEnd.timeSelected) {
                AlertDialog.Builder(context)
                    .setTitle("Invalid data")
                    .setMessage("No end time selected")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Okay", null)
                    .show()
            } else {

                val startTimes = tvStart.text.split(":")
                val endTimes = tvEnd.text.split(":")

                var atLeastOneDay = false
                dayCheckboxes.forEach {
                    if (it.isChecked) {
                        atLeastOneDay = true
                    }
                }

                startTime = startTimes[0].toInt() * 60 + startTimes[1].toInt()
                endTime = endTimes[0].toInt() * 60 + endTimes[1].toInt()

                val nameString = name.text.toString()

                if (startTime == endTime) {
                    AlertDialog.Builder(context)
                        .setTitle("Invalid data")
                        .setMessage("Start and end time cannot be the same")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Okay", null)
                        .show()
                } else if (!atLeastOneDay) {
                    AlertDialog.Builder(context)
                        .setTitle("Invalid data")
                        .setMessage("No days selected")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Okay", null)
                        .show()
                } else if (startTime >= endTime) {
                    AlertDialog.Builder(context)
                        .setTitle("Invalid data")
                        .setMessage("Start time is after end time")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Okay", null)
                        .show()
                } else if (endTime - startTime < 10) {
                    AlertDialog.Builder(context)
                        .setTitle("Invalid data")
                        .setMessage("Minimum event duration is 10 minutes")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Okay", null)
                        .show()
                } else if (nameString.isEmpty()) {
                    AlertDialog.Builder(context)
                        .setTitle("Invalid data")
                        .setMessage("Event name cannot be blank")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Okay", null)
                        .show()
                } else {
                    // Data is valid, update/insert in database

                    var daysBitmask = 0
                    dayCheckboxes.forEachIndexed { i: Int, checkBox: SwitchCompat ->
                        if (checkBox.isChecked) {
                            daysBitmask = daysBitmask or (1 shl i)
                        }
                    }

                    var newEvent = TimetableEvent(
                        eventId ?: -1,
                        timetableId,
                        startTime,
                        endTime - startTime,
                        daysBitmask,
                        nameString,
                        if (notes.text.isEmpty()) null else notes.text.toString(),
                        r30.isChecked,
                        r1.isChecked,
                        r2.isChecked,
                        rMorn.isChecked,
                        if (goal.selectedItemPosition == 0) null else goals[goal.selectedItemPosition - 1].id
                    )

                    val eventId2 = DB.updateOrCreateTimetableEvent(newEvent)

                    // Add/remove pictures

                    newPhotos.forEach {
                        try {
                            DB.addTimetableEventPhoto(eventId2, it)
                        } catch (_: Exception) {
                        }
                    }

                    imagesToRemove.forEach {
                        try {
                            DB.removeTimetableEventPhoto(eventId2, it)
                        } catch (_: Exception) {
                        }
                    }

                    // Reschedule all notifications

                    var needToRescheduleNotifications = true

                    if(!newEvent.remind30Mins && !newEvent.remind1Hr && !newEvent.remind2Hrs && !newEvent.remindMorning) {
                        if (eventId == null || (!originalEventData!!.remind30Mins && !originalEventData.remind1Hr && !originalEventData.remind2Hrs && !originalEventData.remindMorning)){
                            needToRescheduleNotifications = false
                        }
                    }

                    if(needToRescheduleNotifications) {
                        Notifications.scheduleForTTEvent(context!!, newEvent, eventId == null)
                    }

                    (activity!! as MainActivity).hideKeyboard()

                    unsavedData = false
                    (activity as MainActivity).onBackPressed()
                }
            }

        }

        val deleteButton = root.findViewById(R.id.bDelete) as Button
        deleteButton.visibility = if (eventId != null) View.VISIBLE else View.INVISIBLE
        if (eventId != null) {
            deleteButton.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Delete event")
                    .setMessage("Are you sure you want to delete this event? This cannot be undone.")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Delete") { _, _ ->
                        Notifications.unscheduleNotificationsForTTEvent(context!!, eventId)
                        DB.deleteTimetableEvent(eventId)
                        super.unsavedData = false
                        (activity!! as MainActivity).hideKeyboard()
                        (activity as MainActivity).onBackPressed()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        // Time


        tvStart.setTime(startTime / 60, startTime % 60)
        tvEnd.setTime(endTime / 60, endTime % 60)

        tvStart.setOnClickListener {
            unsavedData = true
        }

        tvEnd.setOnClickListener {
            unsavedData = true
        }


        val unsavedCL = { _: View ->
            unsavedData = true
        }

        name.addTextChangedListener { unsavedData = true }
        goal.onItemSelectedListener =
            SpinnerChangeDetector(unsavedCL)
        notes.addTextChangedListener { unsavedData = true }
        r30.setOnClickListener(unsavedCL)
        r1.setOnClickListener(unsavedCL)
        r2.setOnClickListener(unsavedCL)
        rMorn.setOnClickListener(unsavedCL)

        dayCheckboxes.forEach {
            it.setOnClickListener(unsavedCL)
        }

        return root
    }


    private fun setDayCheckBoxes(bitMask: Int) {
        for (i in 0..6) {
            dayCheckboxes[i].isChecked = (bitMask and (1 shl i)) != 0
        }
    }
}
