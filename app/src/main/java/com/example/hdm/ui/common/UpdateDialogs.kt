// ZASTĄP CAŁY PLIK: com/example/hdm/ui/common/UpdateDialogs.kt
package com.example.hdm.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.hdm.services.UpdateInfo

@Composable
fun UpdateAvailableDialog(
    info: UpdateInfo,
    onDownloadClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.SystemUpdate, contentDescription = "Aktualizacja") },
        title = { Text("Dostępna nowa wersja") },
        text = {
            Column {
                Text(
                    "Dostępna jest nowa wersja aplikacji:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    info.latestVersion,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                if (info.changelog.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Lista zmian:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp) // Ogranicz wysokość listy
                            .padding(top = 8.dp)
                    ) {
                        items(info.changelog) { change ->
                            Text(
                                "• $change",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDownloadClick
            ) {
                Text("Pobierz i zainstaluj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Później")
            }
        }
    )
}

@Composable
fun DownloadingDialog(progress: Int) {
    Dialog(
        onDismissRequest = { /* Nie można zamknąć */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (progress > 0) {
                    CircularProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                }
                Column {
                    Text(
                        text = "Pobieranie aktualizacji...",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (progress > 0) "$progress%" else "Oczekiwanie...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun UpdateErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = "Błąd") },
        title = { Text("Błąd aktualizacji") },
        text = {
            Text(message)
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("OK")
            }
        }
    )
}

// --- POCZĄTEK ZMIANY ---
// Nowy dialog, który zastępuje powiadomienie
@Composable
fun InstallReadyDialog(
    onInstallClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Sukces", tint = Color(0xFF2E7D32)) },
        title = { Text("Pobieranie zakończone") },
        text = {
            Text("Aktualizacja jest gotowa do instalacji.")
        },
        confirmButton = {
            TextButton(
                onClick = onInstallClick
            ) {
                Text("Zainstaluj teraz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Później")
            }
        }
    )
}
// --- KONIEC ZMIANY ---