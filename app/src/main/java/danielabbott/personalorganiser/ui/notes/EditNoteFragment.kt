package danielabbott.personalorganiser.ui.notes

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import danielabbott.personalorganiser.MainActivity
import danielabbott.personalorganiser.R
import danielabbott.personalorganiser.data.DB
import danielabbott.personalorganiser.data.Note
import danielabbott.personalorganiser.data.Settings
import danielabbott.personalorganiser.data.Tag
import danielabbott.personalorganiser.ui.DataEntryFragmentBasic

// text_from_share is only used if noteId == null. It is the text provided by a share intent
class EditNoteFragment(val noteId: Long? = null, val text_from_share: String? = null, var tagsToAdd: ArrayList<Tag>? = null) :
    DataEntryFragmentBasic() {


    private lateinit var textArea: EditText
    private lateinit var tagsll: LinearLayout


    private var tags = ArrayList<Tag>()
    private var newTags = ArrayList<Tag>() // tags to be added to the note (tag IDs are -1)
    private var deletedTags = ArrayList<Tag>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        DB.init(context!!)
        val root = inflater.inflate(R.layout.fragment_edit_note, container, false)
        (activity!! as MainActivity).setToolbarTitle("Edit Note")

        textArea = root.findViewById<EditText>(R.id.textArea)
        tagsll = root.findViewById<LinearLayout>(R.id.tagsll)


        if (noteId == null) {
            if (text_from_share != null) {
                textArea.setText(text_from_share)
                unsavedData = true
            }

            tagsToAdd?.forEach {
                newTags.add(it)
                addTagTV(it)
            }
        } else {
            var e: Note
            try {
                e = DB.getNote(noteId)
            } catch (e: Exception) {
                val fragmentTransaction = fragmentManager!!.beginTransaction()
                fragmentTransaction.replace(R.id.fragmentView, NotesFragment())
                fragmentTransaction.commit()
                return root
            }

            textArea.setText(e.contents)

            tags = e.tags
            tags.forEach {
                addTagTV(it)
            }
        }

        root.findViewById<Button>(R.id.addTag).setOnClickListener {
            DialogAddTag { tagName ->
                val tagUpper = tagName.toUpperCase()
                var tagAlreadyExists = false
                for (i in 0 until tags.size) {
                    if (tags[i].tag.toUpperCase().equals(tagUpper)) {
                        tagAlreadyExists = true
                        break
                    }
                }
                for (i in 0 until newTags.size) {
                    if (newTags[i].tag.toUpperCase().equals(tagUpper)) {
                        tagAlreadyExists = true
                        break
                    }
                }

                if (!tagAlreadyExists) {
                    var tag = Tag(-1, tagName)
                    newTags.add(tag)
                    addTagTV(tag)
                    unsavedData = true
                }
            }.show(fragmentManager!!, null)
        }

        textArea.addTextChangedListener {
            unsavedData = true
        }

        val save: ImageView = root.findViewById(R.id.save)
        save.setOnClickListener { _ ->
            val notes = textArea.text.toString()

            var e = Note(noteId ?: -1, notes, tags)
            val newNoteId = DB.updateOrCreateNote(e)

            newTags.forEach {
                val tagID = DB.createOrGetTag(it.tag)
                DB.addTagToNote(newNoteId, tagID)
            }

            (activity!! as MainActivity).hideKeyboard()

            unsavedData = false
            (activity as MainActivity).onBackPressed()
        }



        setHasOptionsMenu(true)
        return root
    }

    private fun addTagTV(tagObj: Tag) {
        var tv = LayoutInflater.from(context!!).inflate(R.layout.tag, tagsll, false) as TextView
        tv.text = tagObj.tag
        tv.setOnLongClickListener { _ ->
            // Remove tag
            tagsll.removeView(tv)

            if (tags.contains(tagObj)) {
                tags.remove(tagObj)
                DB.removeTagFromNote(noteId!!, tagObj.id)
            } else {
                newTags.remove(tagObj)
            }

            unsavedData = true
            true
        }
        tagsll.addView(tv)
    }

    lateinit var qieMenuItem: MenuItem
    lateinit var replaceMenuItem: MenuItem
    var deleteNoteMenuItem: MenuItem? = null

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()

        val qit = Settings.getQIT(context!!)

        qieMenuItem = menu.add(qit.substring(0, Math.min(qit.length, 4)))
        qieMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)


        replaceMenuItem = menu.add("Replace")

        if(noteId != null) {
            deleteNoteMenuItem = menu.add("Delete")
        }

        activity!!.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item == qieMenuItem) {
            textArea.text.insert(textArea.selectionEnd, Settings.getQIT(context!!))
        } else if (item == replaceMenuItem) {
            var find: String? = null
            if(textArea.selectionStart != textArea.selectionEnd) {
                find = textArea.text.toString().substring(textArea.selectionStart, textArea.selectionEnd)
            }
            DialogReplace (textArea.text.toString(), find) { new_text ->
                textArea.setText(new_text)
            }.show(fragmentManager!!, null)
        }
        else if (deleteNoteMenuItem != null && item == deleteNoteMenuItem) {
            android.app.AlertDialog.Builder(activity)
                .setTitle("Delete note")
                .setMessage("Are you sure you want to delete this note? This cannot be undone.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Delete") { _, _ ->
                    DB.deleteNote(noteId!!)

                    // Reload notes page
                    val fragmentTransaction = fragmentManager!!.beginTransaction()
                    fragmentTransaction.replace(R.id.fragmentView, NotesFragment())
                    fragmentTransaction.commit()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        return super.onOptionsItemSelected(item)
    }
}