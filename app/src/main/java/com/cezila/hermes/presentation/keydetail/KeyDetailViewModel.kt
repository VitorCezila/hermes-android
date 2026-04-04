package com.cezila.hermes.presentation.keydetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.cezila.hermes.core.domain.usecase.DeleteKeyUseCase
import com.cezila.hermes.core.domain.usecase.GetKeyByIdUseCase
import com.cezila.hermes.presentation.mvi.BaseViewModel
import com.cezila.hermes.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeyDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getKeyByIdUseCase: GetKeyByIdUseCase,
    private val deleteKeyUseCase: DeleteKeyUseCase,
) : BaseViewModel<KeyDetailUiState, KeyDetailUiEvent, KeyDetailUiEffect>(
    initialState = KeyDetailUiState(),
) {

    private val keyId: String = savedStateHandle.toRoute<Route.KeyDetail>().keyId

    init {
        loadKey()
    }

    private fun loadKey() {
        viewModelScope.launch(Dispatchers.IO) {
            getKeyByIdUseCase(keyId)
                .onSuccess { key ->
                    setState { copy(key = key, isLoading = false) }
                }
                .onFailure { error ->
                    setState { copy(isLoading = false, error = error.message) }
                }
        }
    }

    override fun handleEvent(event: KeyDetailUiEvent) {
        when (event) {
            KeyDetailUiEvent.OnBackClick ->
                sendEffect(KeyDetailUiEffect.NavigateBack)

            KeyDetailUiEvent.OnExportClick -> {
                val armoredKey = currentState.key?.armoredPublicKey
                if (!armoredKey.isNullOrBlank()) {
                    sendEffect(KeyDetailUiEffect.SharePublicKey(armoredKey))
                }
            }

            KeyDetailUiEvent.OnDeleteClick ->
                setState { copy(showDeleteDialog = true) }

            KeyDetailUiEvent.OnDeleteDismiss ->
                setState { copy(showDeleteDialog = false) }

            KeyDetailUiEvent.OnDeleteConfirm -> {
                setState { copy(showDeleteDialog = false) }
                viewModelScope.launch(Dispatchers.IO) {
                    deleteKeyUseCase(keyId)
                        .onSuccess { sendEffect(KeyDetailUiEffect.NavigateBack) }
                        .onFailure { error ->
                            setState { copy(error = error.message) }
                        }
                }
            }
        }
    }
}
