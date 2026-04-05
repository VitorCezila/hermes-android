package com.cezila.hermes.presentation.onboarding

import app.cash.turbine.test
import com.cezila.hermes.core.domain.usecase.GetAllKeysUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val getAllKeysUseCase: GetAllKeysUseCase = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getAllKeysUseCase() } returns flowOf(emptyList())
        viewModel = OnboardingViewModel(getAllKeysUseCase)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_showImportOrCreateSheetIsFalse() {
        assertFalse(viewModel.state.value.showImportOrCreateSheet)
    }

    @Test
    fun onImportOrCreateClick_opensSheet() {
        viewModel.onEvent(OnboardingUiEvent.OnImportOrCreateClick)
        assertTrue(viewModel.state.value.showImportOrCreateSheet)
    }

    @Test
    fun onSheetDismiss_closesSheet() {
        viewModel.onEvent(OnboardingUiEvent.OnImportOrCreateClick)
        viewModel.onEvent(OnboardingUiEvent.OnSheetDismiss)
        assertFalse(viewModel.state.value.showImportOrCreateSheet)
    }

    @Test
    fun onLearnMoreClick_emitsNavigateToLearnEncryptionEffect() = runTest {
        viewModel.effect.test {
            viewModel.onEvent(OnboardingUiEvent.OnLearnMoreClick)
            assert(awaitItem() == OnboardingUiEffect.NavigateToLearnEncryption)
        }
    }

    @Test
    fun onGenerateKeyClick_closesSheetAndEmitsNavigateToKeyGenerationEffect() = runTest {
        viewModel.onEvent(OnboardingUiEvent.OnImportOrCreateClick)

        viewModel.effect.test {
            viewModel.onEvent(OnboardingUiEvent.OnGenerateKeyClick)
            assertFalse(viewModel.state.value.showImportOrCreateSheet)
            assert(awaitItem() == OnboardingUiEffect.NavigateToKeyGeneration)
        }
    }

    @Test
    fun onImportKeyClick_closesSheet() {
        viewModel.onEvent(OnboardingUiEvent.OnImportOrCreateClick)
        viewModel.onEvent(OnboardingUiEvent.OnImportKeyClick)
        assertFalse(viewModel.state.value.showImportOrCreateSheet)
    }

    @Test
    fun onImportKeyClick_doesNotEmitAnyEffect() = runTest {
        viewModel.onEvent(OnboardingUiEvent.OnImportOrCreateClick)

        viewModel.effect.test {
            viewModel.onEvent(OnboardingUiEvent.OnImportKeyClick)
            expectNoEvents()
        }
    }
}
