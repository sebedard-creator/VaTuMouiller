package com.sebed.vatumouiller.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.sebed.vatumouiller.widget.WeatherWidget
import com.sebed.vatumouiller.VaTuMouillerApplication
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DebugActivity : AppCompatActivity() {

    private lateinit var btnTrigger: Button
    private lateinit var btnBgPermission: Button
    private lateinit var tvLogs: TextView
    private lateinit var tvOpacityLabel: TextView
    private lateinit var sbOpacity: SeekBar

    private val container by lazy {
        (application as VaTuMouillerApplication).container
    }

    // Gestionnaire d'autorisation pour la localisation premier plan
    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            log("Permissions de localisation au premier plan accordées.")
            checkBackgroundLocationPermission()
        } else {
            log("Permissions de localisation refusées.")
        }
    }

    // Gestionnaire d'autorisation pour la localisation arrière-plan
    private val requestBgLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            log("Permission de localisation en arrière-plan accordée.")
            btnBgPermission.visibility = View.GONE
        } else {
            log("Permission de localisation en arrière-plan refusée.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.sebed.vatumouiller.R.layout.activity_debug)

        btnTrigger = findViewById(com.sebed.vatumouiller.R.id.btn_trigger)
        btnBgPermission = findViewById(com.sebed.vatumouiller.R.id.btn_request_bg_permission)
        tvLogs = findViewById(com.sebed.vatumouiller.R.id.tv_logs)
        tvOpacityLabel = findViewById(com.sebed.vatumouiller.R.id.tv_opacity_label)
        sbOpacity = findViewById(com.sebed.vatumouiller.R.id.sb_opacity)

        // Charger l'opacité actuelle et l'afficher
        val currentOpacity = container.weatherRepository.getWidgetOpacity()
        val currentProgress = (currentOpacity * 100).toInt()
        sbOpacity.progress = currentProgress
        tvOpacityLabel.text = "Opacité du fond du Widget : $currentProgress%"

        sbOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val opacity = progress / 100f
                container.weatherRepository.saveWidgetOpacity(opacity)
                tvOpacityLabel.text = "Opacité du fond du Widget : $progress%"

                // Mettre à jour le widget Glance en temps réel
                lifecycleScope.launch {
                    try {
                        val glanceIds = GlanceAppWidgetManager(this@DebugActivity)
                            .getGlanceIds(WeatherWidget::class.java)
                        glanceIds.forEach { glanceId ->
                            WeatherWidget().update(this@DebugActivity, glanceId)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnTrigger.setOnClickListener {
            checkPermissionsAndFetch()
        }

        btnBgPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestBgLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }

        checkForegroundPermissions()
    }

    private fun checkForegroundPermissions() {
        if (!container.locationHelper.hasLocationPermissions()) {
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            checkBackgroundLocationPermission()
        }
    }

    private fun checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bgGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!bgGranted) {
                btnBgPermission.visibility = View.VISIBLE
                log("Note : La permission de localisation en arrière-plan est requise pour le widget.")
            } else {
                btnBgPermission.visibility = View.GONE
            }
        }
    }

    private fun checkPermissionsAndFetch() {
        if (!container.locationHelper.hasLocationPermissions()) {
            log("Erreur : Permissions de localisation manquantes.")
            checkForegroundPermissions()
            return
        }

        lifecycleScope.launch {
            log("Démarrage de la localisation GPS...")
            val location = container.locationHelper.getCurrentLocation()
            if (location == null) {
                log("Impossible de récupérer la position GPS. Assurez-vous que la localisation de l'appareil est activée.")
                return@launch
            }

            val lat = location.latitude
            val lon = location.longitude
            log("GPS obtenu : Lat = $lat, Lon = $lon")
            log("Appels réseau en cours aux 3 API (Open-Meteo, Tomorrow.io, OpenWeatherMap)...")

            val result = container.weatherRepository.getForecast(lat, lon)
            result.fold(
                onSuccess = { forecast ->
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                    val timeStr = sdf.format(Date(forecast.timestamp))
                    log("--- SUCCÈS ---")
                    log("Statut des services météo :")
                    log("- Open-Meteo : ${if (forecast.openMeteoSucceeded) "✅ SUCCESS" else "❌ FAILED (${forecast.openMeteoError ?: "Inconnu"})"}")
                    log("- Tomorrow.io : ${if (forecast.tomorrowSucceeded) "✅ SUCCESS" else "❌ FAILED (${forecast.tomorrowError ?: "Inconnu"})"}")
                    log("- OpenWeatherMap : ${if (forecast.openWeatherSucceeded) "✅ SUCCESS" else "❌ FAILED (${forecast.openWeatherError ?: "Inconnu"})"}")
                    log("----------------")
                    log("H+1 (Prochaine heure) : ${forecast.h1}% de précipitation")
                    log("H+2 (Dans 2h) : ${forecast.h2}% de précipitation")
                    log("H+3 (Dans 3h) : ${forecast.h3}% de précipitation")
                    log("H+4 (Dans 4h) : ${forecast.h4}% de précipitation")
                    log("Source : ${if (forecast.isCached) "Cache" else "Réseau (Moyenne des API fonctionnelles)"}")
                    log("Mis à jour à : $timeStr")
                },
                onFailure = { error ->
                    log("--- ÉCHEC ---")
                    log("Erreur : ${error.localizedMessage}")
                }
            )
        }
    }

    private fun log(message: String) {
        val currentLogs = tvLogs.text.toString()
        val newLogs = "$currentLogs\n> $message"
        tvLogs.text = newLogs
    }
}
