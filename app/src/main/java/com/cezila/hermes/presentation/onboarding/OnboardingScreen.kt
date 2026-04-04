package com.cezila.hermes.presentation.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onEvent: (OnboardingUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hermes",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "The quiet space for\nyour digital truth.",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Your privacy stays on your device. Start by importing or creating a key.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onEvent(OnboardingUiEvent.OnImportOrCreateClick) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Import or Create Key")
                }
                OutlinedButton(
                    onClick = { onEvent(OnboardingUiEvent.OnLearnMoreClick) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Learn about Local Encryption")
                }
            }

            Text(
                text = "SECURE INSTANCE  \u2022  NO CLOUD TETHER",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (state.showImportOrCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { onEvent(OnboardingUiEvent.OnSheetDismiss) },
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = "How would you like to start?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                ListItem(
                    headlineContent = { Text("Generate New Key") },
                    supportingContent = { Text("Create a new RSA-4096 or Ed25519 key pair") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable { onEvent(OnboardingUiEvent.OnGenerateKeyClick) },
                )
                ListItem(
                    headlineContent = { Text("Import Existing Key") },
                    supportingContent = { Text("Load a .asc or .gpg file from your device") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.FileUpload,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable { onEvent(OnboardingUiEvent.OnImportKeyClick) },
                )
            }
        }
    }
}
