package danielabbott.personalorganiser.ui.todolist


import android.app.AlertDialog
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import danielabbott.personalorganiser.DateTimeUtil
import danielabbott.personalorganiser.MainActivity
import danielabbott.personalorganiser.Notifications
import danielabbott.personalorganiser.R
import danielabbott.personalorganiser.data.DB
import danielabbott.personalorganiser.data.DB.context
import danielabbott.personalorganiser.data.ToDoListTaskListData
import kotlinx.android.synthetic.main.to_do_list_item.view.*

// List of tasks
class ToDoListRecyclerViewAdapter(
    // For switching fragments (pages)
    private val parentFragmentManager: FragmentManager,
    private val activity: MainActivity
) : RecyclerView.Adapter<ToDoListRecyclerViewAdapter.ViewHolder>() {

    // An item in the recycler view - either a task or a section header
    inner class Item(
        val id: Long,

        // If null then this is a task rather than a section header
        val header_title: String?,
        val name: String?,
        val dateTime: Long?,
        val hasTime: Boolean,
        val colour: Int,
        val hasNotes: Boolean
    )

    private var items = ArrayList<Item>()

    constructor(
        parentFragmentManager: FragmentManager,
        activity: MainActivity,
        v: List<ToDoListTaskListData>
    ) : this(parentFragmentManager, activity) {
        val today = DateTimeUtil.getDaySinceEpoch(System.currentTimeMillis())

        var addedNoTimeHeader = false
        var addedOverdueHeader = false
        var addedTodayHeader = false
        var addedTomorrowHeader = false
        var addedLaterHeader = false

        v.forEach {
            // Time now

            if (it.dateTime == null) {
                if (!addedNoTimeHeader) {
                    items.add(Item(it.id, "No Deadline", null, null, false, 0, false))
                    addedNoTimeHeader = true
                }
            } else {
                val taskDay = DateTimeUtil.getDaySinceEpoch(it.dateTime!!)

                if (it.dateTime!! < System.currentTimeMillis()) {
                    if (!addedOverdueHeader) {
                        items.add(Item(it.id, "Overdue", null, null, false, 0, false))
                        addedOverdueHeader = true
                    }
                } else if (taskDay == today) {
                    if (!addedTodayHeader) {
                        items.add(Item(it.id, "Today", null, null, false, 0, false))
                        addedTodayHeader = true
                    }
                } else if (taskDay == today + 1) {
                    if (!addedTomorrowHeader) {
                        items.add(Item(it.id, "Tomorrow", null, null, false, 0, false))
                        addedTomorrowHeader = true
                    }
                } else {
                    if (!addedLaterHeader) {
                        items.add(Item(it.id, "Later", null, null, false, 0, false))
                        addedLaterHeader = true
                    }
                }
            }
            val colour = it.colour ?: 0xffffff
            items.add(
                Item(
                    it.id,
                    null,
                    it.name,
                    it.dateTime,
                    it.hasTime,
                    (colour and 0xffffff) or 0x80000000.toInt(),
                    it.hasNotes
                )
            )
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == 0) {
            // Task
            ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.to_do_list_item, parent, false)
            )
        } else {
            // Section header
            ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.to_do_list_item_section_header, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == items.size) {
            // Extra space at bottom (empty View)
            holder.colourLayout.setBackgroundColor(0x00ffffff)
            holder.name.text = "";
            holder.dateTime?.text = ""
            holder.divider?.visibility = View.INVISIBLE
            holder.dots?.visibility = View.INVISIBLE
            holder.layout.setOnClickListener(null)
            holder.layout.setOnLongClickListener(null)
            return
        }
        holder.colourLayout.setBackgroundResource(R.drawable.rounded_corners)

        val item = items[position]
        if (item.header_title == null) {
            // Task (not section header)

            //holder.colourLayout.setBackgroundColor(item.colour)
            holder.colourLayout.background!!.colorFilter =
                PorterDuffColorFilter(item.colour, PorterDuff.Mode.MULTIPLY)

            holder.divider?.visibility = View.INVISIBLE

            holder.name.text = item.name

            if (item.dateTime == null) {
                holder.dateTime!!.text = ""
            } else {
                val date = DateTimeUtil.getDateString(context, item.dateTime)
                if (item.hasTime) {
                    val time = DateTimeUtil.getTimeString(item.dateTime)
                    holder.dateTime!!.text = "$date $time"
                } else {
                    holder.dateTime!!.text = date
                }
            }

            if (holder.dots != null) {
                holder.dots.visibility = if (item.hasNotes) View.VISIBLE else View.INVISIBLE;
            }

            holder.layout.setOnClickListener {
                // When clicked, open edit page for the task
                val fragment = EditToDoListTaskFragment(item.id)
                val fragmentTransaction = parentFragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.fragmentView, fragment).addToBackStack(null)
                fragmentTransaction.commit()
            }

            holder.layout.setOnLongClickListener {
                // Delete the task (after asking for confirmation)

                // Short form of the task name
                var name = item.name!!
                if (name.length > 20) {
                    name = name.substring(0, 17) + "..."
                }

                AlertDialog.Builder(activity)
                    .setTitle("Delete task")
                    .setMessage("Are you sure you want to delete the task '$name'? This cannot be undone.")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Delete") { _, _ ->
                        DB.deleteToDoListTask(item.id)
                        Notifications.unscheduleNotificationsForTask(activity.applicationContext, item.id)

                        // Reload page
                        val f = parentFragmentManager.fragments[0]
                        parentFragmentManager
                            .beginTransaction()
                            .detach(f)
                            .attach(f)
                            .commit();
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
        } else {
            holder.name.text = item.header_title
            //holder.colourLayout.setBackgroundColor(0x00ffffff)
            holder.colourLayout.background!!.colorFilter =
                PorterDuffColorFilter(0xffffffff.toInt(), PorterDuff.Mode.MULTIPLY)
            holder.layout.setOnClickListener(null)
            holder.layout.setOnLongClickListener(null)
            if (position == 0) {
                holder.divider?.visibility = View.INVISIBLE
            } else {
                holder.divider?.visibility = View.VISIBLE
            }
        }

    }

    override fun getItemCount(): Int = items.size + 1

    override fun getItemViewType(position: Int): Int {
        // 0=task, 1=section header
        // The value returned determines which layout will be inflated in onCreateViewHolder

        return if (position == items.size || items[position].header_title == null) 0 else 1
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val colourLayout: LinearLayout = mView.colourLayout
        val layout: LinearLayout = mView.layout
        val name: TextView = mView.name
        val dateTime: TextView? = mView.date_time
        val dots: TextView? = mView.dots
        val divider: View? = mView.divider
    }
}
