package danielabbott.personalorganiser.ui.timers

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import danielabbott.personalorganiser.R
import kotlin.math.max
import kotlin.math.min

// A dialog with a text field
class DialogNewTimer(
    private val creatingNew: Boolean,
    private val originalName: String?,
    private val originalTime: Int?,
    private val callback: (String?, Int, Int, Int) -> Unit
) : DialogFragment() {

    private fun roundTime(x: Int): Int {
        return if (x >= 0) x % 60 else 60 + (x % 60)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.timer_set, null)

        val textBox = view.findViewById<TextView>(R.id.timerName)
        if (originalName != null) {
            textBox.text = originalName
        }

        val tvHours = view.findViewById<TextView>(R.id.hours)
        val tvMinutes = view.findViewById<TextView>(R.id.minutes)
        val tvSeconds = view.findViewById<TextView>(R.id.seconds)

        if (originalTime != null) {
            tvHours.text = ((originalTime / 60) / 60).toString().padStart(2, '0')
            tvMinutes.text = ((originalTime / 60) % 60).toString().padStart(2, '0')
            tvSeconds.text = (originalTime % 60).toString().padStart(2, '0')
        }

        view.findViewById<ImageButton>(R.id.bHrAdd).setOnClickListener {
            tvHours.text =
                min(tvHours.text.toString().toInt() + 1, 99).toString().padStart(2, '0')
        }
        view.findViewById<ImageButton>(R.id.bHrSub).setOnClickListener {
            tvHours.text =
                max(tvHours.text.toString().toInt() - 1, 0).toString().padStart(2, '0')
        }
        view.findViewById<ImageButton>(R.id.bMinAdd).setOnClickListener {
            tvMinutes.text =
                roundTime(tvMinutes.text.toString().toInt() + 1).toString().padStart(2, '0')
        }
        view.findViewById<ImageButton>(R.id.bMinSub).setOnClickListener {
            tvMinutes.text =
                roundTime(tvMinutes.text.toString().toInt() - 1).toString().padStart(2, '0')
        }
        view.findViewById<ImageButton>(R.id.bSecAdd).setOnClickListener {
            tvSeconds.text =
                roundTime(tvSeconds.text.toString().toInt() + 1).toString().padStart(2, '0')
        }
        view.findViewById<ImageButton>(R.id.bSecSub).setOnClickListener {
            tvSeconds.text =
                roundTime(tvSeconds.text.toString().toInt() - 1).toString().padStart(2, '0')
        }


        val builder = AlertDialog.Builder(activity!!)
            .setView(view)
            .setPositiveButton(if (creatingNew) "Create" else "Update") { _, _ ->
                val name = textBox.text?.toString()
                callback(
                    name,
                    tvHours.text.toString().toInt(),
                    tvMinutes.text.toString().toInt(),
                    tvSeconds.text.toString().toInt()
                )
            }
            .setNegativeButton("Cancel", null)
        return builder.create()
    }

}
