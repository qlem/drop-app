package run.drop.app.location

import android.location.Location

interface OnLocationUpdateListener {
    fun onLocationUpdateListener(location: Location)
}