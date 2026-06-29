package com.sebed.vatumouiller.data.api

import com.sebed.vatumouiller.data.model.OpenWeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherApi {
    @GET("data/4.0/onecall/timeline/1h")
    suspend fun getForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String
    ): OpenWeatherResponse
}
