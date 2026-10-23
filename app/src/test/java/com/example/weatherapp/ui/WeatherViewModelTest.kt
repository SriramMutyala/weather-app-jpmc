package com.example.weatherapp.ui

import app.cash.turbine.test
import com.example.weatherapp.MainDispatcherRule
import com.example.weatherapp.data.location.Coordinates
import com.example.weatherapp.data.location.LocationProvider
import com.example.weatherapp.data.local.LastCityStore
import com.example.weatherapp.domain.DataResult
import com.example.weatherapp.domain.WeatherError
import com.example.weatherapp.domain.WeatherRepository
import com.example.weatherapp.domain.model.Weather
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: WeatherRepository = mockk()
    private val lastCityStore: LastCityStore = mockk(relaxed = true)
    private val locationProvider: LocationProvider = mockk()

    private val sampleWeather = Weather(
        cityLabel = "Chicago, IL, US",
        description = "Light rain",
        iconUrl = "https://openweathermap.org/img/wn/10d@2x.png",
        temperature = 54, feelsLike = 52, tempMin = 50, tempMax = 58,
        humidityPercent = 80, windMph = 7,
    )

    private fun viewModel() = WeatherViewModel(repository, lastCityStore, locationProvider)

    @Test
    fun `empty query search surfaces EMPTY_QUERY error without calling repository`() = runTest {
        val vm = viewModel()
        vm.onQueryChange("   ")
        vm.search()
        advanceUntilIdle()

        val content = vm.state.value.content
        assertThat(content).isInstanceOf(WeatherUiState.Content.Error::class.java)
        assertThat((content as WeatherUiState.Content.Error).type)
            .isEqualTo(WeatherUiState.ErrorType.EMPTY_QUERY)
        coVerify(exactly = 0) { repository.getWeatherForCity(any()) }
    }

    @Test
    fun `successful search emits Success and persists the city`() = runTest {
        coEvery { repository.getWeatherForCity("Chicago") } returns DataResult.Success(sampleWeather)
        val vm = viewModel()

        vm.onQueryChange("Chicago")
        vm.state.test {
            assertThat(awaitItem().content).isEqualTo(WeatherUiState.Content.Idle)
            vm.search()
            assertThat(awaitItem().content).isEqualTo(WeatherUiState.Content.Loading)
            val success = awaitItem().content
            assertThat(success).isEqualTo(WeatherUiState.Content.Success(sampleWeather))
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { lastCityStore.save("Chicago") }
    }

    @Test
    fun `city-not-found failure maps to CITY_NOT_FOUND error`() = runTest {
        coEvery { repository.getWeatherForCity(any()) } returns
            DataResult.Failure(WeatherError.CITY_NOT_FOUND)
        val vm = viewModel()

        vm.onQueryChange("Nowheresville")
        vm.search()
        advanceUntilIdle()

        val content = vm.state.value.content as WeatherUiState.Content.Error
        assertThat(content.type).isEqualTo(WeatherUiState.ErrorType.CITY_NOT_FOUND)
    }

    @Test
    fun `onStart with permission loads weather by location`() = runTest {
        every { locationProvider.hasPermission() } returns true
        coEvery { locationProvider.currentCoordinates() } returns Coordinates(41.88, -87.63)
        coEvery { repository.getWeatherForLocation(41.88, -87.63) } returns
            DataResult.Success(sampleWeather)
        val vm = viewModel()

        vm.onStart()
        advanceUntilIdle()

        assertThat(vm.state.value.content).isEqualTo(WeatherUiState.Content.Success(sampleWeather))
        coVerify(exactly = 0) { repository.getWeatherForCity(any()) }
    }

    @Test
    fun `onStart without permission auto-loads the last searched city`() = runTest {
        every { locationProvider.hasPermission() } returns false
        every { lastCityStore.lastCity } returns flowOf("Denver")
        coEvery { repository.getWeatherForCity("Denver") } returns DataResult.Success(sampleWeather)
        val vm = viewModel()

        vm.onStart()
        advanceUntilIdle()

        assertThat(vm.state.value.query).isEqualTo("Denver")
        assertThat(vm.state.value.content).isEqualTo(WeatherUiState.Content.Success(sampleWeather))
    }

    @Test
    fun `onStart without permission and no saved city stays Idle`() = runTest {
        every { locationProvider.hasPermission() } returns false
        every { lastCityStore.lastCity } returns flowOf(null)
        val vm = viewModel()

        vm.onStart()
        advanceUntilIdle()

        assertThat(vm.state.value.content).isEqualTo(WeatherUiState.Content.Idle)
    }
}
