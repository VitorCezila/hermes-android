package com.cezila.hermes.presentation.keyimport

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyImportScreen(
    state: KeyImportUiState,
    onEvent: (KeyImportUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val (content, fileName, size) = withContext(Dispatchers.IO) {
                val resolver = context.contentResolver
                var name = uri.lastPathSegment ?: "file"
                var fileSize = -1L

                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex >= 0) name = cursor.getString(nameIndex)
                        if (sizeIndex >= 0) fileSize = cursor.getLong(sizeIndex)
                    }
                }

                val text = resolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
                Triple(text, name, fileSize)
            }

            if (size > MAX_FILE_SIZE_BYTES) {
                snackbarHostState.showSnackbar("File exceeds 2 MB limit")
            } else {
                onEvent(KeyImportUiEvent.OnFileRead(content, fileName))
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Import Contact Key") },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(KeyImportUiEvent.OnBack) },
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
                value = state.armoredText,
                onValueChange = { onEvent(KeyImportUiEvent.OnArmoredTextChange(it)) },
                label = { Text("PGP Public Key Block") },
                placeholder = { Text("-----BEGIN PGP PUBLIC KEY BLOCK-----") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                isError = state.validationStatus is ValidationStatus.Invalid,
                supportingText = {
                    when (val status = state.validationStatus) {
                        is ValidationStatus.Invalid -> Text(
                            text = when (status.reason) {
                                ValidationError.InvalidFormat -> "Invalid PGP public key format"
                                ValidationError.SecretKeyDetected -> "This contains a private key. Only public keys can be imported."
                                ValidationError.DuplicateKey -> "This key is already in your keychain."
                            },
                        )
                        ValidationStatus.Valid -> Text(
                            text = "Valid public key detected",
                            color = MaterialTheme.colorScheme.primary,
                        )
                        else -> {}
                    }
                },
                trailingIcon = {
                    when (state.validationStatus) {
                        ValidationStatus.Validating -> Box(
                            modifier = Modifier.padding(12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                        ValidationStatus.Valid -> Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        else -> {}
                    }
                },
                enabled = !state.isLoading,
                maxLines = Int.MAX_VALUE,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(text = "or", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            OutlinedButton(
                onClick = { fileLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            ) {
                Icon(
                    imageVector = Icons.Filled.AttachFile,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text("Pick from file (.asc / .gpg)")
            }

            if (state.fileName != null) {
                Text(
                    text = state.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = { onEvent(KeyImportUiEvent.OnImportClick) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.validationStatus == ValidationStatus.Valid && !state.isLoading,
            ) {
                Text("Import Contact")
            }

            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
