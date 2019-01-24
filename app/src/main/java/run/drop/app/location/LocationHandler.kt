package run.drop.app.location

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.location.Location
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.gms.common.api.ResolvableApiException

class LocationHandler(context: Context) {

    companion object {
        const val REQUEST_SETTINGS_CODE = 42
        var lastLocation: Location? = null
    }

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

        val builder: LocationSettingsRequest.Builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
        val result: Task<LocationSettingsResponse>? = LocationServices.getSettingsClient(context)
                .checkLocationSettings(builder.build())

        result?.addOnCompleteListener {
            try {
                it.getResult(ApiException::class.java)
                client.requestLocationUpdates(locationRequest, locationCallback, null)
            } catch (e: ApiException) {
                when (e.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->
                        try {
                            val resolvable: ResolvableApiException = e as ResolvableApiException
                            resolvable.startResolutionForResult(context as Activity, REQUEST_SETTINGS_CODE)
                        } catch (e: IntentSender.SendIntentException) {
                            e.printStackTrace()
                        } catch (e: ClassCastException) {
                            e.printStackTrace()
                        }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val activity = context as Activity
                        Toast.makeText(activity, "Accurate location not supported", Toast.LENGTH_SHORT).show()
                        activity.finish()
                    }
                }
            } catch (e: SecurityException) {
                Log.e("LocationHandler", "ACCESS_FINE_LOCATION permission required")
                e.printStackTrace()
            }
        }
    }

    fun removeLocationUpdates() {
        client.removeLocationUpdates(locationCallback)
    }

    fun setOnLocationUpdateListener(listener: OnLocationUpdateListener) {
        onLocationUpdateListener = listener
    }
}