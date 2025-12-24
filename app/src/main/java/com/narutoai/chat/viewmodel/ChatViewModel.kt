package com.narutoai.chat.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.narutoai.chat.api.LlamaClient
import com.narutoai.chat.models.Character
import com.narutoai.chat.models.ChatMessage
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    
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
    
    private val llamaClient = LlamaClient() // URL will be set in app
    
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
                
                val result = llamaClient.chat(
                    systemPrompt = systemPrompt,
                    userMessage = text,
                    conversationHistory = history.dropLast(1) // Exclude the message we just added
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
    }
}
