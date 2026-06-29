package com.sebed.vatumouiller.data.api

import com.sebed.vatumouiller.data.model.OpenWeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherApi {
    @GET("data/3.0/onecall")
    suspend fun getForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("exclude") exclude: String = "current,minutely,daily,alerts",
        @Query("appid") apiKey: String
    ): OpenWeatherResponse
}
