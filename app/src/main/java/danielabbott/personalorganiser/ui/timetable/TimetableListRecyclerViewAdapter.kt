package danielabbott.personalorganiser.ui.timetable


import android.app.Activity
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import danielabbott.personalorganiser.Notifications
import danielabbott.personalorganiser.R
import danielabbott.personalorganiser.data.DB
import danielabbott.personalorganiser.data.Settings
import kotlinx.android.synthetic.main.timetable_list_item.view.*


class TimetableListRecyclerViewAdapter(
    private val mValues: List<Pair<Long, String>>,
    private val activity: Activity,
    private val parentFragmentManager: FragmentManager
) : RecyclerView.Adapter<TimetableListRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.timetable_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        holder.mContentView.text = item.second

        if (position == mValues.size - 1) {
            holder.divider.visibility = View.INVISIBLE
        }

        holder.mView.setOnClickListener(View.OnClickListener {
            // Load the timetable

            Settings.setActiveTimetable(item.first, activity.applicationContext)
            Notifications.scheduleAllNotifications(activity.applicationContext)
            val fragment = TimetableFragment()
            val fragmentTransaction = parentFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.fragmentView, fragment)
            fragmentTransaction.commit()
        })
        holder.mView.setOnLongClickListener(View.OnLongClickListener {
            // Show a dialog to ask the user if they want to delete this timetable
            AlertDialog.Builder(activity)
                .setMessage("Are you sure you want to delete the timetable named ${item.second}?")
                .setPositiveButton("Delete",
                    DialogInterface.OnClickListener { _, _ ->
                        // Delete data in database
                        DB.deleteTimetable(item.first)

                        // Go back to timetable view
                        val fragment = OpenTimetableListFragment()
                        val fragmentTransaction = parentFragmentManager.beginTransaction()
                        fragmentTransaction.replace(R.id.fragmentView, fragment)
                        fragmentTransaction.commit()
                    })
                .setNegativeButton("Cancel", null)
                .show()
            true
        })
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mContentView: TextView = mView.content
        val divider: View = mView.divider
    }
}
