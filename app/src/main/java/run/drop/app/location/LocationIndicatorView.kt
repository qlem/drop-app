package run.drop.app.location

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class LocationIndicatorView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val pen: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        pen.color = Color.RED
    }

    fun update() {
        pen.color = Color.GREEN
        postDelayed({
            pen.color = Color.BLUE
            invalidate()
        }, 1500)
        invalidate()
    }

    fun stop() {
        pen.color = Color.RED
        invalidate()
    }

    fun start() {
        pen.color = Color.BLUE
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        canvas!!.drawCircle(30F, 30F, 30F, pen)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(60, 60)
    }
}