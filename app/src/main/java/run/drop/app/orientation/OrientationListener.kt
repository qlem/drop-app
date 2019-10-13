package run.drop.app.orientation

import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorEvent
import run.drop.app.location.LocationManager


open class OrientationListener : SensorEventListener {

    companion object {
        var North: Double = 0.0
        private val gravity = FloatArray(3)
        private val geomagnetic = FloatArray(3)
    }
    private var smoothed = FloatArray(3)

    private fun lowPassFilter(input: FloatArray?, prev: FloatArray?): FloatArray {
        if (input != null && prev != null)
            if (input.size == prev.size) {

                for (i in input.indices) {
                    prev[i] = prev[i] + 0.2f * (input[i] - prev[i])
                }
            }
        return prev!!
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        var changeRequired = false
        val rotation = FloatArray(9)
        val orientation = FloatArray(3)

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            smoothed = lowPassFilter(event.values, gravity)
            gravity[0] = smoothed[0]
            gravity[1] = smoothed[1]
            gravity[2] = smoothed[2]
            changeRequired = true
        }
        else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            smoothed = lowPassFilter(event.values, geomagnetic)
            geomagnetic[0] = smoothed[0]
            geomagnetic[1] = smoothed[1]
            geomagnetic[2] = smoothed[2]
            changeRequired = true
        }

        if (changeRequired) {
            SensorManager.getRotationMatrix(rotation, null, gravity, geomagnetic)
            SensorManager.getOrientation(rotation, orientation)
            North = orientation[0].toDouble()
            North = Math.toDegrees(North)

            if (LocationManager.geoField != null) {
                North += LocationManager.geoField!!.declination.toDouble()
            }

            if (North < 0) {
                North += 360
            }
        }
    }
}