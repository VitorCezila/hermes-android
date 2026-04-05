package com.cezila.hermes.presentation.keygeneration

import androidx.lifecycle.viewModelScope
import com.cezila.hermes.core.data.di.IoDispatcher
import com.cezila.hermes.core.domain.usecase.GenerateKeyPairUseCase
import com.cezila.hermes.presentation.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeyGenerationViewModel @Inject constructor(
    private val generateKeyPairUseCase: GenerateKeyPairUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel<KeyGenerationUiState, KeyGenerationUiEvent, KeyGenerationUiEffect>(
    initialState = KeyGenerationUiState(),
) {

    override fun handleEvent(event: KeyGenerationUiEvent) {
        when (event) {
            is KeyGenerationUiEvent.OnNameChange ->
                setState { copy(ownerName = event.value, nameError = null) }

            is KeyGenerationUiEvent.OnEmailChange ->
                setState { copy(ownerEmail = event.value, emailError = null) }

            is KeyGenerationUiEvent.OnAlgorithmChange ->
                setState { copy(selectedAlgorithm = event.algorithm) }

            is KeyGenerationUiEvent.OnExpiryChange ->
                setState { copy(expiryDays = event.days) }

            is KeyGenerationUiEvent.OnPassphraseChange ->
                setState { copy(passphrase = event.value, passphraseError = null, confirmPassphraseError = null) }

            is KeyGenerationUiEvent.OnConfirmPassphraseChange ->
                setState { copy(confirmPassphrase = event.value, confirmPassphraseError = null) }

            KeyGenerationUiEvent.OnGenerateClick -> generate()

            KeyGenerationUiEvent.OnBack ->
                sendEffect(KeyGenerationUiEffect.NavigateBack)
        }
    }

    private fun generate() {
        if (!validate()) return

        viewModelScope.launch(ioDispatcher) {
            setState { copy(isLoading = true, generalError = null) }
            val passphraseArray = currentState.passphrase.toCharArray()
            try {
                val result = generateKeyPairUseCase(
                    GenerateKeyPairUseCase.Params(
                        ownerName = currentState.ownerName.trim(),
                        ownerEmail = currentState.ownerEmail.trim(),
                        algorithm = currentState.selectedAlgorithm,
                        passphrase = passphraseArray,
                        expiryDays = currentState.expiryDays,
                    )
                )
                result.fold(
                    onSuccess = { sendEffect(KeyGenerationUiEffect.NavigateToKeys) },
                    onFailure = { setState { copy(generalError = it.message ?: "Key generation failed") } },
                )
            } finally {
                passphraseArray.fill(' ')
                setState { copy(isLoading = false) }
            }
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
    }

    private fun validate(): Boolean {
        val state = currentState
        var isValid = true

        if (state.ownerName.isBlank()) {
            setState { copy(nameError = "Name is required") }
            isValid = false
        }

        if (!EMAIL_REGEX.matches(state.ownerEmail.trim())) {
            setState { copy(emailError = "Enter a valid email address") }
            isValid = false
        }

        if (state.passphrase.length < 8) {
            setState { copy(passphraseError = "Passphrase must be at least 8 characters") }
            isValid = false
        }

        if (state.passphrase != state.confirmPassphrase) {
            setState { copy(confirmPassphraseError = "Passphrases do not match") }
            isValid = false
        }

        return isValid
    }
}
