package com.example.weatherapp.data

import com.example.weatherapp.data.remote.WeatherApi
import com.example.weatherapp.data.remote.dto.GeocodingDto
import com.example.weatherapp.data.remote.dto.WeatherResponseDto
import com.example.weatherapp.domain.DataResult
import com.example.weatherapp.domain.WeatherError
import com.example.weatherapp.domain.WeatherRepository
import com.example.weatherapp.domain.model.Weather
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import kotlin.math.roundToInt

class WeatherRepositoryImpl @Inject constructor(
    private val api: WeatherApi,
    private val ioDispatcher: CoroutineDispatcher,
) : WeatherRepository {

    override suspend fun getWeatherForCity(city: String): DataResult<Weather> =
        withContext(ioDispatcher) {
            // Bias the query to the US, since the app targets US cities. Both endpoints accept
            // "city,state,country"; appending ",US" keeps results stateside.
            val query = if (city.contains(",")) city.trim() else "${city.trim()},US"

            // 1) Try the deprecated single-call by-city-name endpoint first (one request).
            when (val byCity = fetchByCityName(query)) {
                is DataResult.Success -> byCity
                is DataResult.Failure ->
                    // 2) Fall back to the recommended Geocoding -> weather-by-coords flow.
                    //    Propagate a network error as-is (retrying won't help); otherwise try geocoding.
                    if (byCity.error == WeatherError.NETWORK) byCity
                    else fetchByGeocoding(query)
            }
        }

    /** Deprecated by-city-name path: q=city,state,country -> current weather in one call. */
    private suspend fun fetchByCityName(query: String): DataResult<Weather> =
        runApiCall {
            val dto = api.currentWeatherByCity(query)
            DataResult.Success(dto.toWeather(fallbackLabel = dto.name.orEmpty()))
        }

    /** Recommended path: geocode city -> lat/lon -> current weather by coordinates. */
    private suspend fun fetchByGeocoding(query: String): DataResult<Weather> =
        runApiCall {
            val match = api.geocode(query).firstOrNull()
                ?: return@runApiCall DataResult.Failure(WeatherError.CITY_NOT_FOUND)

            val lat = match.lat
            val lon = match.lon
            if (lat == null || lon == null) {
                return@runApiCall DataResult.Failure(WeatherError.CITY_NOT_FOUND)
            }

            val dto = api.currentWeather(lat = lat, lon = lon)
            DataResult.Success(dto.toWeather(fallbackLabel = match.toLabel()))
        }

    override suspend fun getWeatherForLocation(lat: Double, lon: Double): DataResult<Weather> =
        withContext(ioDispatcher) {
            runApiCall {
                val dto = api.currentWeather(lat = lat, lon = lon)
                DataResult.Success(dto.toWeather(fallbackLabel = dto.name.orEmpty()))
            }
        }

    /** Centralised exception → typed-error mapping so every call site handles failures uniformly. */
    private inline fun runApiCall(block: () -> DataResult<Weather>): DataResult<Weather> =
        try {
            block()
        } catch (e: IOException) {
            DataResult.Failure(WeatherError.NETWORK)
        } catch (e: HttpException) {
            // 404 from geocoding/weather means the location wasn't found; anything else is unknown.
            val error = if (e.code() == 404) WeatherError.CITY_NOT_FOUND else WeatherError.UNKNOWN
            DataResult.Failure(error)
        } catch (e: Exception) {
            DataResult.Failure(WeatherError.UNKNOWN)
        }
}

// --- DTO → domain mapping ---------------------------------------------------------------

private fun WeatherResponseDto.toWeather(fallbackLabel: String): Weather {
    val condition = weather?.firstOrNull()
    val icon = condition?.icon.orEmpty()
    return Weather(
        cityLabel = name?.takeIf { it.isNotBlank() } ?: fallbackLabel,
        description = condition?.description.orEmpty().replaceFirstChar { it.uppercase() },
        iconUrl = if (icon.isNotBlank()) WeatherApi.ICON_URL.format(icon) else "",
        temperature = main?.temp.roundOrZero(),
        feelsLike = main?.feelsLike.roundOrZero(),
        tempMin = main?.tempMin.roundOrZero(),
        tempMax = main?.tempMax.roundOrZero(),
        humidityPercent = main?.humidity ?: 0,
        windMph = wind?.speed.roundOrZero(),
    )
}

private fun GeocodingDto.toLabel(): String =
    listOfNotNull(name, state, country).joinToString(", ")

private fun Double?.roundOrZero(): Int = this?.roundToInt() ?: 0
