package danielabbott.personalorganiser.ui.goals

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import danielabbott.personalorganiser.ui.DateSelectView


class MilestoneDialog(
    private val callback: (String, Long?) -> Unit,
    private val name_: String?,
    private val date_: Long?
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var textBox = EditText(context)
        textBox.hint = "Milestone name..."
        textBox.isSingleLine = true
        textBox.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        textBox.gravity = Gravity.CENTER_HORIZONTAL
        val lp = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textBox.layoutParams = lp

        if (name_ != null) {
            textBox.setText(name_)
        }

        var date = DateSelectView(context!!)
        date.gravity = Gravity.CENTER_HORIZONTAL
        date.layoutParams = lp
        date.textSize = 20.0f

        if (date_ != null) {
            date.setDate(date_)
        }

        var layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.addView(textBox)
        layout.addView(date)

        val builder = AlertDialog.Builder(activity!!)
            .setView(layout)
            .setPositiveButton("Confirm", DialogInterface.OnClickListener { _, _ ->
                if (textBox.text != null && textBox.text.isNotEmpty()) {
                    val name = textBox.text.toString()
                    callback(name, date.getDate())
                }
            })
            .setNegativeButton("Cancel", null)
        return builder.create()
    }
}
