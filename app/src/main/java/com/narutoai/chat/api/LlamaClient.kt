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
    private val apiKey: String = "YOUR_GROQ_API_KEY" // À configurer dans Settings
) {
    
    private val baseUrl = "https://api.groq.com/openai/v1"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
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
            
            // Add conversation history (last 10 messages for context)
            conversationHistory.takeLast(10).forEach { (role, content) ->
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
                put("model", "llama-3.3-70b-versatile") // Groq's fastest uncensored model
                put("messages", messages)
                put("temperature", 0.8)
                put("max_tokens", 1000)
                put("stream", false)
            }
            
            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
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
                .url("$baseUrl/models")
                .addHeader("Authorization", "Bearer $apiKey")
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
