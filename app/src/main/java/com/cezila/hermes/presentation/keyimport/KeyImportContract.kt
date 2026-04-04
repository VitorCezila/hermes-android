package com.cezila.hermes.presentation.keyimport

import com.cezila.hermes.core.domain.mvi.UiEffect
import com.cezila.hermes.core.domain.mvi.UiEvent
import com.cezila.hermes.core.domain.mvi.UiState

data class KeyImportUiState(
    val armoredText: String = "",
    val fileName: String? = null,
    val validationStatus: ValidationStatus = ValidationStatus.Empty,
    val isLoading: Boolean = false,
) : UiState

sealed interface ValidationStatus {
    data object Empty : ValidationStatus
    data object Validating : ValidationStatus
    data object Valid : ValidationStatus
    data class Invalid(val reason: ValidationError) : ValidationStatus
}

enum class ValidationError {
    InvalidFormat,
    SecretKeyDetected,
    DuplicateKey,
}

sealed interface KeyImportUiEvent : UiEvent {
    data class OnArmoredTextChange(val text: String) : KeyImportUiEvent
    data class OnFileRead(val content: String, val fileName: String) : KeyImportUiEvent
    data object OnImportClick : KeyImportUiEvent
    data object OnBack : KeyImportUiEvent
}

sealed interface KeyImportUiEffect : UiEffect {
    data object NavigateBack : KeyImportUiEffect
    data object ImportSuccess : KeyImportUiEffect
}
