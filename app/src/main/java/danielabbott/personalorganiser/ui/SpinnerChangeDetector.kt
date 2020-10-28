package danielabbott.personalorganiser.ui

import android.view.View
import android.widget.AdapterView

class SpinnerChangeDetector(val onClick: (View) -> Unit) : AdapterView.OnItemSelectedListener {
    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    private var x = false

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        // First call to this function is ignored (for some reason Android calls it when the view is created)
        if (x && view != null) {
            onClick(view)
        }
        x = true
    }
}