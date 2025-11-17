// Ścieżka: com/example/hdm/ui/bhpstats/BhpStatsScreen.kt
package com.example.hdm.ui.bhpstats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext // Może być potrzebny w przyszłości
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.hdm.model.BhpStatsUser
import com.example.hdm.model.UserManager
import kotlinx.coroutines.flow.map // Potrzebny do mapowania StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BhpStatsScreen(
    navController: NavController,
    viewModel: BhpStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loggedInUserState by UserManager.loggedInUser.collectAsStateWithLifecycle()
    val currentUserLogin = loggedInUserState?.login // Pobierz login ze stanu

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(message = "Błąd: $it", duration = SnackbarDuration.Long)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Statystyki BHP") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Powrót")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Odśwież")
                    }
                    // Usunięto przycisk Reset
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F5F5)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { // Twoje statystyki
                        UserStatsCard(
                            userStats = uiState.currentUserStats,
                            userPosition = uiState.userPositionInRanking
                        )
                    }

                    if (uiState.ranking.isNotEmpty()) { // Ranking
                        item {
                            Text(
                                text = "🏆 Ranking magazynierów (Top ${uiState.ranking.size})",
                                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        itemsIndexed(uiState.ranking) { index, stats ->
                            RankingItemCard(
                                index = index,
                                stats = stats,
                                currentUserLogin = currentUserLogin
                            )
                        }
                    } else if (!uiState.isLoading) { // Pusty ranking
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Text("Ranking jest jeszcze pusty.", modifier = Modifier.padding(16.dp), color = Color.Gray, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserStatsCard(userStats: BhpStatsUser?, userPosition: Int) {
    val stats = userStats // Lokalna zmienna dla smart cast

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        if (stats != null) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row( // Nagłówek karty
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box( // Ikona
                        modifier = Modifier.size(56.dp).background(brush = Brush.linearGradient(colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500))), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Person, null, modifier = Modifier.size(32.dp), tint = Color.White) }

                    Column { // Nazwa i pozycja
                        Text(stats.fullName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                        if (userPosition > 0) {
                            Text("#$userPosition w rankingu", fontSize = 14.sp, color = Color(0xFF757575))
                        } else {
                            Text("Poza rankingiem", fontSize = 14.sp, color = Color(0xFF757575))
                        }
                    }
                }
                HorizontalDivider()
                Row( // Punkty, Poprawne, Błędne
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(Icons.Default.EmojiEvents, "Punkty", stats.totalPoints.toString(), Color(0xFFFFD700))
                    StatItem(Icons.Default.CheckCircle, "Poprawne", stats.correctAnswers.toString(), Color(0xFF4CAF50))
                    StatItem(Icons.Default.Cancel, "Błędne", stats.wrongAnswers.toString(), Color(0xFFE53935))
                }
                Column( // Skuteczność
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Skuteczność", fontSize = 14.sp, color = Color(0xFF757575))
                        Text("${stats.accuracy.toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                    }
                    LinearProgressIndicator(
                        progress = { stats.accuracy / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Color(0xFF1976D2), trackColor = Color(0xFFE0E0E0)
                    )
                }
            }
        } else { // Widok braku statystyk
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.Quiz, null, modifier = Modifier.size(64.dp), tint = Color(0xFF757575))
                Text("Brak statystyk BHP", fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Odpowiadaj na pytania przed zalogowaniem...", fontSize = 14.sp, color = Color(0xFF757575), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun RankingItemCard(index: Int, stats: BhpStatsUser, currentUserLogin: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (stats.login == currentUserLogin) Color(0xFF1976D2).copy(alpha = 0.1f) else Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box( // Pozycja w kółku
                    modifier = Modifier.size(40.dp).background(
                        color = when (index) { 0 -> Color(0xFFFFD700); 1 -> Color(0xFFC0C0C0); 2 -> Color(0xFFCD7F32); else -> Color(0xFFE0E0E0) },
                        shape = CircleShape
                    ), contentAlignment = Alignment.Center
                ) { Text("${index + 1}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (index < 3) Color.White else Color(0xFF757575)) }

                Column { // Nazwa i info
                    Text(
                        stats.fullName,
                        fontSize = 16.sp,
                        fontWeight = if (stats.login == currentUserLogin) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        "${stats.totalAnswers} ${getQuizSuffix(stats.totalAnswers)} • ${stats.accuracy.toInt()}% skuteczności",
                        fontSize = 12.sp, color = Color(0xFF757575)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) { // Punkty
                Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(20.dp), tint = Color(0xFFFFD700))
                Text(stats.totalPoints.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
            }
        }
    }
}

private fun getQuizSuffix(count: Int): String {
    return when {
        count == 1 -> "quiz"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "quizy"
        else -> "quizów"
    }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = color)
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
        Text(label, fontSize = 12.sp, color = Color(0xFF757575))
    }
}