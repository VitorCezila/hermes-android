package com.cezila.hermes.presentation.keygeneration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cezila.hermes.R
import com.cezila.hermes.core.domain.model.KeyAlgorithm
import com.cezila.hermes.core.ui.theme.CryptoFontFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KeyGenerationScreen(
    state: KeyGenerationUiState,
    onEvent: (KeyGenerationUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var passphraseVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPassphraseVisible by rememberSaveable { mutableStateOf(false) }

    val expiryOptions = listOf(
        0 to stringResource(R.string.keygen_expiry_never),
        365 to stringResource(R.string.keygen_expiry_1yr),
        730 to stringResource(R.string.keygen_expiry_2yr),
        1095 to stringResource(R.string.keygen_expiry_3yr),
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.keygen_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(KeyGenerationUiEvent.OnBack) },
                        enabled = !state.isLoading,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
                label = { Text(stringResource(R.string.keygen_name_label)) },
                singleLine = true,
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            )

            OutlinedTextField(
                value = state.ownerEmail,
                onValueChange = { onEvent(KeyGenerationUiEvent.OnEmailChange(it)) },
                label = { Text(stringResource(R.string.keygen_email_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = state.emailError != null,
                supportingText = state.emailError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            )

            Text(
                text = stringResource(R.string.keygen_algorithm_label),
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

            Text(
                text = stringResource(R.string.keygen_expiry_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                expiryOptions.forEach { (days, label) ->
                    FilterChip(
                        selected = state.expiryDays == days,
                        onClick = { onEvent(KeyGenerationUiEvent.OnExpiryChange(days)) },
                        label = { Text(label) },
                        enabled = !state.isLoading,
                    )
                }
            }

            OutlinedTextField(
                value = state.passphrase,
                onValueChange = { onEvent(KeyGenerationUiEvent.OnPassphraseChange(it)) },
                label = { Text(stringResource(R.string.passphrase)) },
                singleLine = true,
                visualTransformation = if (passphraseVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passphraseVisible = !passphraseVisible }) {
                        Icon(
                            imageVector = if (passphraseVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passphraseVisible) stringResource(R.string.hide_passphrase)
                                                 else stringResource(R.string.show_passphrase),
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
                label = { Text(stringResource(R.string.confirm_passphrase)) },
                singleLine = true,
                visualTransformation = if (confirmPassphraseVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { confirmPassphraseVisible = !confirmPassphraseVisible }) {
                        Icon(
                            imageVector = if (confirmPassphraseVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (confirmPassphraseVisible) stringResource(R.string.hide_passphrase)
                                                 else stringResource(R.string.show_passphrase),
                        )
                    }
                },
                isError = state.confirmPassphraseError != null,
                supportingText = state.confirmPassphraseError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            )

            Text(
                text = stringResource(R.string.keygen_private_key_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.isLoading) {
                Text(
                    text = stringResource(R.string.keygen_generating),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = { onEvent(KeyGenerationUiEvent.OnGenerateClick) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            ) {
                Text(stringResource(R.string.keygen_button))
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
                    text = stringResource(R.string.keygen_on_device_badge),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = CryptoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
