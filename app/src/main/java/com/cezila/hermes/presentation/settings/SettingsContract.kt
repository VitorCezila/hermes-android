package com.cezila.hermes.presentation.settings

import com.cezila.hermes.core.domain.mvi.UiEffect
import com.cezila.hermes.core.domain.mvi.UiEvent
import com.cezila.hermes.core.domain.mvi.UiState

data class SettingsUiState(
    val appVersion: String = "",
    val showClearAllConfirmation: Boolean = false,
    val isClearingKeys: Boolean = false,
) : UiState

sealed interface SettingsUiEvent : UiEvent {
    data object OnClearAllKeysClick : SettingsUiEvent
    data object OnClearAllKeysConfirm : SettingsUiEvent
    data object OnClearAllKeysDismiss : SettingsUiEvent
    data object OnBackClick : SettingsUiEvent
    data class OnVersionLoaded(val version: String) : SettingsUiEvent
}

sealed interface SettingsUiEffect : UiEffect {
    data object NavigateBack : SettingsUiEffect
    data class ShowToast(val message: String) : SettingsUiEffect
}
