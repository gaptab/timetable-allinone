package danielabbott.personalorganiser.ui.notes

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.DialogFragment
import danielabbott.personalorganiser.data.Settings

class DialogReplace(val original_text: String, val initial_find_val: String?, val callback: (String) -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val find = EditText(context)
        find.hint = "Find..."
        find.isSingleLine = true
        find.inputType = InputType.TYPE_CLASS_TEXT
        var lp = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        find.layoutParams = lp


        val replace = EditText(context)
        replace.hint = "Replace..."
        replace.isSingleLine = true
        replace.inputType = InputType.TYPE_CLASS_TEXT
        replace.layoutParams = lp


        if(initial_find_val == null) {
            find.setText(Settings.getMostRecentFindString(context!!))
        }
        else {
            find.setText(initial_find_val)
        }

        replace.setText(Settings.getMostRecentReplaceString(context!!))

        val ignoreCase = SwitchCompat(context!!)
        ignoreCase.setText("Ignore case")
        ignoreCase.layoutParams = lp

        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.addView(find)
        layout.addView(replace)
        layout.addView(ignoreCase)

        val builder = AlertDialog.Builder(activity!!)
            .setView(layout)
            .setPositiveButton("Replace", DialogInterface.OnClickListener { _, _ ->
                Settings.setMostRecentFindString(context!!, find.text.toString())
                Settings.setMostRecentReplaceString(context!!, replace.text.toString())
                if(find.text.isNotEmpty()) {
                    callback(original_text.replace(find.text.toString(), replace.text.toString(), ignoreCase.isChecked))
                }
            })
            .setNegativeButton("Cancel", null)
        return builder.create()
    }
}
