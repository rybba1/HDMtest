package com.example.hdm.ui.nagoya

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.hdm.R
import com.example.hdm.model.JapanReportHeader
import com.example.hdm.model.PalletEntry
import com.example.hdm.model.WarehouseWorker
import kotlinx.coroutines.launch

@Composable
fun JapanReportProcessingScreen(
    navController: NavController,
    orderId: String
) {
    val viewModel: JapanReportProcessingViewModel = viewModel()
    val palletForNokEditing by viewModel.palletForNokEditing.collectAsStateWithLifecycle()
    val isReadOnly by viewModel.isReadOnly.collectAsStateWithLifecycle()
    val isMixedNewStatusMode by viewModel.isMixedNewStatusMode.collectAsStateWithLifecycle()

    if (palletForNokEditing != null) {
        NokPhotoScreen(
            viewModel = viewModel,
            onClose = { viewModel.closeNokScreen() }
        )
    } else {
        ReportListScreen(
            navController = navController,
            orderId = orderId,
            viewModel = viewModel,
            isReadOnly = isReadOnly,
            isMixedNewStatusMode = isMixedNewStatusMode
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportListScreen(
    navController: NavController,
    orderId: String,
    viewModel: JapanReportProcessingViewModel,
    isReadOnly: Boolean,
    isMixedNewStatusMode: Boolean
) {
    val report by viewModel.reportForUi.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedPalletForDialog by remember { mutableStateOf<PalletEntry?>(null) }
    var showInstructionDialog by remember { mutableStateOf(false) }

    if (showInstructionDialog) {
        InstructionDialog(onDismiss = { showInstructionDialog = false })
    }

    LaunchedEffect(orderId) {
        viewModel.loadReport(orderId)
    }

    LaunchedEffect(uiState.operationState) {
        when (val state = uiState.operationState) {
            is OperationState.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar(state.message)
                    navController.popBackStack()
                }
                viewModel.resetOperationState()
            }
            is OperationState.Error -> {
                scope.launch { snackbarHostState.showSnackbar(state.message, withDismissAction = true) }
                viewModel.resetOperationState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Raport: $orderId") },
                actions = {
                    IconButton(
                        onClick = { viewModel.sendReport() },
                        enabled = !isReadOnly
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Wyślij raport")
                    }
                }
            )
        }
    ) { padding ->
        val currentReport = report
        if (currentReport == null || uiState.operationState is OperationState.Loading) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    val hdlStatus = currentReport.header.hdlStatus
                    if (isReadOnly) {
                        InfoCard("Ten raport ma status 'Pending' i jest dostępny tylko do odczytu.")
                    } else if (hdlStatus.equals("Pending", ignoreCase = true)) {
                        InfoCard("Raport odblokowany do edycji z powodu zmiany na liście palet.", isWarning = true)
                    }
                }
                item {
                    WorkerSelection(
                        uiState = uiState,
                        onQueryChange = viewModel::onWorkerSearchQueryChange,
                        onWorkerSelected = { worker ->
                            viewModel.onWorkerSelected(worker)
                            showInstructionDialog = true
                        },
                        onDismiss = viewModel::onDropdownDismiss,
                        isEnabled = !isReadOnly
                    )
                }
                item {
                    ReportHeaderDetails(header = currentReport.header)
                }
                items(currentReport.pallets, key = { it.carrierNumber }) { pallet ->
                    val isPalletEditable = !isReadOnly && (!isMixedNewStatusMode || pallet.status.equals("NEW", ignoreCase = true))
                    PalletItem(
                        pallet = pallet,
                        onClick = {
                            if (uiState.selectedWorker == null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Najpierw wybierz pracownika, aby wprowadzić zmiany!")
                                }
                            } else {
                                selectedPalletForDialog = pallet
                            }
                        },
                        isEnabled = isPalletEditable
                    )
                }
            }
        }
    }

    if (selectedPalletForDialog != null) {
        UpdateStatusDialog(
            pallet = selectedPalletForDialog!!,
            onDismiss = { selectedPalletForDialog = null },
            onConfirmOk = {
                viewModel.updatePalletStatusToOk(it)
                selectedPalletForDialog = null
            },
            onConfirmNok = {
                viewModel.selectPalletForNok(it)
                selectedPalletForDialog = null
            }
        )
    }
}

@Composable
private fun InstructionDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Instrukcja Weryfikacji Palet") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Image(
                    painter = painterResource(id = R.drawable.nagoya_photo),
                    contentDescription = "Instrukcja obrazkowa",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Przed załadunkiem palet na wysyłkę należy sprawdzić stan jakościowy każdej palety szczególnie zwracając uwagę na wyszczególnione punkty poniżej:\n\n" +
                            "• Weryfikacja górnej przekładki pod kątem zabrudzeń i uszkodzeń mechanicznych\n\n" +
                            "• Weryfikacja opakowania kartonowego oraz folii pod kątem zabrudzeń i uszkodzeń mechanicznych\n\n" +
                            "• Weryfikacja elementów drewnianych palet pod kątem zabrudzeń i uszkodzeń mechanicznych",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Potwierdzam")
            }
        }
    )
}

@Composable
private fun InfoCard(message: String, isWarning: Boolean = false) {
    val backgroundColor = if (isWarning) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = "Informacja", modifier = Modifier.padding(end = 8.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun UpdateStatusDialog(
    pallet: PalletEntry,
    onDismiss: () -> Unit,
    onConfirmOk: (PalletEntry) -> Unit,
    onConfirmNok: (PalletEntry) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zmień status palety") },
        text = { Text("Wybierz nowy status dla palety:\n${pallet.carrierNumber}") },
        confirmButton = {
            TextButton(onClick = { onConfirmNok(pallet) }) {
                Text("Zgłoś uszkodzenie (NOK)")
            }
        },
        dismissButton = {
            TextButton(onClick = { onConfirmOk(pallet) }) {
                Text("Zatwierdź jako OK")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerSelection(
    uiState: ReportProcessingUiState,
    onQueryChange: (String) -> Unit,
    onWorkerSelected: (WarehouseWorker) -> Unit,
    onDismiss: () -> Unit,
    isEnabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Wybierz pracownika", style = MaterialTheme.typography.titleMedium)
        ExposedDropdownMenuBox(
            expanded = uiState.isDropdownExpanded,
            onExpandedChange = {}
        ) {
            OutlinedTextField(
                value = uiState.workerSearchQuery,
                onValueChange = onQueryChange,
                label = { Text("Wpisz min. 3 litery...") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                singleLine = true,
                enabled = isEnabled,
                colors = if (uiState.selectedWorker == null && isEnabled) {
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.error,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    )
                }
            )
            ExposedDropdownMenu(
                expanded = uiState.isDropdownExpanded,
                onDismissRequest = onDismiss
            ) {
                uiState.workerSearchResults.forEach { worker ->
                    DropdownMenuItem(
                        text = { Text(worker.fullName) },
                        onClick = { onWorkerSelected(worker) }
                    )
                }
            }
        }
        if (uiState.selectedWorker == null && isEnabled) {
            Text(
                "Wybór jest obowiązkowy, aby edytować raport.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        uiState.lastModifier?.let {
            Text(
                "Ostatnia modyfikacja: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReportHeaderDetails(header: JapanReportHeader) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Szczegóły zlecenia", style = MaterialTheme.typography.titleLarge)
            Text("Feniks: ${header.feniks}", style = MaterialTheme.typography.bodyMedium)
            Text("Status HDL: ${header.hdlStatus}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PalletItem(pallet: PalletEntry, onClick: () -> Unit, isEnabled: Boolean) {
    val cardColor = when (pallet.status.uppercase()) {
        "OK" -> Color(0xFFC8E6C9)
        "NOK" -> Color(0xFFFFCDD2)
        "RECHECK" -> Color(0xFFFFF9C4)
        "REPACK" -> MaterialTheme.colorScheme.secondaryContainer
        "ACCEPTED" -> MaterialTheme.colorScheme.tertiaryContainer
        "NEW" -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val photoCount = listOfNotNull(pallet.nokPhoto1, pallet.nokPhoto2, pallet.nokPhoto3, pallet.nokPhoto4).size

    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = isEnabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(pallet.carrierNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Status: ${pallet.status}", fontWeight = FontWeight.Bold)
            pallet.whWorkerHalf?.let {
                Text("Zmodyfikowano przez: $it")
            }
            if (photoCount > 0) {
                Text("Dołączone zdjęcia: $photoCount", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}