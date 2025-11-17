package com.example.hdm.ui.description

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hdm.Screen
import com.example.hdm.ui.header.HeaderViewModel

// SYSTEM WALIDACJI dla EventDescriptionScreen
enum class DescriptionValidationState {
    EMPTY, TOO_SHORT, VALID, INVALID, IN_PROGRESS
}

data class DescriptionValidation(
    val state: DescriptionValidationState,
    val message: String = ""
)

// THEME SYSTEM dla EventDescriptionScreen (zgodny z PlacementTheme)
object DescriptionTheme {
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
    val InProgressBorder = Color(0xFFBBDEFB)

    val DarkText = Color(0xFF212121)
    val MediumText = Color(0xFF424242)
    val LightText = Color(0xFF757575)

    val cornerRadius = 12.dp
    val borderWidth = 1.dp
    val animationDuration = 250
}

@Composable
private fun DescriptionValidationIcon(
    validationState: DescriptionValidationState,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (validationState) {
        DescriptionValidationState.VALID -> Icons.Default.CheckCircle to DescriptionTheme.ValidColor
        DescriptionValidationState.INVALID -> Icons.Default.Error to DescriptionTheme.InvalidColor
        DescriptionValidationState.TOO_SHORT -> Icons.Default.Warning to DescriptionTheme.WarningColor
        DescriptionValidationState.EMPTY -> Icons.Default.Edit to DescriptionTheme.EmptyColor
        DescriptionValidationState.IN_PROGRESS -> Icons.Default.Schedule to DescriptionTheme.InProgressColor
    }

    val scale by animateFloatAsState(
        targetValue = if (validationState == DescriptionValidationState.VALID) 1.1f else 1f,
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
private fun DescriptionStatusCard(
    validation: DescriptionValidation,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (validation.state) {
            DescriptionValidationState.VALID -> DescriptionTheme.ValidBackground
            DescriptionValidationState.INVALID -> DescriptionTheme.InvalidBackground
            DescriptionValidationState.TOO_SHORT -> DescriptionTheme.WarningBackground
            DescriptionValidationState.EMPTY -> DescriptionTheme.NeutralBackground
            DescriptionValidationState.IN_PROGRESS -> DescriptionTheme.InProgressBackground
        },
        animationSpec = tween(DescriptionTheme.animationDuration),
        label = "backgroundColor"
    )

    val textColor = when (validation.state) {
        DescriptionValidationState.VALID -> DescriptionTheme.ValidColor
        DescriptionValidationState.INVALID -> DescriptionTheme.InvalidColor
        DescriptionValidationState.TOO_SHORT -> DescriptionTheme.WarningColor
        DescriptionValidationState.EMPTY -> DescriptionTheme.LightText
        DescriptionValidationState.IN_PROGRESS -> DescriptionTheme.InProgressColor
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(DescriptionTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DescriptionValidationIcon(validationState = validation.state)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = validation.message,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (validation.state == DescriptionValidationState.VALID) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun DescriptionProgressCard(
    currentLength: Int,
    targetLength: Int = 150,
    modifier: Modifier = Modifier
) {
    val progress = (currentLength.toFloat() / targetLength.toFloat()).coerceIn(0f, 1f)

    val validation = when {
        currentLength == 0 -> DescriptionValidation(
            state = DescriptionValidationState.EMPTY,
            message = "Zacznij pisać opis zdarzenia"
        )
        currentLength < 50 -> DescriptionValidation(
            state = DescriptionValidationState.TOO_SHORT,
            message = "Opis jest za krótki - dodaj więcej szczegółów"
        )
        currentLength < targetLength -> DescriptionValidation(
            state = DescriptionValidationState.IN_PROGRESS,
            message = "Dobry postęp - możesz dodać więcej szczegółów"
        )
        else -> DescriptionValidation(
            state = DescriptionValidationState.VALID,
            message = "Opis jest wystarczająco szczegółowy"
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(DescriptionTheme.cornerRadius),
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
                    "Jakość opisu",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = DescriptionTheme.DarkText
                )
                DescriptionValidationIcon(validationState = validation.state)
            }

            Spacer(modifier = Modifier.height(12.dp))

            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(
                    durationMillis = 500,
                    easing = FastOutSlowInEasing
                ),
                label = "progress"
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when (validation.state) {
                    DescriptionValidationState.VALID -> DescriptionTheme.ValidColor
                    DescriptionValidationState.IN_PROGRESS -> DescriptionTheme.InProgressColor
                    DescriptionValidationState.TOO_SHORT -> DescriptionTheme.WarningColor
                    DescriptionValidationState.INVALID -> DescriptionTheme.InvalidColor
                    else -> DescriptionTheme.LightText
                },
                trackColor = DescriptionTheme.NeutralBackground,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$currentLength znaków",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (validation.state) {
                        DescriptionValidationState.VALID -> DescriptionTheme.ValidColor
                        DescriptionValidationState.IN_PROGRESS -> DescriptionTheme.InProgressColor
                        DescriptionValidationState.TOO_SHORT -> DescriptionTheme.WarningColor
                        DescriptionValidationState.INVALID -> DescriptionTheme.InvalidColor
                        else -> DescriptionTheme.DarkText
                    }
                )

                Text(
                    text = "Cel: $targetLength",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DescriptionTheme.MediumText
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = validation.message,
                style = MaterialTheme.typography.bodySmall,
                color = when (validation.state) {
                    DescriptionValidationState.VALID -> DescriptionTheme.ValidColor
                    DescriptionValidationState.IN_PROGRESS -> DescriptionTheme.InProgressColor
                    DescriptionValidationState.TOO_SHORT -> DescriptionTheme.WarningColor
                    DescriptionValidationState.INVALID -> DescriptionTheme.InvalidColor
                    else -> DescriptionTheme.LightText
                }
            )
        }
    }
}


@Composable
private fun EnhancedDescriptionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    validation: DescriptionValidation,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = when (validation.state) {
            DescriptionValidationState.VALID -> DescriptionTheme.ValidColor
            DescriptionValidationState.INVALID -> DescriptionTheme.InvalidColor
            DescriptionValidationState.TOO_SHORT -> DescriptionTheme.WarningColor
            else -> DescriptionTheme.NeutralBorder
        },
        animationSpec = tween(DescriptionTheme.animationDuration),
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when (validation.state) {
            DescriptionValidationState.VALID -> DescriptionTheme.ValidBackground.copy(alpha = 0.3f)
            DescriptionValidationState.INVALID -> DescriptionTheme.InvalidBackground.copy(alpha = 0.3f)
            DescriptionValidationState.TOO_SHORT -> DescriptionTheme.WarningBackground.copy(alpha = 0.3f)
            else -> Color.White
        },
        animationSpec = tween(DescriptionTheme.animationDuration),
        label = "backgroundColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = DescriptionTheme.borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(DescriptionTheme.cornerRadius)
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(DescriptionTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 200.dp),
            label = {
                Text(
                    "Opis zdarzenia",
                    color = when (validation.state) {
                        DescriptionValidationState.VALID -> DescriptionTheme.ValidColor
                        DescriptionValidationState.INVALID -> DescriptionTheme.InvalidColor
                        DescriptionValidationState.TOO_SHORT -> DescriptionTheme.WarningColor
                        else -> DescriptionTheme.MediumText
                    }
                )
            },
            placeholder = {
                Text(
                    "Opisz szczegółowo okoliczności wystąpienia uszkodzeń palet...",
                    color = DescriptionTheme.LightText
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(DescriptionTheme.cornerRadius)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDescriptionScreen(
    navController: NavController,
    reportViewModel: HeaderViewModel // <-- POPRAWKA: Przyjmujemy ViewModel jako parametr
) {


    val reportHeader by reportViewModel.reportHeaderState.collectAsState()

    val savedPallets by reportViewModel.savedPallets.collectAsState()

    // Efekt, który uruchomi się ZAWSZE, GDY ZMIENI SIĘ ZAWARTOŚĆ LISTY PALET
    // (dodanie, usunięcie LUB EDYCJA istniejącej palety).
    LaunchedEffect(key1 = savedPallets) {
        reportViewModel.generateAndSetEventDescription()
    }

    val descriptionValidation = remember(reportHeader.opisZdarzenia) {
        val length = reportHeader.opisZdarzenia.length
        when {
            length == 0 -> DescriptionValidation(
                state = DescriptionValidationState.EMPTY,
                message = "Opis jest wymagany"
            )
            length < 50 -> DescriptionValidation(
                state = DescriptionValidationState.TOO_SHORT,
                message = "Opis powinien zawierać więcej szczegółów"
            )
            length >= 150 -> DescriptionValidation(
                state = DescriptionValidationState.VALID,
                message = "Opis jest wystarczająco szczegółowy"
            )
            else -> DescriptionValidation(
                state = DescriptionValidationState.IN_PROGRESS,
                message = "Możesz dodać więcej szczegółów"
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Opis Zdarzenia",
                        color = DescriptionTheme.DarkText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wróć"
                        )
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
                if (descriptionValidation.state != DescriptionValidationState.VALID) {
                    DescriptionStatusCard(validation = descriptionValidation)
                }

                val continueButtonScale by animateFloatAsState(
                    targetValue = if (reportHeader.opisZdarzenia.length >= 50) 1f else 0.95f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "continueButtonScale"
                )

                Button(
                    onClick = { navController.navigate(Screen.Signature.route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .scale(continueButtonScale),
                    enabled = reportHeader.opisZdarzenia.length >= 50,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (reportHeader.opisZdarzenia.length >= 50)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (reportHeader.opisZdarzenia.length >= 50)
                            Color.White
                        else
                            DescriptionTheme.LightText
                    ),
                    shape = RoundedCornerShape(DescriptionTheme.cornerRadius)
                ) {
                    if (reportHeader.opisZdarzenia.length >= 50) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (reportHeader.opisZdarzenia.length >= 50)
                            "Przejdź do podpisu"
                        else
                            "Napisz opis o długości min. 50 znaków",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DescriptionProgressCard(
                currentLength = reportHeader.opisZdarzenia.length,
                targetLength = 150
            )

            // Główne pole opisu - zajmuje większość ekranu
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Zajmuje całą dostępną przestrzeń
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(DescriptionTheme.cornerRadius),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = DescriptionTheme.MediumText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Opis zdarzenia:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = DescriptionTheme.DarkText
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    EnhancedDescriptionTextField(
                        value = reportHeader.opisZdarzenia,
                        onValueChange = { reportViewModel.updateOpisZdarzenia(it) },
                        validation = descriptionValidation,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}