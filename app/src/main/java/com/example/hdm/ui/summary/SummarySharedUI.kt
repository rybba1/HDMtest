package com.example.hdm.ui.summary

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


// VALIDATION SYSTEM
enum class SummaryValidationState {
    EMPTY, VALID, INVALID, WARNING
}

data class SummaryValidation(
    val state: SummaryValidationState,
    val message: String = ""
)

// THEME
object SummaryTheme {
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

// WSPÓLNE KOMPONENTY UI

@Composable
fun SummaryValidationIcon(
    validationState: SummaryValidationState,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (validationState) {
        SummaryValidationState.VALID -> Icons.Default.CheckCircle to SummaryTheme.ValidColor
        SummaryValidationState.INVALID -> Icons.Default.Error to SummaryTheme.InvalidColor
        SummaryValidationState.WARNING -> Icons.Default.Warning to SummaryTheme.WarningColor
        SummaryValidationState.EMPTY -> Icons.Default.AddCircleOutline to SummaryTheme.EmptyColor
    }

    val scale by animateFloatAsState(
        targetValue = if (validationState == SummaryValidationState.VALID) 1.1f else 1f,
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
fun SummaryStatusCard(
    validation: SummaryValidation,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (validation.state) {
            SummaryValidationState.VALID -> SummaryTheme.ValidBackground
            SummaryValidationState.INVALID -> SummaryTheme.InvalidBackground
            SummaryValidationState.WARNING -> SummaryTheme.WarningBackground
            SummaryValidationState.EMPTY -> SummaryTheme.NeutralBackground
        },
        animationSpec = tween(SummaryTheme.animationDuration),
        label = "backgroundColor"
    )

    val textColor = when (validation.state) {
        SummaryValidationState.VALID -> SummaryTheme.ValidColor
        SummaryValidationState.INVALID -> SummaryTheme.InvalidColor
        SummaryValidationState.WARNING -> SummaryTheme.WarningColor
        SummaryValidationState.EMPTY -> SummaryTheme.LightText
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(SummaryTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryValidationIcon(validationState = validation.state)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = validation.message,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (validation.state == SummaryValidationState.VALID) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}


@Composable
fun PalletCountCard(
    palletCount: Int,
    modifier: Modifier = Modifier
) {
    val validation = when {
        palletCount == 0 -> SummaryValidation(
            state = SummaryValidationState.EMPTY,
            message = "Nie dodano żadnych palet"
        )
        palletCount == 1 -> SummaryValidation(
            state = SummaryValidationState.WARNING,
            message = "Dodano 1 paletę - możesz dodać więcej"
        )
        else -> SummaryValidation(
            state = SummaryValidationState.VALID,
            message = "Dodano $palletCount palet"
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(SummaryTheme.cornerRadius),
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
                    "Liczba palet",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = SummaryTheme.DarkText
                )
                SummaryValidationIcon(validationState = validation.state)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = palletCount.toString(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = when (validation.state) {
                    SummaryValidationState.VALID -> SummaryTheme.ValidColor
                    SummaryValidationState.WARNING -> SummaryTheme.WarningColor
                    SummaryValidationState.EMPTY -> SummaryTheme.InvalidColor
                    else -> SummaryTheme.DarkText
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = validation.message,
                style = MaterialTheme.typography.bodySmall,
                color = when (validation.state) {
                    SummaryValidationState.VALID -> SummaryTheme.ValidColor
                    SummaryValidationState.WARNING -> SummaryTheme.WarningColor
                    SummaryValidationState.EMPTY -> SummaryTheme.InvalidColor
                    else -> SummaryTheme.LightText
                }
            )
        }
    }
}