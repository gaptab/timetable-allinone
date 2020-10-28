package danielabbott.personalorganiser.ui.goals

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import danielabbott.personalorganiser.MainActivity
import danielabbott.personalorganiser.Notifications
import danielabbott.personalorganiser.R
import danielabbott.personalorganiser.data.DB
import danielabbott.personalorganiser.data.Goal
import danielabbott.personalorganiser.data.Milestone
import danielabbott.personalorganiser.ui.DataEntryFragment
import danielabbott.personalorganiser.ui.LimitedColourPickerView

class EditGoalFragment(private val goalId: Long?) : DataEntryFragment() {


    private var colour = LimitedColourPickerView.colours[0]
    private lateinit var goalColourView: LinearLayout

    private var newMilestones = ArrayList<Milestone>()
    private var milestonesToRemove = ArrayList<Milestone>()
    private var milestonesChanged = ArrayList<Milestone>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_edit_goal, container, false)
        (activity!! as MainActivity).setToolbarTitle("Edit Goal")

        super.init(root)

        val goalName = root.findViewById<TextView>(R.id.goalName)
        val nameWithColour = root.findViewById<TextView>(R.id.goalName)
        val name = root.findViewById<EditText>(R.id.name)
        val notes = root.findViewById<EditText>(R.id.notes)
        val colourChangeButton = root.findViewById<Button>(R.id.setColour)
        goalColourView = root.findViewById<LinearLayout>(R.id.goalNameColour)
        val recyclerView = root.findViewById<RecyclerView>(R.id.list)
        picturePreviewsView = root.findViewById<LinearLayout>(R.id.PicturePreviews)


        // Load data
        if (goalId != null) {
            val e = DB.getGoal(goalId)

            colour = e.colour

            notes.setText(e.notes ?: "")
            goalName.text = e.name
            name.setText(e.name)

        }

        // Milestones list

        // Data
        val milestonesArray = ArrayList<Milestone>()
        if (goalId != null) {
            milestonesArray.addAll(DB.getMilestones(goalId))
        }

        val recyclerViewOriginalHeight = recyclerView.layoutParams.height
        if (milestonesArray.isEmpty()) {
            recyclerView.layoutParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0)
        }

        // Initialise RecyclerView
        with(recyclerView) {
            layoutManager = LinearLayoutManager(context)

            adapter = MilestoneRecyclerViewAdapter(milestonesArray,

                // ON CLICK
                {
                    // On single click, show modify dialog
                    MilestoneDialog({ name: String, deadline: Long? ->
                        unsavedData = true
                        it.name = name
                        it.deadline = deadline
                        if (it.id >= 0) {
                            milestonesChanged.add(it)
                        }
                        (adapter!! as MilestoneRecyclerViewAdapter).update()
                    }, it.name, it.deadline).show(fragmentManager!!, null)
                },

                // ON LONG CLICK
                {
                    // On long tap, delete (confirm first)
                    android.app.AlertDialog.Builder(context)
                        .setTitle("Delete milestone")
                        .setMessage("Delete milestone '${it.name}'?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Delete") { _, _ ->
                            unsavedData = true
                            if (it.id >= 0) {
                                milestonesChanged.remove(it)
                                milestonesToRemove.add(it)
                            } else {
                                newMilestones.remove(it)
                            }
                            if ((adapter as MilestoneRecyclerViewAdapter).remove(it) == 0) {
                                recyclerView.layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    0
                                )
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                })
        }

        if (goalId == null) {
            findUnusedColour()
        }
        setGradient()


        // Button for setting colour of goal
        colourChangeButton.setOnClickListener {
            var colourPicker = LimitedColourPickerView(context!!)

            val builder = AlertDialog.Builder(activity!!)
                .setView(colourPicker)
                .show()

            colourPicker.setOnClickListener {
                if (colourPicker.selectedColour != null) {
                    // A colour was picked

                    super.unsavedData = true
                    colour = colourPicker.selectedColour!!
                    setGradient()
                }
                builder.dismiss()
            }

        }

        // Save button
        val fab: FloatingActionButton = root.findViewById(R.id.fab_save)
        fab.setOnClickListener { _ ->
            if (name.text.isEmpty()) {
                android.app.AlertDialog.Builder(context)
                    .setTitle("Invalid data")
                    .setMessage("Goal name cannot be blank")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Okay", null)
                    .show()
            } else {
                // Data is valid, update/insert in database

                var e = Goal(
                    goalId ?: -1,
                    name.text.toString(),
                    colour,
                    if (notes.text.isEmpty()) null else notes.text.toString()
                )

                val id = DB.updateOrCreateGoal(e)

                // Add/remove milestones

                newMilestones.forEach {
                    try {
                        it.goal_id = id
                        DB.addMilestone(it)
                    } catch (_: Exception) {
                    }
                }

                milestonesToRemove.forEach {
                    try {
                        DB.removeMilestone(it)
                    } catch (_: Exception) {
                    }
                }

                milestonesChanged.forEach {
                    try {
                        DB.updateMilestone(it)
                    } catch (_: Exception) {
                    }
                }

                // Add/remove pictures

                newPhotos.forEach {
                    try {
                        DB.addGoalPhoto(id, it)
                    } catch (_: Exception) {
                    }
                }

                imagesToRemove.forEach {
                    try {
                        DB.removeGoalPhoto(id, it)
                    } catch (_: Exception) {
                    }
                }

                (activity!! as MainActivity).hideKeyboard()

                super.unsavedData = false
                (activity as MainActivity).onBackPressed()


            }

        }

        val deleteButton = root.findViewById(R.id.bDelete) as Button
        deleteButton.visibility = if (goalId != null) View.VISIBLE else View.INVISIBLE


        if (goalId != null) {
            deleteButton.setOnClickListener {
                showDeleteDialog(context!!, goalId) {
                    super.unsavedData = false
                    (activity as MainActivity).onBackPressed()
                }
            }


            // Get images in background
            object : Thread() {
                override fun run() {
                    DB.getGoalPhotos(goalId).forEach {
                        addImage(Uri.parse(it))
                    }
                }
            }.start()
        }

        root.findViewById<Button>(R.id.addMilestone).setOnClickListener {
            MilestoneDialog({ name: String, date: Long? ->
                unsavedData = true
                val m = Milestone(-1, name, date, goalId ?: -1)
                newMilestones.add(m)
                if ((recyclerView.adapter as MilestoneRecyclerViewAdapter).add(m) == 1) {
                    val sv = root.findViewById(R.id.scroll) as NestedScrollView
                    sv.scrollTo(0, sv.bottom)
                }

                recyclerView.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    recyclerViewOriginalHeight
                )
            }, null, null).show(fragmentManager!!, null)
        }

        name.addTextChangedListener {
            super.unsavedData = true
            nameWithColour.text = name.text
        }
        notes.addTextChangedListener {
            super.unsavedData = true
        }

        return root
    }

    companion object {

        private fun doDelete(
            deleteEvents: Boolean,
            goalId: Long,
            context: Context,
            onDelete: () -> Unit
        ) {
            DB.deleteGoal(goalId, deleteEvents)
            if (deleteEvents) {
                Notifications.scheduleAllNotifications(context.applicationContext)
            }
            onDelete()
        }

        fun showDeleteDialog(
            context: Context,
            goalId: Long,
            onDelete: () -> Unit
        ) {
            android.app.AlertDialog.Builder(context)
                .setTitle("Delete goal")
                .setMessage("Are you sure you want to delete this goal? This cannot be undone.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Delete") { _, _ ->
                    if (DB.goalHasAssociatedEventsOrTasks(goalId)) {
                        android.app.AlertDialog.Builder(context)
                            .setTitle("Delete event")
                            .setMessage(
                                "Do you want to delete any associated timetable events or " +
                                        "To Do list tasks?"
                            )
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton("Delete events/tasks") { _, _ ->
                                doDelete(true, goalId, context, onDelete)
                            }
                            .setNegativeButton("Keep events/tasks") { _, _ ->
                                doDelete(false, goalId, context, onDelete)
                            }
                            .show()
                    } else {
                        doDelete(true, goalId, context, onDelete)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun findUnusedColour() {
        val colours = DB.allUsedGoalColours()


        var i: Int = 0
        while (i < LimitedColourPickerView.colours.count() * 3) {
            val c = LimitedColourPickerView.colours[i % LimitedColourPickerView.colours.count()]
            if (!colours.contains(c)) {
                colour = c
                return
            }
            i += 3
        }
    }

    // Sets the colour at the top of the page
    private fun setGradient() {
        val gradient = GradientDrawable()
        val c = IntArray(2) { 0xffffff }
        c[0] = colour or 0xff000000.toInt()
        gradient.colors = c
        gradient.gradientType = GradientDrawable.LINEAR_GRADIENT
        goalColourView.background = gradient
    }
}