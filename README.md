# Weather App

A basic Android weather app. Search for a US city (or use device location) and see its
current conditions, pulling live data from the [OpenWeatherMap](https://openweathermap.org) API.

## Features

- **City search** — enter a US city. The repository tries the by-city-name weather call first
  (`/data/2.5/weather?q=city,state,US`) and, if that fails, falls back to the recommended
  Geocoding flow (`/geo/1.0/direct` → lat/lon → weather by coordinates).
- **Weather icon** — downloaded from OpenWeatherMap and displayed; Coil provides automatic
  in-memory + disk image caching.
- **Last-city auto-load** — the last successful search is persisted (DataStore) and reloaded
  on next launch.
- **Location** — on launch the app requests location permission; if granted, weather for the
  device's current location is loaded by default. Denial falls back silently to the last city.
- **Defensive** — typed error results for network / not-found / location / unknown failures,
  each mapped to a distinct user-facing message; partial/garbled API payloads degrade
  gracefully rather than crashing.

## Tech

| Concern        | Choice                                    |
|----------------|-------------------------------------------|
| Language       | Kotlin                                    |
| Architecture   | MVVM (immutable `StateFlow` UI state)     |
| UI             | Jetpack Compose + Material 3              |
| Concurrency    | Kotlin Coroutines / Flow                  |
| Networking     | Retrofit + Moshi + OkHttp                 |
| Image loading  | Coil (disk + memory cache)                |
| DI             | Hilt                                      |
| Persistence    | DataStore (Preferences)                   |
| Location       | Google Play Services FusedLocationProvider|
| Unit test      | JUnit4 + MockK + Turbine + Truth          |

## Project layout

```
data/
  remote/        Retrofit WeatherApi, ApiKeyInterceptor, DTOs (Geocoding + Weather)
  local/         LastCityStore (DataStore)
  location/      LocationProvider (FusedLocation wrapper)
  WeatherRepositoryImpl  DTO -> domain mapping + typed error handling
domain/          Weather model, WeatherRepository interface, DataResult / WeatherError
di/              Hilt modules (network + app)
ui/              MainActivity, WeatherViewModel, WeatherUiState, WeatherScreen, theme
```

## Setup

1. Get a free API key at <https://openweathermap.org/api> (the free "Current Weather" +
   "Geocoding" tiers are sufficient).
2. Add it to `local.properties` (already git-ignored):

   ```properties
   OPENWEATHER_API_KEY=your_key_here
   ```

   It is injected into `BuildConfig.OPENWEATHER_API_KEY` and attached to every request by
   `ApiKeyInterceptor`, so the key never appears in source or Retrofit signatures.

## Build & test

```bash
./gradlew :app:testDebugUnitTest   # run unit tests
./gradlew :app:assembleDebug       # build debug APK
```

## Notes / trade-offs

- Search is biased to the US by appending `,US` to the query (per the requirement to enter a
  US city). A `"City,State"` input is passed through verbatim.
- The by-city-name weather endpoint is deprecated per the OpenWeatherMap docs, so it is used
  only as a fast first attempt; the non-deprecated Geocoding flow is the fallback.
- Only coarse/fine foreground location is requested — no background location — since a
  one-shot current-location fix is all the app needs.
- `onStart()` prefers an existing location grant, else the last-searched city; it never nags
  if location is unavailable at launch.
