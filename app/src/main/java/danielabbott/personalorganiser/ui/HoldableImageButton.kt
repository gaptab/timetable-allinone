package danielabbott.personalorganiser.ui

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageButton

class HoldableImageButton : AppCompatImageButton {

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private lateinit var callbackRunner: Runnable
    private var mHandler: Handler? = null
    private var fingerDown = false
    private var delay = 500L

    private fun init() {
        mHandler = Handler()
        callbackRunner = Runnable {
            if (fingerDown) {
                super.callOnClick()
                mHandler!!.postDelayed(callbackRunner, delay)
                delay = 100
            }
        }
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }

        if (event.action == MotionEvent.ACTION_DOWN) {
            fingerDown = true
            delay = 500L
            callbackRunner.run()
        } else if (event.action == MotionEvent.ACTION_UP) {
            fingerDown = false
        }
        return true
    }

}