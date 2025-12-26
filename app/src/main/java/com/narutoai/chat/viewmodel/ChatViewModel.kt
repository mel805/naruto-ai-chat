package com.narutoai.chat.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.narutoai.chat.api.GroqClient
import com.narutoai.chat.api.ImageGenerationClient
import com.narutoai.chat.api.VideoGenerationClient
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
     */
    fun generateImageFromConversation() {
        val character = _selectedCharacter.value ?: return
        val apiKey = _replicateApiKey.value
        
        if (apiKey.isNullOrBlank()) {
            _error.value = "Clé API Replicate requise pour générer des images"
            return
        }
        
        _isGeneratingImage.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                // Prendre les derniers messages pour le contexte
                val context = _messages.value.takeLast(5).joinToString("\n") { msg ->
                    val role = if (msg.isUser) "User" else character.name
                    "$role: ${msg.content}"
                }
                
                val result = imageClient.generateImageFromContext(
                    characterName = character.name,
                    conversationContext = context,
                    groqClient = groqClient,
                    replicateApiKey = apiKey
                )
                
                result.fold(
                    onSuccess = { imageUrl ->
                        _generatedImageUrl.value = imageUrl
                        _isGeneratingImage.value = false
                        
                        // Ajouter un message avec l'image
                        val imageMessage = ChatMessage(
                            content = "[Image générée] $imageUrl",
                            isUser = false
                        )
                        _messages.value = _messages.value + imageMessage
                    },
                    onFailure = { exception ->
                        _error.value = "Erreur génération image: ${exception.message}"
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
     * Génère une vidéo basée sur le contexte de la conversation
     */
    fun generateVideoFromConversation() {
        val character = _selectedCharacter.value ?: return
        val apiKey = _replicateApiKey.value
        
        if (apiKey.isNullOrBlank()) {
            _error.value = "Clé API Replicate requise pour générer des vidéos"
            return
        }
        
        _isGeneratingVideo.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val context = _messages.value.takeLast(5).joinToString("\n") { msg ->
                    val role = if (msg.isUser) "User" else character.name
                    "$role: ${msg.content}"
                }
                
                val result = videoClient.generateVideoFromContext(
                    characterName = character.name,
                    conversationContext = context,
                    groqClient = groqClient,
                    imageGenerationClient = imageClient,
                    replicateApiKey = apiKey
                )
                
                result.fold(
                    onSuccess = { videoUrl ->
                        _generatedVideoUrl.value = videoUrl
                        _isGeneratingVideo.value = false
                        
                        // Ajouter un message avec la vidéo
                        val videoMessage = ChatMessage(
                            content = "[Vidéo générée] $videoUrl",
                            isUser = false
                        )
                        _messages.value = _messages.value + videoMessage
                    },
                    onFailure = { exception ->
                        _error.value = "Erreur génération vidéo: ${exception.message}"
                        _isGeneratingVideo.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = "Erreur: ${e.message}"
                _isGeneratingVideo.value = false
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
