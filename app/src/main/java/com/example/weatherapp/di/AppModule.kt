package com.example.weatherapp.di

import android.content.Context
import com.example.weatherapp.data.WeatherRepositoryImpl
import com.example.weatherapp.data.location.LocationProvider
import com.example.weatherapp.data.local.LastCityStore
import com.example.weatherapp.data.remote.WeatherApi
import com.example.weatherapp.domain.WeatherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideWeatherRepository(
        api: WeatherApi,
        ioDispatcher: CoroutineDispatcher,
    ): WeatherRepository = WeatherRepositoryImpl(api, ioDispatcher)

    @Provides
    @Singleton
    fun provideLastCityStore(@ApplicationContext context: Context): LastCityStore =
        LastCityStore(context)

    @Provides
    @Singleton
    fun provideLocationProvider(@ApplicationContext context: Context): LocationProvider =
        LocationProvider(context)
}
