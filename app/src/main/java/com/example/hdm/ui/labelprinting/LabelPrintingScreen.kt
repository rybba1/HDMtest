package com.example.hdm.ui.labelprinting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.hdm.Screen
import com.example.hdm.model.WarehouseWorker
import kotlinx.coroutines.delay

object LabelPrintingTheme {
    val ValidColor = Color(0xFF2E7D32)
    val InvalidColor = Color(0xFFD32F2F)
    val EmptyColor = Color(0xFF757575)
    val WarningColor = Color(0xFFFF9800)

    val ValidBackground = Color(0xFFF1F8F2)
    val InvalidBackground = Color(0xFFFFF3F3)
    val WarningBackground = Color(0xFFFFF3E0)
    val NeutralBackground = Color(0xFFFAFAFA)

    val ValidBorder = Color(0xFFE8F5E8)
    val InvalidBorder = Color(0xFFFFEBEE)
    val WarningBorder = Color(0xFFFFF3E0)
    val NeutralBorder = Color(0xFFE0E0E0)

    val DarkText = Color(0xFF212121)
    val MediumText = Color(0xFF424242)
    val LightText = Color(0xFF757575)

    val cornerRadius = 12.dp
    val borderWidth = 1.dp
    val animationDuration = 250
}

enum class FieldValidationState {
    EMPTY, VALID, INVALID, WARNING
}

data class FieldValidation(
    val state: FieldValidationState,
    val message: String = ""
)

@Composable
private fun ValidationIcon(
    validationState: FieldValidationState,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (validationState) {
        FieldValidationState.VALID -> Icons.Default.Check to LabelPrintingTheme.ValidColor
        FieldValidationState.INVALID -> Icons.Default.ErrorOutline to LabelPrintingTheme.InvalidColor
        FieldValidationState.WARNING -> Icons.Default.Warning to LabelPrintingTheme.WarningColor
        FieldValidationState.EMPTY -> return
    }

    AnimatedVisibility(
        visible = validationState != FieldValidationState.EMPTY,
        enter = fadeIn(animationSpec = tween(LabelPrintingTheme.animationDuration)),
        exit = fadeOut(animationSpec = tween(150))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = modifier.size(18.dp)
        )
    }
}

@Composable
private fun CleanTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    validationState: FieldValidationState,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val borderColor by animateColorAsState(
        targetValue = when (validationState) {
            FieldValidationState.VALID -> LabelPrintingTheme.ValidBorder
            FieldValidationState.INVALID -> LabelPrintingTheme.InvalidBorder
            FieldValidationState.WARNING -> LabelPrintingTheme.WarningBorder
            FieldValidationState.EMPTY -> LabelPrintingTheme.NeutralBorder
        },
        animationSpec = tween(LabelPrintingTheme.animationDuration),
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when (validationState) {
            FieldValidationState.VALID -> LabelPrintingTheme.ValidBackground
            FieldValidationState.INVALID -> LabelPrintingTheme.InvalidBackground
            FieldValidationState.WARNING -> LabelPrintingTheme.WarningBackground
            FieldValidationState.EMPTY -> Color.White
        },
        animationSpec = tween(LabelPrintingTheme.animationDuration),
        label = "backgroundColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(LabelPrintingTheme.cornerRadius)
            )
            .border(
                width = LabelPrintingTheme.borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(LabelPrintingTheme.cornerRadius)
            )
            .clip(RoundedCornerShape(LabelPrintingTheme.cornerRadius))
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = LabelPrintingTheme.MediumText) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            enabled = enabled,
            readOnly = readOnly,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                errorBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                focusedTextColor = LabelPrintingTheme.DarkText,
                unfocusedTextColor = LabelPrintingTheme.DarkText,
                disabledTextColor = LabelPrintingTheme.LightText,
                focusedLabelColor = LabelPrintingTheme.MediumText,
                unfocusedLabelColor = LabelPrintingTheme.LightText
            ),
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ValidationIcon(validationState = validationState)
                    trailingIcon?.invoke()
                }
            }
        )
    }
}

@Composable
private fun ProgressCard(
    completedSteps: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = completedSteps.toFloat() / totalSteps.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "progress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LabelPrintingTheme.NeutralBackground
        ),
        shape = RoundedCornerShape(LabelPrintingTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Postęp formularza",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LabelPrintingTheme.DarkText
                )
                Text(
                    text = "$completedSteps/$totalSteps",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (completedSteps == totalSteps) LabelPrintingTheme.ValidColor else LabelPrintingTheme.DarkText
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (progress >= 1f) LabelPrintingTheme.ValidColor else MaterialTheme.colorScheme.primary,
                trackColor = Color(0xFFE0E0E0)
            )
        }
    }
}

@Composable
private fun PrintingDialog(
    state: PrintingState,
    onDismiss: () -> Unit,
    onNewForm: () -> Unit
) {
    if (state is PrintingState.Idle) return

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                when (state) {
                    is PrintingState.Processing -> {
                        PrintingProcessContent(
                            currentStep = state.step,
                            progress = state.progress
                        )
                    }
                    is PrintingState.Success -> {
                        val scale by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "successScale"
                        )
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Sukces",
                            tint = LabelPrintingTheme.ValidColor,
                            modifier = Modifier.size(64.dp).scale(scale)
                        )
                        Text(
                            state.message,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Dokumenty zostaną wkrótce wydrukowane.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = LabelPrintingTheme.MediumText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onNewForm,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LabelPrintingTheme.ValidColor
                            )
                        ) {
                            Text("Nowy formularz")
                        }
                    }
                    is PrintingState.Error -> {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Błąd",
                            tint = LabelPrintingTheme.InvalidColor,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            "Błąd wysyłania",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = LabelPrintingTheme.InvalidColor
                        )
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = LabelPrintingTheme.MediumText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LabelPrintingTheme.InvalidColor
                            )
                        ) {
                            Text("OK")
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun PrintingProcessContent(
    currentStep: PrintingStep,
    progress: Float
) {
    val steps = listOf(
        PrintingStep.VALIDATING_DATA to ("Walidacja danych" to Icons.Default.FactCheck),
        PrintingStep.PREPARING_REQUEST to ("Przygotowanie żądania" to Icons.Default.Description),
        PrintingStep.SENDING_TO_SERVER to ("Wysyłanie do serwera" to Icons.Default.CloudUpload),
        PrintingStep.WAITING_FOR_RESPONSE to ("Oczekiwanie na odpowiedź" to Icons.Default.HourglassEmpty),
        PrintingStep.FINALIZING to ("Finalizacja" to Icons.Default.CheckCircle)
    )

    val currentStepIndex = steps.indexOfFirst { it.first == currentStep }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(80.dp),
                strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.primary
            )

            if (currentStepIndex >= 0) {
                Icon(
                    steps[currentStepIndex].second.second,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Text(
            "Wysyłanie żądania druku...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = LabelPrintingTheme.DarkText
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            steps.forEachIndexed { index, (stepEnum, stepData) ->
                PrintingProcessStep(
                    stepName = stepData.first,
                    icon = stepData.second,
                    isActive = index == currentStepIndex,
                    isCompleted = index < currentStepIndex
                )
            }
        }

        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            label = "realProgress"
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color(0xFFE0E0E0),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${(animatedProgress * 100).toInt()}% ukończono",
                style = MaterialTheme.typography.bodySmall,
                color = LabelPrintingTheme.MediumText,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun PrintingProcessStep(
    stepName: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    isCompleted: Boolean
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isActive -> Color(0xFFE3F2FD)
            isCompleted -> LabelPrintingTheme.ValidBackground
            else -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "stepBg"
    )

    val iconColor by animateColorAsState(
        targetValue = when {
            isActive -> Color(0xFF1976D2)
            isCompleted -> LabelPrintingTheme.ValidColor
            else -> LabelPrintingTheme.LightText
        },
        animationSpec = tween(300),
        label = "iconColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = when {
                        isActive -> Color(0xFF1976D2).copy(alpha = 0.2f)
                        isCompleted -> LabelPrintingTheme.ValidColor.copy(alpha = 0.2f)
                        else -> Color.Transparent
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = stepName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
            color = when {
                isActive -> LabelPrintingTheme.DarkText
                isCompleted -> LabelPrintingTheme.ValidColor
                else -> LabelPrintingTheme.LightText
            },
            modifier = Modifier.weight(1f)
        )

        if (isActive) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF1976D2)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelPrintingScreen(
    navController: NavController,
    viewModel: LabelPrintingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // --- POCZĄTEK POPRAWKI ---
    LaunchedEffect(Unit) {
        viewModel.logoutAndNavigateToLogin.collect {
            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                }
            }
        }
    }
    // --- KONIEC POPRAWKI ---

    val BARCODE_SCAN_KEY = "lp_barcode_scan_result"
    val PRINTER_SCAN_KEY = "lp_printer_scan_result"

    val barcodeScannerResult = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<String>(BARCODE_SCAN_KEY)
    barcodeScannerResult?.observe(navController.currentBackStackEntry!!) { barcode ->
        if (barcode != null) {
            viewModel.addBarcode(barcode)
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>(BARCODE_SCAN_KEY)
        }
    }

    val printerScannerResult = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<String>(PRINTER_SCAN_KEY)
    printerScannerResult?.observe(navController.currentBackStackEntry!!) { printerName ->
        if (printerName != null) {
            viewModel.setPrinter(printerName)
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>(PRINTER_SCAN_KEY)
        }
    }

    PrintingDialog(
        state = uiState.printingState,
        onDismiss = { viewModel.resetPrintingState() },
        onNewForm = {
            viewModel.resetForm()
            viewModel.resetPrintingState()
        }
    )

    LaunchedEffect(uiState.printingState) {
        if (uiState.printingState is PrintingState.Error) {
            delay(3000)
            viewModel.resetPrintingState()
        }
    }

    val fieldValidations = remember(uiState) {
        mapOf(
            "worker" to when {
                uiState.selectedWorker != null -> FieldValidation(
                    state = FieldValidationState.VALID,
                    message = "Pracownik wybrany"
                )
                uiState.workerSearchQuery.isNotEmpty() -> FieldValidation(
                    state = FieldValidationState.INVALID,
                    message = "Wybierz pracownika z listy"
                )
                else -> FieldValidation(
                    state = FieldValidationState.EMPTY,
                    message = "Wybierz pracownika"
                )
            },
            "address" to FieldValidation(
                state = if (uiState.deliveryAddress.isNotEmpty()) FieldValidationState.VALID else FieldValidationState.EMPTY,
                message = if (uiState.deliveryAddress.isNotEmpty()) "Adres wybrany" else "Wybierz adres dostawy"
            ),
            "carrier" to when {
                uiState.carrier.isNotEmpty() && uiState.carrier != "Inne" -> FieldValidation(
                    state = FieldValidationState.VALID,
                    message = "Przewoźnik wybrany"
                )
                uiState.carrier == "Inne" && uiState.customCarrierName.isNotEmpty() -> FieldValidation(
                    state = FieldValidationState.VALID,
                    message = "Własny przewoźnik podany"
                )
                uiState.carrier == "Inne" -> FieldValidation(
                    state = FieldValidationState.INVALID,
                    message = "Podaj nazwę przewoźnika"
                )
                else -> FieldValidation(
                    state = FieldValidationState.EMPTY,
                    message = "Wybierz przewoźnika"
                )
            },
            "barcodes" to FieldValidation(
                state = if (uiState.scannedBarcodes.isNotEmpty()) FieldValidationState.VALID else FieldValidationState.EMPTY,
                message = if (uiState.scannedBarcodes.isNotEmpty()) "Kody zeskanowane (${uiState.scannedBarcodes.size})" else "Zeskanuj kody nośników"
            ),
            "printer" to FieldValidation(
                state = if (uiState.printer.isNotEmpty()) FieldValidationState.VALID else FieldValidationState.EMPTY,
                message = if (uiState.printer.isNotEmpty()) "Drukarka wybrana" else "Wybierz drukarkę"
            )
        )
    }

    val completedFields = fieldValidations.values.count { it.state == FieldValidationState.VALID }
    val totalFields = fieldValidations.size

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Drukowanie Dokumentów",
                        fontWeight = FontWeight.Bold,
                        color = LabelPrintingTheme.DarkText
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            ProgressCard(
                completedSteps = completedFields,
                totalSteps = totalFields
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(LabelPrintingTheme.cornerRadius),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Pracownik",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = LabelPrintingTheme.DarkText
                                )
                                ValidationIcon(validationState = fieldValidations["worker"]!!.state)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            WorkerSelection(
                                query = uiState.workerSearchQuery,
                                onQueryChange = viewModel::onWorkerQueryChanged,
                                selectedWorker = uiState.selectedWorker,
                                onWorkerSelected = viewModel::onWorkerSelected,
                                suggestions = uiState.workerSearchResults,
                                isDropdownExpanded = uiState.isWorkerDropdownExpanded,
                                onDismiss = viewModel::onWorkerDropdownDismiss,
                                validationState = fieldValidations["worker"]!!.state,
                                isEnabled = false
                            )
                            if (fieldValidations["worker"]!!.state == FieldValidationState.INVALID) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = LabelPrintingTheme.InvalidColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = fieldValidations["worker"]!!.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LabelPrintingTheme.InvalidColor
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(LabelPrintingTheme.cornerRadius),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Adres dostawy",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = LabelPrintingTheme.DarkText
                                )
                                ValidationIcon(validationState = fieldValidations["address"]!!.state)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            AddressSelection(
                                selectedAddress = uiState.deliveryAddress,
                                onAddressSelected = viewModel::onAddressSelected,
                                validationState = fieldValidations["address"]!!.state
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(LabelPrintingTheme.cornerRadius),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Przewoźnik",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = LabelPrintingTheme.DarkText
                                )
                                ValidationIcon(validationState = fieldValidations["carrier"]!!.state)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            CarrierSelection(
                                selectedCarrier = uiState.carrier,
                                onCarrierSelected = viewModel::onCarrierSelected,
                                customCarrierName = uiState.customCarrierName,
                                onCustomCarrierNameChanged = viewModel::onCustomCarrierNameChanged,
                                registrationNumber = uiState.registrationNumber,
                                onRegistrationChanged = viewModel::onRegistrationChanged,
                                validationState = fieldValidations["carrier"]!!.state
                            )
                            if (fieldValidations["carrier"]!!.state == FieldValidationState.INVALID) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth() ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = LabelPrintingTheme.InvalidColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = fieldValidations["carrier"]!!.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LabelPrintingTheme.InvalidColor
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.scannedBarcodes.isEmpty())
                                LabelPrintingTheme.WarningBackground
                            else
                                Color.White
                        ),
                        shape = RoundedCornerShape(LabelPrintingTheme.cornerRadius),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Numery nośników (${uiState.scannedBarcodes.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = LabelPrintingTheme.DarkText
                                )
                                ValidationIcon(validationState = fieldValidations["barcodes"]!!.state)
                            }

                            if (uiState.scannedBarcodes.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = LabelPrintingTheme.WarningColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Musisz zeskanować przynajmniej jeden kod",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LabelPrintingTheme.WarningColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            } else {
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Button(
                                onClick = { navController.navigate(Screen.Scanner.withArgs("VALIDATE_PALLET", BARCODE_SCAN_KEY)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.scannedBarcodes.isEmpty())
                                        LabelPrintingTheme.WarningColor
                                    else
                                        MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(LabelPrintingTheme.cornerRadius)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Skanuj")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Skanuj kod nośnika")
                            }

                            if (uiState.scannedBarcodes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = LabelPrintingTheme.ValidBackground
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        uiState.scannedBarcodes.forEach { barcode ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                            ) {
                                                Text(
                                                    barcode,
                                                    modifier = Modifier.weight(1f),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = LabelPrintingTheme.DarkText
                                                )
                                                IconButton(onClick = { viewModel.removeBarcode(barcode) }) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Usuń",
                                                        tint = LabelPrintingTheme.InvalidColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(LabelPrintingTheme.cornerRadius),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Drukarka",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = LabelPrintingTheme.DarkText
                                )
                                ValidationIcon(validationState = fieldValidations["printer"]!!.state)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            CleanTextField(
                                value = uiState.printer,
                                onValueChange = {},
                                label = "Nazwa drukarki",
                                validationState = fieldValidations["printer"]!!.state,
                                readOnly = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { navController.navigate(Screen.Scanner.withArgs("ALL_FORMATS", PRINTER_SCAN_KEY)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(LabelPrintingTheme.cornerRadius)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Skanuj")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Skanuj drukarkę")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.sendPrintRequest() },
                enabled = uiState.isFormValid && uiState.printingState is PrintingState.Idle,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isFormValid) LabelPrintingTheme.ValidColor else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (uiState.isFormValid) Color.White else LabelPrintingTheme.LightText
                ),
                shape = RoundedCornerShape(LabelPrintingTheme.cornerRadius)
            ) {
                Icon(
                    Icons.Default.Print,
                    contentDescription = "Drukuj",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Wydrukuj Dokumenty",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerSelection(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedWorker: WarehouseWorker?,
    onWorkerSelected: (WarehouseWorker) -> Unit,
    suggestions: List<WarehouseWorker>,
    isDropdownExpanded: Boolean,
    onDismiss: () -> Unit,
    validationState: FieldValidationState,
    isEnabled: Boolean = true
) {
    ExposedDropdownMenuBox(expanded = isDropdownExpanded && isEnabled, onExpandedChange = {}) {
        CleanTextField(
            value = query,
            onValueChange = onQueryChange,
            label = "Imię i nazwisko pracownika",
            validationState = validationState,
            modifier = Modifier.menuAnchor(),
            singleLine = true,
            enabled = isEnabled
        )
        ExposedDropdownMenu(expanded = isDropdownExpanded, onDismissRequest = onDismiss) {
            suggestions.forEach { worker ->
                DropdownMenuItem(
                    text = {
                        Text(
                            worker.fullName,
                            color = LabelPrintingTheme.DarkText
                        )
                    },
                    onClick = { onWorkerSelected(worker) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressSelection(
    selectedAddress: String,
    onAddressSelected: (String) -> Unit,
    validationState: FieldValidationState
) {
    val addresses = listOf(
        "MEIKO Trans Polska (LEON)",
        "MEIKO Trans Polska (GAUDI)",
        "NGK Ceramics Polska Sp. z o.o. (DPF)",
        "NGK Ceramics Polska Sp. z o.o. (LSH)",
        "NGK Ceramics Polska Sp. z o.o. (NNOx)"
    )
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        CleanTextField(
            value = selectedAddress,
            onValueChange = {},
            readOnly = true,
            label = "Wybierz adres dostawy",
            validationState = validationState,
            modifier = Modifier.menuAnchor(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            addresses.forEach { address ->
                DropdownMenuItem(
                    text = {
                        Text(
                            address,
                            color = LabelPrintingTheme.DarkText
                        )
                    },
                    onClick = {
                        onAddressSelected(address)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarrierSelection(
    selectedCarrier: String,
    onCarrierSelected: (String) -> Unit,
    customCarrierName: String,
    onCustomCarrierNameChanged: (String) -> Unit,
    registrationNumber: String,
    onRegistrationChanged: (String) -> Unit,
    validationState: FieldValidationState
) {
    val carriers = listOf("TRANSRotondo", "OKTrans", "NGK", "Inne")
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            CleanTextField(
                value = selectedCarrier,
                onValueChange = {},
                readOnly = true,
                label = "Wybierz przewoźnika",
                validationState = if (selectedCarrier.isEmpty()) FieldValidationState.EMPTY else FieldValidationState.VALID,
                modifier = Modifier.menuAnchor(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                carriers.forEach { carrier ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                carrier,
                                color = LabelPrintingTheme.DarkText
                            )
                        },
                        onClick = {
                            onCarrierSelected(carrier)
                            expanded = false
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = selectedCarrier == "Inne",
            enter = fadeIn(animationSpec = tween(LabelPrintingTheme.animationDuration)) + slideInVertically(),
            exit = fadeOut(animationSpec = tween(150)) + slideOutVertically()
        ) {
            CleanTextField(
                value = customCarrierName,
                onValueChange = onCustomCarrierNameChanged,
                label = "Nazwa przewoźnika",
                validationState = when {
                    selectedCarrier != "Inne" -> FieldValidationState.VALID
                    customCarrierName.isNotEmpty() -> FieldValidationState.VALID
                    else -> FieldValidationState.INVALID
                }
            )
        }

        AnimatedVisibility(
            visible = selectedCarrier == "NGK" || selectedCarrier == "Inne",
            enter = fadeIn(animationSpec = tween(LabelPrintingTheme.animationDuration)) + slideInVertically(),
            exit = fadeOut(animationSpec = tween(150)) + slideOutVertically()
        ) {
            CleanTextField(
                value = registrationNumber,
                onValueChange = onRegistrationChanged,
                label = "Numer rejestracyjny",
                validationState = if (registrationNumber.isNotEmpty()) FieldValidationState.VALID else FieldValidationState.EMPTY
            )
        }
    }
}