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
        if (location == null) {
            // Tente de s'exécuter à nouveau plus tard si le GPS est temporairement inaccessible
            return Result.retry()
        }

        // Interroger les 3 API météo et mettre à jour le cache
        val result = repository.getForecast(location.latitude, location.longitude)
        
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
