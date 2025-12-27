package com.narutoai.chat.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Client pour génération d'images et vidéos sur Freebox
 * Utilise Stable Diffusion WebUI API installée localement
 */
class FreeboxMediaClient {
    
    // Client principal pour génération (timeout long)
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    // Client pour ping (timeout COURT pour ne pas bloquer l'app)
    private val pingClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()
    
    companion object {
        // URL Freebox avec Stable Diffusion WebUI - PORT 33437
        private const val FREEBOX_BASE_URL = "http://88.174.155.230:33437"
        
        // Configuration par défaut pour images hyper-réalistes
        private const val DEFAULT_STEPS = 30
        private const val DEFAULT_CFG_SCALE = 7.0
        private const val DEFAULT_WIDTH = 512
        private const val DEFAULT_HEIGHT = 768
        
        // Modèles recommandés pour réalisme
        private const val REALISTIC_MODEL = "realisticVisionV51_v51VAE"
    }
    
    /**
     * Génère une image hyper-réaliste
     * @param prompt Description de l'image à générer
     * @param negativePrompt Ce qu'il faut éviter
     * @param style Style de l'image (realistic, anime, etc.)
     * @return URL ou base64 de l'image générée
     */
    suspend fun generateImage(
        prompt: String,
        negativePrompt: String = getDefaultNegativePrompt(),
        style: String = "realistic",
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Enrichir le prompt selon le style
            val enhancedPrompt = enhancePromptForStyle(prompt, style)
            
            val jsonBody = JSONObject().apply {
                put("prompt", enhancedPrompt)
                put("negative_prompt", negativePrompt)
                put("steps", DEFAULT_STEPS)
                put("cfg_scale", DEFAULT_CFG_SCALE)
                put("width", width)
                put("height", height)
                put("sampler_name", "DPM++ 2M Karras")
                put("restore_faces", true) // Important pour les visages réalistes
                put("enable_hr", true) // High-res fix pour meilleure qualité
                put("hr_scale", 2.0)
                put("denoising_strength", 0.5)
            }
            
            val request = Request.Builder()
                .url("$FREEBOX_BASE_URL/sdapi/v1/txt2img")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("Erreur génération: ${response.code}")
                    )
                }
                
                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Réponse vide"))
                
                val jsonResponse = JSONObject(responseBody)
                val images = jsonResponse.getJSONArray("images")
                
                if (images.length() == 0) {
                    return@withContext Result.failure(IOException("Aucune image générée"))
                }
                
                // Retourner la première image en base64
                val imageBase64 = images.getString(0)
                Result.success("data:image/png;base64,$imageBase64")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Génère une vidéo courte à partir d'une image
     * Utilise Deforum ou AnimateDiff si installé sur la Freebox
     */
    suspend fun generateVideoFromImage(
        imageBase64: String,
        motionPrompt: String = "smooth camera movement, cinematic"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Configuration pour animation courte (2-4 secondes)
            val jsonBody = JSONObject().apply {
                put("init_images", JSONArray().put(imageBase64))
                put("prompt", motionPrompt)
                put("steps", 20)
                put("cfg_scale", 7.0)
                put("denoising_strength", 0.3) // Faible pour garder l'image originale
                put("max_frames", 24) // ~1 seconde à 24fps
            }
            
            val request = Request.Builder()
                .url("$FREEBOX_BASE_URL/sdapi/v1/img2img")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            // Note: Pour de vraies vidéos, il faudrait utiliser une extension comme
            // AnimateDiff ou Deforum. Ici on fait plusieurs frames en img2img
            val frames = mutableListOf<String>()
            
            repeat(24) { frameIndex ->
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            val jsonResponse = JSONObject(responseBody)
                            val images = jsonResponse.getJSONArray("images")
                            if (images.length() > 0) {
                                frames.add(images.getString(0))
                            }
                        }
                    }
                }
                delay(100) // Petite pause entre frames
            }
            
            if (frames.isEmpty()) {
                return@withContext Result.failure(IOException("Aucune frame générée"))
            }
            
            // Pour l'instant, retourner la dernière frame
            // Dans une implémentation complète, il faudrait créer un vrai fichier vidéo
            Result.success("data:image/png;base64,${frames.last()}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Génère une vidéo directement depuis un prompt
     * Nécessite AnimateDiff ou extension vidéo
     */
    suspend fun generateVideoFromPrompt(
        prompt: String,
        duration: Int = 2 // secondes
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // D'abord générer une image de base
            val imageResult = generateImage(prompt)
            val imageBase64 = imageResult.getOrNull()
                ?: return@withContext Result.failure(IOException("Échec génération image de base"))
            
            // Puis l'animer
            generateVideoFromImage(imageBase64.split(",")[1], prompt)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Teste si l'API Freebox est accessible
     * Utilise un timeout COURT (5s) pour ne pas bloquer l'app
     */
    suspend fun ping(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$FREEBOX_BASE_URL/sdapi/v1/sd-models")
                .get()
                .build()
            
            // Utiliser pingClient avec timeout court (5s)
            pingClient.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtient la liste des modèles disponibles
     */
    suspend fun getAvailableModels(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$FREEBOX_BASE_URL/sdapi/v1/sd-models")
                .get()
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Erreur: ${response.code}"))
                }
                
                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Réponse vide"))
                
                val jsonArray = JSONArray(responseBody)
                val models = mutableListOf<String>()
                
                for (i in 0 until jsonArray.length()) {
                    val model = jsonArray.getJSONObject(i)
                    models.add(model.getString("title"))
                }
                
                Result.success(models)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Enrichit le prompt selon le style demandé
     */
    private fun enhancePromptForStyle(prompt: String, style: String): String {
        val baseEnhancement = when (style.lowercase()) {
            "realistic" -> "photorealistic, ultra detailed, 8k uhd, high quality, professional photography, natural lighting"
            "anime" -> "anime style, manga art, vibrant colors, detailed anime character"
            "artistic" -> "artistic, oil painting, detailed, beautiful composition"
            "cinematic" -> "cinematic lighting, movie still, dramatic, film grain"
            "portrait" -> "portrait photography, professional headshot, studio lighting, sharp focus"
            else -> "high quality, detailed, professional"
        }
        
        return "$prompt, $baseEnhancement, masterpiece, best quality"
    }
    
    /**
     * Prompt négatif par défaut pour éviter les défauts
     */
    private fun getDefaultNegativePrompt(): String {
        return """
            (worst quality, low quality, normal quality:1.4),
            bad anatomy, bad hands, missing fingers, extra digit, fewer digits,
            blurry, ugly, duplicate, mutilated, deformed, disfigured,
            watermark, signature, text, error, jpeg artifacts,
            lowres, bad proportions, gross proportions, cropped
        """.trimIndent()
    }
    
    /**
     * Génère une image de personnage hyper-réaliste
     * Optimisé pour les portraits de personnages
     */
    suspend fun generateCharacterPortrait(
        characterName: String,
        physicalDescription: String,
        style: String = "realistic"
    ): Result<String> = withContext(Dispatchers.IO) {
        val detailedPrompt = buildCharacterPrompt(characterName, physicalDescription, style)
        
        generateImage(
            prompt = detailedPrompt,
            style = style,
            width = 512,
            height = 768 // Portrait orientation
        )
    }
    
    /**
     * Construit un prompt détaillé pour un personnage
     */
    private fun buildCharacterPrompt(
        name: String,
        physicalDescription: String,
        style: String
    ): String {
        val stylePrefix = when (style.lowercase()) {
            "anime" -> "anime character, manga style"
            "realistic" -> "photorealistic portrait, professional photography"
            else -> "high quality portrait"
        }
        
        return """
            $stylePrefix, $name, $physicalDescription,
            detailed face, expressive eyes, natural skin texture,
            professional lighting, sharp focus, 8k uhd,
            full body shot, character design, concept art
        """.trimIndent()
    }
}
