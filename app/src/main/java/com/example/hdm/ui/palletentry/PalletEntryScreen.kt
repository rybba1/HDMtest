package com.example.hdm.ui.palletentry

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.hdm.Screen
import com.example.hdm.model.DamageInfo
import com.example.hdm.model.DamageInstance
import com.example.hdm.model.DamagedPallet
import com.example.hdm.model.UploadStatus // Import kluczowy
import com.example.hdm.services.CodeValidator
import com.example.hdm.ui.header.BarcodeData
import com.example.hdm.ui.header.BarcodeFetchState
import com.example.hdm.ui.header.HeaderViewModel
import com.example.hdm.ui.header.SubmissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Objects
import java.util.UUID

enum class PalletValidationState {
    EMPTY, VALID, INVALID, WARNING
}

data class PalletFieldValidation(
    val state: PalletValidationState,
    val message: String = ""
)

object PalletTheme {
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

@Composable
private fun PalletValidationIcon(
    validationState: PalletValidationState,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (validationState) {
        PalletValidationState.VALID -> Icons.Default.Check to PalletTheme.ValidColor
        PalletValidationState.INVALID -> Icons.Default.ErrorOutline to PalletTheme.InvalidColor
        PalletValidationState.WARNING -> Icons.Default.Warning to PalletTheme.WarningColor
        PalletValidationState.EMPTY -> return
    }
    AnimatedVisibility(
        visible = validationState != PalletValidationState.EMPTY,
        enter = fadeIn(animationSpec = tween(PalletTheme.animationDuration)),
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
private fun CleanPalletTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    validationState: PalletValidationState,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    placeholder: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val borderColor by animateColorAsState(
        targetValue = when (validationState) {
            PalletValidationState.VALID -> PalletTheme.ValidBorder
            PalletValidationState.INVALID -> PalletTheme.InvalidBorder
            PalletValidationState.WARNING -> PalletTheme.WarningBorder
            PalletValidationState.EMPTY -> PalletTheme.NeutralBorder
        },
        animationSpec = tween(PalletTheme.animationDuration),
        label = "borderColor"
    )
    val backgroundColor by animateColorAsState(
        targetValue = when (validationState) {
            PalletValidationState.VALID -> PalletTheme.ValidBackground
            PalletValidationState.INVALID -> PalletTheme.InvalidBackground
            PalletValidationState.WARNING -> PalletTheme.WarningBackground
            PalletValidationState.EMPTY -> Color.White
        },
        animationSpec = tween(PalletTheme.animationDuration),
        label = "backgroundColor"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (enabled) backgroundColor else PalletTheme.NeutralBackground.copy(alpha = 0.5f),
                shape = RoundedCornerShape(PalletTheme.cornerRadius)
            )
            .border(
                width = PalletTheme.borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(PalletTheme.cornerRadius)
            )
            .clip(RoundedCornerShape(PalletTheme.cornerRadius))
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = PalletTheme.MediumText) },
            placeholder = placeholder?.let { { Text(it, color = PalletTheme.LightText) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                errorBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                focusedTextColor = PalletTheme.DarkText,
                unfocusedTextColor = PalletTheme.DarkText,
                disabledTextColor = PalletTheme.LightText,
                focusedLabelColor = PalletTheme.MediumText,
                unfocusedLabelColor = PalletTheme.LightText
            ),
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PalletValidationIcon(validationState = validationState)
                    trailingIcon?.invoke()
                }
            }
        )
    }
}

@Composable
private fun PalletProgressCard(
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
            containerColor = PalletTheme.NeutralBackground
        ),
        shape = RoundedCornerShape(PalletTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Postęp palety",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PalletTheme.DarkText
                )
                Text(
                    text = "$completedSteps/$totalSteps",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (completedSteps == totalSteps) PalletTheme.ValidColor else PalletTheme.DarkText
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (progress >= 1f) PalletTheme.ValidColor else MaterialTheme.colorScheme.primary,
                trackColor = Color(0xFFE0E0E0)
            )
        }
    }
}

@Composable
private fun CleanImageCard(
    imagePath: String?,
    title: String,
    buttonText: String,
    isRequired: Boolean = true,
    onTakePhoto: () -> Unit,
    enabled: Boolean = true,
    uploadStatus: UploadStatus, // NOWY PARAMETR
    modifier: Modifier = Modifier
) {
    val validationState = when {
        imagePath != null && uploadStatus == UploadStatus.SUCCESS -> PalletValidationState.VALID
        uploadStatus == UploadStatus.UPLOADING -> PalletValidationState.WARNING
        uploadStatus == UploadStatus.FAILED -> PalletValidationState.INVALID
        isRequired -> PalletValidationState.EMPTY
        else -> PalletValidationState.EMPTY
    }

    val cardColor by animateColorAsState(
        targetValue = when (validationState) {
            PalletValidationState.VALID -> PalletTheme.ValidBackground
            PalletValidationState.INVALID -> PalletTheme.InvalidBackground
            PalletValidationState.WARNING -> PalletTheme.WarningBackground // Użyjemy warning dla UPLOADING
            PalletValidationState.EMPTY -> Color.White
        },
        animationSpec = tween(PalletTheme.animationDuration),
        label = "cardColor"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) cardColor else PalletTheme.NeutralBackground.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(PalletTheme.cornerRadius),
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
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = PalletTheme.DarkText
                )
                // Zmieniona ikona walidacji na nowy spinner/error
                LoadingSpinnerOrErrorIcon(
                    uploadStatus = uploadStatus,
                    onRetry = onTakePhoto // Ponowne kliknięcie przycisku "Zrób zdjęcie" będzie ponowieniem
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (imagePath != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = Uri.fromFile(File(imagePath))),
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, PalletTheme.NeutralBorder, RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = onTakePhoto,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled && uploadStatus != UploadStatus.UPLOADING, // Zablokuj przycisk podczas wysyłania
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        uploadStatus == UploadStatus.FAILED -> PalletTheme.InvalidColor
                        imagePath != null -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    }
                ),
                shape = RoundedCornerShape(PalletTheme.cornerRadius)
            ) {
                val icon = when (uploadStatus) {
                    UploadStatus.FAILED -> Icons.Default.SyncProblem
                    else -> Icons.Default.CameraAlt
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    when (uploadStatus) {
                        UploadStatus.FAILED -> "Spróbuj wysłać ponownie"
                        else -> buttonText
                    }
                )
            }
        }
    }
}

private val damageTypeMap = mapOf(
    "Folia" to listOf("Przecięcie / Rozerwanie", "Wilgoć", "Dziura", "Otarcia", "Inne (opis)"),
    "Karton" to listOf("Dziura", "Zgniecenie", "Rozerwanie/Pęknięcie", "Wilgoć", "Otarcia", "Inne (opis)"),
    "Paleta" to listOf("Pęknięcie", "Brak elementów", "Wystające elementy", "Pleśń/Zabrudzenia", "Brak/Uszkodzenie oznaczeń", "Inne (opis)"),
    "Big bag" to listOf("Dziura", "Przetarcie", "Przecięcie", "Brak/Uszkodzenie oznaczeń/", "Zamoczenie", "Brak/Uszkodzenie slipsheet", "Inne (opis)"),
    "Skrzynie drewniane i pojemniki metalowe" to listOf("Wgniecenie", "Dziura", "Otarcia", "Brak elementów", "Zamoczenie", "Inne (opis)"),
    "Inne" to listOf("Inne (opis)")
)

// Funkcja isPristine() jest teraz w modelu DamagedPallet.kt

@Composable
private fun SavingIndicator(
    submissionState: SubmissionState,
    modifier: Modifier = Modifier
) {
    // Ten wskaźnik pokazuje teraz tylko stan FINALIZACJI (wysyłania JSON)
    AnimatedVisibility(
        visible = submissionState is SubmissionState.InProgress || submissionState is SubmissionState.Error,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        val (icon, text, color) = when (submissionState) {
            is SubmissionState.InProgress -> Triple(null, submissionState.message, MaterialTheme.colorScheme.primary)
            is SubmissionState.Error -> Triple(Icons.Default.Error, submissionState.message, MaterialTheme.colorScheme.error)
            else -> Triple(null, "", Color.Transparent)
        }

        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(PalletTheme.cornerRadius),
            border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = color)
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = color, strokeWidth = 2.dp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text, color = color, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ===== NOWY KOMPONENT: Wskaźnik wysyłania w tle =====
@Composable
private fun BackgroundUploadIndicator(
    pallet: DamagedPallet,
    modifier: Modifier = Modifier
) {
    val failedCount = pallet.getFailedPhotoCount()
    // ===== POCZĄTEK POPRAWKI (getUploadingPhotoCount -> isAnyPhotoUploading) =====
    val isUploading = pallet.isAnyPhotoUploading()

    AnimatedVisibility(
        visible = failedCount > 0 || isUploading,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        val (icon, text, color) = when {
            isUploading -> Triple( // Zmiana z uploadingCount > 0
                null,
                "Wysyłanie zdjęć w tle...", // Uproszczona wiadomość
                PalletTheme.WarningColor
            )
            failedCount > 0 -> Triple(
                Icons.Default.SyncProblem,
                "Nie udało się wysłać $failedCount zdjęć. Sprawdź sieć i spróbuj zapisać ponownie.",
                PalletTheme.InvalidColor
            )
            else -> Triple(null, "", Color.Transparent)
        }
        // ===== KONIEC POPRAWKI =====

        if (text.isNotEmpty()) {
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(PalletTheme.cornerRadius),
                border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = color)
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = color, strokeWidth = 2.dp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text, color = color, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
// ===============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalletEntryScreen(
    navController: NavController,
    reportViewModel: HeaderViewModel,
    palletId: String?
) {
    val currentPallet by reportViewModel.currentPalletState.collectAsState()
    val reportHeader by reportViewModel.reportHeaderState.collectAsState()
    val savedPallets by reportViewModel.savedPallets.collectAsState()
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var confirmationDialogText by remember { mutableStateOf("") }

    val submissionState by reportViewModel.submissionState.collectAsStateWithLifecycle()

    val isUploadingPhotos = currentPallet.isAnyPhotoUploading()
    val isFinalizing = submissionState is SubmissionState.InProgress
    // ===== POPRAWKA: 'isBusy' JEST UŻYWANE TYLKO DO DOLNYCH PRZYCISKÓW =====
    val isBusy = isUploadingPhotos || isFinalizing
    // ====================================================================

    val PALLET_SCAN_RESULT_KEY = "pallet_scan_result"

    fun checkDuplicatePalletNumber(
        currentPalletNumber: String,
        currentPalletId: String,
        savedPallets: List<DamagedPallet>
    ): Boolean {
        if (currentPalletNumber.isBlank()) return false
        return savedPallets.any { pallet ->
            pallet.id != currentPalletId &&
                    !pallet.brakNumeruPalety &&
                    pallet.numerPalety.equals(currentPalletNumber, ignoreCase = true)
        }
    }

    LaunchedEffect(key1 = palletId) {
        if (palletId != null) {
            reportViewModel.loadPalletForEditing(palletId)
        } else {
            val isStaleData = savedPallets.any { it.id == currentPallet.id && it.serverIndex != null }
            if (isStaleData || currentPallet.serverIndex != null) {
                reportViewModel.resetCurrentPallet()
            }
        }
    }

    // Nawigacja po pomyślnym zapisie (dla przycisku "Zakończ")
    LaunchedEffect(key1 = Unit) {
        reportViewModel.navigateToSummary.collect {
            navController.navigate(Screen.PalletSummary.route)
        }
    }

    var tempImageFile by rememberSaveable { mutableStateOf<File?>(null) }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val scannedValue by savedStateHandle
        ?.getStateFlow<String?>(PALLET_SCAN_RESULT_KEY, null)
        ?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }

    LaunchedEffect(scannedValue, savedStateHandle) {
        if (scannedValue != null) {
            reportViewModel.onPalletNumberChange(scannedValue!!)
            savedStateHandle?.remove<String>(PALLET_SCAN_RESULT_KEY)
        }
    }

    val coreFieldValidations = remember(currentPallet, savedPallets, reportHeader.rodzajPalet) {
        mapOf(
            "palletNumber" to when {
                currentPallet.numerPalety.isBlank() -> PalletFieldValidation(
                    state = PalletValidationState.EMPTY,
                    message = if (reportHeader.rodzajPalet == "Wyrób gotowy") "Podaj numer nośnika palety" else "Podaj numer palety"
                )
                checkDuplicatePalletNumber(currentPallet.numerPalety, currentPallet.id, savedPallets) -> PalletFieldValidation(
                    state = PalletValidationState.INVALID,
                    message = "Taki numer palety już istnieje w raporcie"
                )
                !CodeValidator.isPalletNumberValid(currentPallet.numerPalety, reportHeader.rodzajPalet) -> PalletFieldValidation(
                    state = PalletValidationState.INVALID,
                    message = "Niepoprawny format numeru palety dla typu '${reportHeader.rodzajPalet}'"
                )
                else -> PalletFieldValidation(
                    state = PalletValidationState.VALID,
                    message = "Numer palety podany"
                )
            },
            "labelPhoto" to PalletFieldValidation(
                state = if (currentPallet.zdjecieDuzejEtykietyUri != null) PalletValidationState.VALID else PalletValidationState.EMPTY,
                message = if (currentPallet.zdjecieDuzejEtykietyUri != null) "Zdjęcie etykiety dodane" else "Zrób zdjęcie etykiety"
            ),
            "damages" to PalletFieldValidation(
                state = when {
                    currentPallet.damageInstances.isEmpty() -> PalletValidationState.EMPTY
                    currentPallet.damageInstances.all { damageInstance ->
                        damageInstance.details.isNotEmpty() &&
                                damageInstance.details.any { detail ->
                                    detail.types.any { type -> type.size.isNotBlank() }
                                }
                    } -> PalletValidationState.VALID
                    else -> PalletValidationState.INVALID
                },
                message = when {
                    currentPallet.damageInstances.isEmpty() -> "Dodaj przynajmniej jedno uszkodzenie"
                    else -> "Opisz uszkodzenia i podaj rozmiary"
                }
            ),
            "wholePalletPhoto" to PalletFieldValidation(
                state = if (currentPallet.zdjecieCalejPaletyUri != null) PalletValidationState.VALID else PalletValidationState.EMPTY,
                message = if (currentPallet.zdjecieCalejPaletyUri != null) "Zdjęcie całej palety dodane" else "Zrób zdjęcie całej palety"
            )
        )
    }

    val conditionalFieldValidations = remember(currentPallet.rodzajTowaru, currentPallet.numerLotu, currentPallet.brakNumeruLotu, reportHeader.rodzajPalet) {
        mapOf(
            "productType" to when {
                reportHeader.rodzajPalet !in listOf("Surowiec", "Inne") -> PalletFieldValidation(
                    state = PalletValidationState.VALID,
                    message = "Nie wymagane"
                )
                currentPallet.rodzajTowaru.isNullOrBlank() -> PalletFieldValidation(
                    state = PalletValidationState.EMPTY,
                    message = "Podaj rodzaj towaru"
                )
                currentPallet.rodzajTowaru!!.length < 3 -> PalletFieldValidation(
                    state = PalletValidationState.WARNING,
                    message = "Bardzo krótka nazwa"
                )
                else -> PalletFieldValidation(
                    state = PalletValidationState.VALID,
                    message = "Rodzaj towaru podany"
                )
            },
            "lotNumber" to when {
                reportHeader.rodzajPalet !in listOf("Surowiec", "Inne") -> PalletFieldValidation(
                    state = PalletValidationState.VALID,
                    message = "Nie wymagane"
                )
                currentPallet.brakNumeruLotu -> PalletFieldValidation(
                    state = PalletValidationState.VALID,
                    message = "Zaznaczono brak numeru LOT"
                )
                currentPallet.numerLotu.isNullOrBlank() -> PalletFieldValidation(
                    state = PalletValidationState.EMPTY,
                    message = "Podaj numer LOT lub zaznacz brak"
                )
                else -> PalletFieldValidation(
                    state = PalletValidationState.VALID,
                    message = "Numer LOT podany"
                )
            }
        )
    }

    val completedCoreFields = coreFieldValidations.values.count { it.state == PalletValidationState.VALID }
    val totalCoreFields = coreFieldValidations.size

    val isCurrentPalletComplete = coreFieldValidations.values.all { it.state == PalletValidationState.VALID } &&
            conditionalFieldValidations.values.all { it.state != PalletValidationState.INVALID && it.state != PalletValidationState.EMPTY }

    val palletNumberValidation = coreFieldValidations["palletNumber"]!!
    val rodzajTowaruValidation = conditionalFieldValidations["productType"]!!
    val numerLotuValidation = conditionalFieldValidations["lotNumber"]!!

    val barcodeFetchState by reportViewModel.barcodeFetchState.collectAsStateWithLifecycle()
    val isFetchingApiData = barcodeFetchState is BarcodeFetchState.Loading

    // ===== ZMIANA: Launchery wywołują `uploadPhotoInBackground` =====
    val cameraPermissionLauncherPallet = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val palletType = reportHeader.rodzajPalet
                if (palletType.isNotBlank()) {
                    navController.navigate(
                        Screen.Scanner.withArgs(
                            scanType = "VALIDATE_PALLET_BY_TYPE",
                            returnKey = PALLET_SCAN_RESULT_KEY,
                            validationContext = palletType
                        )
                    )
                } else {
                    Toast.makeText(context, "Najpierw wybierz rodzaj towaru w nagłówku.", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.d("PalletEntryScreen", "Odmówiono uprawnień do kamery")
            }
        }
    )

    val takeLabelPictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK && tempImageFile != null) {
                val path = tempImageFile!!.absolutePath
                reportViewModel.updatePalletZdjecieEtykiety(path) // Najpierw aktualizuj UI
                reportViewModel.uploadPhotoInBackground( // Potem wyślij w tle
                    pathString = path,
                    imageType = "PhotoLabel",
                    damageInstanceId = null
                )
            }
        }
    )

    val takeDamagePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK && tempImageFile != null) {
                val path = tempImageFile!!.absolutePath
                // ZMIANA: `addDamageInstance` teraz tylko dodaje lokalnie i zwraca ID
                val newInstanceId = reportViewModel.addDamageInstance(path)
                // Używamy zwróconego ID do wysłania zdjęcia
                reportViewModel.uploadPhotoInBackground(
                    pathString = path,
                    imageType = "PhotosDamage",
                    damageInstanceId = newInstanceId
                )
            }
        }
    )

    val takeWholePalletPictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK && tempImageFile != null) {
                val path = tempImageFile!!.absolutePath
                reportViewModel.updatePalletZdjecieCalejPalety(path) // Najpierw aktualizuj UI
                reportViewModel.uploadPhotoInBackground( // Potem wyślij w tle
                    pathString = path,
                    imageType = "PhotosOverview",
                    damageInstanceId = null
                )
            }
        }
    )
    // =========================================================

    LaunchedEffect(currentPallet.damageInstances.size) {
        if (currentPallet.damageInstances.isNotEmpty() && lazyListState.layoutInfo.totalItemsCount > 0) {
            delay(300)
            lazyListState.animateScrollToItem(index = lazyListState.layoutInfo.totalItemsCount - 1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Wprowadzanie Danych Palety",
                        fontWeight = FontWeight.Bold,
                        color = PalletTheme.DarkText
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
            if (showConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmationDialog = false },
                    title = { Text("Potwierdzenie") },
                    text = { Text(confirmationDialogText) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showConfirmationDialog = false
                                reportViewModel.resetCurrentPallet()
                                navController.navigate(Screen.PalletSummary.route)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PalletTheme.InvalidColor)
                        ) {
                            Text("Kontynuuj mimo to")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmationDialog = false }) {
                            Text("Anuluj")
                        }
                    }
                )
            }

            if (barcodeFetchState is BarcodeFetchState.RequiresSelection) {
                BarcodeDataSelectionDialog(
                    options = (barcodeFetchState as BarcodeFetchState.RequiresSelection).options,
                    onDismiss = { reportViewModel.clearBarcodeFetchState() },
                    onSelect = { selectedDataList ->
                        reportViewModel.applyBarcodeDataSelection(selectedDataList)
                        Toast.makeText(context, "Zastosowano ${selectedDataList.size} alokacji", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            if (barcodeFetchState is BarcodeFetchState.Success) {
                LaunchedEffect(barcodeFetchState) {
                    Toast.makeText(context, "Pobrano dane: ${(barcodeFetchState as BarcodeFetchState.Success).data.rodzajTowaru}", Toast.LENGTH_SHORT).show()
                    delay(2000)
                    reportViewModel.clearBarcodeFetchState()
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(PalletTheme.cornerRadius),
                border = CardDefaults.outlinedCardBorder()
            ) {
                val palletIndex = remember(palletId, savedPallets, currentPallet.serverIndex) {
                    val p = savedPallets.find { it.id == palletId }
                    p?.serverIndex ?: currentPallet.serverIndex
                }
                val titleText = if (palletIndex != null) {
                    "Edycja Palety ${palletIndex + 1}"
                } else {
                    "Paleta ${savedPallets.size + 1}"
                }
                val subtitleText = "Zapisano: ${savedPallets.size}"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PalletTheme.DarkText
                    )
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PalletTheme.MediumText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            PalletProgressCard(
                completedSteps = completedCoreFields,
                totalSteps = totalCoreFields
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                // ===== POPRAWKA: Używamy !isFinalizing =====
                userScrollEnabled = !isFinalizing
                // ==========================================
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(PalletTheme.cornerRadius),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                if (reportHeader.rodzajPalet == "Wyrób gotowy") "Numer nośnika palety" else "Numer palety",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = PalletTheme.DarkText
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CleanPalletTextField(
                                    value = currentPallet.numerPalety,
                                    onValueChange = { reportViewModel.onPalletNumberChange(it) },
                                    label = "Wpisz lub zeskanuj numer",
                                    validationState = palletNumberValidation.state,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    // ===== POPRAWKA: Używamy !isFinalizing =====
                                    enabled = !currentPallet.brakNumeruPalety && !isFinalizing,
                                    // ==========================================
                                    trailingIcon = {
                                        if (isFetchingApiData) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        }
                                    }
                                )

                                Button(
                                    onClick = {
                                        val palletType = reportHeader.rodzajPalet
                                        if (palletType.isNotBlank()) {
                                            when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                                                PackageManager.PERMISSION_GRANTED -> {
                                                    navController.navigate(
                                                        Screen.Scanner.withArgs(
                                                            scanType = "VALIDATE_PALLET_BY_TYPE",
                                                            returnKey = PALLET_SCAN_RESULT_KEY,
                                                            validationContext = palletType
                                                        )
                                                    )
                                                }
                                                else -> cameraPermissionLauncherPallet.launch(Manifest.permission.CAMERA)
                                            }
                                        } else {
                                            Toast.makeText(context, "Najpierw wybierz rodzaj towaru w nagłówku.", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    // ===== POPRAWKA: Używamy !isFinalizing =====
                                    enabled = !currentPallet.brakNumeruPalety && !isFinalizing,
                                    // ==========================================
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(PalletTheme.cornerRadius),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = "Skanuj",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Skanuj")
                                }
                            }

                            if (barcodeFetchState is BarcodeFetchState.Error) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = PalletTheme.WarningColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = (barcodeFetchState as BarcodeFetchState.Error).message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PalletTheme.WarningColor
                                    )
                                }
                            }
                            if (palletNumberValidation.state == PalletValidationState.INVALID ||
                                (palletNumberValidation.state == PalletValidationState.EMPTY && !currentPallet.brakNumeruPalety)) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = PalletTheme.InvalidColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = palletNumberValidation.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PalletTheme.InvalidColor
                                    )
                                }
                            }
                        }
                    }
                }

                if (reportHeader.rodzajPalet == "Surowiec" || reportHeader.rodzajPalet == "Inne") {
                    item {
                        val rawMaterialSuggestions by reportViewModel.rawMaterialSuggestions.collectAsState()
                        var isDropdownExpanded by remember { mutableStateOf(false) }

                        LaunchedEffect(rawMaterialSuggestions) {
                            isDropdownExpanded = rawMaterialSuggestions.isNotEmpty()
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(PalletTheme.cornerRadius),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Szczegóły towaru (dla Surowca/Inne)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = PalletTheme.DarkText
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                ExposedDropdownMenuBox(
                                    // ===== POPRAWKA: Używamy !isFinalizing =====
                                    expanded = isDropdownExpanded && !isFinalizing,
                                    // ==========================================
                                    onExpandedChange = {}
                                ) {
                                    CleanPalletTextField(
                                        value = currentPallet.rodzajTowaru ?: "",
                                        onValueChange = { reportViewModel.updatePalletRodzajTowaru(it) },
                                        label = "Rodzaj towaru",
                                        placeholder = "Wpisz pierwsze 2 litery symbolu towaru",
                                        validationState = rodzajTowaruValidation.state,
                                        modifier = Modifier.menuAnchor(),
                                        singleLine = false,
                                        // ===== POPRAWKA: Używamy !isFinalizing =====
                                        enabled = !isFetchingApiData && !isFinalizing
                                        // ==========================================
                                    )

                                    ExposedDropdownMenu(
                                        // ===== POPRAWKA: Używamy !isFinalizing =====
                                        expanded = isDropdownExpanded && !isFinalizing,
                                        // ==========================================
                                        onDismissRequest = { reportViewModel.clearRawMaterialSuggestions() }
                                    ) {
                                        rawMaterialSuggestions.forEach { suggestion ->
                                            DropdownMenuItem(
                                                text = { Text(suggestion) },
                                                onClick = {
                                                    reportViewModel.updatePalletRodzajTowaru(suggestion)
                                                    reportViewModel.clearRawMaterialSuggestions()
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                CleanPalletTextField(
                                    value = currentPallet.numerLotu ?: "",
                                    onValueChange = { reportViewModel.updatePalletNumerLotu(it) },
                                    label = "Nr lotu (opcjonalnie)",
                                    validationState = numerLotuValidation.state,
                                    singleLine = false,
                                    // ===== POPRAWKA: Używamy !isFinalizing =====
                                    enabled = !currentPallet.brakNumeruLotu && !isFetchingApiData && !isFinalizing
                                    // ==========================================
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                AnimatedVisibility(
                                    visible = currentPallet.numerLotu.isNullOrBlank(),
                                    enter = fadeIn(animationSpec = tween(PalletTheme.animationDuration)) + slideInVertically(),
                                    exit = fadeOut(animationSpec = tween(150)) + slideOutVertically()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            // ===== POPRAWKA: Używamy !isFinalizing =====
                                            .clickable(enabled = !isFetchingApiData && !isFinalizing) { reportViewModel.updateBrakNumeruLotu(!currentPallet.brakNumeruLotu) }
                                            // ==========================================
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = currentPallet.brakNumeruLotu,
                                            onCheckedChange = { reportViewModel.updateBrakNumeruLotu(it) },
                                            // ===== POPRAWKA: Używamy !isFinalizing =====
                                            enabled = !isFetchingApiData && !isFinalizing
                                            // ==========================================
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Brak Numeru LOT",
                                            color = PalletTheme.DarkText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    CleanImageCard(
                        imagePath = currentPallet.zdjecieDuzejEtykietyUri,
                        title = "Zdjęcie etykiety palety/towaru",
                        buttonText = if (currentPallet.zdjecieDuzejEtykietyUri != null) "Zmień zdjęcie etykiety" else "Zrób zdjęcie etykiety",
                        isRequired = true,
                        onTakePhoto = {
                            val imageFile = createImageFile(context, "label_")
                            if (imageFile != null) {
                                tempImageFile = imageFile
                                val imageUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    imageFile
                                )
                                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                                    putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                                    putExtra("android.intent.extras.CAMERA_FACING", 0)
                                }
                                takeLabelPictureLauncher.launch(intent)
                            }
                        },
                        // ===== POPRAWKA: Używamy !isFinalizing =====
                        enabled = !isFinalizing,
                        // ==========================================
                        uploadStatus = currentPallet.zdjecieDuzejEtykietyUploadStatus
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(PalletTheme.cornerRadius),
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
                                    "Zarejestrowane uszkodzenia",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = PalletTheme.DarkText
                                )
                                if (currentPallet.damageInstances.isNotEmpty()) {
                                    PalletValidationIcon(validationState = PalletValidationState.VALID)
                                }
                            }
                            if (currentPallet.damageInstances.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Brak dodanych uszkodzeń",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PalletTheme.LightText
                                )
                            }
                        }
                    }
                }

                items(currentPallet.damageInstances, key = { it.id }) { damageInstance ->
                    CleanDamageInstanceCard(
                        instance = damageInstance,
                        viewModel = reportViewModel,
                        modifier = Modifier.padding(vertical = 4.dp),
                        // ===== POPRAWKA: Używamy !isFinalizing =====
                        isEnabled = !isFinalizing,
                        // ==========================================
                        uploadStatus = damageInstance.uploadStatus,
                        onRetryUpload = {
                            reportViewModel.uploadPhotoInBackground(
                                pathString = damageInstance.photoUri,
                                imageType = "PhotosDamage",
                                damageInstanceId = damageInstance.id
                            )
                        }
                    )
                }

                if (currentPallet.damageInstances.size < 4) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentPallet.damageInstances.isEmpty())
                                    PalletTheme.WarningBackground
                                else
                                    PalletTheme.NeutralBackground
                            ),
                            shape = RoundedCornerShape(PalletTheme.cornerRadius),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (currentPallet.damageInstances.isEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = PalletTheme.WarningColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Musisz dodać przynajmniej jedno zdjęcie uszkodzenia",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = PalletTheme.WarningColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                Button(
                                    onClick = {
                                        val imageFile = createImageFile(context, "damage_")
                                        if (imageFile != null) {
                                            tempImageFile = imageFile
                                            val imageUri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                imageFile
                                            )
                                            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                                                putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                                                putExtra("android.intent.extras.CAMERA_FACING", 0)
                                            }
                                            takeDamagePictureLauncher.launch(intent)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    // ===== POPRAWKA: Używamy !isFinalizing =====
                                    enabled = !isFinalizing,
                                    // ==========================================
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (currentPallet.damageInstances.isEmpty())
                                            PalletTheme.WarningColor
                                        else
                                            MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(PalletTheme.cornerRadius)
                                ) {
                                    Icon(
                                        Icons.Default.AddAPhoto,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Dodaj zdjęcie uszkodzenia (${currentPallet.damageInstances.size}/4)")
                                }
                            }
                        }
                    }
                }

                if (currentPallet.damageInstances.isNotEmpty()) {
                    item {
                        CleanImageCard(
                            imagePath = currentPallet.zdjecieCalejPaletyUri,
                            title = "Zdjęcie całej palety",
                            buttonText = if (currentPallet.zdjecieCalejPaletyUri != null) "Zmień zdjęcie całej palety" else "Zrób zdjęcie całej palety",
                            isRequired = true,
                            onTakePhoto = {
                                val imageFile = createImageFile(context, "whole_pallet_")
                                if (imageFile != null) {
                                    tempImageFile = imageFile
                                    val imageUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        imageFile
                                    )
                                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                                        putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                                        putExtra("android.intent.extras.CAMERA_FACING", 0)
                                    }
                                    takeWholePalletPictureLauncher.launch(intent)
                                }
                            },
                            // ===== POPRAWKA: Używamy !isFinalizing =====
                            enabled = !isFinalizing,
                            // ==========================================
                            uploadStatus = currentPallet.zdjecieCalejPaletyUploadStatus
                        )
                    }
                }
            } // Koniec LazyColumn

            Spacer(modifier = Modifier.height(16.dp))

            // Wskaźnik finalizacji (wysyłania JSON)
            SavingIndicator(submissionState = submissionState)

            // Wskaźnik wysyłania zdjęć w tle
            BackgroundUploadIndicator(pallet = currentPallet)

            Spacer(modifier = Modifier.height(8.dp))

            val isFormValid = isCurrentPalletComplete
            val isPristine = currentPallet.isPristine()
            val isError = submissionState is SubmissionState.Error
            val hasSavedPallets = savedPallets.isNotEmpty()

            Button(
                onClick = {
                    if (isError) {
                        reportViewModel.saveAndPrepareNextPallet(isFormValid) // Ponów zapis
                    } else if (isFormValid) {
                        reportViewModel.saveAndPrepareNextPallet(isFormValid) // Zapisz
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(0)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isFormValid && !isBusy, // Włączony tylko, gdy forma jest OK i nic się nie dzieje
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isError -> PalletTheme.InvalidColor
                        isFormValid -> PalletTheme.ValidColor
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (isFormValid || isError) Color.White else PalletTheme.LightText
                ),
                shape = RoundedCornerShape(PalletTheme.cornerRadius)
            ) {
                val icon = when {
                    isError -> Icons.Default.SyncProblem
                    isFormValid -> Icons.Default.Add
                    else -> Icons.Default.Add
                }
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))

                val text = when {
                    isError -> "Spróbuj zapisać ponownie"
                    isFormValid -> "Zapisz i dodaj następną"
                    else -> "Uzupełnij pola"
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    when {
                        isPristine && hasSavedPallets -> {
                            // Formularz jest pusty, ale są zapisane palety - po prostu wyjdź
                            reportViewModel.resetCurrentPallet()
                            navController.navigate(Screen.PalletSummary.route)
                        }
                        isFormValid -> {
                            // Formularz jest kompletny, zapisz i wyjdź
                            reportViewModel.saveAndExitToSummary(isFormValid)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = ((isPristine && hasSavedPallets) || isFormValid) && !isBusy, // Aktywny gdy (pusty I są zapisane palety) LUB kompletny
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isPristine && hasSavedPallets -> MaterialTheme.colorScheme.secondary
                        isFormValid -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = when {
                        (isPristine && hasSavedPallets) || isFormValid -> Color.White
                        else -> PalletTheme.LightText
                    }
                ),
                shape = RoundedCornerShape(PalletTheme.cornerRadius)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isPristine && hasSavedPallets -> "Pomiń i zobacz podsumowanie"
                        isFormValid -> "Zapisz i zobacz podsumowanie"
                        !hasSavedPallets -> "Dodaj przynajmniej jedną paletę"
                        else -> "Uzupełnij wszystkie pola"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CleanDamageInstanceCard(
    instance: DamageInstance,
    viewModel: HeaderViewModel,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    uploadStatus: UploadStatus, // NOWY PARAMETR
    onRetryUpload: () -> Unit // NOWY PARAMETR
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (uploadStatus) {
                UploadStatus.SUCCESS -> PalletTheme.ValidBackground
                UploadStatus.FAILED -> PalletTheme.InvalidBackground
                UploadStatus.UPLOADING -> PalletTheme.WarningBackground
                else -> Color.White
            }
        ),
        shape = RoundedCornerShape(PalletTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = rememberAsyncImagePainter(model = Uri.fromFile(File(instance.photoUri))),
                    contentDescription = "Zdjęcie uszkodzenia",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, PalletTheme.NeutralBorder, RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                // Przycisk usuwania - w górnym prawym rogu
                IconButton(
                    onClick = { viewModel.removeDamageInstance(instance.id) },
                    enabled = isEnabled,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Usuń zdjęcie",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Ikonka statusu - w górnym lewym rogu
                if (uploadStatus == UploadStatus.SUCCESS) {
                    val scale by rememberInfiniteTransition(label = "successPulse").animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .scale(scale)
                            .background(
                                PalletTheme.ValidColor,
                                shape = CircleShape
                            )
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Wysłano",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Overlay dla statusu wysyłania - używamy Crossfade zamiast AnimatedVisibility w Box
                androidx.compose.animation.Crossfade(
                    targetState = uploadStatus == UploadStatus.UPLOADING || uploadStatus == UploadStatus.FAILED,
                    label = "uploadStatusOverlay",
                    modifier = Modifier.matchParentSize()
                ) { showOverlay ->
                    if (showOverlay) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uploadStatus == UploadStatus.UPLOADING) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color.White)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Wysyłanie...", color = Color.White)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.SyncProblem, contentDescription = "Błąd", tint = PalletTheme.InvalidColor, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Błąd wysyłania", color = PalletTheme.InvalidColor, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(onClick = onRetryUpload, colors = ButtonDefaults.buttonColors(containerColor = PalletTheme.InvalidColor)) {
                                        Text("Ponów")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Czego dotyczy uszkodzenie na tym zdjęciu?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = PalletTheme.DarkText
            )

            Spacer(modifier = Modifier.height(8.dp))

            damageTypeMap.keys.forEach { categoryName ->
                val isCategorySelected = instance.details.any { it.category == categoryName }
                val selectedCategoryDetail = instance.details.find { it.category == categoryName }

                Column {
                    if (categoryName != "Inne") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(enabled = isEnabled) {
                                    viewModel.updateDamageCategorySelection(instance.id, categoryName, !isCategorySelected)
                                }
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isCategorySelected,
                                onCheckedChange = {
                                    viewModel.updateDamageCategorySelection(instance.id, categoryName, it)
                                },
                                enabled = isEnabled
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(categoryName, color = PalletTheme.DarkText, style = MaterialTheme.typography.bodyMedium)
                        }

                        if (isCategorySelected && selectedCategoryDetail != null) {
                            Column(modifier = Modifier.padding(start = 32.dp)) {
                                val typesForCategory = damageTypeMap[categoryName] ?: emptyList()
                                typesForCategory.forEach { damageTypeName ->
                                    val existingDamageInfo = selectedCategoryDetail.types.find { it.type == damageTypeName }
                                    val isChecked = existingDamageInfo != null

                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.weight(1f).clickable(enabled = isEnabled) {
                                                    viewModel.updateDamageTypeInfo(instance.id, categoryName, damageTypeName, !isChecked, null, null)
                                                },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { selected ->
                                                        viewModel.updateDamageTypeInfo(instance.id, categoryName, damageTypeName, selected, null, null)
                                                    },
                                                    enabled = isEnabled
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(damageTypeName, color = PalletTheme.DarkText, style = MaterialTheme.typography.bodySmall)
                                            }
                                            if (isChecked) {
                                                OutlinedTextField(
                                                    value = existingDamageInfo?.size ?: "",
                                                    onValueChange = { newSize ->
                                                        val filtered = newSize.filter { it.isDigit() || it == '.' || it == ',' }
                                                            .replace(',', '.')
                                                            .let { str ->
                                                                val dotIndex = str.indexOf('.')
                                                                if (dotIndex != -1) {
                                                                    str.substring(0, dotIndex + 1) + str.substring(dotIndex + 1).replace(".", "")
                                                                } else str
                                                            }
                                                        viewModel.updateDamageTypeInfo(instance.id, categoryName, damageTypeName, true, size = filtered, description = existingDamageInfo?.description)
                                                    },
                                                    label = { Text("Rozmiar (cm)", style = MaterialTheme.typography.bodySmall) },
                                                    modifier = Modifier.width(110.dp).padding(start = 8.dp),
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.bodySmall,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PalletTheme.DarkText, unfocusedTextColor = PalletTheme.DarkText),
                                                    enabled = isEnabled
                                                )
                                            }
                                        }
                                        if (isChecked && damageTypeName == "Inne (opis)") {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            OutlinedTextField(
                                                value = existingDamageInfo?.description ?: "",
                                                onValueChange = { newDescription ->
                                                    viewModel.updateDamageTypeInfo(instance.id, categoryName, damageTypeName, true, size = existingDamageInfo?.size, description = newDescription)
                                                },
                                                label = { Text("Opisz uszkodzenie", style = MaterialTheme.typography.bodySmall) },
                                                modifier = Modifier.fillMaxWidth().padding(start = 40.dp),
                                                singleLine = false,
                                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PalletTheme.DarkText, unfocusedTextColor = PalletTheme.DarkText),
                                                enabled = isEnabled
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(enabled = isEnabled) {
                                    viewModel.updateDamageCategorySelection(instance.id, categoryName, !isCategorySelected)
                                }
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isCategorySelected,
                                onCheckedChange = {
                                    viewModel.updateDamageCategorySelection(instance.id, categoryName, it)
                                },
                                enabled = isEnabled
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(categoryName, color = PalletTheme.DarkText, style = MaterialTheme.typography.bodyMedium)
                        }

                        if (isCategorySelected && selectedCategoryDetail != null) {
                            val inneOpisInfo = selectedCategoryDetail.types.find { it.type == "Inne (opis)" } ?: DamageInfo(
                                type = "Inne (opis)"
                            )
                            Column(
                                modifier = Modifier.padding(start = 32.dp, top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = inneOpisInfo.description,
                                    onValueChange = { newDesc ->
                                        viewModel.updateDamageTypeInfo(instance.id, "Inne", "Inne (opis)", true, size = inneOpisInfo.size, description = newDesc)
                                    },
                                    label = { Text("Opisz uszkodzenie...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PalletTheme.DarkText, unfocusedTextColor = PalletTheme.DarkText),
                                    enabled = isEnabled
                                )
                                OutlinedTextField(
                                    value = inneOpisInfo.size,
                                    onValueChange = { newSize ->
                                        val filtered = newSize.filter { it.isDigit() || it == '.' || it == ',' }
                                            .replace(',', '.')
                                            .let { str ->
                                                val dotIndex = str.indexOf('.')
                                                if (dotIndex != -1) {
                                                    str.substring(0, dotIndex + 1) + str.substring(dotIndex + 1).replace(".", "")
                                                } else str
                                            }
                                        viewModel.updateDamageTypeInfo(instance.id, "Inne", "Inne (opis)", true, size = filtered, description = inneOpisInfo.description)
                                    },
                                    label = { Text("Rozmiar (cm)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PalletTheme.DarkText, unfocusedTextColor = PalletTheme.DarkText),
                                    enabled = isEnabled
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun BarcodeDataSelectionDialog(
    options: List<BarcodeData>,
    onDismiss: () -> Unit,
    onSelect: (List<BarcodeData>)-> Unit
) {
    var selections by remember { mutableStateOf(emptySet<BarcodeData>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wybierz dane") },
        text = {
            Column {
                Text("Na palecie znaleziono wiele alokacji. Zaznacz właściwe:")
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(options) { data ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selections = if (data in selections) {
                                        selections - data
                                    } else {
                                        selections + data
                                    }
                                }
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = data in selections,
                                onCheckedChange = {
                                    selections = if (data in selections) {
                                        selections - data
                                    } else {
                                        selections + data
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Towar: ${data.rodzajTowaru}", fontWeight = FontWeight.Bold, color = PalletTheme.DarkText)
                                Text("LOT: ${data.numerLotu}", color = PalletTheme.MediumText)
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelect(selections.toList()) },
                enabled = selections.isNotEmpty()
            ) {
                Text("Zatwierdź")
            }
        }
    )
}

private fun createImageFile(appContext: Context, prefix: String): File? {
    return try {
        val imageDirectory = File(appContext.filesDir, "pallet_images")
        imageDirectory.mkdirs()
        val imageFile = File(imageDirectory, "${prefix}${System.currentTimeMillis()}.jpg")
        imageFile
    } catch (e: Exception) {
        Log.e("PalletEntryScreen", "Błąd tworzenia pliku na zdjęcie: ${e.message}")
        null
    }
}

@Composable
private fun LoadingSpinnerOrErrorIcon(
    uploadStatus: UploadStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animacja pulsowania dla uploading
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Animacja bounce dla sukcesu
    var showSuccess by remember { mutableStateOf(false) }
    val bounceScale by animateFloatAsState(
        targetValue = if (showSuccess) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounceScale"
    )

    // Animacja shake dla błędu
    var showError by remember { mutableStateOf(false) }
    val shakeOffset by animateFloatAsState(
        targetValue = if (showError) 0f else 10f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "shakeOffset"
    )

    LaunchedEffect(uploadStatus) {
        when (uploadStatus) {
            UploadStatus.SUCCESS -> {
                showSuccess = true
                delay(50)
                showSuccess = false
            }
            UploadStatus.FAILED -> {
                showError = true
                // Animacja shake - kilka ruchów w lewo-prawo
                repeat(3) {
                    delay(50)
                    showError = !showError
                }
                showError = false
            }
            else -> {
                showSuccess = false
                showError = false
            }
        }
    }

    AnimatedContent(
        targetState = uploadStatus,
        transitionSpec = {
            when (targetState) {
                UploadStatus.SUCCESS -> {
                    // Sukces wlatuje z scale up
                    (fadeIn(animationSpec = tween(300)) +
                            scaleIn(initialScale = 0.3f, animationSpec = tween(300))) togetherWith
                            fadeOut(animationSpec = tween(150))
                }
                UploadStatus.FAILED -> {
                    // Błąd pojawia się z fade
                    fadeIn(animationSpec = tween(200)) togetherWith
                            fadeOut(animationSpec = tween(200))
                }
                else -> {
                    fadeIn(animationSpec = tween(200)) togetherWith
                            fadeOut(animationSpec = tween(200))
                }
            }
        },
        label = "StatusIcon"
    ) { status ->
        when (status) {
            UploadStatus.UPLOADING -> {
                Box(
                    modifier = modifier.scale(pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = PalletTheme.WarningColor
                    )
                }
            }
            UploadStatus.FAILED -> {
                Box(
                    modifier = modifier
                        .offset(x = if (showError) shakeOffset.dp else 0.dp)
                        .clickable(onClick = onRetry)
                        .background(
                            PalletTheme.InvalidColor.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SyncProblem,
                        contentDescription = "Błąd wysyłania - kliknij aby ponowić",
                        tint = PalletTheme.InvalidColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            UploadStatus.SUCCESS -> {
                Box(
                    modifier = modifier
                        .scale(if (showSuccess) bounceScale else 1f)
                        .background(
                            PalletTheme.ValidColor.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Wysłano pomyślnie",
                        tint = PalletTheme.ValidColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            UploadStatus.IDLE -> {
                // Nic nie pokazuj, jeśli jest idle
            }
        }
    }
}