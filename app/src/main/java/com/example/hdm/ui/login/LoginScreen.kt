// Ścieżka: com/example/hdm/ui/login/LoginScreen.kt
package com.example.hdm.ui.login

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.hdm.Screen
import com.example.hdm.services.UpdateState
import com.example.hdm.ui.common.DownloadingDialog
import com.example.hdm.ui.common.InstallReadyDialog
import com.example.hdm.ui.common.UpdateAvailableDialog
import com.example.hdm.ui.common.UpdateErrorDialog
import com.example.hdm.model.SafetyTips
import com.example.hdm.model.SafetyQuizzes
import com.example.hdm.model.UserManager
import com.example.hdm.repository.QuizStatisticsRepository
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.delay
import com.example.hdm.repository.QuizAnswerStats
import androidx.compose.foundation.BorderStroke // Dodano import


sealed class SafetyContent {
    data class Tip(val tip: com.example.hdm.model.SafetyTip) : SafetyContent()
    data class Quiz(val quiz: com.example.hdm.model.SafetyQuiz) : SafetyContent()
}

@Composable
private fun LoginDialog(
    state: LoginState,
    onDismiss: () -> Unit,
    onNavigateToModules: () -> Unit
) {
    if (state is LoginState.Idle) return

    Dialog(
        onDismissRequest = { }, // Celowo puste, dialog znika tylko po sukcesie lub błędzie
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
                    is LoginState.Syncing -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            color = Color(0xFF1976D2)
                        )
                        Text(
                            "Synchronizacja danych...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Pobieranie bazy użytkowników",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF757575)
                        )
                    }
                    is LoginState.Validating -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            color = Color(0xFF1976D2)
                        )
                        Text(
                            "Sprawdzanie karty...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is LoginState.Success -> {
                        val context = LocalContext.current
                        val pendingPoints = remember { QuizStatisticsRepository.getPendingPointsCount(context) }

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
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(64.dp).scale(scale)
                        )
                        Text(
                            "Zalogowano!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            "Witaj, ${state.userName}",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        // Pokaż zdobyte punkty jeśli były
                        if (pendingPoints != 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (pendingPoints > 0) {
                                    Color(0xFF4CAF50).copy(alpha = 0.1f)
                                } else {
                                    Color(0xFFFF9800).copy(alpha = 0.1f)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (pendingPoints > 0) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                    )
                                    Text(
                                        text = if (pendingPoints > 0) {
                                            "+$pendingPoints pkt BHP"
                                        } else {
                                            "$pendingPoints pkt BHP"
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pendingPoints > 0) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                    )
                                }
                            }
                        }

                        // Nawigacja po opóźnieniu
                        LaunchedEffect(Unit) {
                            delay(1500) // Czekaj 1.5 sekundy
                            onNavigateToModules()
                        }
                    }
                    is LoginState.Error -> {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Błąd",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            "Błąd logowania",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF757575)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onDismiss, // Pozwala zamknąć dialog błędu
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1976D2)
                            )
                        ) {
                            Text("OK")
                        }
                    }
                    else -> {} // Obsługa LoginState.Idle (choć nie powinien tu wystąpić)
                }
            }
        }
    }
}


// Funkcja PasswordEntryDialog bez zmian
@Composable
private fun PasswordEntryDialog(
    passwordError: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Autoryzacja",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Wprowadź hasło administratora, aby dodać nowego użytkownika:",
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (password.isNotBlank()) {
                                onConfirm(password)
                            }
                        }
                    ),
                    isError = passwordError,
                    supportingText = if (passwordError) {
                        { Text("Nieprawidłowe hasło", color = Color(0xFFD32F2F)) }
                    } else null
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isNotBlank()) {
                        onConfirm(password)
                    }
                },
                enabled = password.isNotBlank()
            ) {
                Text("Potwierdź")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

// Funkcje getInitialContent i getNextContent bez zmian
private fun getInitialContent(): SafetyContent {
    return if (Math.random() > 0.5) {
        SafetyContent.Tip(SafetyTips.getRandomTip())
    } else {
        SafetyContent.Quiz(SafetyQuizzes.getRandomQuiz())
    }
}
private fun getNextContent(current: SafetyContent): SafetyContent {
    return when (current) {
        is SafetyContent.Quiz -> SafetyContent.Tip(SafetyTips.getRandomTip())
        is SafetyContent.Tip -> SafetyContent.Quiz(SafetyQuizzes.getRandomQuiz())
    }
}

// Funkcja SafetyContentCard - wypełniona
@Composable
private fun SafetyContentCard() {
    var currentContent by remember {
        mutableStateOf(getInitialContent())
    }
    val context = LocalContext.current

    LaunchedEffect(currentContent) {
        if (currentContent is SafetyContent.Tip) {
            delay(10_000L)
            currentContent = getNextContent(currentContent)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        when (val content = currentContent) {
            is SafetyContent.Tip -> {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = content.tip.icon,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFF1976D2)
                        )
                        Text(
                            text = content.tip.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                    }
                    Text(
                        text = content.tip.message,
                        fontSize = 14.sp,
                        color = Color(0xFF424242),
                        lineHeight = 20.sp
                    )
                }
            }
            is SafetyContent.Quiz -> {
                var selectedAnswer by remember(content.quiz.id) { mutableStateOf<Int?>(null) }
                var showResult by remember(content.quiz.id) { mutableStateOf(false) }
                var hasAnswered by remember(content.quiz.id) { mutableStateOf(false) }
                var showStats by remember(content.quiz.id) { mutableStateOf(false) }

                LaunchedEffect(hasAnswered) {
                    if (hasAnswered) {
                        delay(10_000L)
                        showStats = true
                        delay(10_000L)
                        currentContent = getNextContent(currentContent)
                    }
                }

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFFFF6F00)
                        )
                        Text(
                            text = "Quiz BHP",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6F00)
                        )
                    }

                    if (showStats) {
                        if (showResult) { // Pokaż punkty
                            val isCorrect = selectedAnswer == content.quiz.correctAnswerIndex
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCorrect) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFFF9800).copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (isCorrect) "Brawo! +1 pkt BHP" else "Spróbuj kolejnego pytania",
                                        fontSize = 14.sp,
                                        color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                    )
                                }
                            }
                        }
                        val stats = remember(content.quiz.id) { // Pokaż statystyki
                            QuizStatisticsRepository.getStats(context, content.quiz.id)
                        }
                        QuizStatsView(stats = stats, correctAnswerIndex = content.quiz.correctAnswerIndex)

                        Spacer(modifier = Modifier.height(8.dp)) // Pokaż progress bar
                        Text(
                            "Następna wskazówka za chwilę...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

                    } else { // Pokaż pytanie i odpowiedzi
                        Text(
                            content.quiz.question,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF212121),
                            lineHeight = 22.sp
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            content.quiz.answers.forEachIndexed { index, answer ->
                                val isSelected = selectedAnswer == index
                                val isCorrect = index == content.quiz.correctAnswerIndex
                                val backgroundColor = when {
                                    !showResult -> if (isSelected) Color(0xFF1976D2).copy(alpha = 0.1f) else Color.Transparent
                                    isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    isSelected && !isCorrect -> Color(0xFFE53935).copy(alpha = 0.2f)
                                    else -> Color.Transparent
                                }
                                val borderColor = when {
                                    !showResult -> if (isSelected) Color(0xFF1976D2) else Color(0xFFE0E0E0)
                                    isCorrect -> Color(0xFF4CAF50)
                                    isSelected && !isCorrect -> Color(0xFFE53935)
                                    else -> Color(0xFFE0E0E0)
                                }

                                OutlinedButton(
                                    onClick = { if (!hasAnswered) selectedAnswer = index },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !hasAnswered,
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = backgroundColor),
                                    border = BorderStroke(2.dp, borderColor),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            answer,
                                            color = Color(0xFF212121),
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (showResult) {
                                            Icon(
                                                imageVector = if (isCorrect) Icons.Default.CheckCircle else if (isSelected) Icons.Default.Cancel else Icons.Default.Circle,
                                                contentDescription = null,
                                                tint = if (isCorrect) Color(0xFF4CAF50) else if (isSelected) Color(0xFFE53935) else Color.Transparent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!hasAnswered) { // Pokaż przycisk "Sprawdź"
                            Button(
                                onClick = {
                                    if (selectedAnswer != null) {
                                        showResult = true
                                        hasAnswered = true
                                        QuizStatisticsRepository.recordAnswer(context, content.quiz.id, selectedAnswer!!)
                                        val wasCorrect = selectedAnswer == content.quiz.correctAnswerIndex
                                        QuizStatisticsRepository.addPendingPoints(context, content.quiz.id, wasCorrect)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = selectedAnswer != null,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6F00)),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("Sprawdź odpowiedź") }
                        } else { // Pokaż wyjaśnienie (przed statystykami)
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (showResult) {
                                    val isCorrect = selectedAnswer == content.quiz.correctAnswerIndex
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCorrect) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFFF9800).copy(alpha = 0.1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Info,
                                                contentDescription = null,
                                                tint = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                if (isCorrect) "Brawo! +1 pkt BHP" else "Spróbuj kolejnego pytania",
                                                fontSize = 14.sp,
                                                color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                            )
                                        }
                                    }
                                    Text(
                                        content.quiz.explanation,
                                        fontSize = 13.sp,
                                        color = Color(0xFF616161),
                                        lineHeight = 18.sp
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


// Funkcja QuizStatsView bez zmian
@Composable
private fun QuizStatsView(stats: QuizAnswerStats?, correctAnswerIndex: Int) {
    val percentages = remember(stats) { stats?.getPercentages() }
    val answerLabels = remember(percentages) {
        percentages?.let { List(it.size) { ('A' + it).toString() } }
    }

    AnimatedVisibility(
        visible = stats != null && stats.totalAnswers > 0 && percentages != null,
        enter = fadeIn(animationSpec = tween(500))
    ) {
        if (stats != null && percentages != null && answerLabels != null) {
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tak odpowiadali inni (${stats.totalAnswers} ${if (stats.totalAnswers == 1) "osoba" else "osób"}):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
                percentages.forEachIndexed { index, percentage ->
                    val isCorrect = index == correctAnswerIndex
                    val barColor = if (isCorrect) Color(0xFF4CAF50) else Color(0xFF757575)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            answerLabels[index],
                            fontWeight = FontWeight.Bold, color = barColor, modifier = Modifier.width(20.dp)
                        )
                        LinearProgressIndicator(
                            progress = { percentage / 100f },
                            modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)),
                            color = barColor, trackColor = Color(0xFFE0E0E0),
                        )
                        Text(
                            "${percentage.toInt()}%",
                            fontWeight = FontWeight.Bold, color = barColor, modifier = Modifier.width(40.dp), textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }

    AnimatedVisibility( // Komunikat "Bądź pierwszy" bez zmian
        visible = stats == null || stats.totalAnswers == 0,
        enter = fadeIn(animationSpec = tween(500))
    ) {
        Text(
            "Bądź pierwszą osobą, która odpowie na to pytanie!",
            style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}


// Główna funkcja LoginScreen
@Composable
fun LoginScreen(
    navController: NavController,
    scannedData: String?,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    var startAnimation by remember { mutableStateOf(false) }
    val CARD_SCAN_KEY = "login_card_scan"

    LaunchedEffect(Unit) {
        startAnimation = true
        viewModel.syncCredentialsIfNeeded(context)
        viewModel.checkForUpdates()
    }

    // === POCZĄTEK POPRAWKI ===
    // Usuwamy 'by' i pobieramy State, obsługując null
    val cardScanResultState: State<String?> = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>(CARD_SCAN_KEY, null)
        ?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) } // Zapewnij domyślny State, jeśli backStackEntry jest null

    // Odczytaj wartość ze State
    val cardScanResult = cardScanResultState.value
    // === KONIEC POPRAWKI ===


    LaunchedEffect(cardScanResult) {
        if (cardScanResult != null) {
            viewModel.loginWithCard(context, cardScanResult) // Nie potrzeba '!!'
            navController.currentBackStackEntry?.savedStateHandle?.set(CARD_SCAN_KEY, null)
        }
    }

    LaunchedEffect(scannedData) {
        if (scannedData != null && scannedData.isNotBlank()) {
            viewModel.loginWithCard(context, scannedData)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { route ->
            navController.navigate(route)
        }
    }

    if (uiState.showCreateUserPasswordDialog) {
        PasswordEntryDialog(
            passwordError = uiState.createUserPasswordError,
            onDismiss = { viewModel.dismissCreateUserPasswordDialog() },
            onConfirm = { password -> viewModel.onPasswordForCreateUserConfirm(password, context) }
        )
    }

    LoginDialog(
        state = uiState.loginState,
        onDismiss = { viewModel.resetLoginState() },
        onNavigateToModules = {
            navController.navigate(Screen.Modules.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    )

    when (val state = updateState) { // Obsługa aktualizacji bez zmian
        is UpdateState.UpdateAvailable -> UpdateAvailableDialog(
            info = state.info, onDownloadClick = { viewModel.startUpdateDownload(state.info.filename) }, onDismiss = { viewModel.resetUpdateState() }
        )
        is UpdateState.Downloading -> DownloadingDialog(state.progress)
        is UpdateState.Error -> UpdateErrorDialog(
            message = state.message, onDismiss = { viewModel.resetUpdateState() }
        )
        is UpdateState.DownloadReadyToInstall -> {
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(state.apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            InstallReadyDialog(
                onInstallClick = {
                    try { context.startActivity(installIntent) } catch (e: Exception) { Log.e("LoginScreen", "Nie można uruchomić instalatora APK", e) }
                    viewModel.resetUpdateState()
                },
                onDismiss = { viewModel.resetUpdateState() }
            )
        }
        is UpdateState.Idle -> {}
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "contentAlpha"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(
            brush = Brush.verticalGradient(colors = listOf(Color(0xFF1976D2), Color(0xFF42A5F5), Color(0xFF90CAF9)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SafetyContentCard() // Karta BHP/Quizu

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "HDM",
                    fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center
                )
                Text(
                    "System raportowania magazynu",
                    fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ===== POCZĄTEK ZMIANY (Usunięto pola logowania ręcznego) =====
            // USUNIĘTO: OutlinedTextField
            // USUNIĘTO: Button("Zaloguj ręcznie")
            // USUNIĘTO: Spacer
            // USUNIĘTO: Text("— LUB —")
            // USUNIĘTO: Spacer
            // ===== KONIEC ZMIANY =====

            Button( // Przycisk logowania (skanowanie)
                onClick = { navController.navigate(Screen.Scanner.withArgs("ALL_FORMATS", CARD_SCAN_KEY, null)) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !uiState.isSyncInProgress,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1976D2)),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Skanuj kartę pracownika", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton( // Przycisk dodawania użytkownika
                onClick = { viewModel.onAddNewUserClicked() }
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dodaj nowego użytkownika", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        Box( // Numer wersji na dole
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                "v1.6", // UWAGA: Ta wersja jest zahardkodowana tutaj
                fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.alpha(contentAlpha)
            )
        }
    }
}