package danielabbott.personalorganiser.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

// Just a coloured circle
class CircleView : View {

    private var colour_: Int = 0xffff0000.toInt()

    var colour: Int
        get() = colour_
        set(value) {
            colour_ = value
            invalidate()
        }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    fun min(a: Int, b: Int): Int {
        return if (a < b) a else b
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        var paint = Paint()
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
        paint.color = colour or 0xff000000.toInt()

//        canvas.drawPaint(paint)

        val x = paddingLeft
        val y = paddingTop
        val w = width - paddingRight
        val h = height - paddingBottom
        canvas.drawCircle(x + w * 0.5f, y + h * 0.5f, min(w, h) * 0.5f, paint)

    }
}
