package com.cezila.hermes.presentation.decrypt

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cezila.hermes.core.domain.model.DecryptionResult
import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.model.SignatureStatus
import com.cezila.hermes.core.ui.theme.Clay
import com.cezila.hermes.core.ui.theme.CryptoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecryptScreen(
    state: DecryptUiState,
    onEvent: (DecryptUiEvent) -> Unit,
    onPickFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.showPassphraseDialog) {
        PassphraseBottomSheet(
            onConfirm = { passphrase -> onEvent(DecryptUiEvent.OnPassphraseConfirmed(passphrase)) },
            onDismiss = { onEvent(DecryptUiEvent.OnPassphraseDismissed) },
        )
    }

    if (state.decryptionResult != null) {
        val resolvedSigner = remember(state.decryptionResult, state.allKeys) {
            resolveSignerKey(state.decryptionResult.signatureStatus, state.allKeys)
        }
        DecryptionResultBottomSheet(
            result = state.decryptionResult,
            resolvedSigner = resolvedSigner,
            onCopyPlaintext = { onEvent(DecryptUiEvent.OnCopyPlaintext) },
            onSaveFile = { onEvent(DecryptUiEvent.OnSaveDecryptedFile) },
            onDismiss = { onEvent(DecryptUiEvent.OnDismissResult) },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HERMES",
                        fontFamily = CryptoFontFamily,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Profile",
                        modifier = Modifier.padding(end = 12.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
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
                .verticalScroll(rememberScrollState()),
        ) {
            // Hero section
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    text = "Decode your\nmessage.",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 44.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Decrypt and cryptographically verify the\norigin of a PGP-secured payload.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Mode toggle
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "INPUT MODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    InputMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.inputMode == mode,
                            onClick = { onEvent(DecryptUiEvent.OnInputModeChanged(mode)) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = InputMode.entries.size,
                            ),
                            enabled = !state.isLoading,
                        ) {
                            Text(mode.name)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Input panel
            when (state.inputMode) {
                InputMode.TEXT -> TextInputPanel(state = state, onEvent = onEvent)
                InputMode.FILE -> FileInputPanel(state = state, onEvent = onEvent, onPickFile = onPickFile)
            }

            Spacer(Modifier.height(20.dp))

            // Session details
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "TRANSMISSION DETAILS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = CryptoFontFamily,
                            letterSpacing = 1.sp,
                        )
                        Spacer(Modifier.height(2.dp))
                        SessionDetailRow(label = "Protocol", value = "OpenPGP v4")
                        SessionDetailRow(label = "Runtime", value = "Client-Side Only")
                    }
                }
            }

            if (state.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            // Decrypt & Verify CTA
            Button(
                onClick = { onEvent(DecryptUiEvent.OnDecryptClick) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                enabled = state.canDecrypt && !state.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Clay,
                    contentColor = Color.White,
                    disabledContainerColor = Clay.copy(alpha = 0.38f),
                    disabledContentColor = Color.White.copy(alpha = 0.6f),
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = "Decrypt & Verify",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            // Footer
            Spacer(Modifier.height(8.dp))
            Text(
                text = "SECURE INSTANCE — NO CLOUD TETHER",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = CryptoFontFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                letterSpacing = 0.5.sp,
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TextInputPanel(
    state: DecryptUiState,
    onEvent: (DecryptUiEvent) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "PGP MESSAGE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
            )
            Row {
                TextButton(
                    onClick = { onEvent(DecryptUiEvent.OnPasteFromClipboard) },
                    enabled = !state.isLoading,
                ) {
                    Text(
                        text = "PASTE",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = CryptoFontFamily,
                    )
                }
                TextButton(
                    onClick = { onEvent(DecryptUiEvent.OnClearInput) },
                    enabled = state.ciphertextInput.isNotBlank() && !state.isLoading,
                ) {
                    Text(
                        text = "CLEAR",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = CryptoFontFamily,
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        OutlinedTextField(
            value = state.ciphertextInput,
            onValueChange = { onEvent(DecryptUiEvent.OnCiphertextChanged(it)) },
            placeholder = {
                Text(
                    "Paste PGP message here…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            maxLines = Int.MAX_VALUE,
            enabled = !state.isLoading,
        )

        Spacer(Modifier.height(6.dp))

        // Header status badge
        val (statusText, statusColor) = when (state.headerStatus) {
            HeaderStatus.Awaiting -> "AWAITING VALID HEADER…" to MaterialTheme.colorScheme.onSurfaceVariant
            HeaderStatus.Valid -> "VALID PGP MESSAGE" to Clay
            HeaderStatus.Invalid -> "INVALID FORMAT" to MaterialTheme.colorScheme.error
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
            fontFamily = CryptoFontFamily,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun FileInputPanel(
    state: DecryptUiState,
    onEvent: (DecryptUiEvent) -> Unit,
    onPickFile: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "ENCRYPTED FILE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(8.dp))

        if (state.selectedFileName != null) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = Clay,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = state.selectedFileName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(
                        onClick = { onEvent(DecryptUiEvent.OnClearFile) },
                        enabled = !state.isLoading,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove file",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            OutlinedCard(
                onClick = onPickFile,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Encrypted File",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Tap to pick a .pgp or .gpg file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = CryptoFontFamily,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PassphraseBottomSheet(
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Enter Passphrase",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Required to unlock your identity key for decryption.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text("Passphrase") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = {
                    passphrase = ""
                    onDismiss()
                }) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val chars = passphrase.toCharArray()
                        passphrase = ""
                        onConfirm(chars)
                    },
                    enabled = passphrase.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Clay),
                ) {
                    Text("Confirm", color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DecryptionResultBottomSheet(
    result: DecryptionResult,
    resolvedSigner: PgpKey?,
    onCopyPlaintext: () -> Unit,
    onSaveFile: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Decrypted Message",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Signature status
            SignatureStatusBadge(status = result.signatureStatus, resolvedSigner = resolvedSigner)

            // Plaintext display (text mode only)
            if (result.plaintext.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = result.plaintext,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // File display (file mode)
            if (result.plaintextBytes != null) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = Clay,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = result.fileName ?: "decrypted_file",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${result.plaintextBytes!!.size} bytes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (result.plaintext.isNotBlank()) {
                    Button(
                        onClick = onCopyPlaintext,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Clay),
                    ) {
                        Text("Copy", color = Color.White)
                    }
                }
                if (result.plaintextBytes != null) {
                    Button(
                        onClick = onSaveFile,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Clay),
                    ) {
                        Text("Save File", color = Color.White)
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun SignatureStatusBadge(
    status: SignatureStatus,
    resolvedSigner: PgpKey?,
) {
    val (text, color) = when (status) {
        is SignatureStatus.Valid -> {
            val signerDisplay = resolvedSigner?.let { "${it.ownerName} <${it.ownerEmail}>" }
                ?: status.signerKeyId
            "VERIFIED — $signerDisplay" to Clay
        }
        is SignatureStatus.Invalid ->
            "SIGNATURE INVALID" to MaterialTheme.colorScheme.error
        is SignatureStatus.UnknownSigner ->
            "UNKNOWN SIGNER — ${status.signerKeyId}" to MaterialTheme.colorScheme.onSurfaceVariant
        SignatureStatus.None ->
            "UNSIGNED" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = CryptoFontFamily,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            letterSpacing = 0.5.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun resolveSignerKey(
    status: SignatureStatus,
    allKeys: List<PgpKey>,
): PgpKey? = when (status) {
    is SignatureStatus.Valid -> allKeys.firstOrNull { key ->
        key.fingerprint.endsWith(status.signerFingerprint.takeLast(16), ignoreCase = true) ||
            key.fingerprint.equals(status.signerFingerprint, ignoreCase = true)
    }
    else -> null
}
