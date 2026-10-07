package com.aistudio.deproof.sdwk.ai

import com.google.gson.JsonObject
import com.squareup.okhttp3.OkHttpClient
import com.squareup.okhttp3.Request
import com.squareup.okhttp3.RequestBody.Companion.toRequestBody
import com.squareup.okhttp3.logging.HttpLoggingInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GrokRagAgent(private val apiKey: String) {
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    suspend fun queryGrok(prompt: String): String? {
        if (apiKey == "MY_GROK_API_KEY" || apiKey.isEmpty()) {
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JsonObject().apply {
                    addProperty("model", "grok-4")
                    add("messages", com.google.gson.JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("role", "user")
                            addProperty("content", prompt)
                        })
                    })
                    addProperty("temperature", 0.7)
                    addProperty("max_tokens", 150)
                }

                val request = Request.Builder()
                    .url("https://api.x.ai/v1/chat/completions")
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .post(jsonBody.toString().toRequestBody())
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext null
                }

                val responseBody = response.body?.string() ?: return@withContext null
                val parsed = com.google.gson.JsonParser.parseString(responseBody).asJsonObject
                val choices = parsed.get("choices")?.asJsonArray
                val content = choices?.get(0)?.asJsonObject
                    ?.get("message")?.asJsonObject
                    ?.get("content")?.asString

                content
            } catch (e: Exception) {
                null
            }
        }
    }
}
