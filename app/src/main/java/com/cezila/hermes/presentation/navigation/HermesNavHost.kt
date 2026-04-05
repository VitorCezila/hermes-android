package com.cezila.hermes.presentation.navigation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cezila.hermes.presentation.decrypt.DecryptScreen
import com.cezila.hermes.presentation.decrypt.DecryptUiEffect
import com.cezila.hermes.presentation.decrypt.DecryptUiEvent
import com.cezila.hermes.presentation.decrypt.DecryptViewModel
import com.cezila.hermes.presentation.encrypt.EncryptScreen
import com.cezila.hermes.presentation.encrypt.EncryptUiEffect
import com.cezila.hermes.presentation.encrypt.EncryptUiEvent
import com.cezila.hermes.presentation.encrypt.EncryptViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.cezila.hermes.presentation.keydetail.KeyDetailScreen
import com.cezila.hermes.presentation.keydetail.KeyDetailUiEffect
import com.cezila.hermes.presentation.keydetail.KeyDetailViewModel
import com.cezila.hermes.presentation.keygeneration.KeyGenerationScreen
import com.cezila.hermes.presentation.keygeneration.KeyGenerationUiEffect
import com.cezila.hermes.presentation.keygeneration.KeyGenerationViewModel
import com.cezila.hermes.presentation.keyimport.KeyImportScreen
import com.cezila.hermes.presentation.keyimport.KeyImportUiEffect
import com.cezila.hermes.presentation.keyimport.KeyImportViewModel
import com.cezila.hermes.presentation.keys.KeysScreen
import com.cezila.hermes.presentation.keys.KeysUiEffect
import com.cezila.hermes.presentation.keys.KeysViewModel
import com.cezila.hermes.presentation.onboarding.LearnEncryptionScreen
import com.cezila.hermes.presentation.onboarding.OnboardingScreen
import com.cezila.hermes.presentation.onboarding.OnboardingUiEffect
import com.cezila.hermes.presentation.onboarding.OnboardingViewModel
import com.cezila.hermes.presentation.settings.SettingsScreen
import com.cezila.hermes.presentation.settings.SettingsUiEffect
import com.cezila.hermes.presentation.settings.SettingsUiEvent
import com.cezila.hermes.presentation.settings.SettingsViewModel

@Composable
fun HermesNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Route.Onboarding,
        modifier = modifier,
    ) {
        composable<Route.Onboarding> {
            val viewModel: OnboardingViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(viewModel) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        OnboardingUiEffect.NavigateToLearnEncryption ->
                            navController.navigate(Route.LearnEncryption)

                        OnboardingUiEffect.NavigateToKeyGeneration ->
                            navController.navigate(Route.KeyGeneration)

                        OnboardingUiEffect.NavigateToKeys ->
                            navController.navigate(Route.Keys) {
                                popUpTo(Route.Onboarding) { inclusive = true }
                            }
                    }
                }
            }

            OnboardingScreen(
                state = state,
                onEvent = viewModel::onEvent,
            )
        }

        composable<Route.LearnEncryption> {
            LearnEncryptionScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.KeyGeneration> {
            val viewModel: KeyGenerationViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(viewModel) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        KeyGenerationUiEffect.NavigateToKeys ->
                            navController.navigate(Route.Keys) {
                                popUpTo(Route.Onboarding) { inclusive = true }
                            }

                        KeyGenerationUiEffect.NavigateBack ->
                            navController.popBackStack()
                    }
                }
            }

            KeyGenerationScreen(
                state = state,
                onEvent = viewModel::onEvent,
            )
        }

        composable<Route.Keys> {
            val keysViewModel: KeysViewModel = hiltViewModel()
            val keysState by keysViewModel.state.collectAsState()

            val context = LocalContext.current

            LaunchedEffect(keysViewModel) {
                keysViewModel.effect.collect { effect ->
                    when (effect) {
                        KeysUiEffect.NavigateToKeyGeneration ->
                            navController.navigate(Route.KeyGeneration)

                        KeysUiEffect.NavigateToKeyImport ->
                            navController.navigate(Route.KeyImport)

                        is KeysUiEffect.SharePublicKey -> {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, effect.armoredText)
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }

                        is KeysUiEffect.NavigateToKeyDetail ->
                            navController.navigate(Route.KeyDetail(effect.keyId))

                        KeysUiEffect.NavigateToSettings ->
                            navController.navigate(Route.Settings)
                    }
                }
            }

            KeysScreen(
                keysState = keysState,
                onKeysEvent = keysViewModel::onEvent,
            )
        }

        composable<Route.KeyImport> {
            val viewModel: KeyImportViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(viewModel) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        KeyImportUiEffect.NavigateBack -> navController.popBackStack()
                        KeyImportUiEffect.ImportSuccess -> navController.popBackStack()
                    }
                }
            }

            KeyImportScreen(
                state = state,
                onEvent = viewModel::onEvent,
            )
        }

        composable<Route.KeyDetail> {
            val viewModel: KeyDetailViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val context = LocalContext.current

            LaunchedEffect(viewModel) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        KeyDetailUiEffect.NavigateBack -> navController.popBackStack()
                        is KeyDetailUiEffect.SharePublicKey -> {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, effect.armoredText)
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                    }
                }
            }

            KeyDetailScreen(
                state = state,
                onEvent = viewModel::onEvent,
            )
        }

        composable<Route.Settings> {
            val viewModel: SettingsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val context = LocalContext.current

            LaunchedEffect(viewModel) {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                viewModel.onEvent(SettingsUiEvent.OnVersionLoaded(packageInfo.versionName ?: ""))
            }

            LaunchedEffect(viewModel) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        SettingsUiEffect.NavigateBack ->
                            navController.popBackStack()

                        is SettingsUiEffect.ShowToast ->
                            Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            SettingsScreen(
                state = state,
                onEvent = viewModel::onEvent,
            )
        }

        composable<Route.Encrypt> {
            val viewModel: EncryptViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
            ) { uri ->
                uri ?: return@rememberLauncherForActivityResult
                scope.launch {
                    val (bytes, name) = withContext(Dispatchers.IO) {
                        val resolver = context.contentResolver
                        var fileName = uri.lastPathSegment ?: "file"
                        resolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToFirst() && nameIndex >= 0) {
                                fileName = cursor.getString(nameIndex)
                            }
                        }
                        val fileBytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
                        Pair(fileBytes, fileName)
                    }
                    viewModel.onEvent(EncryptUiEvent.OnFileSelected(bytes, name))
                }
            }

            val pendingEncryptedFile = remember { mutableStateOf<ByteArray?>(null) }

            val saveFileLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
            ) { uri ->
                uri ?: return@rememberLauncherForActivityResult
                val bytes = pendingEncryptedFile.value ?: return@rememberLauncherForActivityResult
                scope.launch(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "File saved", Toast.LENGTH_SHORT).show()
                    }
                }
                pendingEncryptedFile.value = null
            }

            LaunchedEffect(viewModel) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        EncryptUiEffect.PickFile ->
                            filePickerLauncher.launch(arrayOf("*/*"))

                        is EncryptUiEffect.SaveEncryptedFile -> {
                            pendingEncryptedFile.value = effect.bytes
                            saveFileLauncher.launch(effect.suggestedName)
                        }

                        is EncryptUiEffect.CopyToClipboard -> {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("PGP Message", effect.text))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }

                        is EncryptUiEffect.ShowToast ->
                            Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            EncryptScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onPickFile = { filePickerLauncher.launch(arrayOf("*/*")) },
            )
        }
        composable<Route.Decrypt> {
            val viewModel: DecryptViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
            ) { uri ->
                uri ?: return@rememberLauncherForActivityResult
                scope.launch {
                    val (bytes, name) = withContext(Dispatchers.IO) {
                        val resolver = context.contentResolver
                        var fileName = uri.lastPathSegment ?: "file"
                        resolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToFirst() && nameIndex >= 0) {
                                fileName = cursor.getString(nameIndex)
                            }
                        }
                        val fileBytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
                        Pair(fileBytes, fileName)
                    }
                    viewModel.onEvent(DecryptUiEvent.OnFileSelected(bytes, name))
                }
            }

            val pendingDecryptedFile = remember { mutableStateOf<ByteArray?>(null) }

            val saveFileLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
            ) { uri ->
                uri ?: return@rememberLauncherForActivityResult
                val bytes = pendingDecryptedFile.value ?: return@rememberLauncherForActivityResult
                scope.launch(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "File saved", Toast.LENGTH_SHORT).show()
                    }
                }
                pendingDecryptedFile.value = null
            }

            LaunchedEffect(viewModel) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        DecryptUiEffect.PickFile ->
                            filePickerLauncher.launch(arrayOf("*/*"))

                        is DecryptUiEffect.SaveDecryptedFile -> {
                            pendingDecryptedFile.value = effect.bytes
                            saveFileLauncher.launch(effect.suggestedName)
                        }

                        is DecryptUiEffect.CopyToClipboard -> {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Decrypted Message", effect.text))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }

                        is DecryptUiEffect.ShowToast ->
                            Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()

                        DecryptUiEffect.RequestClipboardRead -> {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            viewModel.onEvent(DecryptUiEvent.OnCiphertextChanged(text))
                        }
                    }
                }
            }

            DecryptScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onPickFile = { filePickerLauncher.launch(arrayOf("*/*")) },
            )
        }
    }
}
