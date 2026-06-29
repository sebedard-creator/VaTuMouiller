package com.sebed.vatumouiller.worker

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sebed.vatumouiller.VaTuMouillerApplication
import com.sebed.vatumouiller.widget.WeatherWidget

class WeatherUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as VaTuMouillerApplication).container
        val locationHelper = container.locationHelper
        val repository = container.weatherRepository

        // Récupérer la position géographique courante
        val location = locationHelper.getCurrentLocation()
        val lat: Double
        val lon: Double
        if (location != null) {
            lat = location.latitude
            lon = location.longitude
            // Sauvegarder la position GPS valide pour un usage ultérieur en arrière-plan
            val prefs = applicationContext.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putFloat("last_gps_lat", lat.toFloat())
                .putFloat("last_gps_lon", lon.toFloat())
                .apply()
        } else {
            // Récupérer la dernière position GPS enregistrée, sinon Montréal par défaut
            val prefs = applicationContext.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
            lat = prefs.getFloat("last_gps_lat", 45.5017f).toDouble()
            lon = prefs.getFloat("last_gps_lon", -73.5673f).toDouble()
        }

        // Interroger les 3 API météo et mettre à jour le cache
        val result = repository.getForecast(lat, lon)
        
        return if (result.isSuccess) {
            // Forcer le widget Glance à se redessiner avec les nouvelles données du cache
            try {
                val glanceIds = GlanceAppWidgetManager(applicationContext)
                    .getGlanceIds(WeatherWidget::class.java)
                
                glanceIds.forEach { glanceId ->
                    WeatherWidget().update(applicationContext, glanceId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Result.success()
        } else {
            // En cas d'erreur réseau temporaire, planifie un nouvel essai selon la politique de retry
            Result.retry()
        }
    }
}
