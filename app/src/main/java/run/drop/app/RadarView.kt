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
import run.drop.app.sensor.SensorListener
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import run.drop.app.apollo.Apollo
import android.util.Log
import io.sentry.Sentry
import io.sentry.event.BreadcrumbBuilder
import io.sentry.event.UserBuilder


class RadarView : View {

    private var pen: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var location: Location? = null
    private var points: MutableList<PointF> = ArrayList()
    private var drops: List<DroppedAroundQuery.DroppedAround> = ArrayList()

    private val mHandler = Handler()
    private val runnable = object : Runnable {
        override fun run() {
            location = LocationHandler.lastLocation
            if (location != null) {
                updateDrops()
                updatePoints()
            }
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
        pen.alpha = (255 * 0.3).toInt() // 30% transparency
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
        val locationB = Location("drop")

        points.clear()

        for (drop in drops) {
            locationB.latitude = drop.location.latitude
            locationB.longitude = drop.location.longitude

            var brng = location!!.bearingTo(locationB)
            if (brng < 0)
                brng += 360

            val heading = (brng - SensorListener.North) % 360

            if (location!!.distanceTo(locationB).toInt() > 10)
                addPoint(heading.toFloat(), width.toDouble() + 300, height.toDouble() + 300)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        for (point in points) {
            canvas.drawCircle(point.x - 150, point.y - 150, 200f, pen)
        }
    }

    private fun updateDrops() {
        Apollo.client.query(
                DroppedAroundQuery.builder()
                        .latitude(location!!.latitude)
                        .longitude(location!!.longitude)
                        .radius(200.0)
                        .build()).enqueue(object : ApolloCall.Callback<DroppedAroundQuery.Data>() {

            override fun onResponse(response: Response<DroppedAroundQuery.Data>) {
                drops = response.data()!!.droppedAround
            }

            override fun onFailure(e: ApolloException) {
                Log.e("APOLLO", e.message)

                Sentry.getContext().recordBreadcrumb(
                        BreadcrumbBuilder().setMessage("Failed to Update Drops APOLLO").build()
                )

                val email = context.getSharedPreferences("Drop", Context.MODE_PRIVATE).getString("email", "")
                Sentry.getContext().user = UserBuilder().setEmail(email).build()

                Sentry.capture(e)
                Sentry.getContext().clear()

                e.printStackTrace()
            }
        })
    }

    override fun onDetachedFromWindow() {
        mHandler.removeCallbacks(runnable)
        super.onDetachedFromWindow()
    }
}