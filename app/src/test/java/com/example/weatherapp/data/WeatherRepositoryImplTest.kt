package com.example.weatherapp.data

import com.example.weatherapp.data.remote.WeatherApi
import com.example.weatherapp.data.remote.dto.GeocodingDto
import com.example.weatherapp.data.remote.dto.MainDto
import com.example.weatherapp.data.remote.dto.WeatherConditionDto
import com.example.weatherapp.data.remote.dto.WeatherResponseDto
import com.example.weatherapp.data.remote.dto.WindDto
import com.example.weatherapp.domain.DataResult
import com.example.weatherapp.domain.WeatherError
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherRepositoryImplTest {

    private val api: WeatherApi = mockk()

    // Build the repo with a dispatcher backed by the test's own scheduler, so withContext()
    // inside the repo shares the runTest scheduler instead of a disconnected one.
    private fun TestScope.repo() =
        WeatherRepositoryImpl(api, UnconfinedTestDispatcher(testScheduler))

    private val chicagoWeather = WeatherResponseDto(
        name = "Chicago",
        weather = listOf(WeatherConditionDto(description = "light rain", icon = "10d")),
        main = MainDto(temp = 53.6, feelsLike = 51.9, tempMin = 50.0, tempMax = 57.4, humidity = 80),
        wind = WindDto(speed = 7.4),
        sys = null,
    )

    private fun http404() = HttpException(
        Response.error<Any>(404, "".toResponseBody("application/json".toMediaType())),
    )

    @Test
    fun `by-city-name call succeeds and maps DTO to domain in imperial units`() = runTest {
        coEvery { api.currentWeatherByCity("Chicago,US", any()) } returns chicagoWeather

        val result = repo().getWeatherForCity("Chicago")

        assertThat(result).isInstanceOf(DataResult.Success::class.java)
        val w = (result as DataResult.Success).data
        assertThat(w.temperature).isEqualTo(54)      // 53.6 rounds to 54
        assertThat(w.windMph).isEqualTo(7)
        assertThat(w.description).isEqualTo("Light rain") // first char capitalized
        assertThat(w.iconUrl).isEqualTo("https://openweathermap.org/img/wn/10d@2x.png")
        // Geocoding should not be hit when the by-city call already succeeds.
        coVerify(exactly = 0) { api.geocode(any(), any()) }
    }

    @Test
    fun `by-city-name 404 falls back to geocoding flow`() = runTest {
        coEvery { api.currentWeatherByCity(any(), any()) } throws http404()
        coEvery { api.geocode("Chicago,US", any()) } returns listOf(
            GeocodingDto(name = "Chicago", lat = 41.88, lon = -87.63, state = "Illinois", country = "US"),
        )
        coEvery { api.currentWeather(41.88, -87.63, any()) } returns chicagoWeather

        val result = repo().getWeatherForCity("Chicago")

        assertThat(result).isInstanceOf(DataResult.Success::class.java)
        coVerify { api.geocode("Chicago,US", any()) }
    }

    @Test
    fun `both endpoints find nothing yields CITY_NOT_FOUND`() = runTest {
        coEvery { api.currentWeatherByCity(any(), any()) } throws http404()
        coEvery { api.geocode(any(), any()) } returns emptyList()

        val result = repo().getWeatherForCity("Atlantis")

        assertThat(result).isEqualTo(DataResult.Failure(WeatherError.CITY_NOT_FOUND))
    }

    @Test
    fun `network error on by-city call returns NETWORK without falling back`() = runTest {
        coEvery { api.currentWeatherByCity(any(), any()) } throws IOException("offline")

        val result = repo().getWeatherForCity("Chicago")

        assertThat(result).isEqualTo(DataResult.Failure(WeatherError.NETWORK))
        coVerify(exactly = 0) { api.geocode(any(), any()) }
    }
}
