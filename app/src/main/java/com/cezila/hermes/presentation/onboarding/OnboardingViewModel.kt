package com.cezila.hermes.presentation.onboarding

import com.cezila.hermes.presentation.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor() :
    BaseViewModel<OnboardingUiState, OnboardingUiEvent, OnboardingUiEffect>(
        initialState = OnboardingUiState(),
    ) {

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
