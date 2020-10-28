package danielabbott.personalorganiser.ui.timetable

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import danielabbott.personalorganiser.MainActivity
import danielabbott.personalorganiser.Notifications
import danielabbott.personalorganiser.R
import danielabbott.personalorganiser.data.DB
import danielabbott.personalorganiser.data.Settings
import danielabbott.personalorganiser.ui.OnBackPressed

class TimetableFragment : Fragment(), OnBackPressed {

    private lateinit var timetableView: TimetableView
    private lateinit var activeTimetableName: String
    private var activeTimetable: Long = -1


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_timetable, container, false)

        loadTimetable(root)

        setHasOptionsMenu(true)


        return root
    }

    private fun loadTimetable(root: View) {
        // Pick a timetable to show

        // Get the timetable that was selected last
        activeTimetable = Settings.getActiveTimetable(activity!!)

        if (activeTimetable >= 0) {
            try {
                activeTimetableName = DB.getTimetableName(activeTimetable)
            } catch (e: Exception) {
                activeTimetable = -1
            }
        }

        if (activeTimetable < 0) {
            // No active timetable, select one

            try {
                val timetable = DB.getFirstTimetable()
                activeTimetable = timetable.first
                activeTimetableName = timetable.second
            } catch (e: Exception) {
                // No timetables. Create one.
                activeTimetable = DB.createNewTimetable("Timetable")
                activeTimetableName = "Timetable"
            }
            Settings.setActiveTimetable(activeTimetable, activity!!.applicationContext)
            Notifications.scheduleAllNotifications(activity!!.applicationContext)
        }

        (activity!! as MainActivity).setToolbarTitle(activeTimetableName)

        val events = DB.getTimetableEvents(activeTimetable)

        val eventsUI = ArrayList<TimetableEventUI>()
        events.forEach {
            for (day in 0..6) {
                if ((it.days and (1 shl day)) != 0) {
                    eventsUI.add(
                        TimetableEventUI(
                            it.goal_colour ?: 0xffd0d0d0.toInt(),
                            it,
                            day,
                            it.notes != null && it.notes!!.isNotEmpty()
                        )
                    )
                }
            }
        }

        timetableView = root.findViewById(R.id.timetable) as TimetableView
        timetableView.timetableId = activeTimetable
        timetableView.events = eventsUI
        timetableView.setParentFragmentManager(fragmentManager!!)
        val pos = Settings.getTimetablePosition(context!!)
        timetableView.startX = pos.first
        timetableView.startY = pos.second
        val zoom = Settings.getTimetableZoom(context!!)
        timetableView.zoomValueX = zoom.first
        timetableView.zoomValueY = zoom.second
        timetableView.invalidate()
    }

    lateinit var openTimetableMenuItem: MenuItem
    lateinit var newTimetableMenuItem: MenuItem
    lateinit var clearTimetableMenuItem: MenuItem
    lateinit var renameTimetableMenuItem: MenuItem
    lateinit var cloneTimetableMenuItem: MenuItem
    lateinit var deleteTimetableMenuItem: MenuItem

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        openTimetableMenuItem = menu.add("Open timetable")
        newTimetableMenuItem = menu.add("New timetable")
        clearTimetableMenuItem = menu.add("Clear timetable")
        renameTimetableMenuItem = menu.add("Rename timetable")
        cloneTimetableMenuItem = menu.add("Clone timetable")
        deleteTimetableMenuItem = menu.add("Delete timetable")
        activity!!.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item == openTimetableMenuItem) {
            // Go to timetable list page
            Settings.setTimetablePosition(context!!, timetableView.startX, timetableView.startY)
            Settings.setTimetableZoom(context!!, timetableView.zoomValueX, timetableView.zoomValueY)
            val fragment = OpenTimetableListFragment()
            val fragmentTransaction = fragmentManager!!.beginTransaction()
            fragmentTransaction.replace(R.id.fragmentView, fragment).addToBackStack(null)
            fragmentTransaction.commit()
        } else if (item == newTimetableMenuItem) {
            DialogNewTimetable().show(fragmentManager!!, null)
        } else if (item == cloneTimetableMenuItem) {
            DialogNewTimetable(activeTimetable).show(fragmentManager!!, null)
        } else if (item == clearTimetableMenuItem) {
            AlertDialog.Builder(context)
                .setTitle("Clear timetable")
                .setMessage("Are you sure you want to delete all events in this timetable? This cannot be undone.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Delete All") { _, _ ->
                    DB.clearTimetable(Settings.getActiveTimetable(activity!!))
                    Notifications.scheduleAllNotifications(context!!)

                    timetableView.startX = .0f
                    timetableView.startY = .0f
                    Settings.setTimetablePosition(context!!, .0f, .0f)
                    timetableView.zoomValueX = 1.0f
                    timetableView.zoomValueY = 1.0f
                    Settings.setTimetableZoom(context!!, 1.0f, 1.0f)

                    val fragmentTransaction = fragmentManager!!.beginTransaction()
                    fragmentTransaction.replace(R.id.fragmentView, TimetableFragment())
                    fragmentTransaction.commit()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else if (item == deleteTimetableMenuItem) {
            AlertDialog.Builder(context)
                .setTitle("Delete timetable")
                .setMessage("Are you sure you want to delete this timetable? This cannot be undone.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Delete") { _, _ ->
                    DB.deleteTimetable(Settings.getActiveTimetable(activity!!))

                    timetableView.startX = .0f
                    timetableView.startY = .0f
                    Settings.setTimetablePosition(context!!, .0f, .0f)
                    timetableView.zoomValueX = 1.0f
                    timetableView.zoomValueY = 1.0f
                    Settings.setTimetableZoom(context!!, 1.0f, 1.0f)

                    Settings.setActiveTimetable(-1, context!!)
                    loadTimetable(view!!)
                    Notifications.scheduleAllNotifications(context!!)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else if (item == renameTimetableMenuItem) {
            DialogRenameTimetable(activeTimetableName).show(fragmentManager!!, null)
        } else {
            Settings.setTimetablePosition(context!!, timetableView.startX, timetableView.startY)
            Settings.setTimetableZoom(context!!, timetableView.zoomValueX, timetableView.zoomValueY)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed(onNoChangesOrDiscardChanges: () -> Unit) {
        Settings.setTimetablePosition(context!!, timetableView.startX, timetableView.startY)
        Settings.setTimetableZoom(context!!, timetableView.zoomValueX, timetableView.zoomValueY)
        onNoChangesOrDiscardChanges()
    }
}
