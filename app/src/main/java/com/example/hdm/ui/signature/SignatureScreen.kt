package com.example.hdm.ui.signature

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
// import androidx.compose.foundation.Canvas // USUNIĘTE
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
// import androidx.compose.foundation.gestures.detectDragGestures // USUNIĘTE
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
// import androidx.compose.ui.graphics.Path // USUNIĘTE
// import androidx.compose.ui.graphics.StrokeCap // USUNIĘTE
// import androidx.compose.ui.graphics.StrokeJoin // USUNIĘTE
// import androidx.compose.ui.graphics.asAndroidPath // USUNIĘTE
// import androidx.compose.ui.graphics.drawscope.Stroke // USUNIĘTE
// import androidx.compose.ui.input.pointer.pointerInput // USUNIĘTE
// import androidx.compose.ui.layout.onSizeChanged // USUNIĘTE
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
// import androidx.compose.ui.unit.toSize // USUNIĘTE
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle // <-- DODANY IMPORT
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.hdm.Screen
import com.example.hdm.model.PalletPosition // <-- DODANY IMPORT
import com.example.hdm.model.ReportHeader
import com.example.hdm.ui.header.HeaderViewModel
import com.example.hdm.ui.header.ProcessingStep
import com.example.hdm.ui.header.SubmissionState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ===== ZMIANA WALIDACJI: Usunięto stany związane z podpisem =====
enum class SignatureValidationState {
    EMPTY, VALID, INVALID, MISSING_NAME
}

data class SignatureValidation(
    val state: SignatureValidationState,
    val message: String = ""
)
// ==========================================================

object SignatureTheme {
    val ValidColor = Color(0xFF2E7D32)
    val InvalidColor = Color(0xFFD32F2F)
    val EmptyColor = Color(0xFF757575)
    val WarningColor = Color(0xFFFF9800)
    val InProgressColor = Color(0xFF1976D2)

    val ValidBackground = Color(0xFFF1F8F2)
    val InvalidBackground = Color(0xFFFFF3F3)
    val WarningBackground = Color(0xFFFFF3E0)
    val NeutralBackground = Color(0xFFFAFAFA)
    val InProgressBackground = Color(0xFFE3F2FD)

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

@Composable
private fun SignatureValidationIcon(
    validationState: SignatureValidationState,
    modifier: Modifier = Modifier
) {
    // ===== ZMIANA WALIDACJI: Usunięto MISSING_SIGNATURE =====
    val (icon, color) = when (validationState) {
        SignatureValidationState.VALID -> Icons.Default.CheckCircle to SignatureTheme.ValidColor
        SignatureValidationState.INVALID -> Icons.Default.Error to SignatureTheme.InvalidColor
        SignatureValidationState.MISSING_NAME -> Icons.Default.Person to SignatureTheme.WarningColor
        SignatureValidationState.EMPTY -> Icons.Default.PendingActions to SignatureTheme.EmptyColor
    }
    // ====================================================

    val scale by animateFloatAsState(
        targetValue = if (validationState == SignatureValidationState.VALID) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconScale"
    )

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = modifier
            .size(20.dp)
            .scale(scale)
    )
}

@Composable
private fun SignatureStatusCard(
    validation: SignatureValidation,
    modifier: Modifier = Modifier
) {
    // ===== ZMIANA WALIDACJI: Usunięto MISSING_SIGNATURE =====
    val backgroundColor by animateColorAsState(
        targetValue = when (validation.state) {
            SignatureValidationState.VALID -> SignatureTheme.ValidBackground
            SignatureValidationState.INVALID -> SignatureTheme.InvalidBackground
            SignatureValidationState.MISSING_NAME -> SignatureTheme.WarningBackground
            SignatureValidationState.EMPTY -> SignatureTheme.NeutralBackground
        },
        animationSpec = tween(SignatureTheme.animationDuration),
        label = "backgroundColor"
    )

    val textColor = when (validation.state) {
        SignatureValidationState.VALID -> SignatureTheme.ValidColor
        SignatureValidationState.INVALID -> SignatureTheme.InvalidColor
        SignatureValidationState.MISSING_NAME -> SignatureTheme.WarningColor
        SignatureValidationState.EMPTY -> SignatureTheme.LightText
    }
    // ====================================================

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(SignatureTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SignatureValidationIcon(validationState = validation.state)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = validation.message,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (validation.state == SignatureValidationState.VALID) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ProcessingDialog(state: SubmissionState, onDismiss: () -> Unit) {
    if (state is SubmissionState.Idle) return

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
                    is SubmissionState.Generating -> {
                        RealTimeProcessingContent(
                            currentStep = state.step,
                            progress = state.progress
                        )
                    }
                    // ===== ZMIANA: SaveToWaitingRoomSuccess -> FinalizeSuccess =====
                    is SubmissionState.FinalizeSuccess -> {
                        // ==============================================================
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
                            tint = SignatureTheme.ValidColor,
                            modifier = Modifier.size(64.dp).scale(scale)
                        )
                        Text(
                            "Raport wysłany pomyślnie!", // Zmieniono komunikat
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                    is SubmissionState.Error -> {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Błąd",
                            tint = SignatureTheme.InvalidColor,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            state.message,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = SignatureTheme.InvalidColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onDismiss) {
                            Text("Zamknij")
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun RealTimeProcessingContent(
    currentStep: ProcessingStep,
    progress: Float
) {
    // ===== ZMIANA: Nowe kroki processingu =====
    val steps = listOf(
        ProcessingStep.SENDING_COMMENTS to ("Wysyłanie opisu" to Icons.Default.Description),
        ProcessingStep.GENERATING_PDF to ("Generowanie PDF" to Icons.Default.PictureAsPdf),
        ProcessingStep.UPLOADING_PDF to ("Wysyłanie PDF" to Icons.Default.CloudUpload),
        ProcessingStep.VERIFYING_DATA to ("Weryfikacja danych" to Icons.Default.CheckCircle),
        ProcessingStep.FINALIZING to ("Finalizacja sesji" to Icons.Default.DoneAll)
    )
    // ==========================================

    val currentStepIndex = steps.indexOfFirst { it.first == currentStep }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(80.dp),
                strokeWidth = 6.dp,
                color = SignatureTheme.InProgressColor
            )

            if (currentStepIndex >= 0) {
                Icon(
                    steps[currentStepIndex].second.second,
                    contentDescription = null,
                    tint = SignatureTheme.InProgressColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Text(
            "Przetwarzanie raportu...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = SignatureTheme.DarkText
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            steps.forEachIndexed { index, (stepEnum, stepData) ->
                RealTimeProcessingStep(
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
                color = SignatureTheme.InProgressColor,
                trackColor = SignatureTheme.InProgressBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${(animatedProgress * 100).toInt()}% ukończono",
                style = MaterialTheme.typography.bodySmall,
                color = SignatureTheme.MediumText,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

// ... (RealTimeProcessingStep bez zmian) ...
@Composable
private fun RealTimeProcessingStep(
    stepName: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    isCompleted: Boolean
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isActive -> SignatureTheme.InProgressBackground
            isCompleted -> SignatureTheme.ValidBackground
            else -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "stepBg"
    )

    val iconColor by animateColorAsState(
        targetValue = when {
            isActive -> SignatureTheme.InProgressColor
            isCompleted -> SignatureTheme.ValidColor
            else -> SignatureTheme.LightText
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
                        isActive -> SignatureTheme.InProgressColor.copy(alpha = 0.2f)
                        isCompleted -> SignatureTheme.ValidColor.copy(alpha = 0.2f)
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
                isActive -> SignatureTheme.DarkText
                isCompleted -> SignatureTheme.ValidColor
                else -> SignatureTheme.LightText
            },
            modifier = Modifier.weight(1f)
        )

        if (isActive) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = SignatureTheme.InProgressColor
            )
        }
    }
}

// ===== NOWY KOMPONENT 1: ChecklistItem =====
@Composable
private fun ChecklistItem(
    isValid: Boolean,
    text: String,
    isOptional: Boolean = false,
    warning: Boolean = false
) {
    val icon = when {
        isValid -> Icons.Default.CheckCircle
        warning -> Icons.Default.Warning
        isOptional -> Icons.Default.Info
        else -> Icons.Default.Error
    }
    val color = when {
        isValid -> SignatureTheme.ValidColor
        warning -> SignatureTheme.WarningColor
        isOptional -> SignatureTheme.InProgressColor
        else -> SignatureTheme.InvalidColor
    }
    val textColor = when {
        isValid -> SignatureTheme.DarkText
        warning -> SignatureTheme.WarningColor
        isOptional -> SignatureTheme.MediumText
        else -> SignatureTheme.InvalidColor
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = if (!isValid && !isOptional) FontWeight.Medium else FontWeight.Normal
        )
    }
}

// ===== NOWY KOMPONENT 2: ReportChecklist (PRZEBUDOWANY) =====
@Composable
private fun ReportChecklist(
    reportHeader: ReportHeader,
    palletCount: Int,
    palletPositions: List<PalletPosition>,
    isNagoyaFlow: Boolean,
    placementRequired: Boolean
) {
    // --- Walidacja ---
    val magazynierValid = reportHeader.magazynier.isNotBlank()
    val miejsceValid = reportHeader.miejsce.isNotBlank()
    val lokalizacjaValid = reportHeader.lokalizacja.isNotBlank()
    val pojazdValid = reportHeader.rodzajSamochodu.isNotBlank()
    val typPaletValid = reportHeader.rodzajPalet.isNotBlank()
    val paletyValid = palletCount > 0
    val feniksValid = reportHeader.numerDokumentuFeniks.isNotBlank()
    val opisValid = reportHeader.opisZdarzenia.isNotBlank()

    // Czy wszystkie zapisane palety mają pozycję?
    // Ta walidacja jest aktywna tylko jeśli schemat jest wymagany I mamy > 0 palet
    val allPalletsPositioned = !placementRequired || !paletyValid || (palletPositions.size == palletCount)

    // --- Budowanie listy ---
    val checklistItems = mutableListOf<Triple<Boolean, String, Boolean>>() // isValid, text, isOptional

    checklistItems.add(
        Triple(
            magazynierValid,
            if (magazynierValid) "Magazynier: ${reportHeader.magazynier}" else "Brak magazyniera",
            false
        )
    )
    checklistItems.add(
        Triple(
            miejsceValid,
            if (miejsceValid) "Miejsce: ${reportHeader.miejsce}" else "Brak miejsca",
            false
        )
    )
    checklistItems.add(
        Triple(
            lokalizacjaValid,
            if (lokalizacjaValid) "Lokalizacja: ${reportHeader.lokalizacja}" else "Brak lokalizacji",
            false
        )
    )
    checklistItems.add(
        Triple(
            pojazdValid,
            if (pojazdValid) "Pojazd: ${reportHeader.rodzajSamochodu}" else "Brak rodzaju pojazdu",
            false
        )
    )
    checklistItems.add(
        Triple(
            typPaletValid,
            if (typPaletValid) "Rodzaj palet: ${reportHeader.rodzajPalet}" else "Brak rodzaju palet",
            false
        )
    )
    checklistItems.add(
        Triple(
            paletyValid,
            if (paletyValid) "Zapisano palet: $palletCount" else "Brak zapisanych palet",
            false
        )
    )

    if (placementRequired) {
        checklistItems.add(
            Triple(
                allPalletsPositioned,
                if (allPalletsPositioned) "Rozmieszczono palet: $palletCount/$palletCount"
                else "Nierozmieszczone palety: ${palletPositions.size}/$palletCount",
                false
            )
        )
    }

    if (isNagoyaFlow) {
        checklistItems.add(
            Triple(
                feniksValid,
                if (feniksValid) "Numer Feniks: ${reportHeader.numerDokumentuFeniks}" else "Brak numeru Feniks",
                false
            )
        )
    }

    checklistItems.add(
        Triple(
            opisValid,
            if (opisValid) "Dodano opis zdarzenia" else "Brak opisu zdarzenia (Opcjonalne)",
            true // Ostatni element jest opcjonalny
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SignatureTheme.cornerRadius),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Podsumowanie danych raportu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SignatureTheme.DarkText
            )

            // Wyświetl elementy
            checklistItems.forEach { (isValid, text, isOptional) ->
                ChecklistItem(
                    isValid = isValid,
                    text = text,
                    isOptional = isOptional,
                    // Dodatkowy warning dla nierozmieszczonych palet
                    warning = (text.contains("Nierozmieszczone") && !isValid)
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureScreen(
    navController: NavController,
    reportViewModel: HeaderViewModel
) {
    val reportHeader by reportViewModel.reportHeaderState.collectAsState()
    val isNagoyaFlow by reportViewModel.isNagoyaFlow.collectAsState()
    val submissionState by reportViewModel.submissionState.collectAsState()
    val savedPallets by reportViewModel.savedPallets.collectAsStateWithLifecycle()
    val palletPositions by reportViewModel.palletPositions.collectAsStateWithLifecycle()
    val strategy by reportViewModel.strategy.collectAsStateWithLifecycle()
    val placementRequired = strategy?.getPdfConfig()?.showPlacementSchematic == true

    LaunchedEffect(Unit) {
        reportViewModel.logoutAndNavigateToLogin.collect {
            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                }
            }
        }
    }

    // ===== CAŁA LOGIKA PODPISU USUNIĘTA =====
    // var path by remember { mutableStateOf(Path()) }
    // var canvasSize by remember { mutableStateOf<Size?>(null) }
    // ======================================

    if (submissionState !is SubmissionState.Idle) {
        ProcessingDialog(
            state = submissionState,
            onDismiss = { reportViewModel.resetSubmissionState() }
        )
    }

    LaunchedEffect(submissionState) {
        if (submissionState is SubmissionState.Error) {
            delay(3000)
            reportViewModel.resetSubmissionState()
        }
    }

    val signerName = reportHeader.magazynier
    val currentDateTime =
        remember { SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault()).format(Date()) }

    // ===== ZMIANA WALIDACJI: Usunięto 'hasSignature' =====
    val hasValidName = signerName.trim().isNotBlank()

    val isReportDataValid = if (isNagoyaFlow) {
        reportViewModel.isNagoyaReportReadyForGeneration()
    } else {
        reportViewModel.isReportReadyForGeneration()
    }

    // Pełna walidacja (dodana logika dla pozycji palet)
    val allCoreDataValid = hasValidName && isReportDataValid &&
            (!placementRequired || (savedPallets.isNotEmpty() && palletPositions.size == savedPallets.size))

    val signatureValidation = remember(hasValidName, isReportDataValid, allCoreDataValid) {
        when {
            !hasValidName -> SignatureValidation(
                state = SignatureValidationState.MISSING_NAME,
                message = "Magazynier nie został podany w nagłówku raportu"
            )
            !isReportDataValid -> SignatureValidation(
                state = SignatureValidationState.INVALID,
                message = "Raport nie jest kompletny, uzupełnij palety"
            )
            !allCoreDataValid && placementRequired -> SignatureValidation(
                state = SignatureValidationState.INVALID,
                message = "Nie wszystkie palety zostały rozmieszczone na schemacie"
            )
            else -> SignatureValidation(
                state = SignatureValidationState.VALID,
                message = "Gotowe do finalizacji raportu"
            )
        }
    }
    // ====================================================

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Podsumowanie i Finalizacja", color = SignatureTheme.DarkText) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (signatureValidation.state != SignatureValidationState.VALID) {
                    SignatureStatusCard(validation = signatureValidation)
                }

                Button(
                    onClick = {
                        reportViewModel.startFullReportUpload()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = signatureValidation.state == SignatureValidationState.VALID && submissionState is SubmissionState.Idle,
                    shape = RoundedCornerShape(SignatureTheme.cornerRadius)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Finalizuj i wyślij raport", // Zmieniono tekst
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(SignatureTheme.cornerRadius),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Postęp finalizacji", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(12.dp))
                    // ===== ZMIANA WALIDACJI: Usunięto 'hasSignature' =====
                    val completedItems = listOf(hasValidName, isReportDataValid, allCoreDataValid).count { it }
                    val totalItems = 3 // Zwiększono liczbę kroków
                    // ====================================================
                    val progress = completedItems.toFloat() / totalItems.toFloat()
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f).height(6.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    SignatureValidationIcon(validationState = signatureValidation.state)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasValidName) SignatureTheme.ValidBackground else SignatureTheme.InvalidBackground
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (hasValidName) Icons.Default.Person else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasValidName) SignatureTheme.ValidColor else SignatureTheme.InvalidColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Osoba zgłaszająca:", style = MaterialTheme.typography.bodySmall, color = SignatureTheme.MediumText)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (signerName.isNotBlank()) signerName else "Brak danych",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (hasValidName) SignatureTheme.DarkText else SignatureTheme.InvalidColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = SignatureTheme.MediumText, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Data finalizacji:", style = MaterialTheme.typography.bodySmall, color = SignatureTheme.MediumText)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = currentDateTime, style = MaterialTheme.typography.bodySmall, color = SignatureTheme.MediumText, fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (!hasValidName) {
                Text(
                    "⚠️ Wróć do nagłówka i uzupełnij/popraw magazyniera",
                    style = MaterialTheme.typography.bodySmall,
                    color = SignatureTheme.InvalidColor,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // ===== CAŁA SEKCJA PODPISU (Canvas, Przyciski) USUNIĘTA =====
            Spacer(modifier = Modifier.height(16.dp)) // <-- DODANY SPACER

            // vvvv NOWA ZAWARTOŚĆ (ZAKTUALIZOWANA) vvvv
            ReportChecklist(
                reportHeader = reportHeader,
                palletCount = savedPallets.size,
                palletPositions = palletPositions,
                isNagoyaFlow = isNagoyaFlow,
                placementRequired = placementRequired
            )
            // ^^^^ KONIEC NOWEJ ZAWARTOŚCI ^^^^

            Spacer(modifier = Modifier.height(16.dp))

            Text("Status raportu:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = SignatureTheme.DarkText, modifier = Modifier.padding(start = 8.dp))

            Text(
                "Po kliknięciu 'Finalizuj', aplikacja wygeneruje raporty PDF, wyśle je na serwer wraz z pozostałymi danymi i zamknie sesję.",
                style = MaterialTheme.typography.bodyMedium,
                color = SignatureTheme.MediumText,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

        }
    }
}