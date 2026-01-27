package com.example.yourbagbuddy.di

import android.util.Log
import com.example.yourbagbuddy.BuildConfig
import com.example.yourbagbuddy.data.remote.ai.PackingChecklistPromptBuilder
import com.example.yourbagbuddy.data.remote.ai.PackingChecklistResponseParser
import com.example.yourbagbuddy.data.remote.ai.ZaiChatService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * AI / networking wiring.
 *
 * This module hides all provider-specific details (HTTP, headers, timeouts)
 * behind DI so the SmartPackRepository can easily be re-pointed to a backend
 * service in the future without touching UI or domain layers.
 *
 * Currently, SmartPack talks to OpenRouter's OpenAI-compatible chat completions API.
 */
@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    // OpenRouter base URL (OpenAI-compatible chat completions API)
    private const val BASE_URL = "https://openrouter.ai/api/v1/"
    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 15L

    @Provides
    @Singleton
    fun provideAuthorizationInterceptor(): Interceptor {
        return Interceptor { chain ->
            // Use the OpenRouter API key exposed via BuildConfig.
            val apiKey = BuildConfig.OPENROUTER_API_KEY

            if (BuildConfig.DEBUG) {
                // Do NOT log the key itself, only whether it is present.
                Log.d("AiAuth", "Using OPENROUTER_API_KEY present=${apiKey.isNotBlank()}")
            }

            val newRequest = chain.request()
                .newBuilder()
                .apply {
                    if (apiKey.isNotBlank()) {
                        header("Authorization", "Bearer $apiKey")
                        // OpenRouter recommends sending a referer + title.
                        header("HTTP-Referer", "https://yourbagbuddy.app")
                        header("X-Title", "YourBagBuddy")
                    }
                }
                .build()

            chain.proceed(newRequest)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: Interceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)

        // Avoid logging prompts/responses in production. For debug builds only,
        // minimal logging can help while still keeping payloads out of logs.
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                // Log only metadata, not bodies, to avoid leaking sensitive content.
                level = HttpLoggingInterceptor.Level.BASIC
            }
            builder.addInterceptor(logging)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideZaiChatService(
        retrofit: Retrofit
    ): ZaiChatService {
        return retrofit.create(ZaiChatService::class.java)
    }

    @Provides
    @Singleton
    fun providePackingChecklistPromptBuilder(): PackingChecklistPromptBuilder {
        return PackingChecklistPromptBuilder()
    }

    @Provides
    @Singleton
    fun providePackingChecklistResponseParser(): PackingChecklistResponseParser {
        return PackingChecklistResponseParser()
    }
}

