package com.anandharaj.knowmeapp.product.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.anandharaj.knowmeapp.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.io.IOException
import java.util.Locale

object LocationUtils {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    fun initializeLocationClient(context: Context) {
        if (!::fusedLocationClient.isInitialized) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        }
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context, onLocationResult: (Location?) -> Unit) {
        initializeLocationClient(context)

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(context, context.getString(R.string.location_services_disabled_message), Toast.LENGTH_LONG).show()
            onLocationResult(null)
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000
        )
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    onLocationResult(location)
                    fusedLocationClient.removeLocationUpdates(this)
                } ?: run {
                    onLocationResult(null)
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun reverseGeocodeLocation(
        context: Context,
        latitude: Double,
        longitude: Double,
        onAddressResult: (String) -> Unit
    ) {
        val geocoder = Geocoder(context, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            handleGeocodingAsync(geocoder, latitude, longitude, onAddressResult, context)
        } else {
            handleGeocodingBlocking(geocoder, latitude, longitude, onAddressResult, context)
        }
    }

    @SuppressLint("NewApi")
    private fun handleGeocodingAsync(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double,
        onAddressResult: (String) -> Unit,
        context: Context
    ) {
        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) {
                if (addresses.isNotEmpty()) {
                    onAddressResult(formatAddress(addresses[0]))
                } else {
                    onAddressResult(context.getString(R.string.no_address_found_for_coordinates))
                }
            }

            override fun onError(errorMessage: String?) {
                val msg = context.getString(R.string.geocoding_error_prefix, errorMessage ?: context.getString(R.string.geocoding_unknown_error))
                onAddressResult(msg)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleGeocodingBlocking(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double,
        onAddressResult: (String) -> Unit,
        context: Context
    ) {
        Thread {
            try {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    onAddressResult(formatAddress(addresses[0]))
                } else {
                    onAddressResult(context.getString(R.string.no_address_found_for_coordinates))
                }
            } catch (e: IOException) {
                val msg = context.getString(R.string.geocoding_io_error_prefix, e.message ?: "")
                onAddressResult(msg)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: IllegalArgumentException) {
                val msg = context.getString(R.string.geocoding_invalid_coordinates_prefix, e.message ?: "")
                onAddressResult(msg)
            }
        }.start()
    }

    private fun formatAddress(address: Address): String {
        return buildString {
            append(address.locality)
        }.trim()
    }
}