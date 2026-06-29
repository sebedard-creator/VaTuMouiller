package com.sebed.vatumouiller.data.api

import com.sebed.vatumouiller.data.model.TomorrowResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface TomorrowApi {
    @GET("v4/weather/forecast")
    suspend fun getForecast(
        @Query("location") location: String, // format "lat,lon"
        @Query("timesteps") timesteps: String = "1h",
        @Query("fields") fields: String = "precipitationProbability",
        @Query("apikey") apiKey: String
    ): TomorrowResponse
}
