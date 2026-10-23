package com.example.weatherapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.data.location.LocationProvider
import com.example.weatherapp.data.local.LastCityStore
import com.example.weatherapp.domain.DataResult
import com.example.weatherapp.domain.WeatherError
import com.example.weatherapp.domain.WeatherRepository
import com.example.weatherapp.domain.model.Weather
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val lastCityStore: LastCityStore,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(WeatherUiState())
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
    }

    /** True if location permission is already granted — lets the UI skip a redundant request. */
    fun hasLocationPermission(): Boolean = locationProvider.hasPermission()

    /**
     * Called once when the screen first appears. Prefers device location if permission is
     * already granted; otherwise falls back to auto-loading the last searched city.
     * The permission *request* itself is driven by the UI — this only uses an existing grant.
     */
    fun onStart() {
        viewModelScope.launch {
            if (locationProvider.hasPermission()) {
                loadByLocationInternal(silentIfUnavailable = true)
            } else {
                loadLastCity()
            }
        }
    }

    /** Explicitly load weather for the current device location (e.g. after a granted request). */
    fun loadByLocation() {
        viewModelScope.launch { loadByLocationInternal(silentIfUnavailable = false) }
    }

    fun search() {
        val city = _state.value.query.trim()
        if (city.isEmpty()) {
            _state.update { it.copy(content = WeatherUiState.Content.Error(WeatherUiState.ErrorType.EMPTY_QUERY)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(content = WeatherUiState.Content.Loading) }
            when (val result = repository.getWeatherForCity(city)) {
                is DataResult.Success -> {
                    lastCityStore.save(city)
                    emitSuccess(result.data)
                }
                is DataResult.Failure -> emitError(result.error)
            }
        }
    }

    private suspend fun loadLastCity() {
        val last = lastCityStore.lastCity.first()
        if (last.isNullOrBlank()) return // nothing to auto-load; leave Idle
        _state.update { it.copy(query = last, content = WeatherUiState.Content.Loading) }
        when (val result = repository.getWeatherForCity(last)) {
            is DataResult.Success -> emitSuccess(result.data)
            is DataResult.Failure -> emitError(result.error)
        }
    }

    private suspend fun loadByLocationInternal(silentIfUnavailable: Boolean) {
        val coords = locationProvider.currentCoordinates()
        if (coords == null) {
            // On launch we don't want to nag; fall back to last city silently.
            if (silentIfUnavailable) loadLastCity()
            else emitError(WeatherError.LOCATION_UNAVAILABLE)
            return
        }
        _state.update { it.copy(content = WeatherUiState.Content.Loading) }
        when (val result = repository.getWeatherForLocation(coords.lat, coords.lon)) {
            is DataResult.Success -> {
                // Persist the resolved city name so it survives a later permission revocation.
                result.data.cityLabel.takeIf { it.isNotBlank() }?.let { lastCityStore.save(it) }
                emitSuccess(result.data)
            }
            is DataResult.Failure -> emitError(result.error)
        }
    }

    private fun emitSuccess(weather: Weather) {
        _state.update { it.copy(content = WeatherUiState.Content.Success(weather)) }
    }

    private fun emitError(error: WeatherError) {
        val type = when (error) {
            WeatherError.CITY_NOT_FOUND -> WeatherUiState.ErrorType.CITY_NOT_FOUND
            WeatherError.NETWORK -> WeatherUiState.ErrorType.NETWORK
            WeatherError.LOCATION_UNAVAILABLE -> WeatherUiState.ErrorType.LOCATION_UNAVAILABLE
            WeatherError.UNKNOWN -> WeatherUiState.ErrorType.GENERIC
        }
        _state.update { it.copy(content = WeatherUiState.Content.Error(type)) }
    }
}
