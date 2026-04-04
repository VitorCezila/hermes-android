package com.cezila.hermes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.cezila.hermes.presentation.navigation.BottomNavItem
import com.cezila.hermes.presentation.navigation.HermesNavHost
import com.cezila.hermes.presentation.navigation.Route
import com.cezila.hermes.core.ui.theme.HermesAndroidTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HermesAndroidTheme {
                HermesMainScreen()
            }
        }
    }
}

@Composable
private fun HermesMainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = listOf(Route.Keys::class, Route.Encrypt::class, Route.Decrypt::class)
        .any { currentDestination?.hasRoute(it) == true }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomNavItem.entries.forEach { item ->
                        NavigationBarItem(
                            selected = currentDestination?.hasRoute(item.route::class) == true,
                            onClick = {
                                navController.navigate(
                                    route = item.route,
                                    navOptions = navOptions {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    },
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.contentDescription,
                                )
                            },
                            label = { Text(text = item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        HermesNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
