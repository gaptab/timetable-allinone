package danielabbott.personalorganiser.ui.timetable

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import danielabbott.personalorganiser.R
import danielabbott.personalorganiser.data.DB
import danielabbott.personalorganiser.data.Settings

// A dialog with a text field
class DialogRenameTimetable(
    val oldText: String
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var textBox = EditText(context)
        textBox.setText(oldText)
        textBox.isSingleLine = true
        textBox.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        val builder = AlertDialog.Builder(activity!!)
            .setView(textBox)
            .setPositiveButton("Rename", DialogInterface.OnClickListener { _, _ ->

                if (textBox.text != null && textBox.text.isNotEmpty()) {
                    val name = textBox.text.toString()
                    try {
                        DB.renameTimetable(Settings.getActiveTimetable(context!!), name)

                        val fragment = TimetableFragment()
                        val fragmentTransaction = fragmentManager!!.beginTransaction()
                        fragmentTransaction.replace(R.id.fragmentView, fragment)
                        fragmentTransaction.commit()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
                    }
                }
            })
            .setNegativeButton("Cancel", null)
        return builder.create()
    }
}
