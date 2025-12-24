package com.narutoai.chat.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class LlamaClient(
    private val baseUrl: String = "http://88.174.155.230:33437" // Freebox TinyLlama sur port 33437
) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS) // 3 minutes pour Freebox lente
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()
    
    /**
     * Send a message to the AI and get a response
     * @param systemPrompt The character's personality/instructions
     * @param userMessage The user's message
     * @param conversationHistory Previous messages for context
     * @return The AI's response
     */
    suspend fun chat(
        systemPrompt: String,
        userMessage: String,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val messages = JSONArray()
            
            // Add system prompt
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            
            // Add conversation history (last 3 messages only for speed)
            conversationHistory.takeLast(3).forEach { (role, content) ->
                messages.put(JSONObject().apply {
                    put("role", role)
                    put("content", content)
                })
            }
            
            // Add current user message
            messages.put(JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            })
            
            val jsonBody = JSONObject().apply {
                put("model", "tinyllama") // TinyLlama - plus léger pour Freebox
                put("messages", messages)
                put("temperature", 0.3) // Très bas pour cohérence
                put("max_tokens", 10) // ULTRA court - 2-3 mots max
                put("top_p", 0.7)
                put("repeat_penalty", 1.3) // Évite répétitions
                put("num_predict", 10) // Limite stricte
                put("stream", false)
            }
            
            val request = Request.Builder()
                .url("$baseUrl/v1/chat/completions")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: ${response.message}")
                    )
                }
                
                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response"))
                
                val jsonResponse = JSONObject(responseBody)
                val content = jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                
                Result.success(content.trim())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Test if the API is reachable
     */
    suspend fun ping(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/tags")
                .get()
                .build()
            
            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
