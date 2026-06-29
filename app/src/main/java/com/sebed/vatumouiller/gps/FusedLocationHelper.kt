package com.sebed.vatumouiller.gps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

class FusedLocationHelper(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    /**
     * Vérifie si l'application possède au moins l'une des permissions de localisation.
     */
    fun hasLocationPermissions(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    /**
     * Récupère la position courante de l'appareil de manière asynchrone (suspendue).
     * Retourne null en cas d'échec ou de permissions manquantes.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermissions()) {
            return null
        }

        return try {
            // Tenter d'obtenir d'abord la dernière position connue
            val lastLocation = fusedLocationClient.lastLocation.await()
            if (lastLocation != null && isLocationRecent(lastLocation)) {
                lastLocation
            } else {
                // Sinon, demander une mise à jour fraîche avec une consommation d'énergie équilibrée
                val cts = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cts.token
                ).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Détermine si une coordonnée GPS est récente (moins de 5 minutes).
     */
    private fun isLocationRecent(location: Location): Boolean {
        val fiveMinutesInMs = 5 * 60 * 1000
        val locationAge = System.currentTimeMillis() - location.time
        return locationAge < fiveMinutesInMs
    }
}
