package com.cezila.hermes.presentation.encrypt

import androidx.lifecycle.viewModelScope
import com.cezila.hermes.core.domain.usecase.EncryptAndSignUseCase
import com.cezila.hermes.core.domain.usecase.EncryptFileAndSignUseCase
import com.cezila.hermes.core.domain.usecase.GetAllKeysUseCase
import com.cezila.hermes.presentation.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EncryptViewModel @Inject constructor(
    private val getAllKeysUseCase: GetAllKeysUseCase,
    private val encryptAndSignUseCase: EncryptAndSignUseCase,
    private val encryptFileAndSignUseCase: EncryptFileAndSignUseCase,
) : BaseViewModel<EncryptUiState, EncryptUiEvent, EncryptUiEffect>(
    initialState = EncryptUiState(),
) {

    init {
        getAllKeysUseCase()
            .onEach { keys ->
                setState {
                    copy(
                        contacts = keys.filter { !it.isSecret },
                        ownKey = keys.firstOrNull { it.isSecret },
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: EncryptUiEvent) {
        when (event) {
            is EncryptUiEvent.OnRecipientSelected ->
                setState { copy(selectedRecipient = event.key, error = null) }

            is EncryptUiEvent.OnMessageChanged ->
                setState { copy(messageText = event.text, error = null) }

            is EncryptUiEvent.OnFileSelected ->
                setState { copy(selectedFileBytes = event.bytes, selectedFileName = event.name, error = null) }

            EncryptUiEvent.OnClearFile ->
                setState { copy(selectedFileBytes = null, selectedFileName = null) }

            EncryptUiEvent.OnEncryptClick -> onEncryptClick()

            is EncryptUiEvent.OnPassphraseConfirmed -> onPassphraseConfirmed(event.passphrase)

            EncryptUiEvent.OnPassphraseDismissed ->
                setState { copy(showPassphraseDialog = false) }

            EncryptUiEvent.OnCopyOutput ->
                currentState.encryptedOutput?.let { sendEffect(EncryptUiEffect.CopyToClipboard(it)) }

            EncryptUiEvent.OnDismissOutput ->
                setState { copy(encryptedOutput = null) }

            EncryptUiEvent.OnDismissError ->
                setState { copy(error = null) }
        }
    }

    private fun onEncryptClick() {
        val state = currentState
        if (state.selectedRecipient == null) {
            setState { copy(error = "Select a recipient first") }
            return
        }
        if (state.messageText.isBlank() && state.selectedFileBytes == null) {
            setState { copy(error = "Enter a message or attach a file") }
            return
        }
        if (state.ownKey == null) {
            setState { copy(error = "No identity key found. Generate your key first.") }
            return
        }
        setState { copy(showPassphraseDialog = true) }
    }

    private fun onPassphraseConfirmed(passphrase: CharArray) {
        setState { copy(showPassphraseDialog = false) }
        val state = currentState
        val recipient = state.selectedRecipient ?: return
        val ownKey = state.ownKey ?: return

        viewModelScope.launch(Dispatchers.IO) {
            setState { copy(isLoading = true, error = null) }
            try {
                val fileBytes = state.selectedFileBytes
                if (fileBytes != null) {
                    val fileName = state.selectedFileName ?: "payload"
                    encryptFileAndSignUseCase(
                        EncryptFileAndSignUseCase.Params(
                            recipientKey = recipient,
                            fileBytes = fileBytes,
                            fileName = fileName,
                            signerKeyId = ownKey.id,
                            passphrase = passphrase,
                        )
                    ).fold(
                        onSuccess = { encrypted ->
                            sendEffect(EncryptUiEffect.SaveEncryptedFile(encrypted, "$fileName.pgp"))
                        },
                        onFailure = { setState { copy(error = it.toUserMessage()) } },
                    )
                } else {
                    encryptAndSignUseCase(
                        EncryptAndSignUseCase.Params(
                            recipientKey = recipient,
                            plaintext = state.messageText,
                            signerKeyId = ownKey.id,
                            passphrase = passphrase,
                        )
                    ).fold(
                        onSuccess = { armored -> setState { copy(encryptedOutput = armored) } },
                        onFailure = { setState { copy(error = it.toUserMessage()) } },
                    )
                }
            } finally {
                passphrase.fill(' ')
                setState { copy(isLoading = false) }
            }
        }
    }

    private fun Throwable.toUserMessage(): String = when {
        message?.contains("checksum", ignoreCase = true) == true ||
            message?.contains("decryption failed", ignoreCase = true) == true ->
            "Wrong passphrase. Please try again."
        message?.contains("no encryption", ignoreCase = true) == true ->
            "Recipient key does not support encryption."
        else -> "Encryption failed. Please try again."
    }
}
