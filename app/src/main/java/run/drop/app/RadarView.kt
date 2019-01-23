package run.drop.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.location.Location
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import run.drop.app.apollo.Apollo
import java.util.*
import kotlin.collections.ArrayList

class RadarView : View {

    private var pen: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var location: Location? = null
    private var points: MutableList<PointF> = ArrayList()
    private var drops: List<DroppedRadarQuery.Dropped> = ArrayList()

    private val hndlr: Handler = Handler()
    private var timer: Timer = Timer()
    private val updateRadarTask: TimerTask = object : TimerTask()
    {
        private var counter: Int = 0

        override fun run() {
            hndlr.post {
                if (counter == 10) counter = 0
                if (counter == 0) updateDrops()

                updatePoints()
                postInvalidate()
                counter += 1
            }
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
        timer.schedule(updateRadarTask, 1000, 1000)
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
        location = DropActivity.locationHandler?.lastLocation
        if (location != null) {
            for (drop in drops) {
                val y = Math.sin(drop.location.longitude - location!!.longitude) * Math.cos(drop.location.latitude)
                val x = Math.cos(location!!.latitude) * Math.sin(drop.location.latitude) -
                        Math.sin(location!!.latitude) * Math.cos(drop.location.latitude) * Math.cos(drop.location.longitude - location!!.longitude)

                val brng = Math.toDegrees(Math.atan2(y, x)).toInt()

                addPoint(brng.toFloat(), width.toDouble(), height.toDouble())
            }
        }
    }

    private fun updateDrops() {
        Apollo.client.query(
                DroppedRadarQuery.builder().build()).enqueue(object : ApolloCall.Callback<DroppedRadarQuery.Data>() {

                    override fun onResponse(response: Response<DroppedRadarQuery.Data>) {
                        drops = response.data()!!.dropped()
                    }

                    override fun onFailure(e: ApolloException) {
                        Log.e("APOLLO", e.message)
                        e.printStackTrace()
                    }
                })
    }

    override fun onDraw(canvas: Canvas) {
        for (point in points) {
            canvas.drawCircle(point.x, point.y, 60f, pen)
        }
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(updateRadarTask)
        super.onDetachedFromWindow()
    }
}