package com.example.yourbagbuddy.presentation.viewmodel

import com.example.yourbagbuddy.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lightweight client to call OpenRouter directly from the Android app.
 *
 * This is intended for development / prototyping only. For production,
 * you should route requests through your own backend so the API key
 * is never shipped in the APK.
 */
object OpenRouterTravelAi {

    private const val OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"
    private const val MODEL = "openai/gpt-4.1-mini"

    private val client: OkHttpClient by lazy { OkHttpClient() }

    /**
     * Fetches a single short, practical travel tip using OpenRouter.
     * Returns a fallback tip string if anything goes wrong.
     */
    suspend fun fetchTravelTip(): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.OPENROUTER_API_KEY
        if (apiKey.isBlank()) {
            return@withContext "Pack light, pack smart! Roll your clothes to save space."
        }

        val systemMessage = "You are a helpful travel assistant for the YourBagBuddy app. " +
            "Reply with exactly one short, practical travel tip in one sentence. " +
            "Do not include emojis."

        val userMessage = "Give me one short, practical travel tip for everyday travelers."

        val messages = JSONArray().apply {
            put(
                JSONObject().apply {
                    put("role", "system")
                    put("content", systemMessage)
                }
            )
            put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                }
            )
        }

        val bodyJson = JSONObject().apply {
            put("model", MODEL)
            put("messages", messages)
            put("max_tokens", 80)
            put("temperature", 0.7)
        }.toString()

        val requestBody = bodyJson.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(OPENROUTER_URL)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://yourbagbuddy.app") // change to your URL if you have one
            .addHeader("X-Title", "YourBagBuddy")
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful || body.isBlank()) {
                    return@use "Pack light, pack smart! Roll your clothes to save space."
                }

                val root = JSONObject(body)
                val choices = root.getJSONArray("choices")
                if (choices.length() == 0) {
                    return@use "Pack light, pack smart! Roll your clothes to save space."
                }

                val message = choices
                    .getJSONObject(0)
                    .getJSONObject("message")
                val content = message.optString("content").trim()
                if (content.isBlank()) {
                    "Pack light, pack smart! Roll your clothes to save space."
                } else {
                    content
                }
            }
        } catch (_: Exception) {
            "Pack light, pack smart! Roll your clothes to save space."
        }
    }
}

