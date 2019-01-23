package run.drop.app.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

class LocationHandler(context: Context) {

    var lastLocation: Location? = null

    private var client: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

    private var onLocationUpdateListener: OnLocationUpdateListener? = null

    private var locationCallback = object : LocationCallback() {

        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            if (!locationAvailability.isLocationAvailable) {
                Toast.makeText(context, "Location not available", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onLocationResult(result: LocationResult)  {
            lastLocation = result.lastLocation
            onLocationUpdateListener?.onLocationUpdateListener(lastLocation as Location)
        }
    }

    init {
        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 1000
        locationRequest.fastestInterval = 500

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            client.requestLocationUpdates(locationRequest, locationCallback, null)
        } else {
            Toast.makeText(context, "Cannot access to location, permission denied",
                    Toast.LENGTH_SHORT).show()
        }
    }

    fun removeLocationUpdates() {
        client.removeLocationUpdates(locationCallback)
    }

    fun setOnLocationUpdateListener(listener: OnLocationUpdateListener) {
        onLocationUpdateListener = listener
    }
}