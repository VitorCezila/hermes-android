package com.cezila.hermes.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Onboarding : Route
    @Serializable data object LearnEncryption : Route
    @Serializable data object Keys : Route
    @Serializable data object Encrypt : Route
    @Serializable data object Decrypt : Route
    @Serializable data object KeyGeneration : Route
    @Serializable data object KeyImport : Route
    @Serializable data class KeyDetail(val keyId: String) : Route
    @Serializable data object Settings : Route
}
