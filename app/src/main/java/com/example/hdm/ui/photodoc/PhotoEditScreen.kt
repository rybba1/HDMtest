package com.example.hdm.ui.photodoc

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.hdm.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditScreen(
    navController: NavController,
    viewModel: PhotoDocViewModel,
    imageUri: String
) {
    val imageToEdit = viewModel.getImageByUri(imageUri)
    val context = LocalContext.current

    if (imageToEdit == null) {
        // Jeśli obraz z jakiegoś powodu nie istnieje, wróć
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }

    var scannedBarcode by remember { mutableStateOf(imageToEdit.scannedBarcode) }
    var note by remember { mutableStateOf(imageToEdit.note) }
    var isAirFreight by remember { mutableStateOf(imageToEdit.isAirFreight) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges by remember(scannedBarcode, note, isAirFreight) {
        derivedStateOf {
            scannedBarcode != imageToEdit.scannedBarcode ||
                    note != imageToEdit.note ||
                    isAirFreight != imageToEdit.isAirFreight
        }
    }

    val BARCODE_SCAN_RESULT_KEY = "barcode_edit_scan_result"
    val scannerResult = navController.currentBackStackEntry?.savedStateHandle?.get<String>(BARCODE_SCAN_RESULT_KEY)
    LaunchedEffect(scannerResult) {
        scannerResult?.let {
            scannedBarcode = it
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>(BARCODE_SCAN_RESULT_KEY)
        }
    }

    val saveChangesAndExit: () -> Unit = {
        viewModel.updateImageDetails(imageUri, scannedBarcode, note, isAirFreight, context)
        navController.popBackStack()
    }

    BackHandler(enabled = hasUnsavedChanges) {
        showExitDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edycja Załącznika") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) showExitDialog = true else navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    if (hasUnsavedChanges) showSaveDialog = true else navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
                    .height(56.dp),
                shape = RoundedCornerShape(CleanTheme.cornerRadius)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Zapisz zmiany", fontWeight = FontWeight.Medium)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                painter = rememberAsyncImagePainter(model = android.net.Uri.parse(imageUri)),
                contentDescription = "Edytowane zdjęcie",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(CleanTheme.cornerRadius)),
                contentScale = ContentScale.Fit
            )

            CleanTextField(
                value = note,
                onValueChange = { note = it },
                label = "Notatka / Etykieta do zdjęcia",
                validationState = if (note.isNotBlank()) ValidationState.VALID else ValidationState.EMPTY,
                minLines = 3,
                singleLine = false
            )

            CleanTextField(
                value = scannedBarcode,
                onValueChange = { scannedBarcode = it },
                label = "Zeskanowany kod nośnika",
                validationState = if (scannedBarcode.isNotBlank()) ValidationState.VALID else ValidationState.EMPTY,
                trailingIcon = {
                    IconButton(onClick = {
                        navController.navigate(Screen.Scanner.withArgs("VALIDATE_PALLET", BARCODE_SCAN_RESULT_KEY, null))
                    }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Skanuj kod")
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isAirFreight = !isAirFreight }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isAirFreight,
                    onCheckedChange = { isAirFreight = it }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Oznaczyć jako fracht lotniczy (AIR)")
            }
        }
    }

    if (showSaveDialog) {
        SaveDialog(
            onDismiss = { showSaveDialog = false },
            onConfirm = { showSaveDialog = false; saveChangesAndExit() }
        )
    }

    if (showExitDialog) {
        ExitDialog(
            onDismiss = { showExitDialog = false },
            onConfirm = { showExitDialog = false; navController.popBackStack() }
        )
    }
}

@Composable
private fun SaveDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zapisać zmiany?") },
        text = { Text("Czy na pewno chcesz zapisać wprowadzone zmiany w szczegółach zdjęcia?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun ExitDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Niezapisane zmiany") },
        text = { Text("Masz niezapisane zmiany. Czy na pewno chcesz wyjść bez zapisywania?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Wyjdź bez zapisywania")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}