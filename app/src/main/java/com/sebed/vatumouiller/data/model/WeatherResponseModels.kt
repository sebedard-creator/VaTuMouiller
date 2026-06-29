package com.sebed.vatumouiller.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Open-Meteo HRDPS ---
@Serializable
data class OpenMeteoResponse(
    val hourly: OpenMeteoHourly
)

@Serializable
data class OpenMeteoHourly(
    val time: List<String>,
    @SerialName("precipitation_probability") val precipitationProbability: List<Int>
)


// --- Tomorrow.io ---
@Serializable
data class TomorrowResponse(
    val timelines: TomorrowTimelines
)

@Serializable
data class TomorrowTimelines(
    val hourly: List<TomorrowHourlyItem>
)

@Serializable
data class TomorrowHourlyItem(
    val time: String,
    val values: TomorrowValues
)

@Serializable
data class TomorrowValues(
    val precipitationProbability: Double
)


// --- OpenWeatherMap One Call ---
@Serializable
data class OpenWeatherResponse(
    val hourly: List<OpenWeatherHourlyItem>
)

@Serializable
data class OpenWeatherHourlyItem(
    val dt: Long,
    val pop: Double
)
