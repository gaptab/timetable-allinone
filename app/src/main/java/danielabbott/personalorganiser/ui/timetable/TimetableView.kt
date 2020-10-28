package danielabbott.personalorganiser.ui.timetable

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.withTranslation
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.FragmentManager
import danielabbott.personalorganiser.R
import danielabbott.personalorganiser.data.Settings
import kotlin.math.*

class TimetableView : View, GestureDetector.OnGestureListener {

    var startX = 0.0f
    var startY = 0.0f
    var events: ArrayList<TimetableEventUI> = ArrayList<TimetableEventUI>()
    var timetableId: Long = -1

    private var pFragmentManager: FragmentManager? = null
    private var columnHeadersHeight = 0.0f
    private var rowHeadersWidth = 0.0f
    private var rowHeight: Float = 0.0f
    private var columnWidth: Float = 0.0f
    private var mDetector: GestureDetectorCompat
    private var minStartX = 0.0f
    private var minStartY = 0.0f

    // Higher value = zoomed in more
    var zoomValueX = 1.0f
    var zoomValueY = 1.0f

    fun setParentFragmentManager(pfm: FragmentManager) {
        pFragmentManager = pfm
    }

    constructor(context: Context) : super(context) {
        mDetector = GestureDetectorCompat(context, this)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        mDetector = GestureDetectorCompat(context, this)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        mDetector = GestureDetectorCompat(context, this)
    }

    private fun validateValues() {
        if (java.lang.Float.isNaN(startX)) {
            startX = 0.0f
        }
        if (java.lang.Float.isNaN(startY)) {
            startY = 0.0f
        }
        if (java.lang.Float.isNaN(zoomValueX)) {
            zoomValueX = 1.0f
        }
        if (java.lang.Float.isNaN(zoomValueY)) {
            zoomValueY = 1.0f
        }
    }

    override fun onDraw(canvas: Canvas) {
        validateValues()

        val startHour = Settings.getTimetableStartHour(context)
        val endHour = Settings.getTimetableEndHour(context)
        val totalHours = endHour - startHour

        // Fill white
        canvas.drawRGB(255, 255, 255)

        var linePaint = Paint()
        linePaint.style = Paint.Style.STROKE
        linePaint.color = 0xff000000.toInt()
        linePaint.strokeWidth = 4.0f
        linePaint.isAntiAlias = false

        var textPaint = TextPaint()
        textPaint.color = 0xff000000.toInt()
        textPaint.isAntiAlias = true
        textPaint.textAlign = Paint.Align.CENTER

        // Match font size to DPI so it is roughly the same physical size on all screens
        textPaint.textSize =
            Settings.getTimetableFontSize(context) * resources.displayMetrics.density

        // Figure out how big to make the table headers to fit the text

        val egTextBounds = Rect()
        textPaint.getTextBounds("by", 0, 2, egTextBounds)
        columnHeadersHeight =
            ceil(egTextBounds.height().toFloat() + 4f * resources.displayMetrics.density)

        textPaint.getTextBounds("44:444", 0, 6, egTextBounds)
        rowHeadersWidth = ceil(egTextBounds.width().toFloat())

        // Width and height of columns and rows

        rowHeight = columnHeadersHeight * 6.0f * zoomValueY
        columnWidth = rowHeadersWidth * 2.5f * zoomValueX

        if (!zooming) {
            rowHeight = ceil(rowHeight)
            columnWidth = ceil(columnWidth)
        }


        // Size of timetable (bigger then the screen)
        val contentWidth = rowHeadersWidth + 7 * columnWidth
        val contentHeight = columnHeadersHeight + totalHours * rowHeight


        // Stop timetable from being dragged off-screen

        if (startX > 0.0f) {
            startX = 0.0f
        }
        if (startY > 0.0f) {
            startY = 0.0f
        }

        minStartX = -contentWidth + width.toFloat()
        if (startX < minStartX) {
            startX = minStartX
        }

        minStartY = -contentHeight + height.toFloat()
        if (startY < minStartY) {
            startY = minStartY
        }

        if (!zooming) {
            startX = round(startX)
            startY = round(startY)
        }

        // Start drawing

        // Vertical lines
        for (i in 1..7) {
            val x = startX + rowHeadersWidth + columnWidth * i.toFloat()
            canvas.drawLine(x, startY, x, startY + contentHeight, linePaint)
        }


        // Horizontal lines

        for (i in 0 until totalHours + 1) {
            val y = startY + columnHeadersHeight + rowHeight * i.toFloat()
            canvas.drawLine(startX + rowHeadersWidth, y, startX + contentWidth, y, linePaint)
        }

        // Timetable events

        var rectPaint: Paint = Paint()
        rectPaint.style = Paint.Style.FILL
        rectPaint.isAntiAlias = false

        linePaint.strokeWidth = 2.0f
        textPaint.textSize *= 0.9f

        events.forEach {
            // Rectangle


            it.ui_x = startX + rowHeadersWidth + columnWidth * it.day.toFloat() + 2.0f
            it.ui_y =
                startY + columnHeadersHeight + rowHeight * (it.e.startTime / 60.0f - startHour.toFloat())
            it.ui_w = columnWidth - 4.0f
            it.ui_h = rowHeight * (it.e.duration / 60.0f)

            if (it.e.startTime % 60 == 0) {
                it.ui_y += 2.0f
                it.ui_h -= 2.0f
            }
            if ((it.e.startTime + it.e.duration) % 60 == 0) {
                it.ui_h -= 2.0f
            }


            rectPaint.color = it.colour or 0xff000000.toInt()
            canvas.drawRect(it.ui_x, it.ui_y, it.ui_x + it.ui_w, it.ui_y + it.ui_h, rectPaint)


            // Text

            canvas.save() // push default clip area to stack
            canvas.clipRect(it.ui_x, it.ui_y, it.ui_x + it.ui_w, it.ui_y + it.ui_h)



            if (it.hasNotes) {
                val scale = resources.displayMetrics.density
                val x = it.ui_x + it.ui_w - 10 * scale;
                val y = it.ui_y + 4 * scale;
                rectPaint.color = 0xff000000.toInt()
                canvas.drawRect(x, y, x + 4 * scale, y + 4 * scale, rectPaint)
                canvas.drawRect(x - 10 * scale, y, x + (4 - 10) * scale, y + 4 * scale, rectPaint)
                canvas.drawRect(x - 20 * scale, y, x + (4 - 20) * scale, y + 4 * scale, rectPaint)
            }

            val staticLayout = StaticLayout(
                it.e.name,
                textPaint,
                columnWidth.toInt() - (4 * resources.displayMetrics.density).toInt(),
                Layout.Alignment.ALIGN_NORMAL,
                1.0f,
                0.0f,
                true
            )


            val y = if (staticLayout.height - 6 > (rowHeight * (it.e.duration / 60.0f)).toInt()) {
                it.ui_y + (2 * resources.displayMetrics.density).toInt()
            } else {
                it.ui_y + rowHeight * (it.e.duration / 60.0f) * 0.5f - staticLayout.height / 2
            }

            canvas.withTranslation(
                it.ui_x + columnWidth.toInt() / 2,
                y
            ) {
                staticLayout.draw(canvas)
            }


            canvas.restore() // pop default clip area back off the stack

            canvas.drawLine(it.ui_x, it.ui_y, it.ui_x + it.ui_w, it.ui_y, linePaint)
            canvas.drawLine(
                it.ui_x,
                it.ui_y + it.ui_h,
                it.ui_x + it.ui_w,
                it.ui_y + it.ui_h,
                linePaint
            )
        }
        textPaint.textSize /= 0.9f
        textPaint.textAlign = Paint.Align.CENTER
        linePaint.strokeWidth = 4.0f

        // Times on left hand side (overlayed, stays in place when scrolling)

        rectPaint.color = 0xffffffff.toInt()
        canvas.drawRect(0.0f, 0.0f, rowHeadersWidth - 2.0f, height.toFloat(), rectPaint)

        for (i in 0 until totalHours) {
            val y = startY + columnHeadersHeight + rowHeight * i.toFloat()
            canvas.drawText(
                (startHour + i).toString() + ":00",
                rowHeadersWidth * 0.5f,
                y,
                textPaint
            )
        }


        // Days of the week

        val days =
            arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val days2 = arrayOf("Mon", "Tue", "Wed", "Thurs", "Fri", "Sat", "Sun")
        var daysToUse = days

        canvas.drawRect(0.0f, 0.0f, width.toFloat(), columnHeadersHeight - 2.0f, rectPaint)

        // Decide whether to use short-form dates or not depending on whether
        // wednesday (the day with the longest name) fits in the space
        var wednesdayBounds = Rect()
        textPaint.getTextBounds(days[2], 0, days[2].length, wednesdayBounds)

        if (wednesdayBounds.width() > columnWidth) {
            daysToUse = days2
        }

        for (i in 1..7) {
            val x = startX + rowHeadersWidth + columnWidth * i.toFloat()

            canvas.drawText(
                daysToUse[i - 1],
                x - columnWidth * 0.5f,
                columnHeadersHeight * 0.5f + textPaint.textSize * 0.5f - 8,
                textPaint
            )
        }

        canvas.drawRect(0.0f, 0.0f, rowHeadersWidth, columnHeadersHeight, rectPaint)

        canvas.drawLine(rowHeadersWidth, 0.0f, rowHeadersWidth, height.toFloat(), linePaint)
        canvas.drawLine(0.0f, columnHeadersHeight, width.toFloat(), columnHeadersHeight, linePaint)

    }

    override fun onShowPress(e: MotionEvent?) {
    }

    // Timetable was tapped. Go to edit timetable event page.
    override fun onSingleTapUp(event: MotionEvent?): Boolean {
        if (event == null) {
            return true
        }

        val startHour = Settings.getTimetableStartHour(context)

        var tEvent: TimetableEventUI? = null

        // Search for event that was tapped
        events.forEach {
            if (event.x >= it.ui_x && event.x < it.ui_x + it.ui_w
                && event.y >= it.ui_y && event.y < it.ui_y + it.ui_h
            ) {
                tEvent = it
            }
        }

        var startTime: Int = 0
        var endTime: Int = 0
        var day: Int = 0

        if (tEvent == null) {
            // No event was tapped, figure out the day and start/end times of the area that was tapepd

            // Inverse of calculation done when drawing the timetable
            day = ((event.x - startX - rowHeadersWidth) / columnWidth).toInt()
            var clickedTime =
                ((((event.y - startY - columnHeadersHeight) / rowHeight) + startHour.toFloat()) * 60.0f).toInt()

            startTime = clickedTime - (clickedTime % 60)
            endTime = startTime + 60

            events.forEach {
                if (it.day == day
                    && it.e.startTime + it.e.duration < clickedTime && it.e.startTime + it.e.duration > startTime
                ) {
                    startTime = it.e.startTime + it.e.duration
                } else if (it.day == day
                    && it.e.startTime > clickedTime && it.e.startTime < endTime
                ) {
                    endTime = it.e.startTime
                }

            }

            if (endTime - startTime < 10) {
                // Time window is tiny. Just ignore the tap.
                return true
            }
        }

        val fragment =
            TimetableEditEventFragment(timetableId, tEvent?.e?.id, startTime, endTime, 1 shl day)


        validateValues()
        Settings.setTimetablePosition(context!!, startX, startY)
        Settings.setTimetableZoom(context!!, zoomValueX, zoomValueY)

        val fragmentTransaction = pFragmentManager!!.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentView, fragment)
            .addToBackStack(null)
            .commit()
        return true
    }

    private var zooming = false

    override fun onDown(e: MotionEvent?): Boolean {
        return true
    }

    private var scrollTime: Long = 0
    private var animatorX: ObjectAnimator? = null
    private var animatorY: ObjectAnimator? = null

    // When the user swipes their finger fast and lifts off
    // The timetable keeps scrolling for a short while after the finger leaves the screen
    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (zooming) {
            // If the user is doing a zoom guesture then any 'fling's are invalid
            return true
        }

        // Length of time that timetable will keep moving for (in seconds)
        // The longer the time the more distance is moved
        val time = 0.3f

        // The calculations below use the equations for linear motion (SUVAT equations)
        // The calculations are done twice - once in the x axis, once in y

        val x = startX + (System.currentTimeMillis() - scrollTime) * 0.001f * velocityX
        val distX = velocityX * time * 0.5f // Total distance the timetable will move in the x axis
        val accelerationX =
            (-distX * 2.0f) / (time * time) // How fast it slows down (value is negative)
        animatorX = ObjectAnimator.ofFloat(this, "startX", x, x + distX).apply {
            duration = (time * 1000.0f).toLong()
            setInterpolator {
                // it is between 0 and 1
                // t is between 0 and time
                val t = it * time
                val s = accelerationX * t * t * 0.5f + velocityX * t

                // Convert distance to animator time value
                s / distX
            }
            addUpdateListener {
                invalidate()
            }
            start()
        }
        val y = startY + (System.currentTimeMillis() - scrollTime) * 0.001f * velocityY
        val distY = velocityY * time * 0.5f
        val accelerationY = (-distY * 2.0f) / (time * time)
        animatorY = ObjectAnimator.ofFloat(this, "startY", y, y + distY).apply {
            duration = (time * 1000.0f).toLong()
            setInterpolator {
                val t = it * time
                val s = accelerationY * t * t * 0.5f + velocityY * t
                s / distY
            }
            addUpdateListener {
                invalidate()
            }
            start()
        }

        return true
    }

    override fun onScroll(
        event1: MotionEvent,
        event2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (zooming) {
            return true
        }

        // User can pan in 8 directions

        if (abs(abs(distanceX) - abs(distanceY)) / max(
                abs(distanceX),
                abs(distanceY)
            ) < 0.5f
        ) {
            // Diagonal movement
            val d = (abs(distanceX) + abs(distanceY)) * 0.5f
            startX -= d * sign(distanceX)
            startY -= d * sign(distanceY)
        } else {
            // Movement along an axis
            if (abs(distanceX) > abs(distanceY)) {
                startX -= distanceX
            } else {
                startY -= distanceY
            }
        }



        scrollTime = System.currentTimeMillis()

        invalidate()
        return true
    }

    override fun onLongPress(e: MotionEvent?) {
    }


    private var fingerStartZoomDistanceX = 0.0f
    private var fingerStartZoomDistanceY = 0.0f

    // Set when the second finger touches the screen
    private var finger2Id = 0

    private var zoomXOld = 0.0f
    private var zoomYOld = 0.0f
    private var startXOld = 0.0f
    private var startYOld = 0.0f

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return true
        }
        mDetector.onTouchEvent(event)
        if (zooming) {
            val finger2Index = event.findPointerIndex(finger2Id)
            if ((event.actionMasked == MotionEvent.ACTION_POINTER_UP &&
                        (event.actionIndex == finger2Index || event.actionIndex == 0))
                || event.actionMasked == MotionEvent.ACTION_UP
            ) {

                // back to scrolling

                zooming = false
                invalidate()
            } else if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                // A finger moved

                val finger1X = event.x
                val finger1Y = event.y
                val finger2X = event.getX(finger2Index)
                val finger2Y = event.getY(finger2Index)

                val newFingerDistanceX = abs(finger2X - finger1X)
                val newFingerDistanceY = abs(finger2Y - finger1Y)

                val zoomDeltaX = (newFingerDistanceX - fingerStartZoomDistanceX) * 0.001f
                val zoomDeltaY = (newFingerDistanceY - fingerStartZoomDistanceY) * 0.001f


                if (abs(zoomDeltaX) > abs(zoomDeltaY)) {
                    zoomValueX = zoomXOld + zoomDeltaX
                } else {
                    zoomValueY = zoomYOld + zoomDeltaY
                }



                if (zoomValueX < 0.5f) {
                    zoomValueX = 0.5f
                }
                if (zoomValueY < 0.5f) {
                    zoomValueY = 0.5f
                }

                if (zoomValueX > 1.5f) {
                    zoomValueX = 1.5f
                }
                if (zoomValueY > 1.5f) {
                    zoomValueY = 1.5f
                }

                startX = startXOld * (zoomValueX / zoomXOld)
                startY = startYOld * (zoomValueY / zoomYOld)

                invalidate()

            }
        } else {
            if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN && !zooming) {
                // Second finger on screen, now we are zooming in/out
                zooming = true

                finger2Id = event.getPointerId(event.actionIndex)

                val finger1X = event.x
                val finger1Y = event.y
                val finger2X = event.getX(event.actionIndex)
                val finger2Y = event.getY(event.actionIndex)

                fingerStartZoomDistanceX = kotlin.math.abs(finger2X - finger1X)
                fingerStartZoomDistanceY = kotlin.math.abs(finger2Y - finger1Y)

                zoomXOld = zoomValueX
                zoomYOld = zoomValueY
                startXOld = startX
                startYOld = startY

            }
        }
        return true
    }

}

