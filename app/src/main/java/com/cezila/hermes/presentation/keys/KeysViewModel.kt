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
                .onEach { keys -> setState { copy(keys = keys, isLoading = false) } }
                .catch { setState { copy(isLoading = false) } }
                .collect {}
        }
    }

    override fun handleEvent(event: KeysUiEvent) {
        when (event) {
            KeysUiEvent.OnAddKeyClick -> sendEffect(KeysUiEffect.NavigateToKeyGeneration)
        }
    }
}
