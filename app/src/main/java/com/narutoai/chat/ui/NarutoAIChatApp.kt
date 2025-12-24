package com.narutoai.chat.ui

import androidx.compose.runtime.Composable
import com.narutoai.chat.ui.screens.CharacterSelectionScreen
import com.narutoai.chat.ui.screens.ChatScreen
import com.narutoai.chat.viewmodel.ChatViewModel

@Composable
fun NarutoAIChatApp(viewModel: ChatViewModel) {
    val selectedCharacter = viewModel.selectedCharacter.value
    
    if (selectedCharacter == null) {
        CharacterSelectionScreen(
            onCharacterSelected = { character ->
                viewModel.selectCharacter(character)
            }
        )
    } else {
        ChatScreen(
            viewModel = viewModel,
            character = selectedCharacter,
            onBackClick = { viewModel.goBack() }
        )
    }
}
