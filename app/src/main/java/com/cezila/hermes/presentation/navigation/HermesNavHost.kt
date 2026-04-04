package com.cezila.hermes.presentation.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cezila.hermes.presentation.decrypt.DecryptScreen
import com.cezila.hermes.presentation.encrypt.EncryptScreen
import com.cezila.hermes.presentation.keydetail.KeyDetailScreen
import com.cezila.hermes.presentation.keydetail.KeyDetailUiEffect
import com.cezila.hermes.presentation.keydetail.KeyDetailViewModel
import com.cezila.hermes.presentation.keygeneration.KeyGenerationScreen
import com.cezila.hermes.presentation.keygeneration.KeyGenerationUiEffect
import com.cezila.hermes.presentation.keygeneration.KeyGenerationViewModel
import com.cezila.hermes.presentation.keyimport.KeyImportScreen
import com.cezila.hermes.presentation.keyimport.KeyImportUiEffect
import com.cezila.hermes.presentation.keyimport.KeyImportViewModel
import com.cezila.hermes.presentation.keys.KeysScreen
import com.cezila.hermes.presentation.keys.KeysUiEffect
import com.cezila.hermes.presentation.keys.KeysViewModel
import com.cezila.hermes.presentation.onboarding.LearnEncryptionScreen
import com.cezila.hermes.presentation.onboarding.OnboardingScreen
import com.cezila.hermes.presentation.onboarding.OnboardingUiEffect
import com.cezila.hermes.presentation.onboarding.OnboardingViewModel

@Composable
fun HermesNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Route.Onboarding,
        modifier = modifier,
    ) {
        composable<Route.Onboarding> {
            val viewModel: OnboardingViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(viewModel) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        OnboardingUiEffect.NavigateToLearnEncryption ->
                            navController.navigate(Route.LearnEncryption)

                        OnboardingUiEffect.NavigateToKeyGeneration ->
                            navController.navigate(Route.KeyGeneration)

                        OnboardingUiEffect.NavigateToKeys ->
                            navController.navigate(Route.Keys) {
                                popUpTo(Route.Onboarding) { inclusive = true }
                            }
                    }
                }
            }

            OnboardingScreen(
                state = state,
                onEvent = viewModel::onEvent,
            )
        }

        composable<Route.LearnEncryption> {
            LearnEncryptionScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.KeyGeneration> {
            val viewModel: KeyGenerationViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(viewModel) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        KeyGenerationUiEffect.NavigateToKeys ->
                            navController.navigate(Route.Keys) {
                                popUpTo(Route.Onboarding) { inclusive = true }
                            }

                        KeyGenerationUiEffect.NavigateBack ->
                            navController.popBackStack()
                    }
                }
            }

            KeyGenerationScreen(
                state = state,
                onEvent = viewModel::onEvent,
            )
        }

        composable<Route.Keys> {
            val keysViewModel: KeysViewModel = hiltViewModel()
            val keysState by keysViewModel.state.collectAsState()

            val context = LocalContext.current

            LaunchedEffect(keysViewModel) {
                keysViewModel.effect.collect { effect ->
                    when (effect) {
                        KeysUiEffect.NavigateToKeyGeneration ->
                            navController.navigate(Route.KeyGeneration)

                        KeysUiEffect.NavigateToKeyImport ->
                            navController.navigate(Route.KeyImport)

                        is KeysUiEffect.SharePublicKey -> {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, effect.armoredText)
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }

                        is KeysUiEffect.NavigateToKeyDetail ->
                            navController.navigate(Route.KeyDetail(effect.keyId))
                    }
                }
            }

            KeysScreen(
                keysState = keysState,
                onKeysEvent = keysViewModel::onEvent,
            )
        }

        composable<Route.KeyImport> {
            val viewModel: KeyImportViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(viewModel) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        KeyImportUiEffect.NavigateBack -> navController.popBackStack()
                        KeyImportUiEffect.ImportSuccess -> navController.popBackStack()
                    }
                }
            }

            KeyImportScreen(
                state = state,
                onEvent = viewModel::onEvent,
            )
        }

        composable<Route.KeyDetail> {
            val viewModel: KeyDetailViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val context = LocalContext.current

            LaunchedEffect(viewModel) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        KeyDetailUiEffect.NavigateBack -> navController.popBackStack()
                        is KeyDetailUiEffect.SharePublicKey -> {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, effect.armoredText)
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                    }
                }
            }

            KeyDetailScreen(
                state = state,
                onEvent = viewModel::onEvent,
            )
        }

        composable<Route.Encrypt> { EncryptScreen() }
        composable<Route.Decrypt> { DecryptScreen() }
    }
}
