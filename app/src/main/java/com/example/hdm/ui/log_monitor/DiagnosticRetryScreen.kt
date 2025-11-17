package com.example.hdm.ui.log_monitor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticRetryScreen(
    viewModel: LogMonitorViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val isPingingActive by viewModel.isPingingActive.collectAsState()
    val diagnosticEvents by viewModel.diagnosticEvents.collectAsState()
    val diagnosticConfig by viewModel.diagnosticConfig.collectAsState()
    val statistics by viewModel.statistics.collectAsState()

    var showConfigDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Strategii Retry") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Wstecz")
                    }
                },
                actions = {
                    IconButton(onClick = { showConfigDialog = true }) {
                        Icon(Icons.Default.Settings, "Konfiguracja")
                    }
                    IconButton(onClick = { viewModel.savePingLogsToFile() }) {
                        Icon(Icons.Default.Save, "Zapisz logi")
                    }
                    IconButton(onClick = { viewModel.clearDiagnosticLogs() }) {
                        Icon(Icons.Default.Delete, "Wyczyść")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Panel kontrolny
            RetryControlPanel(
                isPingingActive = isPingingActive,
                onStartPing = { viewModel.startContinuousPing() },
                onStopPing = { viewModel.stopContinuousPing() }
            )

            // Panel statystyk
            RetryStatisticsPanel(
                statistics = statistics,
                config = diagnosticConfig
            )

            Divider()

            // Lista logów
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(diagnosticEvents, key = { "${it::class.simpleName}_${it.timestamp}" }) { event ->
                    when (event) {
                        is PingEvent -> RetryPingLogItem(event.log)
                        is NetworkStateEvent -> RetryNetworkEventItem(event.event)
                    }
                }
            }
        }
    }

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
}

@Composable
private fun RetryControlPanel(
    isPingingActive: Boolean,
    onStartPing: () -> Unit,
    onStopPing: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = if (isPingingActive) onStopPing else onStartPing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPingingActive) Color(0xFFE53935) else Color(0xFF43A047)
                )
            ) {
                Icon(
                    imageVector = if (isPingingActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isPingingActive) "STOP" else "START")
            }

            if (isPingingActive) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aktywny...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun RetryStatisticsPanel(
    statistics: PingStatistics,
    config: DiagnosticConfig
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
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

@Composable
private fun RetryPingLogItem(log: PingLog) {
    val backgroundColor = when (log.status) {
        PingStatus.SUCCESS -> Color(0xFFE8F5E9)
        PingStatus.TIMEOUT -> Color(0xFFFFF3E0)
        PingStatus.FAILED -> Color(0xFFFFEBEE)
    }

    val iconColor = when (log.status) {
        PingStatus.SUCCESS -> Color(0xFF388E3C)
        PingStatus.TIMEOUT -> Color(0xFFFB8C00)
        PingStatus.FAILED -> Color(0xFFD32F2F)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = when (log.status) {
                    PingStatus.SUCCESS -> Icons.Default.CheckCircle
                    PingStatus.TIMEOUT -> Icons.Default.Schedule
                    PingStatus.FAILED -> Icons.Default.Error
                },
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = log.status.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                    Text(
                        text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (log.status == PingStatus.SUCCESS) {
                    Text(
                        text = "⏱️ RTT: ${log.roundTripTimeMs}ms | 🖥️ Serwer: ${log.serverProcessingTimeMs?.let { "%.1f ms".format(it) } ?: "N/A"}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (log.retryAttempts > 0) {
                        Text(
                            text = "🔄 Udało się po ${log.retryAttempts + 1} próbie (${log.totalTime}ms)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFB8C00),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        text = log.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = iconColor
                    )

                    if (log.retryAttempts > 0) {
                        Text(
                            text = "🔄 Próbowano ${log.retryAttempts + 1}x (${log.totalTime}ms)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                if (log.ssid != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📡 ${log.ssid} | ${log.rssi}dBm | ${log.linkSpeed}Mbps",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                if (log.clientIp != null) {
                    Text(
                        text = "🌐 ${log.clientIp}:${log.clientPort}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun RetryNetworkEventItem(event: NetworkEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (event.type) {
                    NetworkEventType.AVAILABLE -> Icons.Default.Wifi
                    NetworkEventType.LOST -> Icons.Default.WifiOff
                    NetworkEventType.CAPABILITIES_CHANGED -> Icons.Default.Sync
                },
                contentDescription = null,
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.message,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

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
                            onValueChange = { maxRetries = it },
                            label = { Text("Maksymalna liczba prób") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = baseDelay,
                            onValueChange = { baseDelay = it },
                            label = { Text("Bazowe opóźnienie (ms)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = throttleDelay,
                        onValueChange = { throttleDelay = it },
                        label = { Text("Throttling (ms, 0=wyłączone)") },
                        modifier = Modifier.fillMaxWidth()
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
                                        "• Throttling 150ms: Redukuje obciążenie serwera",
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
                        throttleDelayMs = throttleDelay.toLongOrNull() ?: 0L
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