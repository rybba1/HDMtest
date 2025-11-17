package com.example.hdm.ui.report_type

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hdm.Screen
import com.example.hdm.ui.header.HeaderViewModel
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hdm.model.Base64Attachment // <-- NOWY IMPORT
import com.example.hdm.model.SessionDetailsResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.ExperimentalFoundationApi // <-- NOWY IMPORT

data class ReportTypeOption(
    val title: String,
    val description: String,
    val icon: ImageVector
)

object ReportTypeTheme {
    val ValidColor = Color(0xFF2E7D32)
    val DarkText = Color(0xFF212121)
    val MediumText = Color(0xFF424242)
    val LightText = Color(0xFF757575)
    val ValidBackground = Color(0xFFF1F8F2)
    val NeutralBackground = Color(0xFFFAFAFA)
    val ValidBorder = Color(0xFFE8F5E8)
    val NeutralBorder = Color(0xFFE0E0E0)
    val cornerRadius = 12.dp
    val borderWidth = 1.dp
    val animationDuration = 250
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class) // <-- DODANO ExperimentalFoundationApi
@Composable
fun ReportTypeScreen(
    navController: NavController,
    reportViewModel: HeaderViewModel
) {
    val reportOptions = listOf(
        ReportTypeOption("Rozładunek transferu", "Dokumentacja uszkodzeń w towarze przyjmowanym z Transferu z NGK", Icons.Default.SwapHoriz),
        ReportTypeOption("Rozładunek dostawy", "Dokumentacja uszkodzeń w towarze od zewnętrznego dostawcy.", Icons.Default.LocalShipping),
        ReportTypeOption("Inspekcja Meiko", "Wewnętrzna kontrola jakości towaru na magazynie.", Icons.Default.FactCheck),
        ReportTypeOption("Przygotowanie wysyłki", "Dokumentacja uszkodzeń wykrytych przed wysyłką do klienta.", Icons.Default.Upload)
    )

    val context = LocalContext.current

    val activeSessions by reportViewModel.pendingSessions.collectAsStateWithLifecycle()
    val isLoadingSessions by reportViewModel.isSessionLoading.collectAsStateWithLifecycle()

    var sessionToDelete by remember { mutableStateOf<SessionDetailsResponse?>(null) }

    /** Przechowuje ID sesji wybranej do inspekcji (przez długie przytrzymanie) */
    var sessionToInspectId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        reportViewModel.loadPendingSessions()
    }

    var reportTypeToCreate by remember { mutableStateOf<ReportTypeOption?>(null) }

    if (reportTypeToCreate != null) {
        AlertDialog(
            onDismissRequest = { reportTypeToCreate = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Ostrzeżenie") },
            title = { Text("Niedokończony raport") },
            text = { Text("Istnieje niedokończony raport w aplikacji. Czy chcesz go porzucić i utworzyć nowy?") },
            confirmButton = {
                Button(
                    onClick = {
                        val option = reportTypeToCreate!!
                        reportViewModel.updateReportType(option.title)
                        navController.navigate(Screen.Header.route)
                        reportTypeToCreate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Tak, porzuć i utwórz nowy")
                }
            },
            dismissButton = {
                TextButton(onClick = { reportTypeToCreate = null }) {
                    Text("Anuluj")
                }
            }
        )
    }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Potwierdź usunięcie sesji") },
            text = { Text("Tej operacji nie można cofnąć. Czy na pewno chcesz trwale usunąć sesję '${sessionToDelete!!.headerData?.reportType ?: "B/D"}' z serwera?") },
            confirmButton = {
                Button(
                    onClick = {
                        reportViewModel.deleteSession(sessionToDelete!!._id)
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Tak, usuń")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Anuluj")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Nowy Raport Uszkodzeń",
                        fontWeight = FontWeight.Bold,
                        color = ReportTypeTheme.DarkText
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoadingSessions) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (!isLoadingSessions && activeSessions.isNotEmpty()) {
                item {
                    Text(
                        "Wznów aktywną sesję (z serwera)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ReportTypeTheme.DarkText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(activeSessions) { session ->
                    SessionCard(
                        session = session,
                        onDeleteClick = { sessionToDelete = session },
                        onClick = {
                            reportViewModel.loadSession(session._id) { screen ->
                                navController.navigate(screen.route)
                            }
                        },
                        onLongClick = {
                            sessionToInspectId = session._id
                        }
                    )
                }
                item {
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        "Lub utwórz nowy raport",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ReportTypeTheme.DarkText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else if (!isLoadingSessions) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ReportTypeTheme.NeutralBackground),
                        shape = RoundedCornerShape(ReportTypeTheme.cornerRadius),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Wybierz rodzaj raportu",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ReportTypeTheme.DarkText
                                )
                                Text(
                                    text = "Każdy typ ma specyficzne pola",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ReportTypeTheme.LightText
                                )
                            }
                        }
                    }
                }
            }

            itemsIndexed(reportOptions) { index, option ->
                CleanOptionCard(
                    option = option,
                    index = index,
                    isSelected = false,
                    onClick = {
                        reportViewModel.updateReportType(option.title)
                        navController.navigate(Screen.Header.route)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    SessionDetailsDialog(
        sessionId = sessionToInspectId,
        viewModel = reportViewModel,
        onDismiss = {
            sessionToInspectId = null
            reportViewModel.clearDetailedSession() // Czyścimy dane z ViewModelu
        }
    )
}

@OptIn(ExperimentalFoundationApi::class) // <-- DODANO ExperimentalFoundationApi
@Composable
fun SessionCard(
    session: SessionDetailsResponse,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val formattedDate = remember(session.headerData?.reportDatetime) {
        try {
            val dateTimeString = session.headerData?.reportDatetime ?: ""
            val datePart = dateTimeString.substringBefore("T")
            val timePart = dateTimeString.substringAfter("T").substringBefore(".")
            "$datePart ${timePart.substring(0, 5)}"
        } catch (e: Exception) {
            session.headerData?.reportDatetime ?: "Brak daty" // Fallback
        }
    }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                role = Role.Button
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(ReportTypeTheme.cornerRadius)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Restore,
                contentDescription = "Wznów sesję",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.headerData?.reportType ?: "Nieznany typ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )

                Text(
                    text = "Magazynier: ${session.headerData?.picWh ?: "B/D"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Miejsce: ${session.headerData?.place ?: "B/D"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )

                Text(
                    text = "Ostatnia zmiana: $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = "Liczba palet: ${session.palletsData?.size ?: 0}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Usuń sesję",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun CleanOptionCard(
    option: ReportTypeOption,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = index) {
        delay(index * 100L)
        isVisible = true
    }

    val cardColor by animateColorAsState(
        targetValue = if (isSelected) ReportTypeTheme.ValidBackground else Color.White,
        animationSpec = tween(ReportTypeTheme.animationDuration),
        label = "cardColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) ReportTypeTheme.ValidBorder else ReportTypeTheme.NeutralBorder,
        animationSpec = tween(ReportTypeTheme.animationDuration),
        label = "borderColor"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(400)) +
                slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
        exit = fadeOut() + slideOutVertically()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(
                    color = cardColor,
                    shape = RoundedCornerShape(ReportTypeTheme.cornerRadius)
                )
                .border(
                    width = ReportTypeTheme.borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(ReportTypeTheme.cornerRadius)
                )
                .clip(RoundedCornerShape(ReportTypeTheme.cornerRadius)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(ReportTypeTheme.cornerRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else ReportTypeTheme.NeutralBackground,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else ReportTypeTheme.MediumText
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) ReportTypeTheme.ValidColor else ReportTypeTheme.DarkText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) ReportTypeTheme.MediumText else ReportTypeTheme.LightText,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                val arrowRotation by animateFloatAsState(
                    targetValue = if (isSelected) 90f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "arrowRotation"
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer(rotationZ = arrowRotation),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else ReportTypeTheme.LightText
                    )
                }
            }
        }
    }
}

// ===================================================================
// ===== POCZĄTEK ZMIAN (DIALOG I FUNKCJE POMOCNICZE) =====
// ===================================================================

/**
 * Prosta klasa pomocnicza do przekazywania danych do LazyColumn w dialogu.
 */
private data class PalletPreview(
    val palletNumber: String?,
    val damageDetails: String?,
    val attachments: List<Base64Attachment>
)

/**
 * Okno dialogowe wyświetlające pełne szczegóły sesji,
 * pobierane na żądanie z ViewModelu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDetailsDialog(
    sessionId: String?,
    viewModel: HeaderViewModel,
    onDismiss: () -> Unit
) {
    // Subskrybuj stany z ViewModelu
    val isLoading by viewModel.isDetailedSessionLoading.collectAsStateWithLifecycle()
    val detailedSession by viewModel.detailedSession.collectAsStateWithLifecycle()

    // Uruchom pobieranie danych, gdy dialog ma być widoczny (sessionId nie jest null)
    LaunchedEffect(sessionId) {
        if (sessionId != null) {
            viewModel.loadDetailedSession(sessionId)
        }
    }

    // Przetwarzamy dane sesji na prostszą listę do wyświetlenia
    val palletPreviews = remember(detailedSession) {
        detailedSession?.palletsData?.map { pallet ->
            // Zbierz wszystkie załączniki Base64 z tej jednej palety
            val allAttachments = pallet.attachments_base64?.values?.flatten() ?: emptyList()

            PalletPreview(
                palletNumber = pallet.palletNumberRaw,
                damageDetails = pallet.details,
                attachments = allAttachments
            )
        } ?: emptyList()
    }

    // Wyświetl dialog tylko jeśli mamy ID sesji
    if (sessionId != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.Info, contentDescription = "Szczegóły sesji") },
            title = { Text(detailedSession?.headerData?.reportType ?: "Szczegóły sesji") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 500.dp), // Zwiększona wysokość
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else if (detailedSession != null) {

                        // Lista przewijanych szczegółów
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Sekcja Nagłówka
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Magazynier: ${detailedSession?.headerData?.picWh ?: "B/D"}", fontWeight = FontWeight.Bold)
                                    Text("Miejsce: ${detailedSession?.headerData?.place ?: "B/D"}")
                                    Text("Lokalizacja: ${detailedSession?.headerData?.warehouseLocation ?: "B/D"}")
                                    Text("Pojazd: ${detailedSession?.headerData?.vehicle_type ?: "B/D"}")
                                }
                            }

                            item {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }

                            // Sekcja Palet
                            if (palletPreviews.isEmpty()) {
                                item {
                                    Text("Ta sesja nie zawiera jeszcze żadnych palet.")
                                }
                            }

                            itemsIndexed(palletPreviews) { index, preview ->
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Paleta ${index + 1}: ${preview.palletNumber ?: "[Brak numeru]"}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (!preview.damageDetails.isNullOrBlank()) {
                                        Text(
                                            "Opis uszkodzeń: ${preview.damageDetails}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Podgląd zdjęć dla tej palety
                                    if (preview.attachments.isNotEmpty()) {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(preview.attachments) { attachment ->
                                                val bitmap = remember(attachment.fileId) {
                                                    base64ToBitmap(attachment.dataBase64)
                                                }
                                                if (bitmap != null) {
                                                    Card(shape = RoundedCornerShape(8.dp)) {
                                                        Image(
                                                            bitmap = bitmap,
                                                            contentDescription = "Podgląd palety",
                                                            modifier = Modifier
                                                                .size(100.dp)
                                                                .background(Color.LightGray),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            "Brak zdjęć dla tej palety.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }

                                    if(index < palletPreviews.lastIndex) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Divider()
                                    }
                                }
                            }

                        }
                    } else {
                        // Ten stan wystąpi po nieudanym pobraniu danych
                        Text("Nie można było załadować danych.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Zamknij")
                }
            }
        )
    }
}

/**
 * Funkcja pomocnicza do konwersji ciągu Base64 na ImageBitmap.
 */
// ===== POPRAWIONE (USUNIĘTO @Composable) =====
private fun base64ToBitmap(base64String: String?): ImageBitmap? {
    if (base64String.isNullOrEmpty()) return null
    return try {
        // Usuń ewentualny prefix (choć serwer zdaje się go nie wysyłać)
        val pureBase64 = base64String.substringAfter(",")
        val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size).asImageBitmap()
    } catch (e: Exception) {
        Log.e("Base64Convert", "Błąd dekodowania obrazu Base64", e)
        null
    }
}