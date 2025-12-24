package com.narutoai.chat.models

data class Character(
    val id: String,
    val name: String,
    val description: String,
    val category: CharacterCategory,
    val systemPromptSFW: String,
    val systemPromptNSFW: String,
    val avatarEmoji: String,
    val personality: List<String>,
    val imageUrl: String = "", // URL de l'image (non utilisée)
    val imageResId: Int = 0 // Ressource drawable locale
)

enum class CharacterCategory {
    NARUTO,
    CELEBRITY_MALE,
    CELEBRITY_FEMALE
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatSession(
    val character: Character,
    val messages: List<ChatMessage> = emptyList(),
    val isNSFW: Boolean = false
)
