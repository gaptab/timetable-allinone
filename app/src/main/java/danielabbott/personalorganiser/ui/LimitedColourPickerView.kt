package danielabbott.personalorganiser.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class LimitedColourPickerView : View {

    val columns = 5

    companion object {
        // Size of list must be a multiple of columns
        val colours = listOf<Int>(
            0xff8080,
            0xffbd80,
            0xfffb80,
            0xd3ff80,
            0xb4faac,
            0x80ffd3,
            0x80caff,
            0x9780ff,
            0xee80ff,
            0xff80c4
        )
    }

    var selectedColour: Int? = null

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

    private var rowHeight = 0.0f
    private var columnWidth = 0.0f

    var initDone = false
    private var onTouch: OnTouchListener? = null

    private fun init() {
        setOnTouchListener { view: View, motionEvent: MotionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                val x = motionEvent.x
                val y = motionEvent.y

                val row = (y / rowHeight).toInt()
                val col = (x / columnWidth).toInt()

                val i = row * columns + col

                selectedColour = colours[i]

            }
            onTouch?.onTouch(view, motionEvent)
            false
        }
        initDone = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        var paint = Paint()
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = false

        val x = paddingLeft
        val y = paddingTop
        val w = width - paddingRight
//        val h = height - paddingBottom

        val rows = colours.size / columns

        columnWidth = w / columns.toFloat()
        rowHeight = columnWidth / 2.0f

        // Grid of colours

        var row = 0
        while (row < rows) {
            var column = 0
            while (column < columns) {
                paint.color = colours[row * 5 + column] or 0xff000000.toInt()
                val rectx = x + column * columnWidth
                val recty = y + row * rowHeight
                canvas.drawRect(rectx, recty, rectx + columnWidth, recty + rowHeight, paint)

                column++
            }

            row++
        }

    }

    override fun setOnTouchListener(l: OnTouchListener?) {
        if (!initDone) {
            super.setOnTouchListener(l)
            return
        }
        onTouch = l
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minw: Int = paddingLeft + paddingRight + suggestedMinimumWidth
        val w: Int = View.resolveSizeAndState(minw, widthMeasureSpec, 1)

        val rows = colours.size / columns

        val minh: Int =
            ((View.MeasureSpec.getSize(w) / columns.toFloat()) * 0.5f * rows).toInt() + paddingBottom + paddingTop
        val h: Int = View.resolveSizeAndState(
            minh,
            heightMeasureSpec,
            0
        )

        setMeasuredDimension(w, h)

    }
}
