package danielabbott.personalorganiser.ui.notes

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import danielabbott.personalorganiser.R
import danielabbott.personalorganiser.data.DB
import danielabbott.personalorganiser.data.NotePreview

class NoteRecyclerViewAdapter(
    private val values: List<NotePreview>,
    private val parentFragmentManager: FragmentManager,
    private val activity: Activity
) : RecyclerView.Adapter<NoteRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_notes, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]

        // No more than 2 newline characters in a row
        val s1 = item.contents.replace(Regex("\n\n\n+"), "\n\n")

        // Limit to 7 lines

        var s2 = ""
        var lines = 0
        var cutShort = false

        for (c in s1) {
            if (c == '\n') {
                lines++;

                if (lines > 7) {
                    cutShort = true
                    break
                }
            }
            s2 += c

            if (lines > 7) {
                cutShort = true
                break
            }
        }
        if(cutShort || item.full_contents_length > 100) {
            s2 += "..."
        }

        holder.contentView.text = s2

        holder.noteClickable.setOnClickListener {
            val fragment = EditNoteFragment(item.id)
            val fragmentTransaction = parentFragmentManager!!.beginTransaction()
            fragmentTransaction.replace(R.id.fragmentView, fragment).addToBackStack(null)
            fragmentTransaction.commit()
        }
        holder.noteClickable.setOnLongClickListener {
            showDeleteDialog(item.id, s2.replace('\n', ' ', false))
            true
        }
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val contentView: TextView = view.findViewById(R.id.content)
        val noteClickable: LinearLayout = view.findViewById(R.id.noteClickable)
    }

    private fun showDeleteDialog(noteId: Long, note: String) {
        var noteShortened = note
        if (noteShortened.length > 20) {
            noteShortened = noteShortened.substring(0, 17) + "..."
        }

        android.app.AlertDialog.Builder(activity)
            .setTitle("Delete note")
            .setMessage("Are you sure you want to delete the note '$noteShortened'? This cannot be undone.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Delete") { _, _ ->
                DB.deleteNote(noteId)

                // Reload notes page
                val fragmentTransaction = parentFragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.fragmentView, NotesFragment())
                fragmentTransaction.commit()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}