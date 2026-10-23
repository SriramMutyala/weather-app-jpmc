package com.example.weatherapp.domain.model

/**
 * Domain representation of current weather — decoupled from the OpenWeatherMap wire format
 * so the UI never depends on API-specific DTO shapes. All temperatures are in °F
 * (the app requests imperial units for US cities).
 */
data class Weather(
    val cityLabel: String,       // e.g. "Chicago, IL, US"
    val description: String,     // e.g. "light rain"
    val iconUrl: String,         // fully-qualified OpenWeatherMap icon URL
    val temperature: Int,        // °F, rounded
    val feelsLike: Int,          // °F, rounded
    val tempMin: Int,            // °F, rounded
    val tempMax: Int,            // °F, rounded
    val humidityPercent: Int,
    val windMph: Int,
)
