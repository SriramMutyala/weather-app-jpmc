package com.example.weatherapp.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * One entry from the Geocoding API (/geo/1.0/direct). `state` is only present for US results.
 */
@JsonClass(generateAdapter = true)
data class GeocodingDto(
    @Json(name = "name") val name: String?,
    @Json(name = "lat") val lat: Double?,
    @Json(name = "lon") val lon: Double?,
    @Json(name = "state") val state: String?,
    @Json(name = "country") val country: String?,
)
