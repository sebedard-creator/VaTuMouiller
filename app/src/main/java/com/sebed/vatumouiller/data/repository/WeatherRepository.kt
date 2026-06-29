package com.sebed.vatumouiller.data.repository

import android.content.Context
import com.sebed.vatumouiller.BuildConfig
import com.sebed.vatumouiller.data.api.OpenMeteoApi
import com.sebed.vatumouiller.data.api.TomorrowApi
import com.sebed.vatumouiller.data.api.OpenWeatherApi
import com.sebed.vatumouiller.data.model.OpenMeteoResponse
import com.sebed.vatumouiller.data.model.TomorrowResponse
import com.sebed.vatumouiller.data.model.OpenWeatherResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

data class WeatherForecast(
    val h1: Int,
    val h2: Int,
    val h3: Int,
    val h4: Int,
    val timestamp: Long,
    val isCached: Boolean = false,
    val openMeteoSucceeded: Boolean = false,
    val openMeteoError: String? = null,
    val tomorrowSucceeded: Boolean = false,
    val tomorrowError: String? = null,
    val openWeatherSucceeded: Boolean = false,
    val openWeatherError: String? = null
)

class WeatherRepository(
    private val context: Context,
    private val openMeteoApi: OpenMeteoApi,
    private val tomorrowApi: TomorrowApi,
    private val openWeatherApi: OpenWeatherApi
) {
    private val prefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)

    /**
     * Récupère la prévision consensus pour H+1, H+2, H+3 en interrogeant les 3 API.
     * En cas d'échec total, tente de récupérer les données du cache.
     */
    suspend fun getForecast(lat: Double, lon: Double): Result<WeatherForecast> = coroutineScope {
        val openMeteoDeferred = async {
            runCatching {
                openMeteoApi.getForecast(latitude = lat, longitude = lon)
            }
        }

        val tomorrowDeferred = async {
            runCatching {
                val apiKey = BuildConfig.TOMORROW_IO_KEY
                if (apiKey.isBlank()) throw IllegalStateException("Clé Tomorrow.io manquante")
                tomorrowApi.getForecast(location = "$lat,$lon", apiKey = apiKey)
            }
        }

        val openWeatherDeferred = async {
            runCatching {
                val apiKey = BuildConfig.OPEN_WEATHER_KEY
                if (apiKey.isBlank()) throw IllegalStateException("Clé OpenWeatherMap One Call manquante")
                openWeatherApi.getForecast(latitude = lat, longitude = lon, apiKey = apiKey)
            }
        }

        val openMeteoResult = openMeteoDeferred.await()
        val tomorrowResult = tomorrowDeferred.await()
        val openWeatherResult = openWeatherDeferred.await()

        val openMeteoRes = openMeteoResult.getOrNull()
        val tomorrowRes = tomorrowResult.getOrNull()
        val openWeatherRes = openWeatherResult.getOrNull()

        // Calculer les tranches de 60 minutes glissantes relatives (0-60, 61-120, 121-180, 181-240)
        val nowMs = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = nowMs
        val minutesOfHour = calendar.get(java.util.Calendar.MINUTE)

        // Calculer le début de l'heure pile actuelle (H0)
        val currentHourMs = nowMs - (nowMs % (3600 * 1000))

        // Obtenir les 5 heures absolues nécessaires pour interpoler nos tranches relatives
        val targetH0 = currentHourMs
        val targetH1 = currentHourMs + 3600 * 1000
        val targetH2 = currentHourMs + 2 * 3600 * 1000
        val targetH3 = currentHourMs + 3 * 3600 * 1000
        val targetH4 = currentHourMs + 4 * 3600 * 1000

        var p0 = calculateConsensus(targetH0, openMeteoRes, tomorrowRes, openWeatherRes)
        var p1 = calculateConsensus(targetH1, openMeteoRes, tomorrowRes, openWeatherRes)
        var p2 = calculateConsensus(targetH2, openMeteoRes, tomorrowRes, openWeatherRes)
        var p3 = calculateConsensus(targetH3, openMeteoRes, tomorrowRes, openWeatherRes)
        var p4 = calculateConsensus(targetH4, openMeteoRes, tomorrowRes, openWeatherRes)

        // Lissage : si une API a déjà purgé l'heure courante de son flux (ex: à 11:48, l'heure de 11:00 disparaît)
        if (p0 == null) p0 = p1
        if (p1 == null) p1 = p2
        if (p2 == null) p2 = p3
        if (p3 == null) p3 = p4
        if (p4 == null) p4 = p3

        if (p0 != null && p1 != null && p2 != null && p3 != null && p4 != null) {
            // Pondération : minutes restantes dans l'heure courante vs minutes écoulées dans la suivante
            val wCurrent = 60 - minutesOfHour
            val wNext = minutesOfHour

            val h1Val = ((p0 * wCurrent) + (p1 * wNext)) / 60
            val h2Val = ((p1 * wCurrent) + (p2 * wNext)) / 60
            val h3Val = ((p2 * wCurrent) + (p3 * wNext)) / 60
            val h4Val = ((p3 * wCurrent) + (p4 * wNext)) / 60

            val forecast = WeatherForecast(
                h1 = h1Val,
                h2 = h2Val,
                h3 = h3Val,
                h4 = h4Val,
                timestamp = nowMs,
                openMeteoSucceeded = openMeteoResult.isSuccess,
                openMeteoError = openMeteoResult.exceptionOrNull()?.localizedMessage,
                tomorrowSucceeded = tomorrowResult.isSuccess,
                tomorrowError = tomorrowResult.exceptionOrNull()?.localizedMessage,
                openWeatherSucceeded = openWeatherResult.isSuccess,
                openWeatherError = openWeatherResult.exceptionOrNull()?.localizedMessage
            )
            saveToCache(forecast)
            Result.success(forecast)
        } else {
            // Tenter de lire du cache si échec complet
            val cached = getCachedForecast()
            if (cached != null) {
                Result.success(cached.copy(isCached = true))
            } else {
                Result.failure(Exception("Échec de la récupération des données et aucun cache disponible."))
            }
        }
    }

    private fun calculateConsensus(
        targetMs: Long,
        openMeteo: OpenMeteoResponse?,
        tomorrow: TomorrowResponse?,
        openWeather: OpenWeatherResponse?
    ): Int? {
        val values = mutableListOf<Double>()

        // 1. Open-Meteo
        if (openMeteo != null) {
            findClosestOpenMeteo(targetMs, openMeteo)?.let { values.add(it) }
        }

        // 2. Tomorrow.io
        if (tomorrow != null) {
            findClosestTomorrow(targetMs, tomorrow)?.let { values.add(it) }
        }

        // 3. OpenWeatherMap
        if (openWeather != null) {
            findClosestOpenWeather(targetMs, openWeather)?.let { values.add(it) }
        }

        return if (values.isNotEmpty()) {
            values.average().roundToInt()
        } else {
            null
        }
    }

    private fun findClosestOpenMeteo(targetMs: Long, response: OpenMeteoResponse): Double? {
        val times = response.hourly.time
        val probs = response.hourly.precipitationProbability
        var closestVal: Double? = null
        var minDiff = Long.MAX_VALUE

        for (i in times.indices) {
            try {
                // Open-Meteo UTC time format: "2026-06-28T14:00" (grâce au paramètre timezone=UTC)
                val ldt = LocalDateTime.parse(times[i])
                val timeMs = ldt.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
                val diff = abs(timeMs - targetMs)
                // Tolérer une différence d'au plus 45 minutes par rapport à la cible
                if (diff < minDiff && diff < 45 * 60 * 1000) {
                    minDiff = diff
                    closestVal = probs[i].toDouble()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return closestVal
    }

    private fun findClosestTomorrow(targetMs: Long, response: TomorrowResponse): Double? {
        val items = response.timelines.hourly
        var closestVal: Double? = null
        var minDiff = Long.MAX_VALUE

        for (item in items) {
            try {
                // Tomorrow.io UTC ISO format: "2026-06-28T14:00:00Z"
                val instant = Instant.parse(item.time)
                val timeMs = instant.toEpochMilli()
                val diff = abs(timeMs - targetMs)
                if (diff < minDiff && diff < 45 * 60 * 1000) {
                    minDiff = diff
                    closestVal = item.values.precipitationProbability
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return closestVal
    }

    private fun findClosestOpenWeather(targetMs: Long, response: OpenWeatherResponse): Double? {
        val items = response.data
        var closestVal: Double? = null
        var minDiff = Long.MAX_VALUE

        for (item in items) {
            try {
                val timeMs = item.dt * 1000
                val diff = abs(timeMs - targetMs)
                if (diff < minDiff && diff < 45 * 60 * 1000) {
                    minDiff = diff
                    closestVal = item.pop * 100.0 // OWM pop est une fraction entre 0.0 et 1.0
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return closestVal
    }

    private fun saveToCache(forecast: WeatherForecast) {
        prefs.edit()
            .putInt("cached_h1", forecast.h1)
            .putInt("cached_h2", forecast.h2)
            .putInt("cached_h3", forecast.h3)
            .putInt("cached_h4", forecast.h4)
            .putLong("cached_timestamp", forecast.timestamp)
            .putBoolean("cached_open_meteo_success", forecast.openMeteoSucceeded)
            .putBoolean("cached_tomorrow_success", forecast.tomorrowSucceeded)
            .putBoolean("cached_open_weather_success", forecast.openWeatherSucceeded)
            .apply()
    }

    fun getCachedForecast(): WeatherForecast? {
        if (!prefs.contains("cached_timestamp")) return null
        return WeatherForecast(
            h1 = prefs.getInt("cached_h1", 0),
            h2 = prefs.getInt("cached_h2", 0),
            h3 = prefs.getInt("cached_h3", 0),
            h4 = prefs.getInt("cached_h4", 0),
            timestamp = prefs.getLong("cached_timestamp", 0),
            isCached = true,
            openMeteoSucceeded = prefs.getBoolean("cached_open_meteo_success", false),
            tomorrowSucceeded = prefs.getBoolean("cached_tomorrow_success", false),
            openWeatherSucceeded = prefs.getBoolean("cached_open_weather_success", false)
        )
    }

    fun getWidgetOpacity(): Float {
        return prefs.getFloat("widget_opacity", 0.90f) // Défaut à 90% pour un effet flouté premium
    }

    fun saveWidgetOpacity(opacity: Float) {
        prefs.edit().putFloat("widget_opacity", opacity).apply()
    }
}
