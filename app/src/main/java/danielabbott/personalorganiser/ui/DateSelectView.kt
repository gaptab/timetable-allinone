package danielabbott.personalorganiser.ui

import android.app.DatePickerDialog
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import danielabbott.personalorganiser.DateTimeUtil
import java.util.*

class DateSelectView : AppCompatTextView {

    var dateSelected = false
    var year = 0
    var month = 0 // 1 - 12
    var day = 0

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

    var onDateChange: ((Int, Int, Int) -> Unit)? = null
    var onDateCleared: (() -> Unit)? = null

    private fun init() {
        Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            year = get(Calendar.YEAR)
            month = get(Calendar.MONTH) + 1
            day = get(Calendar.DAY_OF_MONTH)
        }

        setOnClickListener {
            DatePickerDialog(context, 0, { _, y: Int, m: Int, d: Int ->
                setDate(y, m + 1, d)
            }, year, month - 1, day).show()
            onClick?.onClick(this)
        }

        setOnLongClickListener {
            text = "----/--/--"
            dateSelected = false
            onLongClick?.onLongClick(this)
            if (onDateCleared != null) {
                onDateCleared!!()
            }
            true
        }


        text = "----/--/--"
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

    fun setDate(y_: Int, m_: Int, d_: Int) {
        year = y_
        month = m_
        day = d_
        text = DateTimeUtil.getDateString(context, year, month, day)
        dateSelected = true

        onDateChange?.let { it(year, month, day) }
    }

    fun setDate(d: Long) {
        val ymd = DateTimeUtil.getYearMonthDay(d)
        setDate(ymd.first, ymd.second, ymd.third)

    }

    fun getDate(): Long? {
        if (dateSelected) {
            return DateTimeUtil.getDateTimeMillis(year, month, day, 6, 0)
        }
        return null
    }

}
