package com.sebed.vatumouiller.data.api

import com.sebed.vatumouiller.data.model.OpenMeteoResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "precipitation_probability",
        @Query("models") models: String = "gem_hrdps",
        @Query("forecast_days") forecastDays: Int = 1,
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse
}
