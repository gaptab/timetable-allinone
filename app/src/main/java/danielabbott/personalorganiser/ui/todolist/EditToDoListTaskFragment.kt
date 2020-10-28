package danielabbott.personalorganiser.ui.todolist

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.widget.ShareActionProvider
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.MenuItemCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import danielabbott.personalorganiser.DateTimeUtil
import danielabbott.personalorganiser.MainActivity
import danielabbott.personalorganiser.Notifications
import danielabbott.personalorganiser.R
import danielabbott.personalorganiser.data.DB
import danielabbott.personalorganiser.data.Repeat
import danielabbott.personalorganiser.data.ToDoListTask
import danielabbott.personalorganiser.ui.DataEntryFragment
import danielabbott.personalorganiser.ui.DateSelectView
import danielabbott.personalorganiser.ui.SpinnerChangeDetector
import danielabbott.personalorganiser.ui.TimeSelectView

class EditToDoListTaskFragment(val taskId: Long?) : DataEntryFragment() {

    private lateinit var dateForwards: Button
    private lateinit var dateBack: Button
    private lateinit var name: EditText
    private lateinit var notes: EditText
    private lateinit var time: TimeSelectView
    private lateinit var date: DateSelectView
    private lateinit var repeat: Spinner
    private lateinit var r30: SwitchCompat
    private lateinit var r1: SwitchCompat
    private lateinit var r2: SwitchCompat
    private lateinit var rMorn: SwitchCompat

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        DB.init(context!!)
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_edit_to_do_list_task, container, false)
        (activity!! as MainActivity).setToolbarTitle("Edit Task")
        setHasOptionsMenu(true)

        notes = root.findViewById<EditText>(R.id.notes)
        name = root.findViewById<EditText>(R.id.name)
        repeat = root.findViewById<Spinner>(R.id.repeat)
        r30 = root.findViewById<SwitchCompat>(R.id.r30)
        r1 = root.findViewById<SwitchCompat>(R.id.r1)
        r2 = root.findViewById<SwitchCompat>(R.id.r2)
        rMorn = root.findViewById<SwitchCompat>(R.id.rMorn)
        time = root.findViewById<TimeSelectView>(R.id.time)
        date = root.findViewById<DateSelectView>(R.id.date)
        dateForwards = root.findViewById<Button>(R.id.dateForwards)
        dateBack = root.findViewById<Button>(R.id.dateBack)
        val goal = root.findViewById<Spinner>(R.id.goal)

        super.init(root)
        super.initGoals(goal)


        time.setOnClickListener {
            super.unsavedData = true
        }
        time.setOnLongClickListener {
            super.unsavedData = true
            true
        }


        date.setOnClickListener {
            super.unsavedData = true
        }

        var originalTaskData: ToDoListTask? = null

        if (taskId != null) {
            // Load the task from the database

            try {
                originalTaskData = DB.getToDoListTask(taskId)
            } catch (e: Exception) {
                val fragmentTransaction = fragmentManager!!.beginTransaction()
                fragmentTransaction.replace(R.id.fragmentView, ToDoListFragment())
                fragmentTransaction.commit()
                return root
            }

            notes.setText(if (originalTaskData.notes == null) "" else originalTaskData.notes!!)
            name.setText(originalTaskData.name)
            repeat.setSelection(originalTaskData.repeat.n)
            r30.isChecked = originalTaskData.remind30Mins
            r1.isChecked = originalTaskData.remind1Hr
            r2.isChecked = originalTaskData.remind2Hrs
            rMorn.isChecked = originalTaskData.remindMorning
            super.setGoalSpinner(goal, originalTaskData.goal_id)

            if (originalTaskData.repeat != Repeat.NEVER && originalTaskData.dateTime != null) {
                dateForwards.visibility = View.VISIBLE
                dateBack.visibility = View.VISIBLE
            }
            else {
                dateForwards.visibility = View.INVISIBLE
                dateBack.visibility = View.INVISIBLE
            }

            if (originalTaskData.dateTime != null) {
                if (originalTaskData.hasTime) {
                    val t = DateTimeUtil.getHoursAndMinutes(originalTaskData.dateTime!!)
                    time.setTime(t.first, t.second)
                }
                val d = DateTimeUtil.getYearMonthDay(originalTaskData.dateTime!!)
                date.setDate(d.first, d.second, d.third)
            }
        }
        else {
            dateForwards.visibility = View.INVISIBLE
            dateBack.visibility = View.INVISIBLE
        }

        picturePreviewsView = root.findViewById<LinearLayout>(R.id.PicturePreviews)

        // Save button
        val fab: FloatingActionButton = root.findViewById(R.id.fab_save)
        fab.setOnClickListener { _ ->
            val nameString = name.text.toString()

            if (nameString.isEmpty()) {
                AlertDialog.Builder(context)
                    .setTitle("Invalid data")
                    .setMessage("Task name cannot be blank")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Okay", null)
                    .show()
            } else {
                // Data is valid, update/insert in database

                val notes = notes.text

                if (!date.dateSelected && time.timeSelected) {
                    date.setDate(System.currentTimeMillis())
                }

                val dateTime = if (date.dateSelected) DateTimeUtil.getDateTimeMillis(
                    date.year,
                    date.month,
                    date.day,
                    if (time.timeSelected) time.hour else 23,
                    if (time.timeSelected) time.minute else 59
                )
                else null

                var newTask = ToDoListTask(
                    taskId ?: -1,
                    dateTime,
                    time.timeSelected,
                    nameString,
                    if (notes.isEmpty()) null else notes.toString(),
                    r30.isChecked,
                    r1.isChecked,
                    r2.isChecked,
                    rMorn.isChecked,
                    Repeat.fromInt(repeat.selectedItemPosition),
                    if (goal.selectedItemPosition == 0) null else goals[goal.selectedItemPosition - 1].id
                )

                val eventId = DB.updateOrCreateToDoListTask(newTask)

                // Add/remove pictures

                newPhotos.forEach {
                    try {
                        DB.addToDoListTaskPhoto(eventId, it)
                    } catch (_: Exception) {
                    }
                }

                imagesToRemove.forEach {
                    try {
                        DB.removeToDoListTaskPhoto(eventId, it)
                    } catch (_: Exception) {
                    }
                }

                // Reschedule all notifications

                var needToRescheduleNotifications = true

                if(!newTask.remind30Mins && !newTask.remind1Hr && !newTask.remind2Hrs && !newTask.remindMorning) {
                    if (taskId == null || (!originalTaskData!!.remind30Mins && !originalTaskData.remind1Hr && !originalTaskData.remind2Hrs && !originalTaskData.remindMorning)){
                        needToRescheduleNotifications = false
                    }
                }

                if(needToRescheduleNotifications) {
                    Notifications.scheduleForTask(context!!, newTask, taskId == null)
                    //Notifications.scheduleAllNotifications(activity!!.applicationContext)
                }

                (activity!! as MainActivity).hideKeyboard()

                super.unsavedData = false
                (activity as MainActivity).onBackPressed()
            }

        }

        val deleteButton = root.findViewById(R.id.bDelete) as Button
        deleteButton.visibility = if (taskId != null) View.VISIBLE else View.INVISIBLE
        if (taskId != null) {
            deleteButton.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Delete task")
                    .setMessage("Are you sure you want to delete this task? This cannot be undone.")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Delete") { _, _ ->
                        DB.deleteToDoListTask(taskId)
                        Notifications.unscheduleNotificationsForTask(context!!, taskId)
                        super.unsavedData = false
                        (activity!! as MainActivity).hideKeyboard()
                        (activity as MainActivity).onBackPressed()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            // Get images in background
            object : Thread() {
                override fun run() {
                    DB.getToDoListTaskPhotos(taskId).forEach {
                        addImage(Uri.parse(it))
                    }
                }
            }.start()
        }

        date.onDateChange = { _, _, _ ->
            setDateChangeButtonsVisibility()
            setNotificationCheckboxesEnabledState()
        }

        time.onTimeChanged = { _: Int, _: Int ->
            if (!date.dateSelected) {
                date.setDate(System.currentTimeMillis())
            }
            setNotificationCheckboxesEnabledState()
        }

        time.onTimeCleared = {
            setNotificationCheckboxesEnabledState()
        }

        date.onDateCleared = {
            setNotificationCheckboxesEnabledState()
        }

        setNotificationCheckboxesEnabledState()


        dateForwards.setOnClickListener {
            unsavedData = true
            val rep = Repeat.fromInt(repeat.selectedItemPosition)
            if (date.dateSelected && rep != Repeat.NEVER) {
                var ymd: Triple<Int, Int, Int>

                if (rep == Repeat.MONTHLY) {
                    var m = date.month + 1 // Add month
                    var yr = date.year

                    // If gone past december, go to january of next year
                    if (m > 12) {
                        m = 1
                        yr++
                    }

                    val ms = DateTimeUtil.getDateTimeMillis(yr, m, date.day, 6, 0)
                    ymd = DateTimeUtil.getYearMonthDay(ms)
                } else {
                    var m = DateTimeUtil.getDateTimeMillis(date.year, date.month, date.day, 6, 0)
                    m += 24 * 60 * 60 * 1000 * rep.days()
                    ymd = DateTimeUtil.getYearMonthDay(m)
                }
                date.setDate(ymd.first, ymd.second, ymd.third)
            }
        }

        dateBack.setOnClickListener {
            unsavedData = true
            val rep = Repeat.fromInt(repeat.selectedItemPosition)
            if (date.dateSelected && rep != Repeat.NEVER) {
                var ymd: Triple<Int, Int, Int>

                if (rep == Repeat.MONTHLY) {
                    var m = date.month - 1 // Add month
                    var yr = date.year

                    // If gone before january, go to december of previous year
                    if (m < 1) {
                        m = 12
                        yr--
                    }

                    val ms = DateTimeUtil.getDateTimeMillis(yr, m, date.day, 6, 0)
                    ymd = DateTimeUtil.getYearMonthDay(ms)
                } else {
                    var m = DateTimeUtil.getDateTimeMillis(date.year, date.month, date.day, 6, 0)
                    m -= 24 * 60 * 60 * 1000 * rep.days()
                    ymd = DateTimeUtil.getYearMonthDay(m)
                }
                date.setDate(ymd.first, ymd.second, ymd.third)
            }
        }

        date.setOnLongClickListener {
            dateForwards.visibility = View.INVISIBLE
            dateBack.visibility = View.INVISIBLE
            time.reset()
            super.unsavedData = true
            true
        }

        val unsavedCL = { _: View ->
            super.unsavedData = true
        }

        name.addTextChangedListener {
            super.unsavedData = true
            updateShareActionProvider()
        }
        notes.addTextChangedListener {
            super.unsavedData = true
            updateShareActionProvider()
        }

        repeat.onItemSelectedListener =
            SpinnerChangeDetector {
                super.unsavedData = true
                setDateChangeButtonsVisibility()
                setNotificationCheckboxesEnabledState()
            }
        goal.onItemSelectedListener = SpinnerChangeDetector { super.unsavedData = true }
        r30.setOnClickListener(unsavedCL)
        r1.setOnClickListener(unsavedCL)
        r2.setOnClickListener(unsavedCL)
        rMorn.setOnClickListener(unsavedCL)

        return root
    }

    private fun setNotificationCheckboxesEnabledState() {
        val rep = Repeat.fromInt(repeat.selectedItemPosition)
        if (time.timeSelected) {
            r30.isEnabled = true
            r1.isEnabled = true
            r2.isEnabled = true
        } else {
            r30.isEnabled = false
            r1.isEnabled = false
            r2.isEnabled = false
            r30.isChecked = false
            r1.isChecked = false
            r2.isChecked = false
        }

        if (date.dateSelected || rep == Repeat.DAILY) {
            rMorn.isEnabled = true
        } else {
            rMorn.isEnabled = false
            rMorn.isChecked = false
        }
    }

    private fun setDateChangeButtonsVisibility() {
        val rep = Repeat.fromInt(repeat.selectedItemPosition)
        if (rep == Repeat.NEVER || !date.dateSelected) {
            dateForwards.visibility = View.INVISIBLE
            dateBack.visibility = View.INVISIBLE
        } else {
            dateForwards.visibility = View.VISIBLE
            dateBack.visibility = View.VISIBLE
        }
    }


    lateinit var shareMenuItem: MenuItem
    lateinit var shareIntent: Intent

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()

        inflater.inflate(R.menu.share_action_menu_item, menu)
        shareMenuItem = menu.findItem(R.id.action_share)
        val sap = MenuItemCompat.getActionProvider(shareMenuItem) as ShareActionProvider

        shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        updateShareActionProvider()
        sap.setShareIntent(shareIntent)

        activity!!.onCreateOptionsMenu(menu)
    }

    private fun updateShareActionProvider() {
        var t = name.text.toString() + '\n'
        if (date.dateSelected || time.timeSelected) {
            if (date.dateSelected) {
                t += DateTimeUtil.getDateString(context!!, date.getDate()!!) + ' '
            }
            if (time.timeSelected) {
                t += DateTimeUtil.getTimeString(time.getTime()!!)
            }
            t += '\n'
        }
        t += notes.text.toString()

        shareIntent.putExtra(
            Intent.EXTRA_TEXT,
            t
        )
    }

}
