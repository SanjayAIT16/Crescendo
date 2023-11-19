package com.paranid5.crescendo.presentation.main_activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.paranid5.crescendo.presentation.Screens
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivityViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private companion object {
        private const val CUR_SCREEN_STATE = "cur_screen"
        private const val SCREENS_STACK_STATE = "screens_stack"
    }

    val curScreenState = MutableStateFlow(
        savedStateHandle.get<Screens>(CUR_SCREEN_STATE) ?: Screens.Tracks
    )

    val screensStack = MutableStateFlow(
        savedStateHandle[SCREENS_STACK_STATE] ?: mutableListOf<Screens>()
    )
}