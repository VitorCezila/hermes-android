package com.cezila.hermes.presentation.keydetail

import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.mvi.UiEffect
import com.cezila.hermes.core.domain.mvi.UiEvent
import com.cezila.hermes.core.domain.mvi.UiState

data class KeyDetailUiState(
    val key: PgpKey? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showDeleteDialog: Boolean = false,
) : UiState

sealed interface KeyDetailUiEvent : UiEvent {
    data object OnBackClick : KeyDetailUiEvent
    data object OnExportClick : KeyDetailUiEvent
    data object OnDeleteClick : KeyDetailUiEvent
    data object OnDeleteConfirm : KeyDetailUiEvent
    data object OnDeleteDismiss : KeyDetailUiEvent
}

sealed interface KeyDetailUiEffect : UiEffect {
    data object NavigateBack : KeyDetailUiEffect
    data class SharePublicKey(val armoredText: String) : KeyDetailUiEffect
}
