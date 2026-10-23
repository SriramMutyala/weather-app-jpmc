package com.example.weatherapp.data.remote

import com.example.weatherapp.data.remote.dto.GeocodingDto
import com.example.weatherapp.data.remote.dto.WeatherResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * OpenWeatherMap endpoints.
 *
 * The app resolves a city two ways (see [WeatherRepositoryImpl]):
 *  - [currentWeatherByCity]: the by-city-name weather call (q=city,state,country). Per the API
 *    docs this path is DEPRECATED (still functional, no longer bug-fixed), so it is tried first
 *    as a single-request shortcut and falls back to the geocoding flow below on failure.
 *  - [geocode] + [currentWeather]: the recommended flow — resolve city -> lat/lon, then fetch
 *    weather by coordinates.
 *
 * The API key is attached by [ApiKeyInterceptor], so it is not a parameter here.
 */
interface WeatherApi {

    @GET("geo/1.0/direct")
    suspend fun geocode(
        @Query("q") query: String,     // e.g. "Chicago,IL,US"
        @Query("limit") limit: Int = 1,
    ): List<GeocodingDto>

    @GET("data/2.5/weather")
    suspend fun currentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "imperial",
    ): WeatherResponseDto

    // ua-debt: deprecated by-city-name weather endpoint, drop this path if OpenWeatherMap retires it
    @GET("data/2.5/weather")
    suspend fun currentWeatherByCity(
        @Query("q") query: String,     // e.g. "Chicago,IL,US"
        @Query("units") units: String = "imperial",
    ): WeatherResponseDto

    companion object {
        const val BASE_URL = "https://api.openweathermap.org/"
        const val ICON_URL = "https://openweathermap.org/img/wn/%s@2x.png"
    }
}
