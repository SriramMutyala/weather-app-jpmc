package com.example.weatherapp.domain

/**
 * Minimal sealed result for repository calls. Keeps error handling explicit and typed
 * (vs. throwing across layers), so the ViewModel can map each case to a UI state.
 *
 * Named DataResult to avoid colliding with kotlin.Result.
 */
sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>
    data class Failure(val error: WeatherError) : DataResult<Nothing>
}

/** Categorised failures the UI can message distinctly. */
enum class WeatherError {
    CITY_NOT_FOUND,
    NETWORK,
    LOCATION_UNAVAILABLE,
    UNKNOWN,
}
