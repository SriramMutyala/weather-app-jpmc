package com.example.weatherapp.ui

import com.example.weatherapp.domain.model.Weather

/**
 * Single immutable snapshot of the search screen. The ViewModel owns it as StateFlow; the
 * Composable just renders it. `content` is a sealed sub-state so the UI renders exactly one of
 * idle / loading / success / error without ambiguous flag combinations.
 */
data class WeatherUiState(
    val query: String = "",
    val content: Content = Content.Idle,
) {
    sealed interface Content {
        data object Idle : Content
        data object Loading : Content
        data class Success(val weather: Weather) : Content
        data class Error(val type: ErrorType) : Content
    }

    enum class ErrorType {
        EMPTY_QUERY,
        CITY_NOT_FOUND,
        NETWORK,
        LOCATION_UNAVAILABLE,
        GENERIC,
    }
}
