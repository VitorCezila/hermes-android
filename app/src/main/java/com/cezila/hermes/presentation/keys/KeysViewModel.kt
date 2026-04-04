package com.cezila.hermes.presentation.keys

import androidx.lifecycle.viewModelScope
import com.cezila.hermes.core.domain.usecase.GetAllKeysUseCase
import com.cezila.hermes.presentation.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeysViewModel @Inject constructor(
    private val getAllKeysUseCase: GetAllKeysUseCase,
) : BaseViewModel<KeysUiState, KeysUiEvent, KeysUiEffect>(
    initialState = KeysUiState(),
) {

    init {
        viewModelScope.launch {
            getAllKeysUseCase()
                .onEach { keys ->
                    setState {
                        copy(
                            ownKey = keys.firstOrNull { it.isSecret },
                            contacts = keys.filter { !it.isSecret },
                            isLoading = false,
                        )
                    }
                }
                .catch { setState { copy(isLoading = false) } }
                .collect {}
        }
    }

    override fun handleEvent(event: KeysUiEvent) {
        when (event) {
            is KeysUiEvent.OnSearchQueryChange ->
                setState { copy(searchQuery = event.query) }

            KeysUiEvent.OnFabClick ->
                setState { copy(showFabSheet = true) }

            KeysUiEvent.OnSheetDismiss ->
                setState { copy(showFabSheet = false) }

            KeysUiEvent.OnGenerateKeyClick -> {
                setState { copy(showFabSheet = false) }
                sendEffect(KeysUiEffect.NavigateToKeyGeneration)
            }

            KeysUiEvent.OnImportKeyClick -> {
                setState { copy(showFabSheet = false) }
                sendEffect(KeysUiEffect.NavigateToKeyImport)
            }

            KeysUiEvent.OnExportOwnKeyClick -> {
                val armoredKey = currentState.ownKey?.armoredPublicKey
                if (!armoredKey.isNullOrBlank()) {
                    sendEffect(KeysUiEffect.SharePublicKey(armoredKey))
                }
            }

            KeysUiEvent.OnExportAllKeysClick -> {
                val allKeys = listOfNotNull(currentState.ownKey) + currentState.contacts
                val armoredText = allKeys.joinToString("\n\n") { it.armoredPublicKey }
                if (armoredText.isNotBlank()) {
                    sendEffect(KeysUiEffect.SharePublicKey(armoredText))
                }
            }

            is KeysUiEvent.OnKeyCardClick ->
                sendEffect(KeysUiEffect.NavigateToKeyDetail(event.keyId))
        }
    }
}
