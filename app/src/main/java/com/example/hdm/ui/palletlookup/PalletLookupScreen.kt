package com.example.hdm.ui.palletlookup

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.hdm.Screen
import com.example.hdm.model.DisplayableImage
import com.example.hdm.model.PalletInfoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Theme zgodny z LabelPrintingScreen
private object PalletLookupTheme {
    val ValidColor = Color(0xFF2E7D32)
    val InvalidColor = Color(0xFFD32F2F)
    val WarningColor = Color(0xFFFF9800)
    val InfoColor = Color(0xFF1976D2)

    val ValidBackground = Color(0xFFF1F8F2)
    val InvalidBackground = Color(0xFFFFF3F3)
    val WarningBackground = Color(0xFFFFF3E0)
    val InfoBackground = Color(0xFFE3F2FD)

    val DarkText = Color(0xFF212121)
    val MediumText = Color(0xFF424242)
    val LightText = Color(0xFF757575)

    val cornerRadius = 12.dp
}

private suspend fun base64StringToBitmap(base64String: String): Bitmap? = withContext(Dispatchers.Default) {
    return@withContext try {
        val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalletLookupScreen(
    navController: NavController,
    viewModel: PalletLookupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val BARCODE_SCAN_RESULT_KEY = "pallet_lookup_scan_result"

    val scannerResult by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>(BARCODE_SCAN_RESULT_KEY, null)
        ?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }

    LaunchedEffect(scannerResult) {
        scannerResult?.let { value ->
            viewModel.onBarcodeScanned(value)
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>(BARCODE_SCAN_RESULT_KEY)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Sprawdzanie Uszkodzonych Palet",
                        fontWeight = FontWeight.Bold,
                        color = PalletLookupTheme.DarkText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Instrukcja Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = PalletLookupTheme.InfoBackground
                ),
                shape = RoundedCornerShape(PalletLookupTheme.cornerRadius),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PalletLookupTheme.InfoColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Zeskanuj kod palety",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PalletLookupTheme.DarkText
                        )
                        Text(
                            text = "System automatycznie waliduje format numeru",
                            style = MaterialTheme.typography.bodySmall,
                            color = PalletLookupTheme.MediumText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Przycisk skanowania
            Button(
                onClick = {
                    navController.navigate(
                        Screen.Scanner.withArgs("VALIDATE_PALLET", BARCODE_SCAN_RESULT_KEY)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PalletLookupTheme.InfoColor
                ),
                shape = RoundedCornerShape(PalletLookupTheme.cornerRadius)
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Zeskanuj Kod Palety",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Zawartość z animacją
            AnimatedContent(
                targetState = uiState.status,
                label = "ContentAnimator",
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                }
            ) { status ->
                when (status) {
                    LookupStatus.IDLE -> {
                        IdleStateCard()
                    }
                    LookupStatus.LOADING -> {
                        LoadingStateCard()
                    }
                    LookupStatus.SUCCESS -> {
                        uiState.palletInfo?.let {
                            PalletInfoDisplay(
                                pallet = it,
                                labelImages = uiState.labelImages,
                                damageImages = uiState.damageImages,
                                overviewImages = uiState.overviewImages
                            )
                        }
                    }
                    LookupStatus.ERROR -> {
                        ErrorStateCard(message = uiState.errorMessage)
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(PalletLookupTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = PalletLookupTheme.LightText
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Oczekiwanie na skanowanie",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = PalletLookupTheme.DarkText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Zeskanuj kod kreskowy palety, aby wyświetlić szczegóły",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = PalletLookupTheme.MediumText
            )
        }
    }
}

@Composable
private fun LoadingStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = PalletLookupTheme.InfoBackground
        ),
        shape = RoundedCornerShape(PalletLookupTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = PalletLookupTheme.InfoColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Pobieranie danych palety...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = PalletLookupTheme.DarkText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Proszę czekać",
                style = MaterialTheme.typography.bodyMedium,
                color = PalletLookupTheme.MediumText
            )
        }
    }
}

@Composable
private fun ErrorStateCard(message: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = PalletLookupTheme.InvalidBackground
        ),
        shape = RoundedCornerShape(PalletLookupTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = PalletLookupTheme.InvalidColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Błąd podczas wyszukiwania",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PalletLookupTheme.InvalidColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message ?: "Nieznany błąd",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = PalletLookupTheme.DarkText
            )
        }
    }
}

@Composable
private fun PalletInfoDisplay(
    pallet: PalletInfoResponse,
    labelImages: List<DisplayableImage>,
    damageImages: List<DisplayableImage>,
    overviewImages: List<DisplayableImage>
) {
    var selectedImageBase64 by remember { mutableStateOf<String?>(null) }

    if (selectedImageBase64 != null) {
        FullScreenImageViewer(
            base64Data = selectedImageBase64!!,
            onDismiss = { selectedImageBase64 = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CleanInfoCard(title = "Dane Główne", icon = Icons.Default.Inventory2) {
            InfoRow("Numer palety:", pallet.palletNumber)
            InfoRow("Numer lotu:", pallet.lotNumber)
            InfoRow("Symbol towaru:", pallet.itemSymbol)
            InfoRow("Status:", pallet.status)
            InfoRow("Magazyn log.:", pallet.logicalWarehouse)
            InfoRow("Miejsce:", pallet.place)
        }

        CleanInfoCard(title = "Powiązany Raport", icon = Icons.Default.Description) {
            InfoRow("Typ raportu:", pallet.reportType)
            InfoRow("Numer ref.:", pallet.reportRefNo)
            InfoRow("Pracownik WH:", pallet.reportPicWh)
            InfoRow("Data zdarzenia:", pallet.reportDatetime)
            InfoRow("Data dodania:", pallet.timestampAdded)
        }

        CleanInfoCard(title = "Transport", icon = Icons.Default.LocalShipping) {
            InfoRow("Nr kontenera:", pallet.containerNumber)
            InfoRow("Kraj pochodzenia:", pallet.countryOfOrigin)
        }

        CleanInfoCard(title = "Informacje o Uszkodzeniu", icon = Icons.Default.ReportProblem) {
            InfoRow("Klucz uszkodzenia:", pallet.damageTypeKey)
            InfoRow("Szczegóły:", pallet.details)
        }

        CleanInfoCard(title = "Statusy", icon = Icons.Default.Checklist) {
            PrimaryStatusDisplay(repackDone = pallet.repackDone, accepted = pallet.accepted)
        }

        PhotoSection(title = "Zdjęcia Etykiety", images = labelImages, onImageClick = { selectedImageBase64 = it })
        PhotoSection(title = "Zdjęcia Uszkodzeń", images = damageImages, onImageClick = { selectedImageBase64 = it })
        PhotoSection(title = "Zdjęcia Całej Palety", images = overviewImages, onImageClick = { selectedImageBase64 = it })
    }
}

@Composable
private fun CleanInfoCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(PalletLookupTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = PalletLookupTheme.InfoColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PalletLookupTheme.DarkText
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Column(
                modifier = Modifier.padding(start = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                color = PalletLookupTheme.MediumText,
                modifier = Modifier.weight(0.4f)
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = PalletLookupTheme.DarkText,
                modifier = Modifier.weight(0.6f)
            )
        }
    }
}

@Composable
private fun PrimaryStatusDisplay(repackDone: Boolean?, accepted: Boolean?) {
    val status: Triple<String, Color, ImageVector>? = when {
        repackDone == true -> Triple(
            "Paleta przepakowana",
            PalletLookupTheme.InvalidColor,
            Icons.Default.BuildCircle
        )
        accepted == true -> Triple(
            "Zaakceptowane",
            PalletLookupTheme.ValidColor,
            Icons.Default.CheckCircle
        )
        else -> null
    }

    if (status != null) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when(status.second) {
                    PalletLookupTheme.ValidColor -> PalletLookupTheme.ValidBackground
                    else -> PalletLookupTheme.InvalidBackground
                }
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(
                    imageVector = status.third,
                    contentDescription = status.first,
                    tint = status.second,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = status.first,
                    color = status.second,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    } else {
        Text(
            "Brak zdefiniowanego statusu",
            style = MaterialTheme.typography.bodyMedium,
            color = PalletLookupTheme.LightText
        )
    }
}

@Composable
private fun PhotoSection(
    title: String,
    images: List<DisplayableImage>,
    onImageClick: (String) -> Unit
) {
    if (images.isNotEmpty()) {
        CleanInfoCard(title = title, icon = Icons.Default.PhotoLibrary) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(images) { image ->
                    PhotoThumbnail(
                        thumbnail = image.thumbnail,
                        onClick = { onImageClick(image.fullSizeBase64) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(thumbnail: Bitmap?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF5F5F5))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail.asImageBitmap(),
                contentDescription = "Zdjęcie palety",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun FullScreenImageViewer(base64Data: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var size by remember { mutableStateOf(IntSize.Zero) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .onSizeChanged { size = it }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = if (scale > 1f) {
                                offset = Offset.Zero
                                1f
                            } else {
                                3f
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        val newOffset = offset + pan

                        val maxX = (size.width * (newScale - 1) / 2f).coerceAtLeast(0f)
                        val maxY = (size.height * (newScale - 1) / 2f).coerceAtLeast(0f)

                        offset = Offset(
                            x = newOffset.x.coerceIn(-maxX, maxX),
                            y = newOffset.y.coerceIn(-maxY, maxY)
                        )
                        scale = newScale
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            var bitmap by remember { mutableStateOf<Bitmap?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(base64Data) {
                isLoading = true
                bitmap = base64StringToBitmap(base64Data)
                isLoading = false
            }

            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Podgląd zdjęcia",
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = "Nie można załadować obrazu",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Zamknij", tint = Color.White)
            }
        }
    }
}