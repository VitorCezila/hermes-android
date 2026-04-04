package com.cezila.hermes.presentation.keys

import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.mvi.UiEffect
import com.cezila.hermes.core.domain.mvi.UiEvent
import com.cezila.hermes.core.domain.mvi.UiState

data class KeysUiState(
    val ownKey: PgpKey? = null,
    val contacts: List<PgpKey> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val showFabSheet: Boolean = false,
) : UiState {
    val hasKeys: Boolean get() = ownKey != null || contacts.isNotEmpty()

    val filteredContacts: List<PgpKey>
        get() = if (searchQuery.isBlank()) contacts
                else contacts.filter { key ->
                    key.ownerName.contains(searchQuery, ignoreCase = true) ||
                        key.ownerEmail.contains(searchQuery, ignoreCase = true) ||
                        key.fingerprint.contains(searchQuery, ignoreCase = true)
                }
}

sealed interface KeysUiEvent : UiEvent {
    data class OnSearchQueryChange(val query: String) : KeysUiEvent
    data object OnFabClick : KeysUiEvent
    data object OnSheetDismiss : KeysUiEvent
    data object OnGenerateKeyClick : KeysUiEvent
    data object OnImportKeyClick : KeysUiEvent
    data object OnExportOwnKeyClick : KeysUiEvent
    data object OnExportAllKeysClick : KeysUiEvent
}

sealed interface KeysUiEffect : UiEffect {
    data object NavigateToKeyGeneration : KeysUiEffect
    data object NavigateToKeyImport : KeysUiEffect
    data class SharePublicKey(val armoredText: String) : KeysUiEffect
}
