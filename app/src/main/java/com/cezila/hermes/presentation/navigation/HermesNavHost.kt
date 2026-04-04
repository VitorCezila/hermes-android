package com.cezila.hermes.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cezila.hermes.presentation.decrypt.DecryptScreen
import com.cezila.hermes.presentation.encrypt.EncryptScreen
import com.cezila.hermes.presentation.keys.KeysScreen
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

        composable<Route.Keys> { KeysScreen() }
        composable<Route.Encrypt> { EncryptScreen() }
        composable<Route.Decrypt> { DecryptScreen() }
    }
}
