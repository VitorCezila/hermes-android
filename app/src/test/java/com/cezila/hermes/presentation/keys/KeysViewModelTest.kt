package com.cezila.hermes.presentation.keys

import app.cash.turbine.test
import com.cezila.hermes.core.domain.model.KeyAlgorithm
import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.usecase.GetAllKeysUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KeysViewModelTest {

    private val getAllKeysUseCase: GetAllKeysUseCase = mockk()
    private val testDispatcher = StandardTestDispatcher()

    private val fakeKey1 = PgpKey(
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

    private val fakeKey2 = fakeKey1.copy(id = "0xCCDD", fingerprint = "CCDD")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_emptyList_setsIsLoadingFalseAndEmptyKeys() = runTest {
        every { getAllKeysUseCase() } returns flowOf(emptyList())
        val viewModel = KeysViewModel(getAllKeysUseCase)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.keys.isEmpty())
    }

    @Test
    fun init_nonEmptyList_updatesStateWithKeys() = runTest {
        every { getAllKeysUseCase() } returns flowOf(listOf(fakeKey1, fakeKey2))
        val viewModel = KeysViewModel(getAllKeysUseCase)
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.keys.size)
        assertEquals(fakeKey1, viewModel.state.value.keys[0])
    }

    @Test
    fun init_multipleEmissions_updatesStateOnEach() = runTest {
        every { getAllKeysUseCase() } returns flow {
            emit(listOf(fakeKey1))
            delay(100)
            emit(listOf(fakeKey1, fakeKey2))
        }
        val viewModel = KeysViewModel(getAllKeysUseCase)

        advanceUntilIdle()
        assertEquals(2, viewModel.state.value.keys.size)
    }

    @Test
    fun init_beforeFirstEmission_isLoadingIsTrue() = runTest {
        every { getAllKeysUseCase() } returns flow { delay(Long.MAX_VALUE) }
        val viewModel = KeysViewModel(getAllKeysUseCase)

        assertTrue(viewModel.state.value.isLoading)
    }

    @Test
    fun init_whenFlowThrows_isLoadingSetFalse() = runTest {
        every { getAllKeysUseCase() } returns flow { throw RuntimeException("DB gone") }
        val viewModel = KeysViewModel(getAllKeysUseCase)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun onAddKeyClick_emitsNavigateToKeyGenerationEffect() = runTest {
        every { getAllKeysUseCase() } returns flowOf(emptyList())
        val viewModel = KeysViewModel(getAllKeysUseCase)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onEvent(KeysUiEvent.OnAddKeyClick)
            assertEquals(KeysUiEffect.NavigateToKeyGeneration, awaitItem())
        }
    }
}
