package com.sebed.vatumouiller.di

import android.content.Context
import com.sebed.vatumouiller.data.api.OpenMeteoApi
import com.sebed.vatumouiller.data.api.TomorrowApi
import com.sebed.vatumouiller.data.api.OpenWeatherApi
import com.sebed.vatumouiller.data.repository.WeatherRepository
import com.sebed.vatumouiller.gps.FusedLocationHelper
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

class DependencyContainer(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    val openMeteoApi: OpenMeteoApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenMeteoApi::class.java)
    }

    val tomorrowApi: TomorrowApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.tomorrow.io/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TomorrowApi::class.java)
    }

    val openWeatherApi: OpenWeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenWeatherApi::class.java)
    }

    val locationHelper: FusedLocationHelper by lazy {
        FusedLocationHelper(context)
    }

    val weatherRepository: WeatherRepository by lazy {
        WeatherRepository(
            context = context,
            openMeteoApi = openMeteoApi,
            tomorrowApi = tomorrowApi,
            openWeatherApi = openWeatherApi
        )
    }
}
