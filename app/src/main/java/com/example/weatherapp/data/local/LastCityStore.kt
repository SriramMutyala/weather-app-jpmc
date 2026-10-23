package com.example.weatherapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "weather_prefs")

/**
 * Persists the last successfully-searched city so it can be auto-loaded on next launch.
 */
@Singleton
class LastCityStore @Inject constructor(
    private val context: Context,
) {
    private val keyLastCity = stringPreferencesKey("last_city")

    /** Emits the last saved city, or null if none has been saved yet. */
    val lastCity: Flow<String?> = context.dataStore.data.map { it[keyLastCity] }

    suspend fun save(city: String) {
        context.dataStore.edit { it[keyLastCity] = city }
    }
}
