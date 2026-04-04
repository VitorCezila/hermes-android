package com.cezila.hermes.presentation.keys

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.ui.theme.CryptoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeysScreen(
    keysState: KeysUiState,
    onKeysEvent: (KeysUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "VAULT",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = CryptoFontFamily,
                    )
                },
                actions = {
                    if (keysState.hasKeys) {
                        if (keysState.ownKey != null) {
                            IconButton(onClick = { onKeysEvent(KeysUiEvent.OnExportOwnKeyClick) }) {
                                Icon(
                                    imageVector = Icons.Outlined.IosShare,
                                    contentDescription = "Export my public key",
                                )
                            }
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "More options",
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Export All Public Keys") },
                                    onClick = {
                                        showMenu = false
                                        onKeysEvent(KeysUiEvent.OnExportAllKeysClick)
                                    },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            if (!keysState.isLoading && keysState.hasKeys) {
                FloatingActionButton(
                    onClick = { onKeysEvent(KeysUiEvent.OnFabClick) },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add key",
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when {
            keysState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            !keysState.hasKeys -> {
                EmptyKeysState(
                    onGenerateClick = { onKeysEvent(KeysUiEvent.OnGenerateKeyClick) },
                    onImportClick = { onKeysEvent(KeysUiEvent.OnImportKeyClick) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        SectionHeader(text = "MY KEY")
                    }

                    if (keysState.ownKey != null) {
                        item(key = keysState.ownKey.id) {
                            OwnKeyCard(
                                key = keysState.ownKey,
                                onClick = { onKeysEvent(KeysUiEvent.OnKeyCardClick(keysState.ownKey.id)) },
                            )
                        }
                    } else {
                        item {
                            GenerateKeyPromptCard(
                                onClick = { onKeysEvent(KeysUiEvent.OnGenerateKeyClick) },
                            )
                        }
                    }

                    item {
                        SectionHeader(
                            text = "CONTACTS",
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    if (keysState.contacts.isNotEmpty()) {
                        item {
                            OutlinedTextField(
                                value = keysState.searchQuery,
                                onValueChange = { onKeysEvent(KeysUiEvent.OnSearchQueryChange(it)) },
                                placeholder = {
                                    Text(
                                        text = "Search contacts",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    if (keysState.filteredContacts.isNotEmpty()) {
                        items(keysState.filteredContacts, key = { it.id }) { key ->
                            ContactKeyCard(
                                key = key,
                                onClick = { onKeysEvent(KeysUiEvent.OnKeyCardClick(key.id)) },
                            )
                        }
                    } else {
                        item {
                            EmptyContactsCard(
                                onClick = { onKeysEvent(KeysUiEvent.OnImportKeyClick) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (keysState.showFabSheet) {
        ModalBottomSheet(
            onDismissRequest = { onKeysEvent(KeysUiEvent.OnSheetDismiss) },
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = "Add key",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                val generateDisabled = keysState.ownKey != null
                val disabledAlpha = 0.38f
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Generate My Key",
                            color = MaterialTheme.colorScheme.onSurface.let {
                                if (generateDisabled) it.copy(alpha = disabledAlpha) else it
                            },
                        )
                    },
                    supportingContent = {
                        Text(
                            text = if (generateDisabled) "You already have an identity key"
                                   else "Create a new RSA-4096 or Ed25519 key pair",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.let {
                                if (generateDisabled) it.copy(alpha = disabledAlpha) else it
                            },
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.let {
                                if (generateDisabled) it.copy(alpha = disabledAlpha) else it
                            },
                        )
                    },
                    modifier = if (!generateDisabled) {
                        Modifier.clickable { onKeysEvent(KeysUiEvent.OnGenerateKeyClick) }
                    } else {
                        Modifier
                    },
                )
                ListItem(
                    headlineContent = { Text("Import Contact Key") },
                    supportingContent = { Text("Add a contact's public key (.asc or paste)") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.FileUpload,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable { onKeysEvent(KeysUiEvent.OnImportKeyClick) },
                )
            }
        }
    }
}

@Composable
private fun EmptyKeysState(
    onGenerateClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
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
            androidx.compose.material3.Button(
                onClick = onGenerateClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Generate My Key")
            }
            OutlinedButton(
                onClick = onImportClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Import Contact Key")
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

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontFamily = CryptoFontFamily,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun OwnKeyCard(key: PgpKey, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = formatFingerprint(key.fingerprint),
                fontFamily = CryptoFontFamily,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            )
            Text(
                text = key.ownerName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = key.ownerEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                )
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = key.algorithm.displayName,
                            fontFamily = CryptoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ContactKeyCard(key: PgpKey, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = formatFingerprint(key.fingerprint),
                fontFamily = CryptoFontFamily,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = key.ownerName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = key.ownerEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = key.algorithm.displayName,
                            fontFamily = CryptoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        }
    }
}

@Composable
private fun GenerateKeyPromptCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "GENERATE YOUR KEY PAIR",
                fontFamily = CryptoFontFamily,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyContactsCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "IMPORT A CONTACT KEY",
                fontFamily = CryptoFontFamily,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatFingerprint(fingerprint: String): String {
    val clean = fingerprint.takeLast(16).uppercase()
    return clean.chunked(4).joinToString(" ")
}
