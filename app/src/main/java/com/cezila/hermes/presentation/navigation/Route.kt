package com.cezila.hermes.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Keys : Route
    @Serializable data object Encrypt : Route
    @Serializable data object Decrypt : Route
}
