package com.narutoai.chat.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.narutoai.chat.api.GroqClient
import com.narutoai.chat.api.ImageGenerationClient
import com.narutoai.chat.api.VideoGenerationClient
import com.narutoai.chat.api.FreeboxMediaClient
import com.narutoai.chat.api.PollinationAIClient
import com.narutoai.chat.models.Character
import com.narutoai.chat.models.ChatMessage
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _selectedCharacter = mutableStateOf<Character?>(null)
    val selectedCharacter: State<Character?> = _selectedCharacter
    
    private val _messages = mutableStateOf<List<ChatMessage>>(emptyList())
    val messages: State<List<ChatMessage>> = _messages
    
    private val _isNSFWMode = mutableStateOf(false)
    val isNSFWMode: State<Boolean> = _isNSFWMode
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error
    
    private val _isGeneratingImage = mutableStateOf(false)
    val isGeneratingImage: State<Boolean> = _isGeneratingImage
    
    private val _isGeneratingVideo = mutableStateOf(false)
    val isGeneratingVideo: State<Boolean> = _isGeneratingVideo
    
    private val _generatedImageUrl = mutableStateOf<String?>(null)
    val generatedImageUrl: State<String?> = _generatedImageUrl
    
    private val _generatedVideoUrl = mutableStateOf<String?>(null)
    val generatedVideoUrl: State<String?> = _generatedVideoUrl
    
    private val _replicateApiKey = mutableStateOf<String?>(null)
    val replicateApiKey: State<String?> = _replicateApiKey
    
    private val groqClient = GroqClient(application.applicationContext)
    private val imageClient = ImageGenerationClient(application.applicationContext)
    private val videoClient = VideoGenerationClient(application.applicationContext)
    private val freeboxMediaClient = FreeboxMediaClient()
    private val pollinationAIClient = PollinationAIClient()
    
    init {
        viewModelScope.launch {
            groqClient.initialize()
        }
    }
    
    fun selectCharacter(character: Character) {
        _selectedCharacter.value = character
        _messages.value = emptyList()
        _isNSFWMode.value = false
        _error.value = null
        
        // Ajouter message d'accueil automatique si disponible
        if (character.greetingMessage.isNotEmpty()) {
            viewModelScope.launch {
                // Petit délai pour effet naturel
                kotlinx.coroutines.delay(500)
                val greetingMsg = ChatMessage(
                    content = character.greetingMessage,
                    isUser = false
                )
                _messages.value = listOf(greetingMsg)
            }
        }
    }
    
    fun toggleNSFWMode() {
        _isNSFWMode.value = !_isNSFWMode.value
        // Optionally clear messages when switching modes
        // _messages.value = emptyList()
    }
    
    fun sendMessage(text: String) {
        val character = _selectedCharacter.value ?: return
        if (text.isBlank()) return
        
        // Add user message
        val userMessage = ChatMessage(
            content = text,
            isUser = true
        )
        _messages.value = _messages.value + userMessage
        
        // Get AI response
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val systemPrompt = if (_isNSFWMode.value) {
                    character.systemPromptNSFW
                } else {
                    character.systemPromptSFW
                }
                
                // Build conversation history
                val history = _messages.value.takeLast(20).map { msg ->
                    val role = if (msg.isUser) "user" else "assistant"
                    role to msg.content
                }
                
                val result = groqClient.chat(
                    systemPrompt = systemPrompt,
                    userMessage = text,
                    conversationHistory = history.dropLast(1), // Exclude the message we just added
                    temperature = if (_isNSFWMode.value) 0.9 else 0.8,
                    maxTokens = 500
                )
                
                result.fold(
                    onSuccess = { response ->
                        val aiMessage = ChatMessage(
                            content = response,
                            isUser = false
                        )
                        _messages.value = _messages.value + aiMessage
                        _isLoading.value = false
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Unknown error"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearChat() {
        _messages.value = emptyList()
    }
    
    fun goBack() {
        _selectedCharacter.value = null
        _messages.value = emptyList()
        _isNSFWMode.value = false
        _error.value = null
        _generatedImageUrl.value = null
        _generatedVideoUrl.value = null
    }
    
    /**
     * Définit la clé API Replicate pour la génération d'images/vidéos
     */
    fun setReplicateApiKey(key: String) {
        _replicateApiKey.value = key
    }
    
    /**
     * Génère une image basée sur le contexte de la conversation
     * Utilise FreeboxMediaClient (Stable Diffusion local)
     */
    fun generateImageFromConversation() {
        val character = _selectedCharacter.value ?: run {
            _error.value = "Aucun personnage sélectionné"
            return
        }
        
        if (_messages.value.isEmpty()) {
            _error.value = "Discutez d'abord avec le personnage avant de générer une image"
            return
        }
        
        _isGeneratingImage.value = true
        _error.value = null
        
        // Ajouter message de statut
        val statusMessage = ChatMessage(
            content = "🎨 Génération d'image en cours...",
            isUser = false
        )
        _messages.value = _messages.value + statusMessage
        
        viewModelScope.launch {
            try {
                // Prendre les derniers messages pour le contexte
                val context = _messages.value.filter { !it.content.startsWith("🎨") && !it.content.startsWith("❌") }
                    .takeLast(5).joinToString("\n") { msg ->
                        val role = if (msg.isUser) "User" else character.name
                        "$role: ${msg.content}"
                    }
                
                // Créer un prompt d'image avec Groq
                val promptRequest = """
                    Based on this conversation with ${character.name}:
                    $context
                    
                    Physical description of ${character.name}:
                    ${character.physicalDescription}
                    
                    Create a detailed prompt in ENGLISH (max 75 words) for generating a hyper-realistic image of this scene.
                    Include: character's physical features, setting, mood, lighting, and action.
                    Respond ONLY with the English prompt, no explanation.
                """.trimIndent()
                
                val promptResult = groqClient.chat(
                    systemPrompt = "You are an expert at creating detailed prompts for AI image generation. Focus on visual details, lighting, and atmosphere.",
                    userMessage = promptRequest,
                    maxTokens = 150
                )
                
                val imagePrompt = promptResult.getOrNull()
                    ?: run {
                        val errorMsg = "❌ Échec de création du prompt avec Groq"
                        _error.value = errorMsg
                        _isGeneratingImage.value = false
                        _messages.value = _messages.value.dropLast(1) + ChatMessage(
                            content = errorMsg,
                            isUser = false
                        )
                        return@launch
                    }
                
                // Test de connexion Freebox
                val pingResult = freeboxMediaClient.ping()
                val usePollination = pingResult.isFailure
                
                if (usePollination) {
                    // Update status message
                    _messages.value = _messages.value.dropLast(1) + ChatMessage(
                        content = "🎨 Freebox non accessible, utilisation de Pollination AI...",
                        isUser = false
                    )
                }
                
                // Générer l'image (Freebox si dispo, sinon Pollination AI)
                val style = if (character.category == com.narutoai.chat.models.CharacterCategory.NARUTO) "anime" else "realistic"
                
                val result = if (usePollination) {
                    // IMPORTANT: Attendre 3s AVANT d'appeler Pollination après Groq
                    kotlinx.coroutines.delay(3000)
                    
                    // Fallback: Pollination AI (TOUJOURS disponible)
                    pollinationAIClient.generateImage(
                        prompt = imagePrompt,
                        model = if (style == "anime") "flux" else "flux-realism",
                        enhance = true
                    )
                } else {
                    // Primary: Freebox Stable Diffusion (local)
                    freeboxMediaClient.generateImage(
                        prompt = imagePrompt,
                        style = style
                    )
                }
                
                result.fold(
                    onSuccess = { imageUrl ->
                        _generatedImageUrl.value = imageUrl
                        _isGeneratingImage.value = false
                        
                        val source = if (usePollination) "Pollination AI" else "Freebox"
                        // Remplacer le message de statut par l'image
                        _messages.value = _messages.value.dropLast(1) + ChatMessage(
                            content = "✅ Image générée ($source):\n$imageUrl",
                            isUser = false
                        )
                    },
                    onFailure = { exception ->
                        val errorMsg = "❌ Erreur génération image: ${exception.message}"
                        _error.value = errorMsg
                        _isGeneratingImage.value = false
                        _messages.value = _messages.value.dropLast(1) + ChatMessage(
                            content = errorMsg,
                            isUser = false
                        )
                    }
                )
            } catch (e: Exception) {
                val errorMsg = "❌ Erreur: ${e.message}"
                _error.value = errorMsg
                _isGeneratingImage.value = false
                _messages.value = _messages.value.dropLast(1) + ChatMessage(
                    content = errorMsg,
                    isUser = false
                )
            }
        }
    }
    
    /**
     * Génère une vidéo basée sur le contexte de la conversation
     * Utilise FreeboxMediaClient
     */
    fun generateVideoFromConversation() {
        val character = _selectedCharacter.value ?: run {
            _error.value = "Aucun personnage sélectionné"
            return
        }
        
        if (_messages.value.isEmpty()) {
            _error.value = "Discutez d'abord avec le personnage avant de générer une vidéo"
            return
        }
        
        _isGeneratingVideo.value = true
        _error.value = null
        
        // Ajouter message de statut
        val statusMessage = ChatMessage(
            content = "🎬 Génération de vidéo en cours... (cela peut prendre 1-2 minutes)",
            isUser = false
        )
        _messages.value = _messages.value + statusMessage
        
        viewModelScope.launch {
            try {
                val context = _messages.value.filter { !it.content.startsWith("🎬") && !it.content.startsWith("❌") }
                    .takeLast(5).joinToString("\n") { msg ->
                        val role = if (msg.isUser) "User" else character.name
                        "$role: ${msg.content}"
                    }
                
                // Créer un prompt vidéo avec Groq
                val promptRequest = """
                    Based on this conversation with ${character.name}:
                    $context
                    
                    Physical description: ${character.physicalDescription}
                    
                    Create a detailed video prompt in ENGLISH (max 75 words) for a 2-4 second animated scene.
                    Include: character movement, camera angle, lighting, and atmosphere.
                    Respond ONLY with the English prompt, no explanation.
                """.trimIndent()
                
                val promptResult = groqClient.chat(
                    systemPrompt = "You are an expert at creating cinematic video prompts with movement and action details.",
                    userMessage = promptRequest,
                    maxTokens = 150
                )
                
                val videoPrompt = promptResult.getOrNull()
                    ?: run {
                        val errorMsg = "❌ Échec de création du prompt vidéo"
                        _error.value = errorMsg
                        _isGeneratingVideo.value = false
                        _messages.value = _messages.value.dropLast(1) + ChatMessage(
                            content = errorMsg,
                            isUser = false
                        )
                        return@launch
                    }
                
                // Test de connexion Freebox
                val pingResult = freeboxMediaClient.ping()
                val usePollination = pingResult.isFailure
                
                if (usePollination) {
                    // Update status message
                    _messages.value = _messages.value.dropLast(1) + ChatMessage(
                        content = "🎬 Freebox non accessible, génération de frames avec Pollination AI...",
                        isUser = false
                    )
                }
                
                // Générer la vidéo (Freebox si dispo, sinon frames Pollination)
                val result = if (usePollination) {
                    // IMPORTANT: Attendre 3s AVANT d'appeler Pollination après Groq
                    kotlinx.coroutines.delay(3000)
                    
                    // Fallback: Générer plusieurs frames avec Pollination AI
                    pollinationAIClient.generateImage(
                        prompt = "$videoPrompt, animated, cinematic movement",
                        model = "flux",
                        enhance = true
                    ).map { url ->
                        url // Pour l'instant, retourner une seule frame comme "vidéo"
                    }
                } else {
                    // Primary: Freebox Stable Diffusion (local)
                    freeboxMediaClient.generateVideoFromPrompt(
                        prompt = videoPrompt,
                        duration = 2
                    )
                }
                
                result.fold(
                    onSuccess = { videoUrl ->
                        _generatedVideoUrl.value = videoUrl
                        _isGeneratingVideo.value = false
                        
                        val source = if (usePollination) "Pollination AI" else "Freebox"
                        _messages.value = _messages.value.dropLast(1) + ChatMessage(
                            content = "✅ ${if (usePollination) "Image animée" else "Vidéo"} générée ($source):\n$videoUrl",
                            isUser = false
                        )
                    },
                    onFailure = { exception ->
                        val errorMsg = "❌ Erreur génération vidéo: ${exception.message}"
                        _error.value = errorMsg
                        _isGeneratingVideo.value = false
                        _messages.value = _messages.value.dropLast(1) + ChatMessage(
                            content = errorMsg,
                            isUser = false
                        )
                    }
                )
            } catch (e: Exception) {
                val errorMsg = "❌ Erreur: ${e.message}"
                _error.value = errorMsg
                _isGeneratingVideo.value = false
                _messages.value = _messages.value.dropLast(1) + ChatMessage(
                    content = errorMsg,
                    isUser = false
                )
            }
        }
    }
    
    /**
     * Génère une galerie d'images pour un personnage avec Pollination AI
     */
    fun generateCharacterGallery(character: Character, count: Int = 6, onComplete: (List<String>) -> Unit) {
        _isGeneratingImage.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val result = pollinationAIClient.generateCharacterGallery(
                    characterName = character.name,
                    physicalDescription = character.physicalDescription,
                    style = if (character.category == com.narutoai.chat.models.CharacterCategory.NARUTO) "anime" else "realistic",
                    count = count
                )
                
                result.fold(
                    onSuccess = { images ->
                        _isGeneratingImage.value = false
                        onComplete(images)
                    },
                    onFailure = { exception ->
                        _error.value = "Erreur génération galerie: ${exception.message}"
                        _isGeneratingImage.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = "Erreur: ${e.message}"
                _isGeneratingImage.value = false
            }
        }
    }
    
    /**
     * Génère une vignette pour un personnage avec Pollination AI
     */
    fun generateCharacterThumbnail(character: Character, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = pollinationAIClient.generateCharacterThumbnail(
                    characterName = character.name,
                    physicalDescription = character.physicalDescription,
                    style = if (character.category == com.narutoai.chat.models.CharacterCategory.NARUTO) "anime" else "realistic"
                )
                
                result.fold(
                    onSuccess = { thumbnailUrl ->
                        onComplete(thumbnailUrl)
                    },
                    onFailure = { exception ->
                        _error.value = "Erreur génération vignette: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Erreur: ${e.message}"
            }
        }
    }
    
    /**
     * Génère une image à partir d'un prompt personnalisé
     */
    fun generateCustomImage(prompt: String, style: String = "anime") {
        val apiKey = _replicateApiKey.value
        
        if (apiKey.isNullOrBlank()) {
            _error.value = "Clé API Replicate requise"
            return
        }
        
        _isGeneratingImage.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val result = imageClient.generateImage(prompt, style, apiKey)
                
                result.fold(
                    onSuccess = { imageUrl ->
                        _generatedImageUrl.value = imageUrl
                        _isGeneratingImage.value = false
                        
                        val imageMessage = ChatMessage(
                            content = "[Image: $prompt] $imageUrl",
                            isUser = false
                        )
                        _messages.value = _messages.value + imageMessage
                    },
                    onFailure = { exception ->
                        _error.value = "Erreur: ${exception.message}"
                        _isGeneratingImage.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = "Erreur: ${e.message}"
                _isGeneratingImage.value = false
            }
        }
    }
    
    /**
     * Obtient le gestionnaire de clés API Groq
     */
    fun getGroqKeyManager() = groqClient.getKeyManager()
    
    /**
     * Teste la connexion Groq
     */
    fun testGroqConnection(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = groqClient.ping()
            result.fold(
                onSuccess = { onResult(true, null) },
                onFailure = { onResult(false, it.message) }
            )
        }
    }
}
