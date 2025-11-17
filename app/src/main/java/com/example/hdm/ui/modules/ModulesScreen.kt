// ZASTĄP CAŁY PLIK: com/example/hdm/ui/modules/ModulesScreen.kt
package com.example.hdm.ui.modules

import android.Manifest
import android.content.Context
import android.content.Intent // <-- NOWY IMPORT
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.hdm.R
import com.example.hdm.Screen
import com.example.hdm.model.UserManager
import com.example.hdm.services.HealthStatus
import com.example.hdm.services.NetworkHealthMonitor
import com.example.hdm.services.UpdateState
import com.example.hdm.ui.common.DownloadingDialog
import com.example.hdm.ui.common.InstallReadyDialog // <-- NOWY IMPORT
import com.example.hdm.ui.common.UpdateAvailableDialog
import com.example.hdm.ui.common.UpdateErrorDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ModulesTheme {
    val PrimaryColor = Color(0xFF1976D2)
    val SecondaryColor = Color(0xFF2E7D32)
    val AccentColor = Color(0xFFFF9800)
    val DisabledBackground = Color(0xFFFAFAFA)
    val CardBackground = Color.White
    val DarkText = Color(0xFF212121)
    val MediumText = Color(0xFF424242)
    val LightText = Color(0xFF757575)
    val cornerRadius = 12.dp
    val PrimaryBackground = Color(0xFFE3F2FD)
    val SecondaryBackground = Color(0xFFF1F8F2)
}

data class Module(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isEnabled: Boolean = true,
    val route: String? = null,
    val color: Color = ModulesTheme.PrimaryColor,
    val backgroundColor: Color = Color.Transparent,
    val isNew: Boolean = false,
    val badgeCount: Int? = null
)

@Composable
private fun UserHeaderCard(
    loggedInUser: UserManager.LoggedInUser?,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (loggedInUser == null) return

    val backgroundColor by animateColorAsState(
        targetValue = Color(0xFFE8F5E9),
        animationSpec = tween(300),
        label = "headerBg"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(ModulesTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2E7D32)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = loggedInUser.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = "ID: ${loggedInUser.workerId} • ${loggedInUser.login}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            OutlinedButton(
                onClick = onLogout,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFD32F2F)
                ),
                border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = "Wyloguj",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Wyloguj",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun NetworkStatusCard(
    onClick: () -> Unit
) {
    val status by NetworkHealthMonitor.status.collectAsStateWithLifecycle()

    val cardColor by animateColorAsState(
        targetValue = when (status) {
            HealthStatus.OK -> Color(0xFFE8F5E9)
            HealthStatus.CHECKING -> Color(0xFFE3F2FD)
            HealthStatus.WRONG_WIFI, HealthStatus.NO_PERMISSION, HealthStatus.LOCATION_DISABLED -> Color(0xFFFFF3E0)
            HealthStatus.SERVER_ERROR -> Color(0xFFFFEBEE)
        },
        animationSpec = tween(500), label = "statusCardColor"
    )
    val iconAndTextColor by animateColorAsState(
        targetValue = when (status) {
            HealthStatus.OK -> Color(0xFF2E7D32)
            HealthStatus.CHECKING -> Color(0xFF1565C0)
            HealthStatus.WRONG_WIFI, HealthStatus.NO_PERMISSION, HealthStatus.LOCATION_DISABLED -> Color(0xFFF57C00)
            HealthStatus.SERVER_ERROR -> Color(0xFFC62828)
        },
        animationSpec = tween(500), label = "statusTextColor"
    )
    val icon = when (status) {
        HealthStatus.OK -> Icons.Default.Wifi
        HealthStatus.CHECKING -> Icons.Default.Sync
        HealthStatus.WRONG_WIFI -> Icons.Default.SignalWifiOff
        HealthStatus.SERVER_ERROR -> Icons.Default.CloudOff
        HealthStatus.NO_PERMISSION -> Icons.Default.LocationOff
        HealthStatus.LOCATION_DISABLED -> Icons.Default.GpsOff
    }
    val showRefreshButton = status != HealthStatus.OK && status != HealthStatus.CHECKING

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = showRefreshButton, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(ModulesTheme.cornerRadius)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = "Status sieci", tint = iconAndTextColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = status.message,
                color = iconAndTextColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            AnimatedVisibility(
                visible = showRefreshButton,
                enter = fadeIn(animationSpec = tween(delayMillis = 300)),
                exit = fadeOut()
            ) {
                IconButton(onClick = onClick) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Odśwież status", tint = iconAndTextColor.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun AppHeaderCard(
    onTripleClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var clickCount by remember { mutableStateOf(0) }
    var lastClickTime by remember { mutableStateOf(0L) }

    val context = LocalContext.current
    val identifierFileUri by UserManager.identifierFileUri.collectAsStateWithLifecycle()
    var fileName by remember(identifierFileUri) { mutableStateOf<String?>(null) }

    LaunchedEffect(identifierFileUri) {
        fileName = if (identifierFileUri != null) {
            try {
                var name: String? = null
                context.contentResolver.query(identifierFileUri!!, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    }
                }
                name
            } catch (e: Exception) {
                "Błąd odczytu"
            }
        } else {
            null
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "headerScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "headerAlpha"
    )

    val logoOffset by rememberInfiniteTransition(label = "logoFloat").animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoOffset"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(alpha)
            .clickable {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime > 1000) {
                    clickCount = 1
                } else {
                    clickCount++
                }
                lastClickTime = currentTime

                if (clickCount == 3) {
                    clickCount = 0
                    onTripleClick()
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(ModulesTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1976D2).copy(alpha = 0.1f),
                            Color(0xFF42A5F5).copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = RoundedCornerShape(ModulesTheme.cornerRadius)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier
                        .size(64.dp)
                        .offset(y = logoOffset.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Logo HDM",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HDM System",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = ModulesTheme.DarkText
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Elektroniczny system raportowania",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ModulesTheme.MediumText
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = fileName?.substringBeforeLast('.') ?: "Skanowanie...",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ModulesTheme.PrimaryColor
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ModulesTheme.PrimaryColor.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun AnimatedSectionTitle(
    title: String,
    delay: Int = 0
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(600, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(600))
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ModulesTheme.DarkText
        )
    }
}

@Composable
private fun ModuleCard(
    module: Module,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "pressScale"
    )

    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.0f,
        targetValue = if (module.isEnabled) 0.1f else 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val iconRotation by rememberInfiniteTransition(label = "iconRotation").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconRotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale),
        onClick = {
            if (module.isEnabled) {
                isPressed = true
                onClick()
            }
        },
        enabled = module.isEnabled,
        colors = CardDefaults.cardColors(
            containerColor = if (module.isEnabled) ModulesTheme.CardBackground else ModulesTheme.DisabledBackground
        ),
        shape = RoundedCornerShape(ModulesTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Card(
                        modifier = Modifier.size(64.dp),
                        colors = CardDefaults.cardColors(containerColor = module.backgroundColor),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = module.icon,
                                contentDescription = null,
                                tint = module.color,
                                modifier = Modifier
                                    .size(32.dp)
                                    .rotate(if (module.isEnabled) iconRotation else 0f)
                            )
                        }
                    }

                    module.badgeCount?.let { count ->
                        val badgePulse by rememberInfiniteTransition(label = "badgePulse").animateFloat(
                            initialValue = 0.9f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "badgePulse"
                        )

                        Card(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-6).dp)
                                .scale(if (count > 0) badgePulse else 1f)
                                .size(24.dp),
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(
                                containerColor = if (count > 0) Color(0xFFFF4444) else Color(0xFF757575)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (count > 99) "99+" else count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = module.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (module.isEnabled) ModulesTheme.DarkText else ModulesTheme.MediumText
                        )

                        if (module.isNew) {
                            Spacer(modifier = Modifier.width(8.dp))

                            val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
                                initialValue = 0.9f,
                                targetValue = 1.1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulseScale"
                            )

                            Card(
                                modifier = Modifier.scale(pulseScale),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFF4444)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "NOWY",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = module.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ModulesTheme.MediumText
                    )
                }

                if (module.isEnabled) {
                    val arrowScale by animateFloatAsState(
                        targetValue = if (isPressed) 1.2f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessHigh
                        ),
                        label = "arrowScale"
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = module.color,
                        modifier = Modifier.scale(arrowScale)
                    )
                }
            }

            if (module.isEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    module.color.copy(alpha = glowAlpha)
                                ),
                                startX = 0f,
                                endX = Float.POSITIVE_INFINITY
                            )
                        )
                )
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}

@Composable
private fun PasswordEntryDialog(
    passwordError: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var passwordInput by remember { mutableStateOf("") }
    val isError = passwordError || passwordInput.isBlank() && passwordError

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = "Ikona kłódki") },
        title = { Text("Wymagane Hasło") },
        text = {
            Column {
                Text("Aby uzyskać dostęp do tego modułu, podaj hasło.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Hasło") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onConfirm(passwordInput) }),
                    isError = isError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text(
                        text = "Nieprawidłowe hasło.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(passwordInput)  }) {
                Text("Zatwierdź")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModulesScreen(
    navController: NavController,
    viewModel: ModulesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Log.d("ModulesScreen", "Przyznano uprawnienia do aparatu.")
            } else {
                Log.w("ModulesScreen", "Odmówiono uprawnień do aparatu.")
                Toast.makeText(context, "Brak uprawnień do aparatu może uniemożliwić działanie niektórych modułów.", Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        viewModel.checkForUpdates()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loggedInUser by UserManager.loggedInUser.collectAsStateWithLifecycle()

    // --- POCZĄTEK ZMIANY ---
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    // --- KONIEC ZMIANY ---

    val modules = remember {
        listOf(
            Module(id = "label_printing", title = "Drukuj Dokumenty", description = "Generowanie i drukowanie dokumentów przewozowych.", icon = Icons.Default.Print, isEnabled = true, route = Screen.LabelPrinting.route, color = Color(0xFF009688), backgroundColor = Color(0xFFE0F2F1)),
            Module(id = "nagoya_report", title = "Raport Przygotowania Nagoya", description = "Tworzenie i finalizacja raportów wysyłkowych.", icon = Icons.Default.Inventory2, isEnabled = true, route = Screen.NagoyaDashboard.route, color = Color(0xFF673AB7), backgroundColor = Color(0xFFEDE7F6)),
            Module(id = "damage_report", title = "Raport Uszkodzeń", description = "Dokumentacja uszkodzeń palet i towarów.", icon = Icons.Default.ReportProblem, isEnabled = true, route = Screen.ReportType.route, color = ModulesTheme.SecondaryColor, backgroundColor = ModulesTheme.SecondaryBackground),
            Module(id = "photo_documentation", title = "Dokumentacja Zdjęciowa", description = "Przesyłanie zdjęć z notatkami do logistyki.", icon = Icons.Default.PhotoCamera, isEnabled = true, route = Screen.PhotoDoc.route, color = ModulesTheme.PrimaryColor, backgroundColor = ModulesTheme.PrimaryBackground),
            Module(
                id = "pallet_lookup",
                title = "Sprawdzanie Uszkodzonych Palet",
                description = "Wyszukiwanie szczegółowych informacji o uszkodzonych paletach.",
                icon = Icons.Default.Search,
                isEnabled = true,
                route = Screen.PalletLookup.route,
                color = Color(0xFFFF5722),
                backgroundColor = Color(0xFFFFEBEE)
            )
        )
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { route ->
            navController.navigate(route)
        }
    }

    if (uiState.showPasswordDialog) {
        PasswordEntryDialog(
            passwordError = uiState.passwordError,
            onDismiss = { viewModel.dismissPasswordDialog() },
            onConfirm = { password -> viewModel.onPasswordConfirm(password, context) }
        )
    }

    // --- POCZĄTEK ZMIANY ---
    // Pokaż dialogi aktualizacji
    when (val state = updateState) {
        is UpdateState.UpdateAvailable -> {
            UpdateAvailableDialog(
                info = state.info,
                onDownloadClick = { viewModel.startUpdateDownload(state.info.filename) },
                onDismiss = { viewModel.resetUpdateState() }
            )
        }
        is UpdateState.Downloading -> {
            DownloadingDialog(progress = state.progress)
        }
        is UpdateState.Error -> {
            UpdateErrorDialog(
                message = state.message,
                onDismiss = { viewModel.resetUpdateState() }
            )
        }
        is UpdateState.DownloadReadyToInstall -> {
            // Tworzymy intencję instalacji
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(state.apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            InstallReadyDialog(
                onInstallClick = {
                    try {
                        context.startActivity(installIntent)
                    } catch (e: Exception) {
                        Log.e("ModulesScreen", "Nie można uruchomić instalatora APK", e)
                    }
                    viewModel.resetUpdateState()
                },
                onDismiss = { viewModel.resetUpdateState() }
            )
        }
        is UpdateState.Idle -> {
            // Nic nie rób
        }
    }
    // --- KONIEC ZMIANY ---

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moduły Systemu", fontWeight = FontWeight.Medium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (loggedInUser != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    loggedInUser!!.fullName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1B5E20)
                                )
                            }

                            TextButton(
                                onClick = {
                                    UserManager.logoutUser()
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Modules.route) { inclusive = true }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Logout,
                                    contentDescription = "Wyloguj",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFFD32F2F)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Wyloguj",
                                    color = Color(0xFFD32F2F),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                NetworkStatusCard(
                    onClick = { viewModel.rerunNetworkChecks(context) }
                )
            }

            item {
                AppHeaderCard(
                    onTripleClick = { navController.navigate(Screen.LogMonitor.route) }
                )
            }

            item {
                AnimatedSectionTitle(title = "Dostępne Moduły", delay = 400)
            }

            itemsIndexed(modules) { index, module ->
                ModuleCard(
                    module = module,
                    onClick = { viewModel.onModuleClicked(module, context) }
                )
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Wersja aplikacji: 1.9",
                        style = MaterialTheme.typography.bodySmall,
                        color = ModulesTheme.LightText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}