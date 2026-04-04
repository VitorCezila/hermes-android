package com.cezila.hermes.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cezila.hermes.presentation.decrypt.DecryptScreen
import com.cezila.hermes.presentation.encrypt.EncryptScreen
import com.cezila.hermes.presentation.keys.KeysScreen

@Composable
fun HermesNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Route.Keys,
        modifier = modifier,
    ) {
        composable<Route.Keys> { KeysScreen() }
        composable<Route.Encrypt> { EncryptScreen() }
        composable<Route.Decrypt> { DecryptScreen() }
    }
}
