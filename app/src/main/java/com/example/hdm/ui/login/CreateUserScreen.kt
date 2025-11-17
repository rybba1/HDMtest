package com.example.hdm.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.hdm.Screen

// Theme
private object CreateUserTheme {
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
    val NeutralBorder = Color(0xFFE0E0E0)
    val DarkText = Color(0xFF212121)
    val MediumText = Color(0xFF424242)
    val LightText = Color(0xFF757575)
    val cornerRadius = 12.dp
    val animationDuration = 250
}

enum class CreateUserValidationState {
    EMPTY, VALID, INVALID
}

@Composable
private fun ValidationIcon(
    validationState: CreateUserValidationState,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (validationState) {
        CreateUserValidationState.VALID -> Icons.Default.Check to CreateUserTheme.ValidColor
        CreateUserValidationState.INVALID -> Icons.Default.ErrorOutline to CreateUserTheme.InvalidColor
        CreateUserValidationState.EMPTY -> return
    }

    AnimatedVisibility(
        visible = validationState != CreateUserValidationState.EMPTY,
        enter = fadeIn(animationSpec = tween(CreateUserTheme.animationDuration)),
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
    validationState: CreateUserValidationState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPassword: Boolean = false
) {
    val borderColor by animateColorAsState(
        targetValue = when (validationState) {
            CreateUserValidationState.VALID -> CreateUserTheme.ValidBorder
            CreateUserValidationState.INVALID -> CreateUserTheme.InvalidBorder
            CreateUserValidationState.EMPTY -> CreateUserTheme.NeutralBorder
        },
        animationSpec = tween(CreateUserTheme.animationDuration),
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when (validationState) {
            CreateUserValidationState.VALID -> CreateUserTheme.ValidBackground
            CreateUserValidationState.INVALID -> CreateUserTheme.InvalidBackground
            CreateUserValidationState.EMPTY -> Color.White
        },
        animationSpec = tween(CreateUserTheme.animationDuration),
        label = "backgroundColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(CreateUserTheme.cornerRadius)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(CreateUserTheme.cornerRadius)
            )
            .clip(RoundedCornerShape(CreateUserTheme.cornerRadius))
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = CreateUserTheme.MediumText) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedTextColor = CreateUserTheme.DarkText,
                unfocusedTextColor = CreateUserTheme.DarkText,
                disabledTextColor = CreateUserTheme.LightText,
                focusedLabelColor = CreateUserTheme.MediumText,
                unfocusedLabelColor = CreateUserTheme.LightText
            ),
            trailingIcon = {
                ValidationIcon(validationState = validationState)
            }
        )
    }
}

@Composable
private fun CreationDialog(
    state: CreateUserState,
    onDismiss: () -> Unit,
    onBackToLogin: () -> Unit
) {
    if (state is CreateUserState.Idle) return

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
                    is CreateUserState.Sending -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            color = CreateUserTheme.ValidColor
                        )
                        Text(
                            "Tworzenie użytkownika...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Wysyłanie danych do serwera",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CreateUserTheme.MediumText
                        )
                    }
                    is CreateUserState.Success -> {
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
                            tint = CreateUserTheme.ValidColor,
                            modifier = Modifier.size(64.dp).scale(scale)
                        )
                        Text(
                            "Użytkownik utworzony!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CreateUserTheme.ValidColor
                        )
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = CreateUserTheme.MediumText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onBackToLogin,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CreateUserTheme.ValidColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Powrót do logowania")
                        }
                    }
                    is CreateUserState.Error -> {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Błąd",
                            tint = CreateUserTheme.InvalidColor,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            "Błąd tworzenia użytkownika",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CreateUserTheme.InvalidColor
                        )
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = CreateUserTheme.MediumText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CreateUserTheme.InvalidColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Spróbuj ponownie")
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateUserScreen(
    navController: NavController,
    viewModel: CreateUserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val PASSWORD_SCAN_KEY = "create_user_password_scan"

    // Obsługa wyniku skanowania
    val scanResult = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<String>(PASSWORD_SCAN_KEY)

    scanResult?.observe(navController.currentBackStackEntry!!) { scannedPassword ->
        if (scannedPassword != null) {
            viewModel.setPassword(scannedPassword)
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>(PASSWORD_SCAN_KEY)
        }
    }

    // Walidacja pól
    val loginValidation = remember(uiState.login) {
        when {
            uiState.login.isBlank() -> CreateUserValidationState.EMPTY
            uiState.login.length < 3 -> CreateUserValidationState.INVALID
            else -> CreateUserValidationState.VALID
        }
    }

    val passwordValidation = remember(uiState.password) {
        when {
            uiState.password.isBlank() -> CreateUserValidationState.EMPTY
            uiState.password.length < 4 -> CreateUserValidationState.INVALID
            else -> CreateUserValidationState.VALID
        }
    }

    val isFormValid = loginValidation == CreateUserValidationState.VALID &&
            passwordValidation == CreateUserValidationState.VALID

    // Dialog statusu
    CreationDialog(
        state = uiState.creationState,
        onDismiss = { viewModel.resetCreationState() },
        onBackToLogin = {
            viewModel.resetForm()
            navController.popBackStack()
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dodawanie użytkownika", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE3F2FD)
                ),
                shape = RoundedCornerShape(CreateUserTheme.cornerRadius)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Wpisz login i zeskanuj hasło z karty QR pracownika",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CreateUserTheme.DarkText
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Login field
            Text(
                "Login pracownika",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = CreateUserTheme.DarkText
            )
            CleanTextField(
                value = uiState.login,
                onValueChange = viewModel::setLogin,
                label = "Gdy jesteś Jan kowalski to wpisz jkowalski",
                validationState = loginValidation
            )
            if (loginValidation == CreateUserValidationState.INVALID) {
                Text(
                    "Login musi mieć minimum 3 znaki",
                    style = MaterialTheme.typography.bodySmall,
                    color = CreateUserTheme.InvalidColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Password field
            Text(
                "Hasło z karty QR",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = CreateUserTheme.DarkText
            )
            CleanTextField(
                value = uiState.password,
                onValueChange = {},
                label = if (uiState.password.isBlank()) "Zeskanuj kod QR" else "●●●●●●●●",
                validationState = passwordValidation,
                enabled = false,
                isPassword = true
            )

            Button(
                onClick = {
                    navController.navigate(
                        Screen.Scanner.withArgs("ALL_FORMATS", PASSWORD_SCAN_KEY, null)
                    )
                },
                enabled = loginValidation == CreateUserValidationState.VALID,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (loginValidation == CreateUserValidationState.VALID)
                        Color(0xFF1976D2)
                    else
                        CreateUserTheme.NeutralBackground
                ),
                shape = RoundedCornerShape(CreateUserTheme.cornerRadius)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Skanuj hasło z karty")
            }

            Spacer(modifier = Modifier.weight(1f))

            // Submit button
            Button(
                onClick = { viewModel.createUser(context) },
                enabled = isFormValid && uiState.creationState is CreateUserState.Idle,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFormValid) CreateUserTheme.ValidColor else CreateUserTheme.NeutralBackground,
                    contentColor = if (isFormValid) Color.White else CreateUserTheme.LightText
                ),
                shape = RoundedCornerShape(CreateUserTheme.cornerRadius)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Utwórz użytkownika",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}