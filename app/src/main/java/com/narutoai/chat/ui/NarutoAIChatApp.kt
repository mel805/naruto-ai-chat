package com.narutoai.chat.ui

import androidx.compose.runtime.*
import com.narutoai.chat.ui.screens.CharacterSelectionScreen
import com.narutoai.chat.ui.screens.ChatScreen
import com.narutoai.chat.ui.screens.SettingsScreen
import com.narutoai.chat.viewmodel.ChatViewModel

enum class Screen {
    CHARACTER_SELECTION,
    CHAT,
    SETTINGS
}

@Composable
fun NarutoAIChatApp(viewModel: ChatViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.CHARACTER_SELECTION) }
    val selectedCharacter = viewModel.selectedCharacter.value
    
    when (currentScreen) {
        Screen.CHARACTER_SELECTION -> {
            CharacterSelectionScreen(
                onCharacterSelected = { character ->
                    viewModel.selectCharacter(character)
                    currentScreen = Screen.CHAT
                },
                onSettingsClick = {
                    currentScreen = Screen.SETTINGS
                }
            )
        }
        Screen.CHAT -> {
            if (selectedCharacter != null) {
                ChatScreen(
                    viewModel = viewModel,
                    character = selectedCharacter,
                    onBackClick = {
                        viewModel.goBack()
                        currentScreen = Screen.CHARACTER_SELECTION
                    }
                )
            } else {
                currentScreen = Screen.CHARACTER_SELECTION
            }
        }
        Screen.SETTINGS -> {
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = {
                    currentScreen = Screen.CHARACTER_SELECTION
                }
            )
        }
    }
}
