package com.cezila.hermes.presentation.keygeneration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cezila.hermes.core.domain.model.KeyAlgorithm
import com.cezila.hermes.core.ui.theme.CryptoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyGenerationScreen(
    state: KeyGenerationUiState,
    onEvent: (KeyGenerationUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var passphraseVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPassphraseVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Generate Key") },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(KeyGenerationUiEvent.OnBack) },
                        enabled = !state.isLoading,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = state.ownerName,
                onValueChange = { onEvent(KeyGenerationUiEvent.OnNameChange(it)) },
                label = { Text("Name") },
                singleLine = true,
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            )

            OutlinedTextField(
                value = state.ownerEmail,
                onValueChange = { onEvent(KeyGenerationUiEvent.OnEmailChange(it)) },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = state.emailError != null,
                supportingText = state.emailError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            )

            Text(
                text = "Algorithm",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                KeyAlgorithm.entries.forEachIndexed { index, algorithm ->
                    SegmentedButton(
                        selected = state.selectedAlgorithm == algorithm,
                        onClick = { onEvent(KeyGenerationUiEvent.OnAlgorithmChange(algorithm)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = KeyAlgorithm.entries.size),
                        enabled = !state.isLoading,
                    ) {
                        Text(algorithm.displayName)
                    }
                }
            }

            OutlinedTextField(
                value = state.passphrase,
                onValueChange = { onEvent(KeyGenerationUiEvent.OnPassphraseChange(it)) },
                label = { Text("Passphrase") },
                singleLine = true,
                visualTransformation = if (passphraseVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passphraseVisible = !passphraseVisible }) {
                        Icon(
                            imageVector = if (passphraseVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passphraseVisible) "Hide passphrase" else "Show passphrase",
                        )
                    }
                },
                isError = state.passphraseError != null,
                supportingText = state.passphraseError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            )

            OutlinedTextField(
                value = state.confirmPassphrase,
                onValueChange = { onEvent(KeyGenerationUiEvent.OnConfirmPassphraseChange(it)) },
                label = { Text("Confirm Passphrase") },
                singleLine = true,
                visualTransformation = if (confirmPassphraseVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { confirmPassphraseVisible = !confirmPassphraseVisible }) {
                        Icon(
                            imageVector = if (confirmPassphraseVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (confirmPassphraseVisible) "Hide passphrase" else "Show passphrase",
                        )
                    }
                },
                isError = state.confirmPassphraseError != null,
                supportingText = state.confirmPassphraseError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            )

            Text(
                text = "Your private key is stored encrypted on this device only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.isLoading) {
                Text(
                    text = "Generating your key pair. This may take a few seconds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = { onEvent(KeyGenerationUiEvent.OnGenerateClick) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            ) {
                Text("Generate Key")
            }

            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (state.generalError != null) {
                Text(
                    text = state.generalError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "KEY MATERIAL STAYS ON DEVICE",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = CryptoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
