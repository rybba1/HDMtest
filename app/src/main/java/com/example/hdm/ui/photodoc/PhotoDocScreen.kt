package com.example.hdm.ui.photodoc

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import coil.compose.rememberAsyncImagePainter
import com.example.hdm.Screen
import com.example.hdm.model.AttachedImage
import com.example.hdm.model.LocationRepository
import com.example.hdm.model.UserManager
import com.example.hdm.services.CodeValidator
import java.io.File

@Composable
private fun PhotoDocSubmissionDialog(
    submissionState: PhotoDocSubmissionState,
    onDismiss: () -> Unit
) {
    if (submissionState is PhotoDocSubmissionState.Idle) return

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
                when (submissionState) {
                    is PhotoDocSubmissionState.Generating -> {
                        PhotoDocProcessingContent(
                            currentStep = submissionState.step,
                            progress = submissionState.progress
                        )
                    }
                    is PhotoDocSubmissionState.SaveToWaitingRoomSuccess -> {
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
                            tint = CleanTheme.ValidColor,
                            modifier = Modifier.size(64.dp).scale(scale)
                        )
                        Text(
                            "Dokumentacja zapisana w poczekalni.\nWysyłka wkrótce.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                    is PhotoDocSubmissionState.Error -> {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Błąd",
                            tint = CleanTheme.InvalidColor,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            submissionState.message,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = CleanTheme.InvalidColor
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
private fun PhotoDocProcessingContent(
    currentStep: PhotoDocProcessingStep,
    progress: Float
) {
    val steps = listOf(
        PhotoDocProcessingStep.PREPARING_DATA to ("Przygotowanie danych" to Icons.Default.Description),
        PhotoDocProcessingStep.COMPRESSING_IMAGES to ("Kompresja zdjęć" to Icons.Default.PhotoLibrary),
        PhotoDocProcessingStep.GENERATING_XML to ("Generowanie XML" to Icons.Default.Code),
        PhotoDocProcessingStep.PREPARING_UPLOAD to ("Przygotowanie do wysyłki" to Icons.Default.CloudUpload)
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
                color = CleanTheme.ValidColor
            )

            if (currentStepIndex >= 0) {
                Icon(
                    steps[currentStepIndex].second.second,
                    contentDescription = null,
                    tint = CleanTheme.ValidColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Text(
            "Przetwarzanie dokumentacji...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = CleanTheme.DarkText
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            steps.forEachIndexed { index, ( stepEnum, stepData) ->
                PhotoDocProcessingStep(
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
                color = CleanTheme.ValidColor,
                trackColor = CleanTheme.ValidBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${(animatedProgress * 100).toInt()}% ukończono",
                style = MaterialTheme.typography.bodySmall,
                color = CleanTheme.MediumText,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun PhotoDocProcessingStep(
    stepName: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    isCompleted: Boolean
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isActive -> CleanTheme.ValidBackground
            isCompleted -> CleanTheme.ValidBackground
            else -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "stepBg"
    )

    val iconColor by animateColorAsState(
        targetValue = when {
            isActive -> CleanTheme.ValidColor
            isCompleted -> CleanTheme.ValidColor
            else -> CleanTheme.LightText
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
                        isActive -> CleanTheme.ValidColor.copy(alpha = 0.2f)
                        isCompleted -> CleanTheme.ValidColor.copy(alpha = 0.2f)
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
                isActive -> CleanTheme.DarkText
                isCompleted -> CleanTheme.ValidColor
                else -> CleanTheme.LightText
            },
            modifier = Modifier.weight(1f)
        )

        if (isActive) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = CleanTheme.ValidColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDocScreen(
    navController: NavController,
    viewModel: PhotoDocViewModel
) {
    val header by viewModel.header.collectAsStateWithLifecycle()
    val images by viewModel.images.collectAsStateWithLifecycle()
    val nameSuggestions by viewModel.nameSuggestions.collectAsStateWithLifecycle()
    val submissionState by viewModel.submissionState.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.logoutAndNavigateToLogin.collect {
            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                }
            }
        }
    }

    val FENIKS_SCAN_RESULT_KEY = "feniks_doc_scan_result"
    // --- POCZĄTEK POPRAWKI (Skaner Lokalizacji) ---
    val LOCATION_SCAN_RESULT_KEY = "location_scan_result"
    // --- KONIEC POPRAWKI ---

    var tempImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var imageToDelete: AttachedImage? by remember { mutableStateOf(null) }
    var showSendDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var selectedTemplateName by remember { mutableStateOf("") }

    val hasUnsavedChanges by remember(header, images) {
        derivedStateOf {
            header.tytul.isNotBlank() || header.magazynier.isNotBlank() || header.miejsce.isNotBlank() ||
                    header.lokalizacja.isNotBlank() || header.numerFeniks.isNotBlank() || header.opisOgolny.isNotBlank() ||
                    images.isNotEmpty()
        }
    }

    BackHandler(enabled = hasUnsavedChanges) {
        showExitDialog = true
    }

    val scannerResult = navController.currentBackStackEntry?.savedStateHandle?.get<String>(FENIKS_SCAN_RESULT_KEY)
    LaunchedEffect(scannerResult) {
        scannerResult?.let {
            viewModel.updateNumerFeniks(it)
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>(FENIKS_SCAN_RESULT_KEY)
        }
    }

    // --- POCZĄTEK POPRAWKI (Skaner Lokalizacji) ---
    val locationScannerResult = navController.currentBackStackEntry?.savedStateHandle?.get<String>(LOCATION_SCAN_RESULT_KEY)
    LaunchedEffect(locationScannerResult) {
        locationScannerResult?.let {
            viewModel.updateLokalizacja(it, context) // Aktualizuj ViewModel
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>(LOCATION_SCAN_RESULT_KEY) // Wyczyść wynik
        }
    }
    // --- KONIEC POPRAWKI ---

    val loggedInUser by UserManager.loggedInUser.collectAsStateWithLifecycle()

    LaunchedEffect(loggedInUser) {
        loggedInUser?.let { user ->
            if (header.magazynier.isBlank()) {
                viewModel.updateMagazynier(user.fullName, context)
            }
        }
    }

    val fieldValidations by remember(header, images) {
        derivedStateOf {
            mapOf(
                "tytul" to FieldValidation(if (header.tytul.isNotBlank()) ValidationState.VALID else ValidationState.EMPTY),
                "magazynier" to FieldValidation(if (header.magazynier.isNotBlank()) ValidationState.VALID else ValidationState.EMPTY),
                "miejsce" to FieldValidation(if (header.miejsce.isNotBlank()) ValidationState.VALID else ValidationState.EMPTY),
                "lokalizacja" to FieldValidation(if (header.lokalizacja.isNotBlank()) ValidationState.VALID else ValidationState.EMPTY),
                "images" to FieldValidation(if (images.isNotEmpty()) ValidationState.VALID else ValidationState.EMPTY)
            )
        }
    }

    val optionalFieldValidations by remember(header) {
        derivedStateOf {
            mapOf(
                "numerFeniks" to FieldValidation(
                    when {
                        header.numerFeniks.isBlank() -> ValidationState.EMPTY
                        CodeValidator.isValid(header.numerFeniks) -> ValidationState.VALID
                        else -> ValidationState.INVALID
                    }
                )
            )
        }
    }

    val completedFields = fieldValidations.values.count { it.state == ValidationState.VALID }
    val totalFields = fieldValidations.size
    val isFormValid = completedFields == totalFields &&
            optionalFieldValidations.values.all { it.state != ValidationState.INVALID }

    val pickMultipleImagesLauncher = rememberLauncherForActivityResult(
        contract = PickMultipleVisualMedia(),
        onResult = { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                viewModel.addImagesFromGallery(context, uris)
            }
        }
    )

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success -> if (success) tempImageUri?.let { viewModel.addImage(it.toString(), context, "aparatu") } }
    )
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                tempImageUri = createImageFileUri(context)
                takePictureLauncher.launch(tempImageUri)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dokumentacja Zdjęciowa") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) {
                            showExitDialog = true
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    if (isFormValid) {
                        showSendDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
                    .height(56.dp),
                enabled = isFormValid && submissionState is PhotoDocSubmissionState.Idle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFormValid) CleanTheme.ValidColor else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isFormValid) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(CleanTheme.cornerRadius)
            ) {
                if (submissionState is PhotoDocSubmissionState.Generating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFormValid) "Wyślij Dokumentację" else "Uzupełnij formularz",
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            CleanProgressIndicator(completedFields = completedFields, totalFields = totalFields)
            Spacer(modifier = Modifier.height(20.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item { Text("Dane Ogólne", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = CleanTheme.DarkText) }

                item {
                    CleanDropdownField(
                        value = selectedTemplateName,
                        options = templates.map { it.templateName },
                        label = "Wybierz szablon (opcjonalnie)",
                        validationState = ValidationState.EMPTY,
                        onValueChange = { name ->
                            selectedTemplateName = name
                            templates.find { it.templateName == name }?.let {
                                viewModel.applyTemplate(it, context)
                            }
                        },
                        placeholder = "Wybierz, aby wypełnić dane...",
                        trailingIcon = {
                            if (selectedTemplateName.isNotBlank()) {
                                IconButton(onClick = {
                                    viewModel.clearHeader(context)
                                    selectedTemplateName = ""
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Wyczyść szablon")
                                }
                            }
                        }
                    )
                }

                item { CleanTextField(value = header.tytul, onValueChange = viewModel::updateTytul, label = "Tytuł Dokumentacji", validationState = fieldValidations["tytul"]?.state ?: ValidationState.EMPTY, singleLine = true) }
                item {
                    CleanTextField(
                        value = header.magazynier,
                        onValueChange = { },
                        label = "Magazynier",
                        validationState = fieldValidations["magazynier"]?.state ?: ValidationState.EMPTY,
                        enabled = false
                    )
                }
                item { CleanDropdownField(value = header.miejsce, options = LocationRepository.warehouses, label = "Miejsce", validationState = fieldValidations["miejsce"]?.state ?: ValidationState.EMPTY, onValueChange = { viewModel.updateMiejsce(it, context) }, placeholder = "Wybierz miejsce...") }

                // --- POCZĄTEK POPRAWKI (Skaner Lokalizacji) ---
                item {
                    val specialScanOption = "Lokalizacja regałowa (zeskanuj kod)"
                    val standardLocations = LocationRepository.getDocksFor(header.miejsce)
                    val allLocationOptions = listOf(specialScanOption) + standardLocations

                    CleanDropdownField(
                        value = header.lokalizacja,
                        options = allLocationOptions,
                        label = "Lokalizacja (dock)",
                        validationState = fieldValidations["lokalizacja"]?.state ?: ValidationState.EMPTY,
                        onValueChange = { selectedOption ->
                            if (selectedOption == specialScanOption) {
                                // Używamy nowego typu skanowania "QR_CODE_ONLY"
                                navController.navigate(Screen.Scanner.withArgs("QR_CODE_ONLY", LOCATION_SCAN_RESULT_KEY, null))
                            } else {
                                viewModel.updateLokalizacja(selectedOption, context)
                            }
                        },
                        enabled = header.miejsce.isNotBlank(),
                        placeholder = "Wybierz lokalizację..."
                    )
                }
                // --- KONIEC POPRAWKI ---

                item {
                    Card(shape = RoundedCornerShape(CleanTheme.cornerRadius), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = CardDefaults.outlinedCardBorder()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Numer dokumentu Feniks (opcjonalnie)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = CleanTheme.DarkText)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(value = header.numerFeniks, onValueChange = viewModel::updateNumerFeniks, label = { Text("Wpisz lub zeskanuj") }, modifier = Modifier.fillMaxWidth(), isError = optionalFieldValidations["numerFeniks"]?.state == ValidationState.INVALID, singleLine = true, trailingIcon = { if (header.numerFeniks.isNotBlank()) { Icon(imageVector = if (optionalFieldValidations["numerFeniks"]?.state == ValidationState.VALID) Icons.Default.CheckCircle else Icons.Default.Error, contentDescription = "Status walidacji", tint = if (optionalFieldValidations["numerFeniks"]?.state == ValidationState.VALID) CleanTheme.ValidColor else CleanTheme.InvalidColor) } })
                                    if (header.numerFeniks.isNotBlank()) {
                                        Text(text = if (optionalFieldValidations["numerFeniks"]?.state == ValidationState.VALID) "✓ Numer poprawny" else "✖ Niepoprawny format lub cyfra kontrolna", color = if (optionalFieldValidations["numerFeniks"]?.state == ValidationState.VALID) CleanTheme.ValidColor else CleanTheme.InvalidColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                                    }
                                }
                                Button(onClick = { navController.navigate(Screen.Scanner.withArgs("1D_BARCODES", FENIKS_SCAN_RESULT_KEY)) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(CleanTheme.cornerRadius), modifier = Modifier.height(56.dp)) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Skanuj numer Feniks", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Skanuj")
                                }
                            }
                        }
                    }
                }
                item { OutlinedTextField(value = header.opisOgolny, onValueChange = viewModel::updateOpisOgolny, label = { Text("Opis ogólny (opcjonalnie)") }, modifier = Modifier.fillMaxWidth().height(100.dp)) }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Załączniki", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = CleanTheme.DarkText)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SubtleValidationIcon(validationState = fieldValidations["images"]?.state ?: ValidationState.EMPTY)
                            Text(text = "(${images.size})", style = MaterialTheme.typography.titleLarge, color = if (images.isNotEmpty()) CleanTheme.ValidColor else CleanTheme.LightText)
                        }
                    }
                    if (images.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(text = "Kliknij w zdjęcie aby dodać notatkę", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { val permission = Manifest.permission.CAMERA; if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) { tempImageUri = createImageFileUri(context); takePictureLauncher.launch(tempImageUri) } else { cameraPermissionLauncher.launch(permission) } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(CleanTheme.cornerRadius)) { Icon(Icons.Default.PhotoCamera, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Zrób zdjęcie") }

                        Button(
                            onClick = {
                                pickMultipleImagesLauncher.launch(
                                    PickVisualMediaRequest(
                                        mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(CleanTheme.cornerRadius)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dodaj z galerii")
                        }
                    }
                }
                if (images.isNotEmpty()) {
                    val rows = images.chunked(3)
                    items(rows.size) { rowIndex ->
                        Card(
                            shape = RoundedCornerShape(CleanTheme.cornerRadius),
                            colors = CardDefaults.cardColors(containerColor = CleanTheme.ValidBackground),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(width = CleanTheme.borderWidth, color = CleanTheme.ValidBorder)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                            ) {
                                rows[rowIndex].forEach { image ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                            .clickable { navController.navigate(Screen.PhotoEdit.createRoute(image.uri)) }
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(model = Uri.parse(image.uri)),
                                            contentDescription = "Załącznik",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        IconButton(
                                            onClick = { imageToDelete = image; showDeleteDialog = true },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Usuń", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }

                                        // Wskaźnik DLA NOTATKI (etykieta)
                                        if (image.note.isNotBlank()) {
                                            Icon(
                                                imageVector = Icons.Default.Label, // Ikona etykiety
                                                contentDescription = "Zdjęcie z notatką",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart) // Lewy dolny róg
                                                    .padding(4.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                        shape = CircleShape
                                                    )
                                                    .padding(4.dp) // Wewnętrzny padding ikony
                                                    .size(16.dp)
                                            )
                                        }

                                        // Wskaźnik DLA KODU KRESKOWEGO (skaner)
                                        if (image.scannedBarcode.isNotBlank()) {
                                            Icon(
                                                imageVector = Icons.Default.QrCodeScanner, // Ikona kodu
                                                contentDescription = "Zdjęcie ze skanem",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.TopStart) // Lewy górny róg
                                                    .padding(4.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                        shape = CircleShape
                                                    )
                                                    .padding(4.dp) // Wewnętrzny padding ikony
                                                    .size(16.dp)
                                            )
                                        }

                                        // Wskaźnik dla frachtu lotniczego
                                        if (image.isAirFreight) {
                                            Icon(
                                                imageVector = Icons.Default.AirplanemodeActive, // Ikona samolotu
                                                contentDescription = "Fracht lotniczy",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd) // Prawy dolny róg
                                                    .padding(4.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                        shape = CircleShape
                                                    )
                                                    .padding(4.dp) // Wewnętrzny padding ikony
                                                    .size(16.dp)
                                            )
                                        }
                                    }
                                }
                                repeat(3 - rows[rowIndex].size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            shape = RoundedCornerShape(CleanTheme.cornerRadius),
                            colors = CardDefaults.cardColors(containerColor = CleanTheme.NeutralBackground),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(width = CleanTheme.borderWidth, color = CleanTheme.NeutralBorder)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = CleanTheme.LightText, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = "Brak zdjęć", style = MaterialTheme.typography.titleMedium, color = CleanTheme.LightText, fontWeight = FontWeight.Medium)
                                Text(text = "Dodaj przynajmniej jedno zdjęcie aby kontynuować", style = MaterialTheme.typography.bodySmall, color = CleanTheme.LightText, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFormValid) CleanTheme.ValidBackground else CleanTheme.NeutralBackground
                        ),
                        shape = RoundedCornerShape(CleanTheme.cornerRadius),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(
                            width = CleanTheme.borderWidth,
                            color = if (isFormValid) CleanTheme.ValidBorder else CleanTheme.NeutralBorder
                        )
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            val icon = if (isFormValid) Icons.Default.CheckCircle else Icons.Default.Info
                            val iconColor = if (isFormValid) CleanTheme.ValidColor else MaterialTheme.colorScheme.onSurfaceVariant
                            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isFormValid) "Dokumentacja gotowa do wysłania" else "Pozostało ${totalFields - completedFields} pól do wypełnienia",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isFormValid) CleanTheme.ValidColor else CleanTheme.DarkText,
                                fontWeight = if (isFormValid) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; imageToDelete = null },
            title = { Text(text = "Usuń zdjęcie", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = "Czy na pewno chcesz usunąć to zdjęcie?", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ta operacja jest nieodwracalna.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CleanTheme.InvalidColor,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { imageToDelete?.let { viewModel.removeImage(it) }; showDeleteDialog = false; imageToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = CleanTheme.InvalidColor),
                    shape = RoundedCornerShape(CleanTheme.cornerRadius)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Usuń", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; imageToDelete = null },
                    shape = RoundedCornerShape(CleanTheme.cornerRadius)
                ) {
                    Text("Anuluj")
                }
            },
            shape = RoundedCornerShape(CleanTheme.cornerRadius)
        )
    }

    if (showSendDialog) {
        AlertDialog(
            onDismissRequest = { showSendDialog = false },
            title = { Text(text = "Wyślij dokumentację", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = "Czy na pewno chcesz wysłać dokumentację zdjęciową?", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CleanTheme.ValidBackground),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CleanTheme.ValidBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Podsumowanie:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = CleanTheme.ValidColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "• Tytuł: ${header.tytul}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "• Magazynier: ${header.magazynier}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "• Miejsce: ${header.miejsce}", style = MaterialTheme.typography.bodySmall)
                            if (header.lokalizacja.isNotBlank()) {
                                Text(text = "• Lokalizacja: ${header.lokalizacja}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(text = "• Liczba zdjęć: ${images.size}", style = MaterialTheme.typography.bodySmall)
                            if (header.numerFeniks.isNotBlank()) {
                                Text(text = "• Numer Feniks: ${header.numerFeniks}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Po wysłaniu nie będzie możliwości edycji.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CleanTheme.InvalidColor,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSendDialog = false
                        viewModel.generateAndSendXml(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CleanTheme.ValidColor),
                    shape = RoundedCornerShape(CleanTheme.cornerRadius)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wyślij", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSendDialog = false },
                    shape = RoundedCornerShape(CleanTheme.cornerRadius)
                ) {
                    Text("Anuluj")
                }
            },
            shape = RoundedCornerShape(CleanTheme.cornerRadius)
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(text = "Niezapisane zmiany", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = { Text("Wyjście z tego ekranu spowoduje usunięcie wszystkich wprowadzonych danych. Czy na pewno chcesz kontynuować?") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.resetState()
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CleanTheme.InvalidColor),
                    shape = RoundedCornerShape(CleanTheme.cornerRadius)
                ) {
                    Text("Wyjdź i odrzuć zmiany", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false },
                    shape = RoundedCornerShape(CleanTheme.cornerRadius)
                ) {
                    Text("Anuluj")
                }
            },
            shape = RoundedCornerShape(CleanTheme.cornerRadius)
        )
    }

    PhotoDocSubmissionDialog(
        submissionState = submissionState,
        onDismiss = { viewModel.resetSubmissionState() }
    )
}

private fun createImageFileUri(context: Context): Uri {
    val imageDirectory = File(context.filesDir, "photodoc_images")
    imageDirectory.mkdirs()
    val file = File(imageDirectory, "hdm_photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}