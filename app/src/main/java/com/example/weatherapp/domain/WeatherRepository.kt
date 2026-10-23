package com.example.weatherapp.domain

import com.example.weatherapp.domain.model.Weather

/**
 * Abstraction over weather retrieval. The interface lives in the domain layer so the
 * ViewModel depends on it (not on Retrofit), and tests can supply a fake/mock.
 */
interface WeatherRepository {

    /**
     * Fetch current weather for a US city. Tries the by-city-name weather call first, then
     * falls back to geocoding the city to coordinates and fetching by lat/lon.
     */
    suspend fun getWeatherForCity(city: String): DataResult<Weather>

    /** Fetch current weather for explicit coordinates (used for device location). */
    suspend fun getWeatherForLocation(lat: Double, lon: Double): DataResult<Weather>
}
