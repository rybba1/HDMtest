package com.example.hdm.ui.nagoya

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.hdm.model.PalletEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NokPhotoScreen(
    viewModel: JapanReportProcessingViewModel,
    onClose: () -> Unit
) {
    val pallet by viewModel.palletForNokEditing.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (pallet == null) {
        onClose()
        return
    }
    val palletSafe = pallet!!

    val context = LocalContext.current
    var tempImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            tempImageUri?.let {
                Log.d("NokPhotoScreen", "Zdjęcie zostało zrobione pomyślnie: $it")
                viewModel.addPhotoToPallet(palletSafe, it)
            }
        } else {
            Log.w("NokPhotoScreen", "Robienie zdjęcia zostało anulowane lub nie powiodło się")
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("NokPhotoScreen", "Wybrano zdjęcie z galerii: $it")
            viewModel.addPhotoToPallet(palletSafe, it)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("NokPhotoScreen", "Uprawnienia do aparatu zostały przyznane")
            launchCamera(context, scope, snackbarHostState) { uri ->
                tempImageUri = uri
                cameraLauncher.launch(uri)
            }
        } else {
            Log.w("NokPhotoScreen", "Uprawnienia do aparatu zostały odrzucone")
            scope.launch { snackbarHostState.showSnackbar("Brak uprawnień do aparatu.") }
        }
    }

    val photos = listOfNotNull(palletSafe.nokPhoto1, palletSafe.nokPhoto2, palletSafe.nokPhoto3, palletSafe.nokPhoto4)
    val canAddMorePhotos = photos.size < 4

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Zdjęcia dla ${palletSafe.carrierNumber}") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            Log.d("NokPhotoScreen", "Kliknięto przycisk aparatu")
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        enabled = canAddMorePhotos,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Zrób zdjęcie")
                    }
                    Button(
                        onClick = {
                            Log.d("NokPhotoScreen", "Kliknięto przycisk galerii")
                            galleryLauncher.launch("image/*")
                        },
                        enabled = canAddMorePhotos,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Galeria")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Dodaj zdjęcie Etykiety, potem zdjęcie uszkodzenia i całej palety.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (!canAddMorePhotos) {
                    Text(
                        "Osiągnięto maksymalną liczbę zdjęć (4)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (photos.isEmpty()) {
                    Text("Brak dodanych zdjęć. Dodaj maksymalnie 4.")
                } else {
                    Text(
                        "Dodanych zdjęć: ${photos.size}/4",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(photos) { photoUriString ->
                            PhotoThumbnail(
                                uriString = photoUriString,
                                onDelete = {
                                    Log.d("NokPhotoScreen", "Usuwanie zdjęcia: $photoUriString")
                                    viewModel.removePhotoFromPallet(palletSafe, photoUriString)
                                }
                            )
                        }
                    }
                }
            }

            // ✅ NOWY PRZYCISK "ZAPISZ I WYJDŹ"
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Done, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Zapisz i wyjdź do raportu")
            }
        }
    }
}

private fun launchCamera(
    context: Context,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onUriCreated: (Uri) -> Unit
) {
    try {
        Log.d("NokPhotoScreen", "Próba utworzenia URI dla aparatu")
        val uri = context.createTempUri()
        Log.d("NokPhotoScreen", "URI utworzone pomyślnie: $uri")
        onUriCreated(uri)
    } catch (e: RuntimeException) {
        Log.e("NokPhotoScreen", "Błąd konfiguracji aparatu: ${e.message}", e)
        scope.launch {
            snackbarHostState.showSnackbar("Błąd konfiguracji aparatu: ${e.message}")
        }
    } catch (e: Exception) {
        Log.e("NokPhotoScreen", "Błąd tworzenia URI dla aparatu: ${e.message}", e)
        scope.launch {
            snackbarHostState.showSnackbar("Nie można uruchomić aparatu: ${e.message}")
        }
    }
}

@Composable
private fun PhotoThumbnail(uriString: String, onDelete: () -> Unit) {
    Box(contentAlignment = Alignment.TopEnd) {
        Image(
            painter = rememberAsyncImagePainter(model = Uri.parse(uriString)),
            contentDescription = "Zdjęcie palety",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .aspectRatio(1f)
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .clip(MaterialTheme.shapes.small)
        )
        Box(
            modifier = Modifier
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(onClick = onDelete)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Usuń zdjęcie",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun Context.createTempUri(): Uri {
    return try {
        val primaryDir = getExternalFilesDir(null)
        val fallbackDir = cacheDir
        val targetDir = when {
            primaryDir != null && primaryDir.exists() -> {
                Log.d("FileProvider", "Używam external files dir: ${primaryDir.absolutePath}")
                primaryDir
            }
            fallbackDir.exists() -> {
                Log.d("FileProvider", "Używam cache dir: ${fallbackDir.absolutePath}")
                fallbackDir
            }
            else -> {
                Log.d("FileProvider", "Używam files dir: ${filesDir.absolutePath}")
                filesDir
            }
        }
        val photoDir = File(targetDir, "photos").apply {
            if (!exists()) {
                mkdirs()
                Log.d("FileProvider", "Utworzono katalog: $absolutePath")
            }
        }
        val file = File(
            photoDir,
            "hdm_photo_${System.currentTimeMillis()}.jpg"
        ).apply {
            if (!exists()) {
                createNewFile()
            }
        }
        Log.d("FileProvider", "Plik: ${file.absolutePath}")
        Log.d("FileProvider", "Plik istnieje: ${file.exists()}")
        val authority = "${packageName}.provider"
        Log.d("FileProvider", "Authority: $authority")
        val uri = FileProvider.getUriForFile(
            this,
            authority,
            file
        )
        Log.d("FileProvider", "URI: $uri")
        uri
    } catch (e: IllegalArgumentException) {
        Log.e("FileProvider", "IllegalArgumentException - sprawdź file_paths.xml: ${e.message}", e)
        throw RuntimeException("Błąd konfiguracji FileProvider. Sprawdź file_paths.xml", e)
    } catch (e: Exception) {
        Log.e("FileProvider", "Ogólny błąd tworzenia URI: ${e.message}", e)
        throw e
    }
}