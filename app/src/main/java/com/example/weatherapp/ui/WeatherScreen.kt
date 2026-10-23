package com.example.weatherapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.weatherapp.R
import com.example.weatherapp.domain.model.Weather

/**
 * Stateless screen: takes the current [state] and callbacks, renders one of the content
 * sub-states. Kept free of ViewModel/Hilt references so it is trivially previewable/testable.
 */
@Composable
fun WeatherScreen(
    state: WeatherUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onUseLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            )
            IconButton(onClick = onUseLocation) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = stringResource(R.string.use_my_location),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onSearch,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.search_button))
        }

        Spacer(Modifier.height(24.dp))

        when (val content = state.content) {
            WeatherUiState.Content.Idle ->
                CenterMessage(stringResource(R.string.empty_prompt))

            WeatherUiState.Content.Loading ->
                CircularProgressIndicator()

            is WeatherUiState.Content.Success ->
                WeatherDetails(content.weather)

            is WeatherUiState.Content.Error ->
                CenterMessage(content.type.message())
        }
    }
}

@Composable
private fun WeatherDetails(weather: Weather) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = weather.cityLabel,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        AsyncImage(
            model = weather.iconUrl,
            contentDescription = stringResource(R.string.cd_weather_icon),
            // Shown while the icon downloads / if it fails or the URL is blank, so the layout
            // never collapses to an empty gap.
            placeholder = painterResource(R.drawable.ic_weather_placeholder),
            error = painterResource(R.drawable.ic_weather_error),
            fallback = painterResource(R.drawable.ic_weather_error),
            modifier = Modifier
                .size(120.dp)
                .semantics { contentDescription = "Weather: ${weather.description}" },
        )
        Text(
            text = "${weather.temperature}°F",
            style = MaterialTheme.typography.displayMedium,
        )
        Text(
            text = weather.description,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(stringResource(R.string.feels_like, "${weather.feelsLike}°F"))
        Text(stringResource(R.string.high_low, "${weather.tempMax}°F", "${weather.tempMin}°F"))

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Stat(stringResource(R.string.humidity), "${weather.humidityPercent}%")
            Stat(stringResource(R.string.wind), "${weather.windMph} mph")
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun CenterMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Maps an error sub-state to its localized message. */
@Composable
private fun WeatherUiState.ErrorType.message(): String = when (this) {
    WeatherUiState.ErrorType.EMPTY_QUERY -> stringResource(R.string.error_empty_query)
    WeatherUiState.ErrorType.CITY_NOT_FOUND -> stringResource(R.string.error_not_found, "that city")
    WeatherUiState.ErrorType.NETWORK -> stringResource(R.string.error_network)
    WeatherUiState.ErrorType.LOCATION_UNAVAILABLE -> stringResource(R.string.error_location_unavailable)
    WeatherUiState.ErrorType.GENERIC -> stringResource(R.string.error_generic)
}
