package com.cezila.hermes.presentation.settings

import androidx.lifecycle.viewModelScope
import com.cezila.hermes.core.domain.usecase.ClearAllKeysUseCase
import com.cezila.hermes.presentation.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val clearAllKeysUseCase: ClearAllKeysUseCase,
) : BaseViewModel<SettingsUiState, SettingsUiEvent, SettingsUiEffect>(
    initialState = SettingsUiState(),
) {

    override fun handleEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.OnClearAllKeysClick ->
                setState { copy(showClearAllConfirmation = true) }

            SettingsUiEvent.OnClearAllKeysDismiss ->
                setState { copy(showClearAllConfirmation = false) }

            SettingsUiEvent.OnClearAllKeysConfirm -> {
                setState { copy(showClearAllConfirmation = false, isClearingKeys = true) }
                viewModelScope.launch {
                    clearAllKeysUseCase()
                        .fold(
                            onSuccess = {
                                setState { copy(isClearingKeys = false) }
                                sendEffect(SettingsUiEffect.ShowToast("All keys cleared"))
                            },
                            onFailure = {
                                setState { copy(isClearingKeys = false) }
                                sendEffect(SettingsUiEffect.ShowToast("Failed to clear keys"))
                            },
                        )
                }
            }

            SettingsUiEvent.OnBackClick ->
                sendEffect(SettingsUiEffect.NavigateBack)

            is SettingsUiEvent.OnVersionLoaded ->
                setState { copy(appVersion = event.version) }
        }
    }
}
