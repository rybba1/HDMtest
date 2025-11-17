package com.example.hdm.ui.splash

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.hdm.R
import com.example.hdm.Screen
import com.example.hdm.services.DownloadState
import com.example.hdm.services.HdmMonitoringService
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val translationDownloadState by viewModel.translationService.downloadState.collectAsStateWithLifecycle()

    val permissionsToRequest = remember {
        mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(Manifest.permission.FOREGROUND_SERVICE)
            }
        }.toTypedArray()
    }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                Log.d("SplashScreen", "Wszystkie wymagane uprawnienia zostały przyznane.")
            } else {
                Log.w("SplashScreen", "Nie wszystkie uprawnienia zostały przyznane.")
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || permissions[Manifest.permission.POST_NOTIFICATIONS] == true) {
                val serviceIntent = Intent(context, HdmMonitoringService::class.java)
                context.startService(serviceIntent)
            }

            viewModel.triggerStartupTasks()
        }
    )

    LaunchedEffect(Unit) {
        viewModel.openWifiSettingsEvent.collect {
            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
    }

    LaunchedEffect(key1 = true) {
        multiplePermissionsLauncher.launch(permissionsToRequest)
    }

    LaunchedEffect(uiState) {
        if (uiState == SplashUiState.ALL_DONE) {
            delay(500)
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1976D2),
                        Color(0xFF42A5F5),
                        Color(0xFF90CAF9)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(targetState = uiState, label = "StateAnimator") { state ->
            when (state) {
                SplashUiState.INITIAL_LOADING -> InitialLoadingView(downloadState = translationDownloadState)
                SplashUiState.REQUIRES_GUEST_WIFI -> NetworkSwitchPrompt(
                    title = "Wymagane połączenie z Internetem",
                    message = "Do pobrania modeli tłumaczeń potrzebny jest internet. Przejdź do ustawień i połącz się z siecią MEIKO-GUEST.",
                    buttonText = "Otwórz Ustawienia Wi-Fi",
                    onConnectClick = { viewModel.requestOpenWifiSettings() },
                    onCheckAgainClick = { viewModel.startModelDownload() }
                )
                SplashUiState.DOWNLOADING_MODELS -> DownloadingView(state = translationDownloadState)
                SplashUiState.REQUIRES_WMS_WIFI -> NetworkSwitchPrompt(
                    title = "Pobieranie zakończone",
                    message = "Modele tłumaczeń zostały pobrane. Aby kontynuować, wróć do sieci WMS.",
                    buttonText = "Otwórz Ustawienia Wi-Fi",
                    onConnectClick = { viewModel.requestOpenWifiSettings() },
                    onCheckAgainClick = { viewModel.checkIfOnWmsAndProceed() }
                )
                SplashUiState.ALL_DONE -> InitialLoadingView(downloadState = translationDownloadState, text = "Finalizacja...")
            }
        }
    }
}

@Composable
private fun InitialLoadingView(downloadState: DownloadState, text: String? = null) {
    var startAnimation by remember { mutableStateOf(false) }
    val density = LocalDensity.current.density

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    val logoRotationY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 180f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "logoFlip"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "logoAlpha"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, delayMillis = 500, easing = FastOutSlowInEasing),
        label = "textAlpha"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .size(225.dp)
                .scale(logoScale)
                .alpha(logoAlpha)
                .graphicsLayer {
                    rotationY = logoRotationY
                    cameraDistance = 12f * density
                },
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo aplikacji",
                    modifier = Modifier
                        .size(150.dp)
                        .padding(12.dp)
                        .graphicsLayer { rotationY = -logoRotationY }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "HDM",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.alpha(textAlpha),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Elektroniczny system raportowania",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.alpha(textAlpha),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "i przesyłania dokumentacji zdjęciowej",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.alpha(textAlpha),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(60.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(textAlpha)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    val delay = index * 200
                    val animatedScale by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 600, delayMillis = delay, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$index"
                    )
                    Card(
                        modifier = Modifier
                            .size(8.dp)
                            .scale(animatedScale),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f))
                    ) {}
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = text ?: when (downloadState) {
                    DownloadState.DOWNLOADING -> "Pobieranie modeli tłumaczeń..."
                    DownloadState.FAILED -> "Błąd pobierania modeli"
                    else -> "Inicjalizacja aplikacji..."
                },
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NetworkSwitchPrompt(
    title: String,
    message: String,
    buttonText: String,
    onConnectClick: () -> Unit,
    onCheckAgainClick: () -> Unit
) {
    Card(
        modifier = Modifier.padding(32.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Button(onClick = onConnectClick, modifier = Modifier.fillMaxWidth()) {
                Text(buttonText)
            }
            TextButton(onClick = onCheckAgainClick) {
                Text("Sprawdź ponownie połączenie")
            }
        }
    }
}

@Composable
private fun DownloadingView(state: DownloadState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = when (state) {
                DownloadState.DOWNLOADING -> "Pobieranie modeli tłumaczeń..."
                DownloadState.FAILED -> "Błąd pobierania. Sprawdź połączenie z internetem."
                else -> "Proszę czekać..."
            },
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
    }
}