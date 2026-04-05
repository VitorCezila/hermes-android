package com.cezila.hermes.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavItem(
    val route: Route,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
) {
    Keys(
        route = Route.Keys,
        label = "Keys",
        icon = Icons.Outlined.Key,
        contentDescription = "Keys tab",
    ),
    Encrypt(
        route = Route.Encrypt,
        label = "Encrypt",
        icon = Icons.Outlined.Lock,
        contentDescription = "Encrypt tab",
    ),
    Decrypt(
        route = Route.Decrypt,
        label = "Decrypt",
        icon = Icons.Outlined.LockOpen,
        contentDescription = "Decrypt tab",
    ),
}
