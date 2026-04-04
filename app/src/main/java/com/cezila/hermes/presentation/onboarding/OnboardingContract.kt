package com.cezila.hermes.presentation.onboarding

import com.cezila.hermes.core.domain.mvi.UiEffect
import com.cezila.hermes.core.domain.mvi.UiEvent
import com.cezila.hermes.core.domain.mvi.UiState

data class OnboardingUiState(
    val showImportOrCreateSheet: Boolean = false,
) : UiState

sealed interface OnboardingUiEvent : UiEvent {
    data object OnImportOrCreateClick : OnboardingUiEvent
    data object OnLearnMoreClick : OnboardingUiEvent
    data object OnSheetDismiss : OnboardingUiEvent
    data object OnGenerateKeyClick : OnboardingUiEvent  // stub — Phase 2
    data object OnImportKeyClick : OnboardingUiEvent    // stub — Phase 4
}

sealed interface OnboardingUiEffect : UiEffect {
    data object NavigateToLearnEncryption : OnboardingUiEffect
    data object NavigateToKeyGeneration : OnboardingUiEffect
}
