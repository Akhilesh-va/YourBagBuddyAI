package com.example.yourbagbuddy.di

import com.example.yourbagbuddy.BuildConfig
import com.example.yourbagbuddy.data.remote.api.BackendApiService
import com.example.yourbagbuddy.data.remote.api.FeedbackApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import android.util.Log
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
     * Strips trailing slash from script.google.com URLs so we hit /exec (not /exec/) and avoid 302.
     */
    @Provides
    @Singleton
    @Named("FeedbackOkHttp")
    fun provideFeedbackOkHttpClient(): OkHttpClient {
        val stripTrailingSlash = Interceptor { chain ->
            var url = chain.request().url
            if (url.host == "script.google.com" && url.encodedPath.endsWith("/") && url.encodedPath.length > 1) {
                val fixed = url.newBuilder().encodedPath(url.encodedPath.removeSuffix("/")).build()
                url = fixed
            }
            chain.proceed(chain.request().newBuilder().url(url).build())
        }
        val feedbackTracer = Interceptor { chain ->
            val request = chain.request()
            val tag = "FeedbackScript"
            Log.d(tag, ">>> ${request.method} ${request.url}")
            if (request.body != null && request.body is okhttp3.FormBody) {
                val body = request.body as okhttp3.FormBody
                for (i in 0 until body.size) {
                    Log.d(tag, "    body: ${body.name(i)}=${body.value(i).take(50)}${if (body.value(i).length > 50) "..." else ""}")
                }
            }
            if (request.method == "GET" && request.url.querySize > 0) {
                val names = request.url.queryParameterNames
                for (name in names) {
                    Log.d(tag, "    query: $name=${request.url.queryParameter(name)?.take(50) ?: ""}")
                }
            }
            val response = chain.proceed(request)
            Log.d(tag, "<<< ${response.code} ${response.message} ${response.request.url}")
            if (!response.isSuccessful && response.body != null) {
                val peek = response.peekBody(512)
                Log.d(tag, "    body: ${peek.string().replace("\n", " ").take(200)}")
            }
            response
        }
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followRedirects(false)
            .addInterceptor(stripTrailingSlash)
            .addInterceptor(feedbackTracer)
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

    /**
     * Feedback Retrofit uses script.google.com as base so that @Url receives only the path
     * (e.g. /macros/s/xxx/exec). This avoids Retrofit/OkHttp quirks with full URLs (e.g. trailing
     * slash or path stripping) that can cause Apps Script to return 302 redirect.
     */
    @Provides
    @Singleton
    @Named("FeedbackRetrofit")
    fun provideFeedbackRetrofit(
        @Named("FeedbackOkHttp") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://script.google.com/")
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

