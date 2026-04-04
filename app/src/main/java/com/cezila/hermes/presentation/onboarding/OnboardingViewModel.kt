package com.cezila.hermes.presentation.onboarding

import androidx.lifecycle.viewModelScope
import com.cezila.hermes.core.domain.usecase.GetAllKeysUseCase
import com.cezila.hermes.presentation.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val getAllKeysUseCase: GetAllKeysUseCase,
) : BaseViewModel<OnboardingUiState, OnboardingUiEvent, OnboardingUiEffect>(
    initialState = OnboardingUiState(),
) {

    init {
        viewModelScope.launch {
            val keys = getAllKeysUseCase().first()
            if (keys.any { it.isSecret }) {
                sendEffect(OnboardingUiEffect.NavigateToKeys)
            }
        }
    }

    override fun handleEvent(event: OnboardingUiEvent) {
        when (event) {
            OnboardingUiEvent.OnImportOrCreateClick ->
                setState { copy(showImportOrCreateSheet = true) }

            OnboardingUiEvent.OnLearnMoreClick ->
                sendEffect(OnboardingUiEffect.NavigateToLearnEncryption)

            OnboardingUiEvent.OnSheetDismiss ->
                setState { copy(showImportOrCreateSheet = false) }

            OnboardingUiEvent.OnGenerateKeyClick -> {
                setState { copy(showImportOrCreateSheet = false) }
                sendEffect(OnboardingUiEffect.NavigateToKeyGeneration)
            }

            OnboardingUiEvent.OnImportKeyClick ->
                setState { copy(showImportOrCreateSheet = false) }
        }
    }
}
