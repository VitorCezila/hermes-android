package com.cezila.hermes.presentation.decrypt

import com.cezila.hermes.core.domain.model.DecryptionResult
import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.mvi.UiEffect
import com.cezila.hermes.core.domain.mvi.UiEvent
import com.cezila.hermes.core.domain.mvi.UiState

data class DecryptUiState(
    val ownKey: PgpKey? = null,
    val allKeys: List<PgpKey> = emptyList(),
    val inputMode: InputMode = InputMode.TEXT,
    val ciphertextInput: String = "",
    val headerStatus: HeaderStatus = HeaderStatus.Awaiting,
    val selectedFileBytes: ByteArray? = null,
    val selectedFileName: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showPassphraseDialog: Boolean = false,
    val decryptionResult: DecryptionResult? = null,
) : UiState {
    val canDecrypt: Boolean
        get() = ownKey != null &&
            (inputMode == InputMode.TEXT && headerStatus == HeaderStatus.Valid ||
                inputMode == InputMode.FILE && selectedFileBytes != null)
}

enum class InputMode { TEXT, FILE }

enum class HeaderStatus { Awaiting, Valid, Invalid }

sealed interface DecryptUiEvent : UiEvent {
    data class OnCiphertextChanged(val text: String) : DecryptUiEvent
    data object OnPasteFromClipboard : DecryptUiEvent
    data object OnClearInput : DecryptUiEvent
    data class OnFileSelected(val bytes: ByteArray, val name: String) : DecryptUiEvent
    data object OnClearFile : DecryptUiEvent
    data class OnInputModeChanged(val mode: InputMode) : DecryptUiEvent
    data object OnDecryptClick : DecryptUiEvent
    data class OnPassphraseConfirmed(val passphrase: CharArray) : DecryptUiEvent
    data object OnPassphraseDismissed : DecryptUiEvent
    data object OnCopyPlaintext : DecryptUiEvent
    data object OnSaveDecryptedFile : DecryptUiEvent
    data object OnDismissResult : DecryptUiEvent
    data object OnDismissError : DecryptUiEvent
}

sealed interface DecryptUiEffect : UiEffect {
    data object PickFile : DecryptUiEffect
    data class SaveDecryptedFile(val bytes: ByteArray, val suggestedName: String) : DecryptUiEffect
    data class CopyToClipboard(val text: String) : DecryptUiEffect
    data class ShowToast(val message: String) : DecryptUiEffect
    data object RequestClipboardRead : DecryptUiEffect
}
