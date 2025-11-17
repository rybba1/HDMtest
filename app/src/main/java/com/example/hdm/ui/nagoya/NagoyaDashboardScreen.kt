package com.example.hdm.ui.nagoya

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.hdm.Screen
import com.example.hdm.model.JapanControlReport
import com.example.hdm.model.ReportRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

private const val TAG = "JAPAN_DEBUG"
private const val SCANNER_RESULT_KEY = "scanned_pallet_number"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NagoyaDashboardScreen(navController: NavController) {
    val viewModel: NagoyaDashboardViewModel = viewModel()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    var reportToShowAsPdf by remember { mutableStateOf<JapanControlReport?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val scannerResultState = savedStateHandle
        ?.getStateFlow<String?>(SCANNER_RESULT_KEY, null)
        ?.collectAsStateWithLifecycle()

    LaunchedEffect(scannerResultState?.value) {
        scannerResultState?.value?.let { palletNumber ->
            Log.d(TAG, "Odebrano zeskanowany numer palety: $palletNumber")
            val allReports = viewModel.reports.value
            val foundReport = allReports.find { report ->
                report.pallets.any { it.carrierNumber.equals(palletNumber, ignoreCase = true) }
            }

            if (foundReport != null) {
                navController.navigate(Screen.JapanReportProcessing.createRoute(foundReport.header.order))
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Nie znaleziono palety o numerze: $palletNumber")
                }
            }
            savedStateHandle.remove<String>(SCANNER_RESULT_KEY)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initialSync()
        while (true) {
            if (ReportRepository.needsDelayedRefresh.value) {
                Log.d(TAG, "Wykryto wysłany raport. Czekam 30 sekund.")
                delay(30000)
                ReportRepository.delayedRefreshHandled()
            } else {
                delay(10000)
            }
            Log.d(TAG, "Uruchamiam odświeżanie raportów...")
            viewModel.syncReports()
        }
    }

    if (reportToShowAsPdf != null) {
        PdfPreviewDialog(
            report = reportToShowAsPdf!!,
            onDismiss = { reportToShowAsPdf = null },
            onOpenPdf = {
                viewPdfFromBase64(context, it.pdfReportBase64, "${it.header.order}.pdf")
                reportToShowAsPdf = null
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Pulpit Raportów Japan Control") },
                actions = {
                    AnimatedVisibility(
                        visible = isSyncing,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(animation = tween(1000)),
                            label = "sync_rotation_anim"
                        )
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Synchronizowanie...",
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .rotate(rotation)
                        )
                    }
                }
            )
        }
    ) { padding ->
        // Główna zawartość ekranu
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
            // ✅ NOWOŚĆ: Duży, wyśrodkowany przycisk skanera
            Button(
                onClick = {
                    val scanType = "1D_BARCODES"
                    navController.navigate("scanner_screen/$scanType/$SCANNER_RESULT_KEY")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Skanuj paletę")
            }

            // Lista raportów
            if (isSyncing && reports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            } else if (reports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text("Brak raportów do przetworzenia.", modifier = Modifier.align(Alignment.Center))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reports, key = { it.header.order }) { report ->
                        ReportDashboardItem(
                            report = report,
                            onClick = {
                                if (report.header.hdlStatus.equals("Completed", ignoreCase = true)) {
                                    if (report.pdfReportBase64 != null) {
                                        reportToShowAsPdf = report
                                    } else {
                                        Log.w(TAG, "Raport ${report.header.order} jest ukończony, ale brakuje danych PDF.")
                                    }
                                } else {
                                    navController.navigate(Screen.JapanReportProcessing.createRoute(report.header.order))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportDashboardItem(report: JapanControlReport, onClick: () -> Unit) {
    val palletsTotal = report.pallets.size
    val palletsNok = report.pallets.count { it.status.equals("NOK", ignoreCase = true) }
    val palletsRecheck = report.pallets.count { it.status.equals("Recheck", ignoreCase = true) }
    val palletsAccepted = report.pallets.count { it.status.equals("Accepted", ignoreCase = true) }
    val palletsRepack = report.pallets.count { it.status.equals("Repack", ignoreCase = true) }
    val isCompleted = report.header.hdlStatus.equals("Completed", ignoreCase = true)
    val isPendingForReVerification = report.header.hdlStatus.equals("Pending", ignoreCase = true) &&
            report.pallets.any { it.status.equals("HDL_SEND", ignoreCase = true) }
    val allPalletsAreNew = palletsTotal > 0 && report.pallets.all { it.status.equals("NEW", ignoreCase = true) }

    val cardColor = when {
        isCompleted -> Color(0xFFE8F5E9)
        palletsNok > 0 || palletsRecheck > 0 || palletsRepack > 0 || isPendingForReVerification -> Color(0xFFFFEBEE)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(report.header.order, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Status HDL: ${report.header.hdlStatus}", style = MaterialTheme.typography.bodyMedium)
                    if (isPendingForReVerification) {
                        Text(
                            "Do ponownej weryfikacji (zmiana listy palet)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (allPalletsAreNew) {
                        Text(
                            "Palety do weryfikacji",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF673AB7)
                        )
                    }
                    Text("Wszystkich palet: $palletsTotal", style = MaterialTheme.typography.bodySmall)
                    if (palletsNok > 0) {
                        Text("Palety z uszkodzeniem (NOK): $palletsNok", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    if (palletsRecheck > 0) {
                        Text("Palety do zweryfikowania przez magazyn: $palletsRecheck", style = MaterialTheme.typography.bodySmall, color = Color(0xFFF57C00))
                    }
                    if (palletsAccepted > 0) {
                        Text("Palety zaakceptowane przez klienta: $palletsAccepted", style = MaterialTheme.typography.bodySmall, color = Color(0xFF388E3C))
                    }
                    if (palletsRepack > 0) {
                        Text("Palety do przepakowania: $palletsRepack", style = MaterialTheme.typography.bodySmall, color = Color(0xFF0288D1))
                    }
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Przejdź do raportu")
            }
        }
    }
}

@Composable
private fun PdfPreviewDialog(
    report: JapanControlReport,
    onDismiss: () -> Unit,
    onOpenPdf: (JapanControlReport) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Podgląd raportu PDF") },
        text = { Text("Ten raport jest ukończony i zablokowany do edycji. Czy chcesz otworzyć jego podgląd w formacie PDF?") },
        confirmButton = {
            TextButton(onClick = { onOpenPdf(report) }) {
                Text("Otwórz PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

private fun viewPdfFromBase64(context: Context, base64String: String?, fileName: String) {
    if (base64String.isNullOrBlank()) {
        Log.e(TAG, "Dane Base64 dla PDF są puste.")
        return
    }
    try {
        val pdfBytes = Base64.decode(base64String, Base64.DEFAULT)
        val uri = saveBytesToTempFile(context, pdfBytes, fileName)
            ?: throw Exception("Nie udało się utworzyć URI dla pliku PDF")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.e(TAG, "Nie znaleziono aplikacji do otwierania plików PDF.", e)
    } catch (e: Exception) {
        Log.e(TAG, "Błąd podczas otwierania pliku PDF.", e)
    }
}

private fun saveBytesToTempFile(context: Context, bytes: ByteArray, fileName: String): Uri? {
    return try {
        val targetDir = context.getExternalFilesDir(null) ?: context.cacheDir
        val reportsDir = File(targetDir, "pdf_previews").apply { mkdirs() }
        val file = File(reportsDir, fileName)
        FileOutputStream(file).use { it.write(bytes) }
        val authority = "${context.packageName}.provider"
        FileProvider.getUriForFile(context, authority, file)
    } catch (e: Exception) {
        Log.e(TAG, "Nie udało się zapisać pliku PDF do pamięci tymczasowej.", e)
        null
    }
}