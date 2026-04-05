package com.cezila.hermes.presentation.decrypt

import androidx.lifecycle.viewModelScope
import com.cezila.hermes.core.data.di.IoDispatcher
import com.cezila.hermes.core.domain.usecase.DecryptAndVerifyUseCase
import com.cezila.hermes.core.domain.usecase.GetAllKeysUseCase
import com.cezila.hermes.presentation.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DecryptViewModel @Inject constructor(
    private val getAllKeysUseCase: GetAllKeysUseCase,
    private val decryptAndVerifyUseCase: DecryptAndVerifyUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel<DecryptUiState, DecryptUiEvent, DecryptUiEffect>(
    initialState = DecryptUiState(),
) {

    init {
        getAllKeysUseCase()
            .onEach { keys ->
                setState {
                    copy(
                        allKeys = keys,
                        ownKey = keys.firstOrNull { it.isSecret },
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: DecryptUiEvent) {
        when (event) {
            is DecryptUiEvent.OnCiphertextChanged ->
                setState {
                    copy(
                        ciphertextInput = event.text,
                        headerStatus = event.text.detectHeaderStatus(),
                        error = null,
                    )
                }

            DecryptUiEvent.OnPasteFromClipboard ->
                sendEffect(DecryptUiEffect.RequestClipboardRead)

            DecryptUiEvent.OnClearInput ->
                setState {
                    copy(
                        ciphertextInput = "",
                        headerStatus = HeaderStatus.Awaiting,
                        error = null,
                        decryptionResult = null,
                    )
                }

            is DecryptUiEvent.OnFileSelected ->
                setState {
                    copy(
                        selectedFileBytes = event.bytes,
                        selectedFileName = event.name,
                        inputMode = InputMode.FILE,
                        error = null,
                    )
                }

            DecryptUiEvent.OnClearFile ->
                setState { copy(selectedFileBytes = null, selectedFileName = null) }

            is DecryptUiEvent.OnInputModeChanged ->
                setState { copy(inputMode = event.mode, error = null) }

            DecryptUiEvent.OnDecryptClick -> onDecryptClick()

            is DecryptUiEvent.OnPassphraseConfirmed -> onPassphraseConfirmed(event.passphrase)

            DecryptUiEvent.OnPassphraseDismissed ->
                setState { copy(showPassphraseDialog = false) }

            DecryptUiEvent.OnCopyPlaintext ->
                currentState.decryptionResult?.plaintext
                    ?.takeIf { it.isNotBlank() }
                    ?.let { sendEffect(DecryptUiEffect.CopyToClipboard(it)) }

            DecryptUiEvent.OnSaveDecryptedFile -> {
                val result = currentState.decryptionResult ?: return
                val bytes = result.plaintextBytes ?: return
                val name = result.fileName ?: "decrypted_file"
                sendEffect(DecryptUiEffect.SaveDecryptedFile(bytes, name))
            }

            DecryptUiEvent.OnDismissResult ->
                setState { copy(decryptionResult = null) }

            DecryptUiEvent.OnDismissError ->
                setState { copy(error = null) }
        }
    }

    private fun onDecryptClick() {
        if (currentState.ownKey == null) {
            setState { copy(error = "No identity key found. Generate your key first.") }
            return
        }
        setState { copy(showPassphraseDialog = true) }
    }

    private fun onPassphraseConfirmed(passphrase: CharArray) {
        setState { copy(showPassphraseDialog = false) }
        val state = currentState
        val ownKey = state.ownKey ?: return

        viewModelScope.launch(ioDispatcher) {
            setState { copy(isLoading = true, error = null) }
            try {
                decryptAndVerifyUseCase(
                    DecryptAndVerifyUseCase.Params(
                        ciphertext = state.ciphertextInput,
                        fileBytes = if (state.inputMode == InputMode.FILE) state.selectedFileBytes else null,
                        recipientKeyId = ownKey.id,
                        passphrase = passphrase,
                    )
                ).fold(
                    onSuccess = { result -> setState { copy(decryptionResult = result) } },
                    onFailure = { setState { copy(error = it.toUserMessage()) } },
                )
            } finally {
                passphrase.fill(' ')
                setState { copy(isLoading = false) }
            }
        }
    }

    private fun Throwable.toUserMessage(): String = when {
        message?.contains("checksum", ignoreCase = true) == true ||
            message?.contains("integrity", ignoreCase = true) == true ||
            message?.contains("decryption failed", ignoreCase = true) == true ->
            "Wrong passphrase or corrupted message."
        message?.contains("no suitable", ignoreCase = true) == true ||
            message?.contains("not encrypted for", ignoreCase = true) == true ->
            "This message was not encrypted for your key."
        message?.contains("armor", ignoreCase = true) == true ->
            "Invalid PGP message format."
        else -> "Decryption failed. Please try again."
    }

    private fun String.detectHeaderStatus(): HeaderStatus = when {
        isBlank() -> HeaderStatus.Awaiting
        contains("-----BEGIN PGP MESSAGE-----") -> HeaderStatus.Valid
        else -> HeaderStatus.Invalid
    }
}
