package com.cezila.hermes.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnEncryptionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Local Encryption") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            InfoSection(
                title = "What is PGP?",
                body = "Pretty Good Privacy (PGP) is a standard for encrypting and signing data. " +
                    "It uses a pair of mathematically linked keys: a public key you share freely, " +
                    "and a private key you keep secret. Anyone with your public key can send you " +
                    "encrypted messages that only your private key can unlock.",
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            InfoSection(
                title = "Your keys, your device",
                body = "Hermes never transmits your keys to any server. Your private key material " +
                    "is generated and stored entirely on this device. There is no account, " +
                    "no cloud backup, and no recovery mechanism — by design. " +
                    "If you lose your device without an exported backup, the keys are gone.",
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            InfoSection(
                title = "How Hermes protects you",
                body = "All cryptographic operations — key generation, encryption, decryption, " +
                    "and signature verification — happen locally using the Bouncy Castle library. " +
                    "Private key bytes are handled in memory only and never written to logs, " +
                    "network calls, or shared storage. The Android Keystore provides additional " +
                    "hardware-backed protection where available.",
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
