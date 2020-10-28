package danielabbott.personalorganiser.ui.todolist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import danielabbott.personalorganiser.MainActivity
import danielabbott.personalorganiser.R
import danielabbott.personalorganiser.data.DB


class ToDoListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var noTasksText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_to_do_list, container, false)

        // On click listener for the add (+) button
        root.findViewById<FloatingActionButton>(R.id.fab_new).setOnClickListener {
            val fragment = EditToDoListTaskFragment(null)
            val fragmentTransaction = fragmentManager!!.beginTransaction()
            fragmentTransaction.replace(R.id.fragmentView, fragment).addToBackStack(null)
            fragmentTransaction.commit()
        }

        recyclerView = root.findViewById<RecyclerView>(R.id.list)
        noTasksText = root.findViewById<TextView>(R.id.none)

        (activity as MainActivity).setToolbarTitle("To Do List")

        loadList()

        return root
    }

    private fun loadList() {
        if (DB.numberOfToDoListTasks() > 0) {
            // Set up recyclerview (list of task name & dates)

            with(recyclerView) {
                layoutManager = LinearLayoutManager(context)
                adapter = ToDoListRecyclerViewAdapter(
                    fragmentManager!!,
                    (activity as MainActivity),
                    DB.getToDoListTasks()
                )
            }
        } else {
            // Make the 'no tasks to display' text visible
            noTasksText.visibility = View.VISIBLE
        }
    }
}
