package com.cezila.hermes.presentation.keys

import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.mvi.UiEffect
import com.cezila.hermes.core.domain.mvi.UiEvent
import com.cezila.hermes.core.domain.mvi.UiState

data class KeysUiState(
    val keys: List<PgpKey> = emptyList(),
    val isLoading: Boolean = true,
) : UiState

sealed interface KeysUiEvent : UiEvent {
    data object OnAddKeyClick : KeysUiEvent
}

sealed interface KeysUiEffect : UiEffect {
    data object NavigateToKeyGeneration : KeysUiEffect
}
