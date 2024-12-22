package ca.kebs.onloc.android.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.app.ActivityCompat

class LocationService(private var context: Context, private var onLocationUpdate: (Location?, Boolean) -> Unit) {
    private var isUpdatingLocation: Boolean = false
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val locationListener = LocationListener { location -> onLocationUpdate(location, isUpdatingLocation) }
    private val provider = LocationManager.FUSED_PROVIDER

    fun startUpdates() {
        if (
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (!isUpdatingLocation) {
            locationManager.requestLocationUpdates(
                provider,
                15000L,
                0f,
                locationListener,
                Looper.getMainLooper()
            )

            isUpdatingLocation = true
        }
    }

    fun stopUpdates() {
        if (isUpdatingLocation) {
            locationManager.removeUpdates(locationListener)
            isUpdatingLocation = false
            onLocationUpdate(null, false)
        }
    }
}