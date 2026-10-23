package com.example.weatherapp.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Appends the OpenWeatherMap `appid` query param to every request, so no call site has to
 * carry the key. Centralising it here also keeps the key out of Retrofit interface signatures.
 */
class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val url = original.url.newBuilder()
            .addQueryParameter("appid", apiKey)
            .build()
        return chain.proceed(original.newBuilder().url(url).build())
    }
}
