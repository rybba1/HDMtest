package com.example.hdm.ui.log_monitor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.hdm.Screen
import com.example.hdm.model.ArchivedSession
import com.example.hdm.model.LogEntry
import com.example.hdm.model.SessionDetailsResponse
import com.example.hdm.model.UserManager
import com.example.hdm.ui.header.HeaderViewModel
import com.example.hdm.ui.report_type.SessionCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogMonitorScreen(
    navController: NavController,
    viewModel: LogMonitorViewModel = hiltViewModel(),
    reportViewModel: HeaderViewModel
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val archivedSessions by viewModel.archivedSessions.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // === NOWE STANY ===
    val isPingingActive by viewModel.isPingingActive.collectAsStateWithLifecycle()
    val diagnosticEvents by viewModel.diagnosticEvents.collectAsStateWithLifecycle()
    val diagnosticConfig by viewModel.diagnosticConfig.collectAsStateWithLifecycle()
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    var showConfigDialog by remember { mutableStateOf(false) }
    // ==================

    var sessionToDelete by remember { mutableStateOf<SessionDetailsResponse?>(null) }
    val activeSessions by reportViewModel.pendingSessions.collectAsStateWithLifecycle()
    val isLoadingSessions by reportViewModel.isSessionLoading.collectAsStateWithLifecycle()
    var archiveToDelete by remember { mutableStateOf<ArchivedSession?>(null) }


    LaunchedEffect(Unit) {
        viewModel.loadArchivedSessions()
        reportViewModel.loadPendingSessions() // Wczytaj sesje z serwera
    }

    // --- Dialogi (bez zmian) ---
    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Potwierdź usunięcie sesji") },
            text = { Text("Czy na pewno chcesz trwale usunąć niedokończony raport '${sessionToDelete!!.headerData?.reportType ?: "B/D"}' z serwera? Tej operacji nie można cofnąć.") },
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

    if (archiveToDelete != null) {
        AlertDialog(
            onDismissRequest = { archiveToDelete = null },
            title = { Text("Potwierdź usunięcie archiwum") },
            text = { Text("Czy na pewno chcesz usunąć zarchiwizowany raport '${archiveToDelete!!.reportType}'? Tej operacji nie można cofnąć.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteArchivedSession(archiveToDelete!!.archiveId)
                        Toast.makeText(context, "Usunięto archiwum", Toast.LENGTH_SHORT).show()
                        archiveToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Usuń")
                }
            },
            dismissButton = {
                TextButton(onClick = { archiveToDelete = null }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // === NOWY DIALOG KONFIGURACJI ===
    if (showConfigDialog) {
        RetryConfigurationDialog(
            currentConfig = diagnosticConfig,
            onDismiss = { showConfigDialog = false },
            onConfigUpdate = { newConfig ->
                viewModel.updateDiagnosticConfig(newConfig)
                showConfigDialog = false
            }
        )
    }
    // ===============================

    val identifierFileUri by UserManager.identifierFileUri.collectAsStateWithLifecycle()
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                UserManager.setIdentifierFile(context, it)
                Toast.makeText(context, "Plik identyfikacyjny został zapisany.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monitor i Archiwum") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć")
                    }
                },
                actions = {
                    // === NOWY PRZYCISK KONFIGURACJI ===
                    IconButton(onClick = { showConfigDialog = true }) {
                        Icon(Icons.Default.Settings, "Konfiguracja diagnostyki")
                    }
                    // ================================
                    IconButton(onClick = { navController.navigate(Screen.BhpStats.route) }) {
                        Icon(
                            Icons.Default.HealthAndSafety,
                            contentDescription = "Statystyki BHP"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.triggerLogSync()
                        Toast.makeText(context, "Zlecono synchronizację zaległych logów.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Wyślij Zaległe Logi")
                }
                OutlinedButton(
                    onClick = { filePickerLauncher.launch(arrayOf("text/plain")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Wybierz plik identyfikacyjny (.txt)")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            item {
                // === ZAKTUALIZOWANA KARTA DIAGNOSTYKI ===
                DiagnosticToolCard(
                    isPingingActive = isPingingActive,
                    diagnosticEvents = diagnosticEvents,
                    statistics = statistics,
                    config = diagnosticConfig,
                    onStart = { viewModel.startContinuousPing() },
                    onStop = { viewModel.stopContinuousPing() },
                    onClear = { viewModel.clearDiagnosticLogs() },
                    onSave = { viewModel.savePingLogsToFile() }
                )
                // ======================================
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                IdentifierStatusCard(uri = identifierFileUri, context = context)
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Text(
                    "Niedokończone Sesje (${activeSessions.size}):",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (isLoadingSessions) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (activeSessions.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Brak niedokończonych raportów na serwerze.",
                            modifier = Modifier.padding(16.dp),
                            color = Color.Gray
                        )
                    }
                } else {
                    activeSessions.forEach { session -> // Już posortowane z VM
                        // ===== POCZĄTEK POPRAWKI =====
                        SessionCard(
                            session = session,
                            onDeleteClick = { sessionToDelete = session },
                            onClick = {
                                reportViewModel.loadSession(session._id) { screen ->
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Modules.route)
                                    }
                                }
                            },
                            onLongClick = {} // <-- DODANO BRAKUJĄCY PARAMETR
                        )
                        // ===== KONIEC POPRAWKI =====
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Text(
                    "Archiwum zakończonych raportów (${archivedSessions.size}/3):",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (archivedSessions.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Brak zarchiwizowanych raportów.",
                            modifier = Modifier.padding(16.dp),
                            color = Color.Gray
                        )
                    }
                }
            } else {
                items(archivedSessions) { archived ->
                    ArchivedSessionCard(
                        archived = archived,
                        onLoadClick = {
                            reportViewModel.loadArchivedSession(archived)
                            navController.navigate(Screen.Header.route) {
                                popUpTo(Screen.Modules.route)
                            }
                        },
                        onDeleteClick = { archiveToDelete = archived },

                        )

                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Text(
                    "Ostatnie zdarzenia:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (logs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Oczekiwanie na pierwszy log...", textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(logs, key = { it.id }) { logEntry ->
                    LogCard(logEntry = logEntry)
                }
            }
        }
    }
}

@Composable
private fun ArchivedSessionCard(
    archived: ArchivedSession,
    onLoadClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLoadClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Archive,
                contentDescription = "Archiwum",
                modifier = Modifier.size(32.dp),
                tint = Color(0xFF1976D2)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    archived.reportType,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Zakończono: ${formatter.format(Date(archived.completionTimestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    "Palet: ${archived.palletCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Usuń",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// === ZAKTUALIZOWANA KARTA DIAGNOSTYKI ===
@Composable
fun DiagnosticToolCard(
    isPingingActive: Boolean,
    diagnosticEvents: List<DiagnosticEvent>,
    statistics: PingStatistics, // NOWE
    config: DiagnosticConfig, // NOWE
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(diagnosticEvents.size) {
        if (diagnosticEvents.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                onSave()
            } else {
                Toast.makeText(context, "Brak uprawnień do zapisu pliku.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Diagnostyka w Czasie Rzeczywistym", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // === NOWY PANEL STATYSTYK ===
            RetryStatisticsPanel(statistics = statistics, config = config)
            Spacer(modifier = Modifier.height(16.dp))
            // ============================

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isPingingActive) {
                    Button(onClick = onStop, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Stop")
                    }
                } else {
                    Button(onClick = onStart, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Start")
                    }
                }
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f), enabled = diagnosticEvents.isNotEmpty()) {
                    Text("Wyczyść")
                }
                OutlinedButton(onClick = {
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        when (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            PackageManager.PERMISSION_GRANTED -> onSave()
                            else -> permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    } else {
                        onSave()
                    }
                }, modifier = Modifier.weight(1f), enabled = diagnosticEvents.isNotEmpty()) {
                    Text("Zapisz")
                }
            }

            if (diagnosticEvents.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                LazyColumn(
                    state = listState,
                    modifier = Modifier.heightIn(max = 250.dp).fillMaxWidth(),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(diagnosticEvents.size) { index ->
                        val event = diagnosticEvents[index]
                        when (event) {
                            is PingEvent -> PingLogItem(log = event.log) // Zmieniony na nowy
                            is NetworkStateEvent -> NetworkEventItem(event = event.event)
                        }
                    }
                }
            }
        }
    }
}
// ===================================

@Composable
private fun NetworkEventItem(event: NetworkEvent) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val color = when (event.type) {
        NetworkEventType.AVAILABLE -> Color(0xFF2E7D32)
        NetworkEventType.LOST -> MaterialTheme.colorScheme.error
        NetworkEventType.CAPABILITIES_CHANGED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(color.copy(alpha = 0.1f)).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when(event.type) {
                NetworkEventType.AVAILABLE -> Icons.Default.Wifi
                NetworkEventType.LOST -> Icons.Default.WifiOff
                NetworkEventType.CAPABILITIES_CHANGED -> Icons.Default.SyncAlt
            },
            contentDescription = "Zdarzenie sieciowe", tint = color, modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "[${formatter.format(Date(event.timestamp))}] ${event.message}",
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

// === ZAKTUALIZOWANY PingLogItem (z nowymi polami) ===
@Composable
private fun PingLogItem(log: PingLog) {
    val statusColor = when (log.status) {
        PingStatus.SUCCESS -> Color(0xFF2E7D32)
        PingStatus.TIMEOUT -> Color(0xFFF57C00)
        PingStatus.FAILED -> MaterialTheme.colorScheme.error
    }
    val formatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    val rssiColor = log.rssi?.let {
        when {
            it > -60 -> Color(0xFF2E7D32)
            it > -70 -> Color(0xFFF57C00)
            else -> MaterialTheme.colorScheme.error
        }
    } ?: Color.Gray

    val freqText = log.frequency?.let {
        when {
            it in 2400..2500 -> "2.4 GHz"
            it in 5000..5900 -> "5 GHz"
            else -> "$it MHz"
        }
    } ?: "N/A"

    val timeText = when (log.status) {
        PingStatus.SUCCESS -> "${log.roundTripTimeMs} ms (Serwer: ${
            log.serverProcessingTimeMs?.let {
                "%.1f ms".format(
                    it
                )
            } ?: "N/A"
        })"
        PingStatus.TIMEOUT -> log.message
        PingStatus.FAILED -> "Błąd"
    }

    val timeColor = when {
        log.status == PingStatus.TIMEOUT -> Color(0xFFF57C00)
        log.status == PingStatus.FAILED -> MaterialTheme.colorScheme.error
        log.roundTripTimeMs > 1000 -> MaterialTheme.colorScheme.error
        log.roundTripTimeMs > 500 -> Color(0xFFF57C00)
        else -> Color.Unspecified
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        // --- Linia 1: Status, Czas, Czas Całkowity ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when (log.status) {
                    PingStatus.SUCCESS -> Icons.Default.Check
                    PingStatus.TIMEOUT -> Icons.Default.Timer
                    PingStatus.FAILED -> Icons.Default.Close
                },
                contentDescription = "Status",
                tint = statusColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(formatter.format(Date(log.timestamp)), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                timeText,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = timeColor
            )
            if (log.retryAttempts > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "(Retry: ${log.retryAttempts}, Total: ${log.totalTime}ms)",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF57C00)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        // --- Linia 2: Info Sieci ---
        Column(modifier = Modifier.padding(start = 24.dp)) {
            Row {
                Text(
                    "SSID: ${log.ssid ?: "N/A"} | IP: ${log.clientIp ?: "N/A"} | Port: ${log.clientPort ?: "N/A"}",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Row {
                Text(
                    "BSSID: ${log.bssid ?: "N/A"}",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Row {
                Text(
                    "Siła: ",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(log.rssi?.let { "$it dBm" } ?: "N/A",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = rssiColor,
                    fontWeight = FontWeight.Bold)
                Text(" | Prędkość: ${log.linkSpeed?.let { "$it Mbps" } ?: "N/A"} @ $freqText",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray)
            }
            Row {
                Text("Pobieranie: ${log.downstreamBw?.let { "${it / 1000} Mbps" } ?: "N/A"} | Wysyłanie: ${log.upstreamBw?.let { "${it / 1000} Mbps" } ?: "N/A"}",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray)
            }
        }

        if (log.status != PingStatus.SUCCESS || log.serverProcessingTimeMs == null) {
            val responseToShow =
                if (log.status == PingStatus.FAILED) log.message else log.serverResponse
            Text(
                text = "Odpowiedź: ${responseToShow?.take(200)}${if ((responseToShow?.length ?: 0) > 200) "..." else ""}",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
                modifier = Modifier.padding(start = 24.dp)
            )
        }
    }
}
// ===================================

@Composable
private fun IdentifierStatusCard(uri: Uri?, context: Context) {
    var fileName by remember(uri) { mutableStateOf<String?>(null) }
    LaunchedEffect(uri) {
        fileName = if (uri != null) {
            try {
                var name: String? = null
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    }
                }
                name
            } catch (e: Exception) { "Błąd odczytu pliku" }
        } else { null }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (fileName != null && fileName != "Błąd odczytu pliku") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (fileName != null && fileName != "Błąd odczytu pliku") Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = "Status pliku"
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Aktualny Identyfikator Urządzenia", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text(fileName?.substringBeforeLast('.') ?: "Nie wybrano (używane skanowanie)", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LogCard(logEntry: LogEntry) {
    val isSent by logEntry.isSent.collectAsStateWithLifecycle()
    val cardColor by animateColorAsState(
        targetValue = if (isSent) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
        animationSpec = tween(500), label = "cardColor"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSent) Color(0xFF2E7D32) else Color(0xFFF57C00),
        animationSpec = tween(500), label = "iconColor"
    )
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = cardColor)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isSent) Icons.Default.CheckCircle else Icons.Default.Schedule,
                contentDescription = "Status",
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(logEntry.message, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("Użytkownik: ${logEntry.user}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Text(logEntry.getFormattedTimestamp(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

// === NOWE KOMPONENTY UI ===

@Composable
private fun RetryStatisticsPanel(
    statistics: PingStatistics,
    config: DiagnosticConfig
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Strategia:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = config.retryStrategy.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when(config.retryStrategy) {
                        RetryStrategy.NONE -> Color.Gray
                        RetryStrategy.SIMPLE -> Color(0xFF1976D2)
                        RetryStrategy.EXPONENTIAL -> Color(0xFFFB8C00)
                        RetryStrategy.INTELLIGENT -> Color(0xFF388E3C)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    RetryStatItem("Requestów:", "${statistics.totalRequests}")
                    RetryStatItem("Udane:", "${statistics.successfulRequests} (${statistics.successRate}%)", Color(0xFF388E3C))
                    RetryStatItem("Nieudane:", "${statistics.failedRequests} (${statistics.failureRate}%)", Color(0xFFD32F2F))
                }

                Column(horizontalAlignment = Alignment.Start) {
                    RetryStatItem("Retry ogółem:", "${statistics.totalRetries}")
                    RetryStatItem("Śr. czas:", "${statistics.averageResponseTime}ms")
                    if (config.throttleDelayMs > 0) {
                        RetryStatItem("Throttling:", "${config.throttleDelayMs}ms", Color(0xFF1976D2))
                    }
                }
            }
        }
    }
}

@Composable
private fun RetryStatItem(label: String, value: String, color: Color = Color.Unspecified) {
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RetryConfigurationDialog(
    currentConfig: DiagnosticConfig,
    onDismiss: () -> Unit,
    onConfigUpdate: (DiagnosticConfig) -> Unit
) {
    var selectedStrategy by remember { mutableStateOf(currentConfig.retryStrategy) }
    var maxRetries by remember { mutableStateOf(currentConfig.maxRetries.toString()) }
    var baseDelay by remember { mutableStateOf(currentConfig.baseDelayMs.toString()) }
    var throttleDelay by remember { mutableStateOf(currentConfig.throttleDelayMs.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Konfiguracja Strategii Retry") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Wybierz Strategię",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    RetryStrategy.values().forEach { strategy ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStrategy = strategy }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedStrategy == strategy,
                                onClick = { selectedStrategy = strategy }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = strategy.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = when(strategy) {
                                        RetryStrategy.NONE -> "Bez ponownych prób"
                                        RetryStrategy.SIMPLE -> "Stałe opóźnienie między próbami"
                                        RetryStrategy.EXPONENTIAL -> "Rosnące opóźnienie (500ms, 1s, 2s...)"
                                        RetryStrategy.INTELLIGENT -> "Z circuit breaker i adaptacyjnym opóźnieniem"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                if (selectedStrategy != RetryStrategy.NONE) {
                    item {
                        OutlinedTextField(
                            value = maxRetries,
                            onValueChange = { maxRetries = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Maksymalna liczba prób (np. 3)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = baseDelay,
                            onValueChange = { baseDelay = it.filter { c -> c.isDigit() }.take(5) },
                            label = { Text("Bazowe opóźnienie (ms, np. 500)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = throttleDelay,
                        onValueChange = { throttleDelay = it.filter { c -> c.isDigit() }.take(5) },
                        label = { Text("Odstęp między pingami (ms, np. 2000)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "💡 Rekomendacje",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• SIMPLE: Dla sporadycznych timeoutów\n" +
                                        "• EXPONENTIAL: Dla niestabilnej sieci (⭐ Rekomendowane)\n" +
                                        "• INTELLIGENT: Dla chronicznego przeciążenia\n" +
                                        "• Odstęp 2000ms: Standardowy (2 sekundy)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newConfig = DiagnosticConfig(
                        retryStrategy = selectedStrategy,
                        maxRetries = maxRetries.toIntOrNull() ?: 3,
                        baseDelayMs = baseDelay.toLongOrNull() ?: 500L,
                        throttleDelayMs = throttleDelay.toLongOrNull() ?: 2000L // Domyślne 2s
                    )
                    onConfigUpdate(newConfig)
                }
            ) {
                Text("Zastosuj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}