package com.example.weatherapp.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** A device latitude/longitude pair. */
data class Coordinates(val lat: Double, val lon: Double)

/**
 * Thin wrapper over FusedLocationProviderClient. Returns null whenever a location cannot be
 * obtained — permission missing, location off, or no fix available — so callers handle the
 * "no location" path without dealing with exceptions.
 */
class LocationProvider @Inject constructor(
    private val context: Context,
) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    /** True if either coarse or fine location permission is currently granted. */
    fun hasPermission(): Boolean {
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    /**
     * Current location, or null if unavailable. Permission is re-checked here (not just at the
     * call site) to satisfy lint and guard against a revocation between check and use.
     */
    @SuppressLint("MissingPermission") // guarded by hasPermission() immediately below
    suspend fun currentCoordinates(): Coordinates? {
        if (!hasPermission()) return null
        return runCatching {
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .build()
            client.getCurrentLocation(request, null).await()
        }.getOrNull()?.let { Coordinates(it.latitude, it.longitude) }
    }
}
