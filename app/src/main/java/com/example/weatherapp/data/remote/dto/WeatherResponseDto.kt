package com.example.weatherapp.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response from /data/2.5/weather. Only the fields the app uses are modeled; Moshi ignores
 * the rest. Fields are nullable so a partial/garbled payload degrades gracefully instead of
 * crashing deserialization.
 */
@JsonClass(generateAdapter = true)
data class WeatherResponseDto(
    @Json(name = "name") val name: String?,
    @Json(name = "weather") val weather: List<WeatherConditionDto>?,
    @Json(name = "main") val main: MainDto?,
    @Json(name = "wind") val wind: WindDto?,
    @Json(name = "sys") val sys: SysDto?,
)

@JsonClass(generateAdapter = true)
data class WeatherConditionDto(
    @Json(name = "description") val description: String?,
    @Json(name = "icon") val icon: String?,
)

@JsonClass(generateAdapter = true)
data class MainDto(
    @Json(name = "temp") val temp: Double?,
    @Json(name = "feels_like") val feelsLike: Double?,
    @Json(name = "temp_min") val tempMin: Double?,
    @Json(name = "temp_max") val tempMax: Double?,
    @Json(name = "humidity") val humidity: Int?,
)

@JsonClass(generateAdapter = true)
data class WindDto(
    @Json(name = "speed") val speed: Double?,
)

@JsonClass(generateAdapter = true)
data class SysDto(
    @Json(name = "country") val country: String?,
)
