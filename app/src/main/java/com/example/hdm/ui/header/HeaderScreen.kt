@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.hdm.ui.header

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.hdm.Screen
import com.example.hdm.model.LocationRepository
import com.example.hdm.model.UserManager
import com.example.hdm.services.CodeValidator
import java.text.SimpleDateFormat
import java.util.*

// Design System z PalletEntryScreen - używamy tego samego co w innych ekranach
object HeaderTheme {
    val ValidColor = Color(0xFF2E7D32)
    val InvalidColor = Color(0xFFD32F2F)
    val EmptyColor = Color(0xFF757575)
    val WarningColor = Color(0xFFFF9800)

    val ValidBackground = Color(0xFFE8F5E9) // Bardziej wyrazisty zielony
    val InvalidBackground = Color(0xFFFFF3F3)
    val WarningBackground = Color(0xFFFFF3E0)
    val NeutralBackground = Color(0xFFFAFAFA)

    val ValidBorder = Color(0xFF4CAF50) // Bardziej wyrazista zielona ramka
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


enum class HeaderValidationState {
    EMPTY, VALID, INVALID, WARNING
}

data class HeaderFieldValidation(
    val state: HeaderValidationState,
    val message: String = ""
)

@Composable
private fun HeaderValidationIcon(
    validationState: HeaderValidationState,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (validationState) {
        HeaderValidationState.INVALID -> Icons.Default.ErrorOutline to HeaderTheme.InvalidColor
        HeaderValidationState.WARNING -> Icons.Default.Warning to HeaderTheme.WarningColor
        HeaderValidationState.VALID, HeaderValidationState.EMPTY -> return // Brak ikony dla VALID i EMPTY
    }

    AnimatedVisibility(
        visible = validationState != HeaderValidationState.EMPTY && validationState != HeaderValidationState.VALID,
        enter = fadeIn(animationSpec = tween(HeaderTheme.animationDuration)),
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
private fun CleanHeaderTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    validationState: HeaderValidationState,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val borderColor by animateColorAsState(
        targetValue = when (validationState) {
            HeaderValidationState.VALID -> HeaderTheme.ValidBorder
            HeaderValidationState.INVALID -> HeaderTheme.InvalidBorder
            HeaderValidationState.WARNING -> HeaderTheme.WarningBorder
            HeaderValidationState.EMPTY -> HeaderTheme.NeutralBorder
        },
        animationSpec = tween(HeaderTheme.animationDuration),
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when (validationState) {
            HeaderValidationState.VALID -> HeaderTheme.ValidBackground
            HeaderValidationState.INVALID -> HeaderTheme.InvalidBackground
            HeaderValidationState.WARNING -> HeaderTheme.WarningBackground
            HeaderValidationState.EMPTY -> Color.White
        },
        animationSpec = tween(HeaderTheme.animationDuration),
        label = "backgroundColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(HeaderTheme.cornerRadius)
            )
            .border(
                width = HeaderTheme.borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(HeaderTheme.cornerRadius)
            )
            .clip(RoundedCornerShape(HeaderTheme.cornerRadius))
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = HeaderTheme.MediumText) },
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
                focusedTextColor = HeaderTheme.DarkText,
                unfocusedTextColor = HeaderTheme.DarkText,
                disabledTextColor = HeaderTheme.LightText,
                focusedLabelColor = HeaderTheme.MediumText,
                unfocusedLabelColor = HeaderTheme.LightText
            ),
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HeaderValidationIcon(validationState = validationState)
                    trailingIcon?.invoke()
                }
            }
        )
    }
}

@Composable
private fun CleanHeaderDropdownField(
    value: String,
    options: List<String>,
    label: String,
    validationState: HeaderValidationState,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    transparent: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = when {
            transparent -> Color.Transparent
            validationState == HeaderValidationState.VALID -> HeaderTheme.ValidBorder
            validationState == HeaderValidationState.INVALID -> HeaderTheme.InvalidBorder
            validationState == HeaderValidationState.WARNING -> HeaderTheme.WarningBorder
            else -> HeaderTheme.NeutralBorder
        },
        animationSpec = tween(HeaderTheme.animationDuration),
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            transparent -> Color.Transparent
            validationState == HeaderValidationState.VALID -> HeaderTheme.ValidBackground
            validationState == HeaderValidationState.INVALID -> HeaderTheme.InvalidBackground
            validationState == HeaderValidationState.WARNING -> HeaderTheme.WarningBackground
            else -> Color.White
        },
        animationSpec = tween(HeaderTheme.animationDuration),
        label = "backgroundColor"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
            .fillMaxWidth()
            .let {
                if (!transparent) {
                    it.background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(HeaderTheme.cornerRadius)
                    )
                        .border(
                            width = HeaderTheme.borderWidth,
                            color = borderColor,
                            shape = RoundedCornerShape(HeaderTheme.cornerRadius)
                        )
                } else it
            }
            .clip(RoundedCornerShape(HeaderTheme.cornerRadius))
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label, color = HeaderTheme.MediumText) },
            readOnly = true,
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
                focusedTextColor = HeaderTheme.DarkText,
                unfocusedTextColor = HeaderTheme.DarkText,
                disabledTextColor = HeaderTheme.LightText,
                focusedLabelColor = HeaderTheme.MediumText,
                unfocusedLabelColor = HeaderTheme.LightText
            ),
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!transparent) {
                        HeaderValidationIcon(validationState = validationState)
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderScreen(
    navController: NavController,
    headerViewModel: HeaderViewModel
) {
    val reportHeader by headerViewModel.reportHeaderState.collectAsState()
    val strategy by headerViewModel.strategy.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(key1 = Unit) {
        headerViewModel.updateDataGodzina(System.currentTimeMillis())
    }

    val savedPallets by headerViewModel.savedPallets.collectAsStateWithLifecycle()
    val isPalletTypeChangeLocked = savedPallets.isNotEmpty()

    val loggedInUser by UserManager.loggedInUser.collectAsStateWithLifecycle()
    LaunchedEffect(loggedInUser) {
        loggedInUser?.let { user ->
            headerViewModel.setLoggedInUserInHeader(user)
        }
    }

    val FENIKS_SCAN_RESULT_KEY = "feniks_scan_result"
    val LOCATION_SCAN_RESULT_KEY = "location_scan_result"

    val currentBackStackEntry = navController.currentBackStackEntry
    val feniksScannedValue by currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>(FENIKS_SCAN_RESULT_KEY, null)
        ?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }

    val locationScannedValue by currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>(LOCATION_SCAN_RESULT_KEY, null)
        ?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Logika obsługiwana w onClick przycisków
            } else {
                Log.d("HeaderScreen", "Odmówiono uprawnień do kamery")
            }
        }
    )

    LaunchedEffect(feniksScannedValue) {
        feniksScannedValue?.let { value ->
            headerViewModel.updateNumerDokumentuFeniks(value)
            currentBackStackEntry?.savedStateHandle?.remove<String>(FENIKS_SCAN_RESULT_KEY)
        }
    }

    LaunchedEffect(locationScannedValue) {
        locationScannedValue?.let { value ->
            headerViewModel.updateLokalizacja(value)
            currentBackStackEntry?.savedStateHandle?.remove<String>(LOCATION_SCAN_RESULT_KEY)
        }
    }

    val fieldValidations = remember(reportHeader, strategy) {
        mapOf(
            "magazynier" to HeaderFieldValidation(
                state = if (reportHeader.magazynier.isNotBlank()) HeaderValidationState.VALID else HeaderValidationState.EMPTY,
                message = if (reportHeader.magazynier.isNotBlank()) "Magazynier ustawiony" else "Błąd: Brak zalogowanego użytkownika"
            ),
            "miejsce" to HeaderFieldValidation(
                state = if (reportHeader.miejsce.isNotBlank()) HeaderValidationState.VALID else HeaderValidationState.EMPTY,
                message = if (reportHeader.miejsce.isNotBlank()) "Miejsce wybrane" else "Wybierz miejsce"
            ),
            "lokalizacja" to HeaderFieldValidation(
                state = if (reportHeader.lokalizacja.isNotBlank()) HeaderValidationState.VALID else HeaderValidationState.EMPTY,
                message = if (reportHeader.lokalizacja.isNotBlank()) "Lokalizacja podana" else "Podaj lokalizację"
            ),
            "wozek" to HeaderFieldValidation(
                state = if (reportHeader.rodzajWozka.isNotBlank()) HeaderValidationState.VALID else HeaderValidationState.EMPTY,
                message = if (reportHeader.rodzajWozka.isNotBlank()) "Rodzaj wózka wybrany" else "Wybierz rodzaj wózka"
            ),
            "towary" to HeaderFieldValidation(
                state = if (reportHeader.rodzajPalet.isNotBlank()) HeaderValidationState.VALID else HeaderValidationState.EMPTY,
                message = if (reportHeader.rodzajPalet.isNotBlank()) "Rodzaj towaru wybrany" else "Wybierz rodzaj towaru"
            )
        )
    }

    val completedFields = fieldValidations.values.count { it.state == HeaderValidationState.VALID }
    val totalFields = fieldValidations.size
    val isFormValid = headerViewModel.isHeaderDataValid()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        strategy?.getPdfTitle() ?: "Raport Uszkodzeń",
                        fontWeight = FontWeight.Bold,
                        color = HeaderTheme.DarkText
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
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val headerCardColor by animateColorAsState(
                targetValue = if (isFormValid) HeaderTheme.ValidBackground else HeaderTheme.NeutralBackground,
                animationSpec = tween(HeaderTheme.animationDuration),
                label = "headerCardColor"
            )

            val headerBorderColor by animateColorAsState(
                targetValue = if (isFormValid) HeaderTheme.ValidBorder else HeaderTheme.NeutralBorder,
                animationSpec = tween(HeaderTheme.animationDuration),
                label = "headerBorderColor"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(
                        color = headerCardColor,
                        shape = RoundedCornerShape(HeaderTheme.cornerRadius)
                    )
                    .border(
                        width = HeaderTheme.borderWidth,
                        color = headerBorderColor,
                        shape = RoundedCornerShape(HeaderTheme.cornerRadius)
                    ),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(HeaderTheme.cornerRadius),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isFormValid) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isFormValid) HeaderTheme.ValidColor else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFormValid) "Nagłówek gotowy!" else "Nagłówek raportu",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isFormValid) HeaderTheme.ValidColor else HeaderTheme.DarkText
                        )
                        Text(
                            text = if (isFormValid) "Wszystkie pola wypełnione" else "Wypełnij podstawowe informacje",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isFormValid) HeaderTheme.ValidColor else HeaderTheme.LightText
                        )
                    }
                    Text(
                        text = "$completedFields/$totalFields",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (completedFields == totalFields) HeaderTheme.ValidColor else HeaderTheme.MediumText
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                item {
                    val cardColor by animateColorAsState(
                        targetValue = when {
                            fieldValidations["magazynier"]?.state == HeaderValidationState.VALID -> HeaderTheme.ValidBackground
                            else -> Color.White
                        },
                        animationSpec = tween(HeaderTheme.animationDuration),
                        label = "magazynierCardColor"
                    )

                    val borderColor by animateColorAsState(
                        targetValue = when {
                            fieldValidations["magazynier"]?.state == HeaderValidationState.VALID -> HeaderTheme.ValidBorder
                            else -> HeaderTheme.NeutralBorder
                        },
                        animationSpec = tween(HeaderTheme.animationDuration),
                        label = "magazynierBorderColor"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = cardColor,
                                shape = RoundedCornerShape(HeaderTheme.cornerRadius)
                            )
                            .border(
                                width = HeaderTheme.borderWidth,
                                color = borderColor,
                                shape = RoundedCornerShape(HeaderTheme.cornerRadius)
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(HeaderTheme.cornerRadius),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Magazynier",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = HeaderTheme.DarkText
                                )
                                HeaderValidationIcon(validationState = fieldValidations["magazynier"]?.state ?: HeaderValidationState.EMPTY)
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            CleanHeaderTextField(
                                value = reportHeader.magazynier,
                                onValueChange = { },
                                label = "Imię i nazwisko pracownika",
                                validationState = fieldValidations["magazynier"]?.state ?: HeaderValidationState.EMPTY,
                                enabled = false
                            )
                        }
                    }
                }

                // ... (reszta itemów w LazyColumn pozostaje bez zmian) ...
                item {
                    // Miejsce
                    var wasTouched by remember { mutableStateOf(false) }

                    val cardColor by animateColorAsState(
                        targetValue = when {
                            fieldValidations["miejsce"]?.state == HeaderValidationState.VALID -> HeaderTheme.ValidBackground
                            wasTouched && fieldValidations["miejsce"]?.state == HeaderValidationState.EMPTY -> HeaderTheme.InvalidBackground
                            else -> Color.White
                        },
                        animationSpec = tween(HeaderTheme.animationDuration),
                        label = "miejsceCardColor"
                    )

                    val borderColor by animateColorAsState(
                        targetValue = when {
                            fieldValidations["miejsce"]?.state == HeaderValidationState.VALID -> HeaderTheme.ValidBorder
                            wasTouched && fieldValidations["miejsce"]?.state == HeaderValidationState.EMPTY -> HeaderTheme.InvalidBorder
                            else -> HeaderTheme.NeutralBorder
                        },
                        animationSpec = tween(HeaderTheme.animationDuration),
                        label = "miejsceBorderColor"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = cardColor,
                                shape = RoundedCornerShape(HeaderTheme.cornerRadius)
                            )
                            .border(
                                width = HeaderTheme.borderWidth,
                                color = borderColor,
                                shape = RoundedCornerShape(HeaderTheme.cornerRadius)
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(HeaderTheme.cornerRadius),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Miejsce",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = HeaderTheme.DarkText
                                )
                                HeaderValidationIcon(
                                    validationState = when {
                                        fieldValidations["miejsce"]?.state == HeaderValidationState.VALID -> HeaderValidationState.VALID
                                        wasTouched && fieldValidations["miejsce"]?.state == HeaderValidationState.EMPTY -> HeaderValidationState.INVALID
                                        else -> HeaderValidationState.EMPTY
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            CleanHeaderDropdownField(
                                value = reportHeader.miejsce,
                                options = LocationRepository.warehouses,
                                label = "Wybierz magazyn",
                                validationState = fieldValidations["miejsce"]?.state ?: HeaderValidationState.EMPTY,
                                onValueChange = {
                                    wasTouched = true
                                    headerViewModel.updateMiejsce(it)
                                }
                            )
                        }
                    }
                }

                item {
                    // Lokalizacja
                    var wasTouched by remember { mutableStateOf(false) }
                    val lokalizacje = remember(reportHeader.miejsce, strategy) {
                        val baseDocks = LocationRepository.getDocksFor(reportHeader.miejsce)
                        val strategyOptions = strategy?.getInitialLocationOptions() ?: emptyList()
                        strategyOptions + baseDocks
                    }

                    val cardColor by animateColorAsState(
                        targetValue = when {
                            fieldValidations["lokalizacja"]?.state == HeaderValidationState.VALID -> HeaderTheme.ValidBackground
                            wasTouched && fieldValidations["lokalizacja"]?.state == HeaderValidationState.EMPTY -> HeaderTheme.InvalidBackground
                            else -> Color.White
                        },
                        animationSpec = tween(HeaderTheme.animationDuration),
                        label = "lokalizacjaCardColor"
                    )

                    val borderColor by animateColorAsState(
                        targetValue = when {
                            fieldValidations["lokalizacja"]?.state == HeaderValidationState.VALID -> HeaderTheme.ValidBorder
                            wasTouched && fieldValidations["lokalizacja"]?.state == HeaderValidationState.EMPTY -> HeaderTheme.InvalidBorder
                            else -> HeaderTheme.NeutralBorder
                        },
                        animationSpec = tween(HeaderTheme.animationDuration),
                        label = "lokalizacjaBorderColor"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = cardColor,
                                shape = RoundedCornerShape(HeaderTheme.cornerRadius)
                            )
                            .border(
                                width = HeaderTheme.borderWidth,
                                color = borderColor,
                                shape = RoundedCornerShape(HeaderTheme.cornerRadius)
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(HeaderTheme.cornerRadius),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Lokalizacja",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = HeaderTheme.DarkText
                                )
                                HeaderValidationIcon(
                                    validationState = when {
                                        fieldValidations["lokalizacja"]?.state == HeaderValidationState.VALID -> HeaderValidationState.VALID
                                        wasTouched && fieldValidations["lokalizacja"]?.state == HeaderValidationState.EMPTY -> HeaderValidationState.INVALID
                                        else -> HeaderValidationState.EMPTY
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            CleanHeaderDropdownField(
                                value = reportHeader.lokalizacja,
                                options = lokalizacje,
                                label = "Wybierz lokalizację",
                                validationState = fieldValidations["lokalizacja"]?.state ?: HeaderValidationState.EMPTY,
                                onValueChange = { selectedOption ->
                                    wasTouched = true
                                    if (selectedOption.contains("(zeskanuj kod)")) {
                                        when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                                            PackageManager.PERMISSION_GRANTED -> {
                                                // --- POCZĄTEK POPRAWKI ---
                                                // Używamy nowego, bezpiecznego typu "QR_CODE_ONLY"
                                                navController.navigate(Screen.Scanner.withArgs("QR_CODE_ONLY", LOCATION_SCAN_RESULT_KEY))
                                                // --- KONIEC POPRAWKI ---
                                            }
                                            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    } else {
                                        headerViewModel.updateLokalizacja(selectedOption)
                                    }
                                },
                                enabled = reportHeader.miejsce.isNotBlank()
                            )
                        }
                    }
                }

                item {
                    // Rodzaj wózka
                    var wasTouched by remember { mutableStateOf(false) }

                    val cardColor by animateColorAsState(
                        targetValue = when {
                            fieldValidations["wozek"]?.state == HeaderValidationState.VALID -> HeaderTheme.ValidBackground
                            wasTouched && fieldValidations["wozek"]?.state == HeaderValidationState.EMPTY -> HeaderTheme.InvalidBackground
                            else -> Color.White
                        },
                        animationSpec = tween(HeaderTheme.animationDuration),
                        label = "wozekCardColor"
                    )

                    val borderColor by animateColorAsState(
                        targetValue = when {
                            fieldValidations["wozek"]?.state == HeaderValidationState.VALID -> HeaderTheme.ValidBorder
                            wasTouched && fieldValidations["wozek"]?.state == HeaderValidationState.EMPTY -> HeaderTheme.InvalidBorder
                            else -> HeaderTheme.NeutralBorder
                        },
                        animationSpec = tween(HeaderTheme.animationDuration),
                        label = "wozekBorderColor"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = cardColor,
                                shape = RoundedCornerShape(HeaderTheme.cornerRadius)
                            )
                            .border(
                                width = HeaderTheme.borderWidth,
                                color = borderColor,
                                shape = RoundedCornerShape(HeaderTheme.cornerRadius)
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(HeaderTheme.cornerRadius),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Rodzaj wózka",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = HeaderTheme.DarkText
                                )
                                HeaderValidationIcon(
                                    validationState = when {
                                        fieldValidations["wozek"]?.state == HeaderValidationState.VALID -> HeaderValidationState.VALID
                                        wasTouched && fieldValidations["wozek"]?.state == HeaderValidationState.EMPTY -> HeaderValidationState.INVALID
                                        else -> HeaderValidationState.EMPTY
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            CleanHeaderDropdownField(
                                value = reportHeader.rodzajWozka,
                                options = listOf("Wózek nisko unoszący", "Wózek ręczny paletowy", "Wózek czołowy", "Reachtruck", "Inne"),
                                label = "Wybierz typ wózka",
                                validationState = fieldValidations["wozek"]?.state ?: HeaderValidationState.EMPTY,
                                onValueChange = {
                                    wasTouched = true
                                    headerViewModel.updateRodzajWozka(it)
                                }
                            )
                        }
                    }
                }

                item {
                    // Data i godzina
                    val formatterData = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
                    val formatterTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                    val displayDate = remember(reportHeader.dataGodzina) { formatterData.format(Date(reportHeader.dataGodzina)) }
                    val displayTime = remember(reportHeader.dataGodzina) { formatterTime.format(Date(reportHeader.dataGodzina)) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = HeaderTheme.ValidBackground),
                            shape = RoundedCornerShape(HeaderTheme.cornerRadius),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) { // Zmniejszony padding
                                Text(
                                    "Data",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HeaderTheme.MediumText
                                )
                                Spacer(modifier = Modifier.height(2.dp)) // Zmniejszony spacer
                                Text(
                                    displayDate,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = HeaderTheme.DarkText
                                )
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = HeaderTheme.ValidBackground),
                            shape = RoundedCornerShape(HeaderTheme.cornerRadius),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Godzina",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HeaderTheme.MediumText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    displayTime,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = HeaderTheme.DarkText
                                )
                            }
                        }
                    }
                }

                item {
                    // Rodzaj towaru
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(HeaderTheme.cornerRadius),
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
                                    "Rodzaj towaru",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = HeaderTheme.DarkText
                                )
                                if (isPalletTypeChangeLocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Zablokowane",
                                        tint = HeaderTheme.WarningColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            CleanHeaderDropdownField(
                                value = reportHeader.rodzajPalet,
                                options = listOf("Wyrób gotowy", "Surowiec", "Inne"),
                                label = "Wybierz rodzaj towaru",
                                validationState = fieldValidations["towary"]?.state ?: HeaderValidationState.EMPTY,
                                onValueChange = { headerViewModel.updateRodzajPalet(it) },
                                enabled = !isPalletTypeChangeLocked
                            )

                            if (isPalletTypeChangeLocked) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = HeaderTheme.WarningColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Nie można zmienić rodzaju towaru - dodano już palety",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = HeaderTheme.WarningColor
                                    )
                                }
                            }
                        }
                    }
                }

                if (strategy?.isVehicleSectionVisible == true) {
                    item {
                        var wasTouched by remember { mutableStateOf(false) }

                        val cardColor by animateColorAsState(
                            targetValue = when {
                                reportHeader.rodzajSamochodu.isNotBlank() -> HeaderTheme.ValidBackground
                                wasTouched && reportHeader.rodzajSamochodu.isBlank() -> HeaderTheme.InvalidBackground
                                else -> Color.White
                            },
                            animationSpec = tween(HeaderTheme.animationDuration),
                            label = "pojazdCardColor"
                        )

                        val borderColor by animateColorAsState(
                            targetValue = when {
                                reportHeader.rodzajSamochodu.isNotBlank() -> HeaderTheme.ValidBorder
                                wasTouched && reportHeader.rodzajSamochodu.isBlank() -> HeaderTheme.InvalidBorder
                                else -> HeaderTheme.NeutralBorder
                            },
                            animationSpec = tween(HeaderTheme.animationDuration),
                            label = "pojazdBorderColor"
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = cardColor,
                                    shape = RoundedCornerShape(HeaderTheme.cornerRadius)
                                )
                                .border(
                                    width = HeaderTheme.borderWidth,
                                    color = borderColor,
                                    shape = RoundedCornerShape(HeaderTheme.cornerRadius)
                                ),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(HeaderTheme.cornerRadius),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Dane pojazdu",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = HeaderTheme.DarkText
                                    )
                                    HeaderValidationIcon(
                                        validationState = when {
                                            reportHeader.rodzajSamochodu.isNotBlank() -> HeaderValidationState.VALID
                                            wasTouched && reportHeader.rodzajSamochodu.isBlank() -> HeaderValidationState.INVALID
                                            else -> HeaderValidationState.EMPTY
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                CleanHeaderDropdownField(
                                    value = reportHeader.rodzajSamochodu,
                                    options = strategy?.getVehicleTypes() ?: emptyList(),
                                    label = "Rodzaj samochodu",
                                    validationState = if (reportHeader.rodzajSamochodu.isNotBlank()) HeaderValidationState.VALID else HeaderValidationState.EMPTY,
                                    onValueChange = {
                                        wasTouched = true
                                        headerViewModel.updateRodzajSamochodu(it)
                                    }
                                )

                                AnimatedVisibility(
                                    visible = reportHeader.rodzajSamochodu.isNotBlank(),
                                    enter = fadeIn() + slideInVertically(),
                                    exit = fadeOut() + slideOutVertically()
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Spacer(modifier = Modifier.height(12.dp))

                                        when (reportHeader.rodzajSamochodu) {
                                            "Bus", "Solo" -> {
                                                CleanHeaderTextField(
                                                    value = reportHeader.numerAuta,
                                                    onValueChange = { headerViewModel.updateNumerAuta(it) },
                                                    label = "Numer rejestracyjny samochodu",
                                                    validationState = if (reportHeader.numerAuta.isNotBlank()) HeaderValidationState.VALID else HeaderValidationState.EMPTY
                                                )
                                            }
                                            "Naczepa", "Tandem" -> {
                                                CleanHeaderTextField(
                                                    value = reportHeader.numerAuta,
                                                    onValueChange = { headerViewModel.updateNumerAuta(it) },
                                                    label = "Numer rejestracyjny pojazdu",
                                                    validationState = if (reportHeader.numerAuta.isNotBlank()) HeaderValidationState.VALID else HeaderValidationState.EMPTY
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                CleanHeaderTextField(
                                                    value = reportHeader.numerNaczepyKontenera,
                                                    onValueChange = { headerViewModel.updateNumerNaczepyKontenera(it) },
                                                    label = "Numer rejestracyjny naczepy/przyczepy",
                                                    validationState = if (reportHeader.numerNaczepyKontenera.isNotBlank()) HeaderValidationState.VALID else HeaderValidationState.EMPTY
                                                )
                                            }
                                            "Kontener" -> {
                                                val isContainerValid = reportHeader.numerKontenera.isBlank() ||
                                                        CodeValidator.isContainerNumberValid(reportHeader.numerKontenera)

                                                val containerValidationState = when {
                                                    reportHeader.numerKontenera.isBlank() -> HeaderValidationState.EMPTY
                                                    isContainerValid -> HeaderValidationState.VALID
                                                    else -> HeaderValidationState.INVALID
                                                }

                                                CleanHeaderTextField(
                                                    value = reportHeader.numerKontenera,
                                                    onValueChange = {
                                                        val cleaned = it.uppercase().filter { char -> char.isLetterOrDigit() }
                                                        headerViewModel.updateNumerKontenera(cleaned)
                                                    },
                                                    label = "Numer kontenera (np. CSQU3054383)",
                                                    validationState = containerValidationState
                                                )

                                                if (reportHeader.numerKontenera.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isContainerValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                                            contentDescription = null,
                                                            tint = if (isContainerValid) HeaderTheme.ValidColor else HeaderTheme.InvalidColor,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = if (isContainerValid)
                                                                "Numer kontenera poprawny"
                                                            else
                                                                "Niepoprawny format (4 litery + 6 cyfr + cyfra kontrolna)",
                                                            color = if (isContainerValid) HeaderTheme.ValidColor else HeaderTheme.InvalidColor,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                }
                                            }

                                            // --- POCZĄTEK POPRAWKI ---
                                            // Usunięto sekcję dla "OKTRANS", "ROTONDO", "DSV"
                                            // --- KONIEC POPRAWKI ---
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (strategy?.isFeniksNumberVisible == true) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(HeaderTheme.cornerRadius),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Numer dokumentu Feniks (opcjonalnie)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = HeaderTheme.DarkText
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val isFeniksValid = reportHeader.numerDokumentuFeniks.isBlank() ||
                                                CodeValidator.isValid(reportHeader.numerDokumentuFeniks)

                                        val feniksValidationState = when {
                                            reportHeader.numerDokumentuFeniks.isBlank() -> HeaderValidationState.EMPTY
                                            isFeniksValid -> HeaderValidationState.VALID
                                            else -> HeaderValidationState.INVALID
                                        }

                                        CleanHeaderTextField(
                                            value = reportHeader.numerDokumentuFeniks,
                                            onValueChange = { headerViewModel.updateNumerDokumentuFeniks(it) },
                                            label = "Wpisz lub zeskanuj",
                                            validationState = feniksValidationState
                                        )

                                        if (reportHeader.numerDokumentuFeniks.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = if (isFeniksValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                                    contentDescription = null,
                                                    tint = if (isFeniksValid) HeaderTheme.ValidColor else HeaderTheme.InvalidColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (isFeniksValid) "Numer poprawny" else "Niepoprawny format lub cyfra kontrolna",
                                                    color = if (isFeniksValid) HeaderTheme.ValidColor else HeaderTheme.InvalidColor,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }

                                        }
                                    }

                                    Button(
                                        onClick = {
                                            when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                                                PackageManager.PERMISSION_GRANTED -> navController.navigate(Screen.Scanner.withArgs("1D_BARCODES", FENIKS_SCAN_RESULT_KEY))
                                                else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        },
                                        modifier = Modifier.height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(HeaderTheme.cornerRadius)
                                    ) {
                                        Icon(
                                            Icons.Default.QrCodeScanner,
                                            contentDescription = "Skanuj",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Skanuj")
                                    }
                                }
                            }
                        }
                    }


                }
            }

            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFormValid) HeaderTheme.ValidBackground else HeaderTheme.NeutralBackground
                    ),
                    shape = RoundedCornerShape(HeaderTheme.cornerRadius),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = if (isFormValid) Icons.Default.CheckCircle else Icons.Default.Info
                        val iconColor = if (isFormValid) HeaderTheme.ValidColor else HeaderTheme.MediumText

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (isFormValid) "Gotowe - przejdź dalej" else "Uzupełnij wymagane pola",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isFormValid) HeaderTheme.ValidColor else HeaderTheme.DarkText,
                            fontWeight = if (isFormValid) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }

                Button(
                    onClick = {
                        headerViewModel.logHeaderCompletion()
                        val flow = strategy?.getScreenFlow()
                        val currentScreenIndex = flow?.indexOf(Screen.Header) ?: -1
                        if (flow != null && currentScreenIndex != -1 && currentScreenIndex + 1 < flow.size) {
                            val nextScreen = flow[currentScreenIndex + 1]
                            val route = if (nextScreen is Screen.PalletEntry) {
                                Screen.PalletEntry.createRoute()
                            } else {
                                nextScreen.route
                            }
                            navController.navigate(route)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFormValid) HeaderTheme.ValidColor else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isFormValid) Color.White else HeaderTheme.LightText
                    ),
                    shape = RoundedCornerShape(HeaderTheme.cornerRadius)
                ) {
                    if (isFormValid) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isFormValid) "Przejdź dalej" else "Uzupełnij formularz",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}