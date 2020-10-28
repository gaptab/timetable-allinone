package danielabbott.personalorganiser.ui.goals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import danielabbott.personalorganiser.MainActivity
import danielabbott.personalorganiser.R
import danielabbott.personalorganiser.data.DB


class GoalsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_goals_list, container, false)

        // Set the adapter
        val view = root.findViewById<RecyclerView>(R.id.list)
        with(view) {
            layoutManager = LinearLayoutManager(context)
            adapter = GoalRecyclerViewAdapter(DB.getGoals(), fragmentManager!!, activity!!)
        }


        // On click listener for the add (+) button
        root.findViewById<FloatingActionButton>(R.id.fab_new).setOnClickListener {
            val fragment = EditGoalFragment(null)
            val fragmentTransaction = fragmentManager!!.beginTransaction()
            fragmentTransaction.replace(R.id.fragmentView, fragment).addToBackStack(null)
            fragmentTransaction.commit()
        }

        (activity as MainActivity).setToolbarTitle("Goals")

        return root
    }
}
