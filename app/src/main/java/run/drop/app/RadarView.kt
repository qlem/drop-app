package run.drop.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.location.Location
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import run.drop.app.location.LocationHandler
import kotlin.collections.ArrayList

class RadarView : View {

    private var pen: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var location: Location? = null
    private var points: MutableList<PointF> = ArrayList()

    private val mHandler = Handler()
    private val runnable = object : Runnable {
        override fun run() {
            updatePoints()
            mHandler.postDelayed(this, 2000)
        }
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        pen.color = Color.YELLOW
        mHandler.post(runnable)
    }

    private fun addPoint(angle: Float, width: Double, height: Double) {

        val d = Math.sqrt(width / 2 * (width / 2) + height / 2 * (height / 2))
        var x: Double = Math.sin(Math.toRadians(angle.toDouble())) * d.toFloat()
        var y: Double = -Math.cos(Math.toRadians(angle.toDouble())) * d.toFloat()

        if (x > width / 2) {
            val clipFraction = width / 2 / x
            x *= clipFraction
            y *= clipFraction
        }
        else if (x < -width / 2) {
            val clipFraction = -width / 2 / x
            x *= clipFraction
            y *= clipFraction
        }
        if (y > height / 2) {
            val clipFraction = height / 2 / y
            x *= clipFraction
            y *= clipFraction
        }
        else if (y < -height / 2) {
            val clipFraction = -height / 2 / y
            x *= clipFraction
            y *= clipFraction
        }
        x += width / 2
        y += height / 2

        points.add(PointF(x.toFloat(), y.toFloat()))
    }

    private fun updatePoints() {
        val width: Int = context.resources.displayMetrics.widthPixels
        val height: Int = context.resources.displayMetrics.heightPixels

        points.clear()
        location = LocationHandler.lastLocation
        if (location != null) {
            for (drop in DropActivity.drops) {
                val y = Math.sin(drop.longitude - location!!.longitude) * Math.cos(drop.latitude)
                val x = Math.cos(location!!.latitude) * Math.sin(drop.latitude) -
                        Math.sin(location!!.latitude) * Math.cos(drop.latitude) *
                        Math.cos(drop.longitude - location!!.longitude)

                val brng = Math.toDegrees(Math.atan2(y, x)).toInt()
                addPoint(brng.toFloat(), width.toDouble(), height.toDouble())
            }
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        for (point in points) {
            canvas.drawCircle(point.x, point.y, 60f, pen)
        }
    }

    override fun onDetachedFromWindow() {
        mHandler.removeCallbacks(runnable)
        super.onDetachedFromWindow()
    }
}