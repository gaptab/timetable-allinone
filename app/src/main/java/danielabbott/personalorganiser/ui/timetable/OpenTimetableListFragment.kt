package danielabbott.personalorganiser.ui.timetable

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import danielabbott.personalorganiser.MainActivity
import danielabbott.personalorganiser.R
import danielabbott.personalorganiser.data.DB


class OpenTimetableListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_open_timetable_list, container, false)

        val recyclerView = view as RecyclerView

        recyclerView.layoutManager = LinearLayoutManager(context)

        val items = DB.getTimetables()

        recyclerView.adapter =
            TimetableListRecyclerViewAdapter(items, activity!!, fragmentManager!!)


        (activity!! as MainActivity).setToolbarTitle("Open Timetable")

        return view
    }

}
