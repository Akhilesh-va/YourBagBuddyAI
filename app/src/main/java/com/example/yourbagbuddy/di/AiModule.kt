package com.example.yourbagbuddy.di

import com.example.yourbagbuddy.BuildConfig
import com.example.yourbagbuddy.data.remote.api.BackendApiService
import com.example.yourbagbuddy.data.remote.api.FeedbackApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Backend API / networking wiring.
 *
 * This module provides the Retrofit client configured to talk to the
 * YourBagBuddy backend API. Auth tokens are passed per-request via
 * the Authorization header (handled in the repository layer).
 */
@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 120L  // Backend AI calls can be slow on cold starts
    private const val WRITE_TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        // For debug builds, log full request/response bodies to help with debugging.
        // WARNING: This will log sensitive data like tokens - disable in production!
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                // Log full bodies for debugging API issues
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }

        return builder.build()
    }

    /**
     * OkHttp client for feedback (Google Apps Script) that does not follow redirects.
     * Following redirects can turn POST into GET and cause HTTP 405 from the script.
     */
    @Provides
    @Singleton
    @Named("FeedbackOkHttp")
    fun provideFeedbackOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followRedirects(false)
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
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
            // Backend base URL is provided via BuildConfig so we can switch
            // between dev/staging/prod environments without code changes.
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideBackendApiService(
        retrofit: Retrofit
    ): BackendApiService {
        return retrofit.create(BackendApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("FeedbackRetrofit")
    fun provideFeedbackRetrofit(
        @Named("FeedbackOkHttp") okHttpClient: OkHttpClient
    ): Retrofit {
        val feedbackUrl: String = BuildConfig.FEEDBACK_SHEET_URL
        val baseUrl = if (feedbackUrl.endsWith("/")) feedbackUrl else "$feedbackUrl/"
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideFeedbackApiService(
        @Named("FeedbackRetrofit") retrofit: Retrofit
    ): FeedbackApiService {
        return retrofit.create(FeedbackApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("FeedbackSheetUrl")
    fun provideFeedbackSheetUrl(): String = BuildConfig.FEEDBACK_SHEET_URL
}

