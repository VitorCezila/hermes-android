package com.cezila.hermes.presentation.keyimport

import app.cash.turbine.test
import com.cezila.hermes.core.domain.crypto.ParsedPublicKeyInfo
import com.cezila.hermes.core.domain.crypto.PgpKeyParser
import com.cezila.hermes.core.domain.model.KeyAlgorithm
import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.usecase.ImportPublicKeyUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KeyImportViewModelTest {

    private val importPublicKeyUseCase: ImportPublicKeyUseCase = mockk()
    private val pgpKeyParser: PgpKeyParser = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: KeyImportViewModel

    private val fakeParsedKey = ParsedPublicKeyInfo(
        fingerprint = "AABBCCDDEEFF",
        keyId = "0xAABBCCDDEEFF0011",
        algorithm = KeyAlgorithm.ED25519,
        ownerName = "Alice",
        ownerEmail = "alice@example.com",
        createdAt = 1_700_000_000_000L,
        expiresAt = null,
    )

    private val fakePgpKey = PgpKey(
        id = "0xAABBCCDDEEFF0011",
        fingerprint = "AABBCCDDEEFF",
        ownerName = "Alice",
        ownerEmail = "alice@example.com",
        algorithm = KeyAlgorithm.ED25519,
        createdAt = 1_700_000_000_000L,
        expiresAt = null,
        isSecret = false,
        armoredPublicKey = VALID_ARMORED_KEY,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = KeyImportViewModel(importPublicKeyUseCase, pgpKeyParser, testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // --- Validation ---

    @Test
    fun emptyText_keepsEmptyStatus() = runTest {
        viewModel.onEvent(KeyImportUiEvent.OnArmoredTextChange(""))

        assertEquals(ValidationStatus.Empty, viewModel.state.value.validationStatus)
    }

    @Test
    fun blankText_keepsEmptyStatus() = runTest {
        viewModel.onEvent(KeyImportUiEvent.OnArmoredTextChange("   "))

        assertEquals(ValidationStatus.Empty, viewModel.state.value.validationStatus)
    }

    @Test
    fun secretKeyBlock_setsSecretKeyDetected() = runTest {
        viewModel.onEvent(KeyImportUiEvent.OnArmoredTextChange(SECRET_KEY_BLOCK))

        assertEquals(
            ValidationStatus.Invalid(ValidationError.SecretKeyDetected),
            viewModel.state.value.validationStatus,
        )
        coVerify(exactly = 0) { pgpKeyParser.parsePublicKey(any()) }
    }

    @Test
    fun invalidText_parseFails_setsInvalidFormat() = runTest {
        coEvery { pgpKeyParser.parsePublicKey(any()) } returns Result.failure(IllegalArgumentException("bad key"))

        viewModel.onEvent(KeyImportUiEvent.OnArmoredTextChange("not a pgp key"))

        assertEquals(
            ValidationStatus.Invalid(ValidationError.InvalidFormat),
            viewModel.state.value.validationStatus,
        )
    }

    @Test
    fun validKey_parseSucceeds_setsValid() = runTest {
        coEvery { pgpKeyParser.parsePublicKey(any()) } returns Result.success(fakeParsedKey)

        viewModel.onEvent(KeyImportUiEvent.OnArmoredTextChange(VALID_ARMORED_KEY))

        assertEquals(ValidationStatus.Valid, viewModel.state.value.validationStatus)
    }

    // --- File read ---

    @Test
    fun onFileRead_updatesArmoredTextAndFileName() = runTest {
        coEvery { pgpKeyParser.parsePublicKey(any()) } returns Result.success(fakeParsedKey)

        viewModel.onEvent(KeyImportUiEvent.OnFileRead(VALID_ARMORED_KEY, "alice.asc"))

        assertEquals(VALID_ARMORED_KEY, viewModel.state.value.armoredText)
        assertEquals("alice.asc", viewModel.state.value.fileName)
        assertEquals(ValidationStatus.Valid, viewModel.state.value.validationStatus)
    }

    @Test
    fun onArmoredTextChange_clearsFileName() = runTest {
        coEvery { pgpKeyParser.parsePublicKey(any()) } returns Result.success(fakeParsedKey)

        viewModel.onEvent(KeyImportUiEvent.OnFileRead(VALID_ARMORED_KEY, "alice.asc"))
        viewModel.onEvent(KeyImportUiEvent.OnArmoredTextChange("different text"))

        assertEquals(null, viewModel.state.value.fileName)
    }

    // --- Import ---

    @Test
    fun importClick_whenNotValid_doesNotCallUseCase() = runTest {
        viewModel.onEvent(KeyImportUiEvent.OnImportClick)

        coVerify(exactly = 0) { importPublicKeyUseCase(any()) }
    }

    @Test
    fun importClick_success_emitsImportSuccess() = runTest {
        coEvery { pgpKeyParser.parsePublicKey(any()) } returns Result.success(fakeParsedKey)
        coEvery { importPublicKeyUseCase(any()) } returns Result.success(fakePgpKey)

        viewModel.onEvent(KeyImportUiEvent.OnArmoredTextChange(VALID_ARMORED_KEY))

        viewModel.effect.test {
            viewModel.onEvent(KeyImportUiEvent.OnImportClick)
            assertEquals(KeyImportUiEffect.ImportSuccess, awaitItem())
        }
    }

    @Test
    fun importClick_success_setsIsLoadingFalseAfterCompletion() = runTest {
        coEvery { pgpKeyParser.parsePublicKey(any()) } returns Result.success(fakeParsedKey)
        coEvery { importPublicKeyUseCase(any()) } returns Result.success(fakePgpKey)

        viewModel.onEvent(KeyImportUiEvent.OnArmoredTextChange(VALID_ARMORED_KEY))
        viewModel.onEvent(KeyImportUiEvent.OnImportClick)

        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun importClick_duplicateKey_setsDuplicateKeyStatus() = runTest {
        coEvery { pgpKeyParser.parsePublicKey(any()) } returns Result.success(fakeParsedKey)
        coEvery { importPublicKeyUseCase(any()) } returns Result.failure(android.database.sqlite.SQLiteConstraintException("UNIQUE constraint failed"))

        viewModel.onEvent(KeyImportUiEvent.OnArmoredTextChange(VALID_ARMORED_KEY))
        viewModel.onEvent(KeyImportUiEvent.OnImportClick)

        assertEquals(
            ValidationStatus.Invalid(ValidationError.DuplicateKey),
            viewModel.state.value.validationStatus,
        )
    }

    @Test
    fun importClick_genericFailure_setsInvalidFormatStatus() = runTest {
        coEvery { pgpKeyParser.parsePublicKey(any()) } returns Result.success(fakeParsedKey)
        coEvery { importPublicKeyUseCase(any()) } returns Result.failure(RuntimeException("unexpected"))

        viewModel.onEvent(KeyImportUiEvent.OnArmoredTextChange(VALID_ARMORED_KEY))
        viewModel.onEvent(KeyImportUiEvent.OnImportClick)

        assertEquals(
            ValidationStatus.Invalid(ValidationError.InvalidFormat),
            viewModel.state.value.validationStatus,
        )
    }

    // --- Navigation ---

    @Test
    fun onBack_emitsNavigateBackEffect() = runTest {
        viewModel.effect.test {
            viewModel.onEvent(KeyImportUiEvent.OnBack)
            assertEquals(KeyImportUiEffect.NavigateBack, awaitItem())
        }
    }

    companion object {
        private const val VALID_ARMORED_KEY = "-----BEGIN PGP PUBLIC KEY BLOCK-----\nfakekey\n-----END PGP PUBLIC KEY BLOCK-----"
        private const val SECRET_KEY_BLOCK = "-----BEGIN PGP PRIVATE KEY BLOCK-----\nfakekey\n-----END PGP PRIVATE KEY BLOCK-----"
    }
}
