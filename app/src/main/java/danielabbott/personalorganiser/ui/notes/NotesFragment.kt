package danielabbott.personalorganiser.ui.notes

import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import danielabbott.personalorganiser.MainActivity
import danielabbott.personalorganiser.R
import danielabbott.personalorganiser.data.DB
import danielabbott.personalorganiser.data.Settings
import danielabbott.personalorganiser.data.Tag
import danielabbott.personalorganiser.ui.SpinnerChangeDetector

class NotesFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notes_list, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.list)
        with(recyclerView) {
            layoutManager = LinearLayoutManager(context)
            val selected = Settings.getSelectedTagID(context!!)
            var previews = if (selected == -2L) DB.getNotesPreviewsUntagged() else (DB.getNotesPreviews(if (selected < 0) null else selected))
            if(previews.isEmpty() && selected != -1L) {
                Settings.setSelectedTagID(context!!, -1)
                previews = DB.getNotesPreviews(null)
            }
            adapter = NoteRecyclerViewAdapter(
                previews,
                fragmentManager!!, activity!!
            )
        }


        val id = Settings.getSelectedTagID(context!!)
        var selected_tag: Tag? = null

        var tagsStrings = ArrayList<String>()
        var tagIDs = ArrayList<Long>()
        tagsStrings.add("[All]") // -1
        tagsStrings.add("[Untagged]") // -2
        DB.getTags().forEach {
            tagsStrings.add(it.tag)
            if (it.id == id) {
                selected_tag = it
            }
            tagIDs.add(it.id)
        }


        val tagSelect = view.findViewById<Spinner>(R.id.tagSelect)

        tagSelect.adapter = ArrayAdapter<String>(
            context!!,
            android.R.layout.simple_spinner_dropdown_item,
            tagsStrings
        )

        if (id > 0) {
            var i: Int = 2
            for (id_ in tagIDs) {
                if (id_ == id) {
                    tagSelect.setSelection(i)
                    break
                }
                i += 1
            }
        } else {
            tagSelect.setSelection(-id.toInt() - 1)
        }



        tagSelect.onItemSelectedListener = SpinnerChangeDetector {
            Settings.setSelectedTagID(
                context!!,
                if (tagSelect.selectedItemPosition > 1) tagIDs[tagSelect.selectedItemPosition - 2]
                else -(tagSelect.selectedItemPosition.toLong() + 1)
            )

            val fragmentTransaction = fragmentManager!!.beginTransaction()
            fragmentTransaction.replace(R.id.fragmentView, NotesFragment())
            fragmentTransaction.commit()
        }


        // On click listener for the add (+) button
        view.findViewById<FloatingActionButton>(R.id.fab_new).setOnClickListener {
            var tagsAutoAdded: ArrayList<Tag>? = null
            if (selected_tag != null) {
                tagsAutoAdded = ArrayList()
                tagsAutoAdded.add(selected_tag!!)
            }
            val fragment = EditNoteFragment(null, null, tagsAutoAdded)
            val fragmentTransaction = fragmentManager!!.beginTransaction()
            fragmentTransaction.replace(R.id.fragmentView, fragment).addToBackStack(null)
            fragmentTransaction.commit()
        }


        (activity as MainActivity).setToolbarTitle("Notes")

        return view
    }
}