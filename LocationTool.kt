package com.meshai.tools.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.meshai.tools.AgentTool
import com.meshai.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Agent tool for getting the device's current GPS location.
 *
 * Uses the fused location provider path via LocationManager.
 * For production, prefer FusedLocationProviderClient from Google Play Services.
 *
 * Permissions required: ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION
 */
@Singleton
class LocationTool @Inject constructor(
    @ApplicationContext private val context: Context
) : AgentTool {

    override val name = "get_location"
    override val description = "Get the device's current GPS coordinates and address"
    override val inputSchema = """{"accuracy": "high|low (default: low)", "timeout_seconds": "number (default: 10)"}"""

    override suspend fun execute(jsonInput: String): ToolResult {
        if (!hasLocationPermission()) {
            return ToolResult.failure("Location permission (ACCESS_FINE_LOCATION) not granted")
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
            return ToolResult.failure("Location services are disabled on this device")
        }

        return try {
            val input = runCatching { Json.decodeFromString<LocationInput>(jsonInput) }
                .getOrDefault(LocationInput())

            val timeoutMs = (input.timeout_seconds * 1000L).coerceIn(3000L, 30_000L)

            val location = withTimeoutOrNull(timeoutMs) {
                getLastKnownLocation(locationManager) ?: requestFreshLocation(locationManager)
            }

            if (location == null) {
                return ToolResult.failure("Could not obtain location within ${input.timeout_seconds}s")
            }

            Timber.i("[LocationTool] Got location: ${location.latitude}, ${location.longitude} (accuracy: ${location.accuracy}m)")

            ToolResult.success(
                "Location: ${location.latitude}, ${location.longitude} (±${location.accuracy.toInt()}m)",
                mapOf(
                    "latitude" to location.latitude.toString(),
                    "longitude" to location.longitude.toString(),
                    "accuracy_meters" to location.accuracy.toString(),
                    "altitude" to location.altitude.toString(),
                    "provider" to (location.provider ?: "unknown")
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "[LocationTool] Location error")
            ToolResult.failure("Location error: ${e.message}")
        }
    }

    @Suppress("MissingPermission")
    private fun getLastKnownLocation(manager: LocationManager): Location? {
        val providers = manager.getProviders(true)
        return providers
            .mapNotNull { provider ->
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.accuracy } // best accuracy wins
    }

    @Suppress("MissingPermission")
    private suspend fun requestFreshLocation(manager: LocationManager): Location? =
        suspendCancellableCoroutine { cont ->
            val provider = when {
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> {
                    cont.resume(null)
                    return@suspendCancellableCoroutine
                }
            }

            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    cont.resume(location)
                }
            }

            manager.requestSingleUpdate(provider, listener, null)
            cont.invokeOnCancellation { manager.removeUpdates(listener) }
        }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    @Serializable
    private data class LocationInput(
        val accuracy: String = "low",
        val timeout_seconds: Int = 10
    )
}
