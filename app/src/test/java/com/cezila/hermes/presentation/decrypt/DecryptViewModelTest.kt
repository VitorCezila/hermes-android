package com.cezila.hermes.presentation.decrypt

import app.cash.turbine.test
import com.cezila.hermes.core.domain.model.DecryptionResult
import com.cezila.hermes.core.domain.model.KeyAlgorithm
import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.model.SignatureStatus
import com.cezila.hermes.core.domain.usecase.DecryptAndVerifyUseCase
import com.cezila.hermes.core.domain.usecase.GetAllKeysUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DecryptViewModelTest {

    private val getAllKeysUseCase: GetAllKeysUseCase = mockk()
    private val decryptAndVerifyUseCase: DecryptAndVerifyUseCase = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: DecryptViewModel

    private val ownKey = PgpKey(
        id = "0xMYKEY",
        fingerprint = "AABBCCDDEEFF0011",
        ownerName = "Alice",
        ownerEmail = "alice@example.com",
        algorithm = KeyAlgorithm.ED25519,
        createdAt = 1_700_000_000_000L,
        expiresAt = null,
        isSecret = true,
        armoredPublicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n-----END PGP PUBLIC KEY BLOCK-----",
    )

    private val contactKey = PgpKey(
        id = "0xBOBKEY",
        fingerprint = "1122334455667788",
        ownerName = "Bob",
        ownerEmail = "bob@example.com",
        algorithm = KeyAlgorithm.RSA_4096,
        createdAt = 1_700_000_000_000L,
        expiresAt = null,
        isSecret = false,
        armoredPublicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----\nBob\n-----END PGP PUBLIC KEY BLOCK-----",
    )

    private val successResult = DecryptionResult(
        plaintext = "Hello, Alice!",
        plaintextBytes = null,
        fileName = null,
        signatureStatus = SignatureStatus.None,
    )

    private val validCiphertext = "-----BEGIN PGP MESSAGE-----\nencrypted\n-----END PGP MESSAGE-----"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getAllKeysUseCase() } returns flowOf(listOf(ownKey, contactKey))
        viewModel = DecryptViewModel(getAllKeysUseCase, decryptAndVerifyUseCase, testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // --- init ---

    @Test
    fun init_setsOwnKeyFromSecretKey() {
        assertEquals(ownKey, viewModel.state.value.ownKey)
    }

    @Test
    fun init_setsAllKeysIncludingContacts() {
        assertEquals(listOf(ownKey, contactKey), viewModel.state.value.allKeys)
    }

    @Test
    fun init_withNoOwnKey_ownKeyIsNull() {
        every { getAllKeysUseCase() } returns flowOf(listOf(contactKey))
        val vm = DecryptViewModel(getAllKeysUseCase, decryptAndVerifyUseCase, testDispatcher)
        assertNull(vm.state.value.ownKey)
        assertEquals(listOf(contactKey), vm.state.value.allKeys)
    }

    // --- Header status ---

    @Test
    fun onCiphertextChanged_blankText_setsHeaderStatusAwaiting() {
        viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged(""))
        assertEquals(HeaderStatus.Awaiting, viewModel.state.value.headerStatus)
    }

    @Test
    fun onCiphertextChanged_validPgpHeader_setsHeaderStatusValid() {
        viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged(validCiphertext))
        assertEquals(HeaderStatus.Valid, viewModel.state.value.headerStatus)
    }

    @Test
    fun onCiphertextChanged_nonPgpContent_setsHeaderStatusInvalid() {
        viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged("just some random text"))
        assertEquals(HeaderStatus.Invalid, viewModel.state.value.headerStatus)
    }

    @Test
    fun onCiphertextChanged_updatesInputAndClearsError() {
        viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged(validCiphertext))
        assertEquals(validCiphertext, viewModel.state.value.ciphertextInput)
        assertNull(viewModel.state.value.error)
    }

    // --- Clear ---

    @Test
    fun onClearInput_resetsAllTextInputState() {
        viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged(validCiphertext))
        viewModel.onEvent(DecryptUiEvent.OnClearInput)

        assertEquals("", viewModel.state.value.ciphertextInput)
        assertEquals(HeaderStatus.Awaiting, viewModel.state.value.headerStatus)
        assertNull(viewModel.state.value.error)
    }

    // --- File ---

    @Test
    fun onFileSelected_setsFileBytesAndSwitchesToFileMode() {
        val bytes = "data".toByteArray()
        viewModel.onEvent(DecryptUiEvent.OnFileSelected(bytes, "doc.pgp"))

        assertEquals(InputMode.FILE, viewModel.state.value.inputMode)
        assertEquals("doc.pgp", viewModel.state.value.selectedFileName)
        assertNotNull(viewModel.state.value.selectedFileBytes)
    }

    @Test
    fun onClearFile_removesSelectedFile() {
        viewModel.onEvent(DecryptUiEvent.OnFileSelected("data".toByteArray(), "doc.pgp"))
        viewModel.onEvent(DecryptUiEvent.OnClearFile)

        assertNull(viewModel.state.value.selectedFileBytes)
        assertNull(viewModel.state.value.selectedFileName)
    }

    // --- Mode switch ---

    @Test
    fun onInputModeChanged_updatesMode() {
        viewModel.onEvent(DecryptUiEvent.OnInputModeChanged(InputMode.FILE))
        assertEquals(InputMode.FILE, viewModel.state.value.inputMode)
    }

    // --- canDecrypt ---

    @Test
    fun canDecrypt_textMode_validHeader_isTrue() {
        viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged(validCiphertext))
        assertTrue(viewModel.state.value.canDecrypt)
    }

    @Test
    fun canDecrypt_textMode_awaitingHeader_isFalse() {
        viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged(""))
        assertFalse(viewModel.state.value.canDecrypt)
    }

    @Test
    fun canDecrypt_fileMode_fileSelected_isTrue() {
        viewModel.onEvent(DecryptUiEvent.OnFileSelected("data".toByteArray(), "doc.pgp"))
        assertTrue(viewModel.state.value.canDecrypt)
    }

    @Test
    fun canDecrypt_fileMode_noFileSelected_isFalse() {
        viewModel.onEvent(DecryptUiEvent.OnInputModeChanged(InputMode.FILE))
        assertFalse(viewModel.state.value.canDecrypt)
    }

    @Test
    fun canDecrypt_noOwnKey_isFalse() {
        every { getAllKeysUseCase() } returns flowOf(listOf(contactKey))
        val vm = DecryptViewModel(getAllKeysUseCase, decryptAndVerifyUseCase, testDispatcher)
        vm.onEvent(DecryptUiEvent.OnCiphertextChanged(validCiphertext))
        assertFalse(vm.state.value.canDecrypt)
    }

    // --- Passphrase dialog ---

    @Test
    fun onDecryptClick_whenCanDecrypt_showsPassphraseDialog() {
        viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged(validCiphertext))
        viewModel.onEvent(DecryptUiEvent.OnDecryptClick)
        assertTrue(viewModel.state.value.showPassphraseDialog)
    }

    @Test
    fun onDecryptClick_whenNoOwnKey_setsErrorAndDoesNotShowDialog() {
        every { getAllKeysUseCase() } returns flowOf(emptyList())
        val vm = DecryptViewModel(getAllKeysUseCase, decryptAndVerifyUseCase, testDispatcher)
        vm.onEvent(DecryptUiEvent.OnCiphertextChanged(validCiphertext))
        vm.onEvent(DecryptUiEvent.OnDecryptClick)

        assertFalse(vm.state.value.showPassphraseDialog)
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun onPassphraseDismissed_hidesDialog() {
        viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged(validCiphertext))
        viewModel.onEvent(DecryptUiEvent.OnDecryptClick)
        viewModel.onEvent(DecryptUiEvent.OnPassphraseDismissed)
        assertFalse(viewModel.state.value.showPassphraseDialog)
    }

    // --- Decrypt flow ---

    @Test
    fun onPassphraseConfirmed_success_setsDecryptionResult() = runTest {
        coEvery { decryptAndVerifyUseCase(any()) } returns Result.success(successResult)
        viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged(validCiphertext))

        viewModel.onEvent(DecryptUiEvent.OnPassphraseConfirmed("pass".toCharArray()))

        assertEquals(successResult, viewModel.state.value.decryptionResult)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun onPassphraseConfirmed_failure_setsError() = runTest {
        coEvery { decryptAndVerifyUseCase(any()) } returns Result.failure(RuntimeException("checksum failed"))
        viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged(validCiphertext))

        viewModel.onEvent(DecryptUiEvent.OnPassphraseConfirmed("pass".toCharArray()))

        assertNotNull(viewModel.state.value.error)
        assertNull(viewModel.state.value.decryptionResult)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun onPassphraseConfirmed_alwaysWipesPassphrase() = runTest {
        val paramsSlot = slot<DecryptAndVerifyUseCase.Params>()
        coEvery { decryptAndVerifyUseCase(capture(paramsSlot)) } returns Result.success(successResult)
        viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged(validCiphertext))

        viewModel.onEvent(DecryptUiEvent.OnPassphraseConfirmed("s3cr3t".toCharArray()))

        assertTrue(paramsSlot.captured.passphrase.all { it == ' ' })
    }

    @Test
    fun onPassphraseConfirmed_failure_alsoWipesPassphrase() = runTest {
        val paramsSlot = slot<DecryptAndVerifyUseCase.Params>()
        coEvery { decryptAndVerifyUseCase(capture(paramsSlot)) } returns Result.failure(RuntimeException("error"))
        viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged(validCiphertext))

        viewModel.onEvent(DecryptUiEvent.OnPassphraseConfirmed("s3cr3t".toCharArray()))

        assertTrue(paramsSlot.captured.passphrase.all { it == ' ' })
    }

    // --- Effects ---

    @Test
    fun onPasteFromClipboard_emitsRequestClipboardReadEffect() = runTest {
        viewModel.effect.test {
            viewModel.onEvent(DecryptUiEvent.OnPasteFromClipboard)
            assertEquals(DecryptUiEffect.RequestClipboardRead, awaitItem())
        }
    }

    @Test
    fun onCopyPlaintext_withPlaintext_emitsCopyToClipboardEffect() = runTest {
        coEvery { decryptAndVerifyUseCase(any()) } returns Result.success(successResult)
        viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged(validCiphertext))
        viewModel.onEvent(DecryptUiEvent.OnPassphraseConfirmed("pass".toCharArray()))

        viewModel.effect.test {
            viewModel.onEvent(DecryptUiEvent.OnCopyPlaintext)
            val effect = awaitItem()
            assertTrue(effect is DecryptUiEffect.CopyToClipboard)
            assertEquals(successResult.plaintext, (effect as DecryptUiEffect.CopyToClipboard).text)
        }
    }

    @Test
    fun onSaveDecryptedFile_withFileResult_emitsSaveFileEffect() = runTest {
        val fileBytes = "file content".toByteArray()
        val fileResult = DecryptionResult(
            plaintext = "",
            plaintextBytes = fileBytes,
            fileName = "doc.pdf",
            signatureStatus = SignatureStatus.None,
        )
        coEvery { decryptAndVerifyUseCase(any()) } returns Result.success(fileResult)
        viewModel.onEvent(DecryptUiEvent.OnFileSelected(fileBytes, "doc.pgp"))
        viewModel.onEvent(DecryptUiEvent.OnPassphraseConfirmed("pass".toCharArray()))

        viewModel.effect.test {
            viewModel.onEvent(DecryptUiEvent.OnSaveDecryptedFile)
            val effect = awaitItem()
            assertTrue(effect is DecryptUiEffect.SaveDecryptedFile)
        }
    }

    // --- Dismiss ---

    @Test
    fun onDismissResult_clearsDecryptionResult() = runTest {
        coEvery { decryptAndVerifyUseCase(any()) } returns Result.success(successResult)
        viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged(validCiphertext))
        viewModel.onEvent(DecryptUiEvent.OnPassphraseConfirmed("pass".toCharArray()))
        assertNotNull(viewModel.state.value.decryptionResult)

        viewModel.onEvent(DecryptUiEvent.OnDismissResult)
        assertNull(viewModel.state.value.decryptionResult)
    }

    @Test
    fun onDismissError_clearsError() = runTest {
        coEvery { decryptAndVerifyUseCase(any()) } returns Result.failure(RuntimeException("error"))
        viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged(validCiphertext))
        viewModel.onEvent(DecryptUiEvent.OnPassphraseConfirmed("pass".toCharArray()))
        assertNotNull(viewModel.state.value.error)

        viewModel.onEvent(DecryptUiEvent.OnDismissError)
        assertNull(viewModel.state.value.error)
    }

    // --- Error message mapping ---

    @Test
    fun decryptionError_checksumMessage_mapsToWrongPassphraseMessage() = runTest {
        coEvery { decryptAndVerifyUseCase(any()) } returns Result.failure(RuntimeException("checksum failed"))
        viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged(validCiphertext))
        viewModel.onEvent(DecryptUiEvent.OnPassphraseConfirmed("pass".toCharArray()))

        assertTrue(viewModel.state.value.error!!.contains("passphrase", ignoreCase = true))
    }

    @Test
    fun decryptionError_noSuitableKey_mapsToNotEncryptedForYouMessage() = runTest {
        coEvery { decryptAndVerifyUseCase(any()) } returns Result.failure(RuntimeException("no suitable key"))
        viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged(validCiphertext))
        viewModel.onEvent(DecryptUiEvent.OnPassphraseConfirmed("pass".toCharArray()))

        assertTrue(viewModel.state.value.error!!.contains("key", ignoreCase = true))
    }

    @Test
    fun decryptionError_verifyUseCaseIsCalledWithOwnKeyId() = runTest {
        val paramsSlot = slot<DecryptAndVerifyUseCase.Params>()
        coEvery { decryptAndVerifyUseCase(capture(paramsSlot)) } returns Result.success(successResult)
        viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged(validCiphertext))
        viewModel.onEvent(DecryptUiEvent.OnPassphraseConfirmed("pass".toCharArray()))

        assertEquals(ownKey.id, paramsSlot.captured.recipientKeyId)
        coVerify(exactly = 1) { decryptAndVerifyUseCase(any()) }
    }
}
