package danielabbott.personalorganiser.ui

import android.app.TimePickerDialog
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class TimeSelectView : AppCompatTextView {

    var timeSelected = false
    var hour = 0
    var minute = 0

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    var initDone = false
    private var onClick: OnClickListener? = null
    private var onLongClick: OnLongClickListener? = null
    var onTimeChanged: ((Int, Int) -> Unit)? = null
    var onTimeCleared: (() -> Unit)? = null

    private fun init() {
        setOnClickListener {
            TimePickerDialog(context, { _, h: Int, m: Int ->
                setTime(h, m)
            }, hour, minute, true).show()
            onClick?.onClick(this)
        }

        setOnLongClickListener {
            timeSelected = false
            text = "--:--"
            if (onTimeCleared != null) {
                onTimeCleared!!()
            }
            onLongClick?.onLongClick(this)
            true
        }

        text = "--:--"
        initDone = true
    }


    override fun setOnClickListener(l: OnClickListener?) {
        if (!initDone) {
            super.setOnClickListener(l)
            return
        }
        onClick = l
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        if (!initDone) {
            super.setOnLongClickListener(l)
            return
        }
        onLongClick = l
    }

    fun setTime(h: Int, m: Int) {
        // padStart adds an extra 0 on if the value is only 1 digit
        // e.g. '4' -> '04', '53' -> '53'
        hour = h
        minute = m
        val hr = h.toString().padStart(2, '0')
        val min = m.toString().padStart(2, '0')
        text = "$hr:$min"
        timeSelected = true

        if (onTimeChanged != null) {
            onTimeChanged!!(h, m)
        }
    }

    // Minutes since midnight
    fun getTime(): Int? {
        if (timeSelected) {
            return hour * 60 + minute
        }
        return null
    }

    fun reset() {
        timeSelected = false
        text = "--:--"
    }

}
