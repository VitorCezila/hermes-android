package com.cezila.hermes.presentation.keygeneration

import android.util.Patterns
import app.cash.turbine.test
import com.cezila.hermes.core.domain.model.KeyAlgorithm
import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.usecase.GenerateKeyPairUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import java.util.regex.Matcher
import java.util.regex.Pattern

@OptIn(ExperimentalCoroutinesApi::class)
class KeyGenerationViewModelTest {

    private val generateKeyPairUseCase: GenerateKeyPairUseCase = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: KeyGenerationViewModel

    private val mockEmailPattern: Pattern = mockk()
    private val mockMatcher: Matcher = mockk()

    private val fakePgpKey = PgpKey(
        id = "0xAABB",
        fingerprint = "AABB",
        ownerName = "Alice",
        ownerEmail = "alice@example.com",
        algorithm = KeyAlgorithm.ED25519,
        createdAt = 1_700_000_000_000L,
        expiresAt = null,
        isSecret = true,
        armoredPublicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----",
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Patterns::class)
        every { Patterns.EMAIL_ADDRESS } returns mockEmailPattern
        every { mockEmailPattern.matcher(any()) } returns mockMatcher
        every { mockMatcher.matches() } returns true
        viewModel = KeyGenerationViewModel(generateKeyPairUseCase)
    }

    @After
    fun teardown() {
        unmockkStatic(Patterns::class)
        Dispatchers.resetMain()
    }

    private fun setValidState() {
        viewModel.onEvent(KeyGenerationUiEvent.OnNameChange("Alice"))
        viewModel.onEvent(KeyGenerationUiEvent.OnEmailChange("alice@example.com"))
        viewModel.onEvent(KeyGenerationUiEvent.OnPassphraseChange("correct-horse"))
        viewModel.onEvent(KeyGenerationUiEvent.OnConfirmPassphraseChange("correct-horse"))
    }

    // --- State mutations ---

    @Test
    fun onNameChange_updatesStateAndClearsNameError() {
        viewModel.onEvent(KeyGenerationUiEvent.OnNameChange("   "))
        viewModel.onEvent(KeyGenerationUiEvent.OnGenerateClick)
        assertNotNull(viewModel.state.value.nameError)

        viewModel.onEvent(KeyGenerationUiEvent.OnNameChange("Alice"))

        assertEquals("Alice", viewModel.state.value.ownerName)
        assertNull(viewModel.state.value.nameError)
    }

    @Test
    fun onEmailChange_updatesStateAndClearsEmailError() {
        every { mockMatcher.matches() } returns false
        viewModel.onEvent(KeyGenerationUiEvent.OnNameChange("Alice"))
        viewModel.onEvent(KeyGenerationUiEvent.OnGenerateClick)
        assertNotNull(viewModel.state.value.emailError)

        every { mockMatcher.matches() } returns true
        viewModel.onEvent(KeyGenerationUiEvent.OnEmailChange("alice@example.com"))

        assertEquals("alice@example.com", viewModel.state.value.ownerEmail)
        assertNull(viewModel.state.value.emailError)
    }

    @Test
    fun onAlgorithmChange_updatesSelectedAlgorithm() {
        viewModel.onEvent(KeyGenerationUiEvent.OnAlgorithmChange(KeyAlgorithm.ED25519))
        assertEquals(KeyAlgorithm.ED25519, viewModel.state.value.selectedAlgorithm)
    }

    @Test
    fun onPassphraseChange_clearsBothPassphraseErrors() {
        viewModel.onEvent(KeyGenerationUiEvent.OnNameChange("Alice"))
        viewModel.onEvent(KeyGenerationUiEvent.OnPassphraseChange("short"))
        viewModel.onEvent(KeyGenerationUiEvent.OnConfirmPassphraseChange("mismatch"))
        viewModel.onEvent(KeyGenerationUiEvent.OnGenerateClick)
        assertNotNull(viewModel.state.value.passphraseError)
        assertNotNull(viewModel.state.value.confirmPassphraseError)

        viewModel.onEvent(KeyGenerationUiEvent.OnPassphraseChange("newvalue1"))

        assertNull(viewModel.state.value.passphraseError)
        assertNull(viewModel.state.value.confirmPassphraseError)
    }

    @Test
    fun onConfirmPassphraseChange_clearsConfirmError() {
        viewModel.onEvent(KeyGenerationUiEvent.OnNameChange("Alice"))
        viewModel.onEvent(KeyGenerationUiEvent.OnPassphraseChange("correct-horse"))
        viewModel.onEvent(KeyGenerationUiEvent.OnConfirmPassphraseChange("mismatch"))
        viewModel.onEvent(KeyGenerationUiEvent.OnGenerateClick)
        assertNotNull(viewModel.state.value.confirmPassphraseError)

        viewModel.onEvent(KeyGenerationUiEvent.OnConfirmPassphraseChange("correct-horse"))

        assertNull(viewModel.state.value.confirmPassphraseError)
    }

    @Test
    fun onBack_emitsNavigateBackEffect() = runTest {
        viewModel.effect.test {
            viewModel.onEvent(KeyGenerationUiEvent.OnBack)
            assertEquals(KeyGenerationUiEffect.NavigateBack, awaitItem())
        }
    }

    // --- Validation ---

    @Test
    fun validate_blankName_setsNameError() {
        viewModel.onEvent(KeyGenerationUiEvent.OnNameChange("   "))
        viewModel.onEvent(KeyGenerationUiEvent.OnGenerateClick)

        assertNotNull(viewModel.state.value.nameError)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun validate_invalidEmail_setsEmailError() {
        every { mockMatcher.matches() } returns false
        viewModel.onEvent(KeyGenerationUiEvent.OnNameChange("Alice"))
        viewModel.onEvent(KeyGenerationUiEvent.OnGenerateClick)

        assertNotNull(viewModel.state.value.emailError)
    }

    @Test
    fun validate_passphraseTooShort_setsPassphraseError() {
        viewModel.onEvent(KeyGenerationUiEvent.OnNameChange("Alice"))
        viewModel.onEvent(KeyGenerationUiEvent.OnPassphraseChange("1234567"))
        viewModel.onEvent(KeyGenerationUiEvent.OnConfirmPassphraseChange("1234567"))
        viewModel.onEvent(KeyGenerationUiEvent.OnGenerateClick)

        assertNotNull(viewModel.state.value.passphraseError)
    }

    @Test
    fun validate_mismatchedPassphrases_setsConfirmError() {
        viewModel.onEvent(KeyGenerationUiEvent.OnNameChange("Alice"))
        viewModel.onEvent(KeyGenerationUiEvent.OnPassphraseChange("correct-horse"))
        viewModel.onEvent(KeyGenerationUiEvent.OnConfirmPassphraseChange("wrong-horse"))
        viewModel.onEvent(KeyGenerationUiEvent.OnGenerateClick)

        assertNotNull(viewModel.state.value.confirmPassphraseError)
    }

    @Test
    fun validate_multipleErrors_setsAllErrors() {
        every { mockMatcher.matches() } returns false
        viewModel.onEvent(KeyGenerationUiEvent.OnNameChange("   "))
        viewModel.onEvent(KeyGenerationUiEvent.OnPassphraseChange("short"))
        viewModel.onEvent(KeyGenerationUiEvent.OnGenerateClick)

        assertNotNull(viewModel.state.value.nameError)
        assertNotNull(viewModel.state.value.emailError)
        assertNotNull(viewModel.state.value.passphraseError)
    }

    @Test
    fun validate_invalidInput_doesNotCallUseCase() {
        viewModel.onEvent(KeyGenerationUiEvent.OnNameChange("   "))
        viewModel.onEvent(KeyGenerationUiEvent.OnGenerateClick)

        coVerify(exactly = 0) { generateKeyPairUseCase(any()) }
    }

    // --- Generation flow ---

    @Test
    fun generate_success_emitsNavigateToKeysEffect() = runTest {
        coEvery { generateKeyPairUseCase(any()) } returns Result.success(fakePgpKey)
        setValidState()

        viewModel.effect.test {
            viewModel.onEvent(KeyGenerationUiEvent.OnGenerateClick)
            advanceUntilIdle()
            assertEquals(KeyGenerationUiEffect.NavigateToKeys, awaitItem())
        }
    }

    @Test
    fun generate_success_setsIsLoadingFalseAfterCompletion() = runTest {
        coEvery { generateKeyPairUseCase(any()) } returns Result.success(fakePgpKey)
        setValidState()

        viewModel.onEvent(KeyGenerationUiEvent.OnGenerateClick)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun generate_failure_setsGeneralError() = runTest {
        coEvery { generateKeyPairUseCase(any()) } returns Result.failure(RuntimeException("generation failed"))
        setValidState()

        viewModel.onEvent(KeyGenerationUiEvent.OnGenerateClick)
        advanceUntilIdle()

        assertEquals("generation failed", viewModel.state.value.generalError)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun generate_failureWithNullMessage_setsDefaultGeneralError() = runTest {
        coEvery { generateKeyPairUseCase(any()) } returns Result.failure(RuntimeException())
        setValidState()

        viewModel.onEvent(KeyGenerationUiEvent.OnGenerateClick)
        advanceUntilIdle()

        assertEquals("Key generation failed", viewModel.state.value.generalError)
    }

    @Test
    fun generate_alwaysWipesPassphraseArray() = runTest {
        val paramsSlot = slot<GenerateKeyPairUseCase.Params>()
        coEvery { generateKeyPairUseCase(capture(paramsSlot)) } returns Result.success(fakePgpKey)
        setValidState()

        viewModel.onEvent(KeyGenerationUiEvent.OnGenerateClick)
        advanceUntilIdle()

        assertTrue(paramsSlot.captured.passphrase.all { it == ' ' })
    }

    @Test
    fun generate_trimmedOwnerNameAndEmail_passedToUseCase() = runTest {
        val paramsSlot = slot<GenerateKeyPairUseCase.Params>()
        coEvery { generateKeyPairUseCase(capture(paramsSlot)) } returns Result.success(fakePgpKey)

        viewModel.onEvent(KeyGenerationUiEvent.OnNameChange("  Alice  "))
        viewModel.onEvent(KeyGenerationUiEvent.OnEmailChange("  alice@example.com  "))
        viewModel.onEvent(KeyGenerationUiEvent.OnPassphraseChange("correct-horse"))
        viewModel.onEvent(KeyGenerationUiEvent.OnConfirmPassphraseChange("correct-horse"))
        viewModel.onEvent(KeyGenerationUiEvent.OnGenerateClick)
        advanceUntilIdle()

        assertEquals("Alice", paramsSlot.captured.ownerName)
        assertEquals("alice@example.com", paramsSlot.captured.ownerEmail)
    }
}
