package com.cezila.hermes.presentation.encrypt

import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.mvi.UiEffect
import com.cezila.hermes.core.domain.mvi.UiEvent
import com.cezila.hermes.core.domain.mvi.UiState

data class EncryptUiState(
    val contacts: List<PgpKey> = emptyList(),
    val ownKey: PgpKey? = null,
    val selectedRecipient: PgpKey? = null,
    val messageText: String = "",
    val selectedFileBytes: ByteArray? = null,
    val selectedFileName: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showPassphraseDialog: Boolean = false,
    val encryptedOutput: String? = null,
) : UiState {
    val canEncrypt: Boolean
        get() = selectedRecipient != null && (messageText.isNotBlank() || selectedFileBytes != null)
}

sealed interface EncryptUiEvent : UiEvent {
    data class OnRecipientSelected(val key: PgpKey) : EncryptUiEvent
    data class OnMessageChanged(val text: String) : EncryptUiEvent
    data class OnFileSelected(val bytes: ByteArray, val name: String) : EncryptUiEvent
    data object OnClearFile : EncryptUiEvent
    data object OnEncryptClick : EncryptUiEvent
    data class OnPassphraseConfirmed(val passphrase: CharArray) : EncryptUiEvent
    data object OnPassphraseDismissed : EncryptUiEvent
    data object OnCopyOutput : EncryptUiEvent
    data object OnDismissOutput : EncryptUiEvent
    data object OnDismissError : EncryptUiEvent
}

sealed interface EncryptUiEffect : UiEffect {
    data object PickFile : EncryptUiEffect
    data class SaveEncryptedFile(val bytes: ByteArray, val suggestedName: String) : EncryptUiEffect
    data class CopyToClipboard(val text: String) : EncryptUiEffect
    data class ShowToast(val message: String) : EncryptUiEffect
}
