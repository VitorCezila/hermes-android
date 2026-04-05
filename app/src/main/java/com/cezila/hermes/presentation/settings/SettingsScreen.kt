package com.cezila.hermes.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cezila.hermes.R
import com.cezila.hermes.core.ui.theme.CryptoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = CryptoFontFamily,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(SettingsUiEvent.OnBackClick) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            SectionHeader(stringResource(R.string.settings_danger_zone))

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { onEvent(SettingsUiEvent.OnClearAllKeysClick) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isClearingKeys,
            ) {
                Text(
                    text = if (state.isClearingKeys) stringResource(R.string.settings_clearing)
                           else stringResource(R.string.settings_clear_all),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(28.dp))

            SectionHeader(stringResource(R.string.settings_about))

            Spacer(modifier = Modifier.height(12.dp))

            if (state.appVersion.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.settings_version, state.appVersion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = stringResource(R.string.settings_local_first_badge),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = CryptoFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (state.showClearAllConfirmation) {
        AlertDialog(
            onDismissRequest = { onEvent(SettingsUiEvent.OnClearAllKeysDismiss) },
            title = { Text(stringResource(R.string.settings_clear_all)) },
            text = {
                Text(stringResource(R.string.settings_clear_dialog_message))
            },
            confirmButton = {
                TextButton(onClick = { onEvent(SettingsUiEvent.OnClearAllKeysConfirm) }) {
                    Text(stringResource(R.string.settings_clear_dialog_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(SettingsUiEvent.OnClearAllKeysDismiss) }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontFamily = CryptoFontFamily,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = androidx.compose.ui.unit.TextUnit(
            value = 1.5f,
            type = androidx.compose.ui.unit.TextUnitType.Sp,
        ),
    )
}
