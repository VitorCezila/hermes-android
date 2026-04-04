package com.cezila.hermes.presentation.keyimport

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.viewModelScope
import com.cezila.hermes.core.domain.crypto.PgpKeyParser
import com.cezila.hermes.core.domain.usecase.ImportPublicKeyUseCase
import com.cezila.hermes.presentation.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeyImportViewModel @Inject constructor(
    private val importPublicKeyUseCase: ImportPublicKeyUseCase,
    private val pgpKeyParser: PgpKeyParser,
) : BaseViewModel<KeyImportUiState, KeyImportUiEvent, KeyImportUiEffect>(
    initialState = KeyImportUiState(),
) {

    private var validationJob: Job? = null

    override fun handleEvent(event: KeyImportUiEvent) {
        when (event) {
            is KeyImportUiEvent.OnArmoredTextChange -> {
                setState { copy(armoredText = event.text, fileName = null) }
                validateAsync(event.text)
            }

            is KeyImportUiEvent.OnFileRead -> {
                setState { copy(armoredText = event.content, fileName = event.fileName) }
                validateAsync(event.content)
            }

            KeyImportUiEvent.OnImportClick -> importKey()

            KeyImportUiEvent.OnBack -> sendEffect(KeyImportUiEffect.NavigateBack)
        }
    }

    private fun validateAsync(text: String) {
        validationJob?.cancel()
        if (text.isBlank()) {
            setState { copy(validationStatus = ValidationStatus.Empty) }
            return
        }
        setState { copy(validationStatus = ValidationStatus.Validating) }
        validationJob = viewModelScope.launch(Dispatchers.IO) {
            if (text.contains("-----BEGIN PGP PRIVATE KEY BLOCK-----")) {
                setState { copy(validationStatus = ValidationStatus.Invalid(ValidationError.SecretKeyDetected)) }
                return@launch
            }
            pgpKeyParser.parsePublicKey(text).fold(
                onSuccess = { setState { copy(validationStatus = ValidationStatus.Valid) } },
                onFailure = { setState { copy(validationStatus = ValidationStatus.Invalid(ValidationError.InvalidFormat)) } },
            )
        }
    }

    private fun importKey() {
        if (currentState.validationStatus != ValidationStatus.Valid) return
        viewModelScope.launch(Dispatchers.IO) {
            setState { copy(isLoading = true) }
            try {
                importPublicKeyUseCase(ImportPublicKeyUseCase.Params(currentState.armoredText))
                    .fold(
                        onSuccess = { sendEffect(KeyImportUiEffect.ImportSuccess) },
                        onFailure = { error ->
                            val status = if (error is SQLiteConstraintException) {
                                ValidationStatus.Invalid(ValidationError.DuplicateKey)
                            } else {
                                ValidationStatus.Invalid(ValidationError.InvalidFormat)
                            }
                            setState { copy(validationStatus = status) }
                        },
                    )
            } finally {
                setState { copy(isLoading = false) }
            }
        }
    }
}
