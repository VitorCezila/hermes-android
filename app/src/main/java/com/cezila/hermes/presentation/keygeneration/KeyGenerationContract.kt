package com.cezila.hermes.presentation.keygeneration

import com.cezila.hermes.core.domain.model.KeyAlgorithm
import com.cezila.hermes.core.domain.mvi.UiEffect
import com.cezila.hermes.core.domain.mvi.UiEvent
import com.cezila.hermes.core.domain.mvi.UiState

data class KeyGenerationUiState(
    val ownerName: String = "",
    val ownerEmail: String = "",
    val selectedAlgorithm: KeyAlgorithm = KeyAlgorithm.ED25519,
    val expiryDays: Int = 0,
    val passphrase: String = "",
    val confirmPassphrase: String = "",
    val isLoading: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passphraseError: String? = null,
    val confirmPassphraseError: String? = null,
    val generalError: String? = null,
) : UiState

sealed interface KeyGenerationUiEvent : UiEvent {
    data class OnNameChange(val value: String) : KeyGenerationUiEvent
    data class OnEmailChange(val value: String) : KeyGenerationUiEvent
    data class OnAlgorithmChange(val algorithm: KeyAlgorithm) : KeyGenerationUiEvent
    data class OnPassphraseChange(val value: String) : KeyGenerationUiEvent
    data class OnConfirmPassphraseChange(val value: String) : KeyGenerationUiEvent
    data class OnExpiryChange(val days: Int) : KeyGenerationUiEvent
    data object OnGenerateClick : KeyGenerationUiEvent
    data object OnBack : KeyGenerationUiEvent
}

sealed interface KeyGenerationUiEffect : UiEffect {
    data object NavigateToKeys : KeyGenerationUiEffect
    data object NavigateBack : KeyGenerationUiEffect
}
