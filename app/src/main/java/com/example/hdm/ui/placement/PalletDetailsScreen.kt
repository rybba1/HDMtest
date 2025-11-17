package com.example.hdm.ui.placement

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.hdm.Screen
import com.example.hdm.ui.header.HeaderViewModel

@Composable
private fun CleanInfoCard(
    title: String,
    subtitle: String,
    description: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CleanTheme.InProgressBackground),
        shape = RoundedCornerShape(CleanTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CleanTheme.InProgressColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CleanTheme.DarkText
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = CleanTheme.MediumText
                )
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CleanTheme.MediumText
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanStackingSelector(
    selectedStacking: String,
    onStackingChange: (String) -> Unit,
    availableStackingOptions: List<String>,
    hasExistingPallet: Boolean,
    hasExistingPalletOnTop: Boolean,
    hasExistingPalletOnBottom: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(CleanTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Piętrowanie palety",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CleanTheme.DarkText
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Wybierz sposób umieszczenia palety na tej pozycji",
                style = MaterialTheme.typography.bodyMedium,
                color = CleanTheme.MediumText
            )

            Spacer(modifier = Modifier.height(16.dp))

            val stackingOptions = when {
                availableStackingOptions.isEmpty() -> {
                    listOf("Pozycja zajęta")
                }
                availableStackingOptions.size == 1 -> {
                    availableStackingOptions
                }
                else -> {
                    availableStackingOptions
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stackingOptions.forEach { option ->
                    val isSelected = selectedStacking == option
                    val isEnabled = availableStackingOptions.contains(option)

                    val backgroundColor by animateColorAsState(
                        targetValue = when {
                            !isEnabled -> CleanTheme.NeutralBackground
                            isSelected -> CleanTheme.ValidColor
                            else -> Color.White
                        },
                        animationSpec = tween(CleanTheme.animationDuration),
                        label = "stackingBackground"
                    )

                    val textColor by animateColorAsState(
                        targetValue = when {
                            !isEnabled -> CleanTheme.LightText
                            isSelected -> Color.White
                            else -> CleanTheme.DarkText
                        },
                        animationSpec = tween(CleanTheme.animationDuration),
                        label = "stackingText"
                    )

                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.02f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "stackingScale"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .selectable(
                                selected = isSelected,
                                onClick = { if (isEnabled) onStackingChange(option) },
                                enabled = isEnabled
                            ),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        border = if (!isSelected && isEnabled) CardDefaults.outlinedCardBorder() else null,
                        shape = RoundedCornerShape(CleanTheme.cornerRadius),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isSelected) 2.dp else 0.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { if (isEnabled) onStackingChange(option) },
                                enabled = isEnabled,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = if (isSelected) Color.White else CleanTheme.ValidColor,
                                    unselectedColor = if (isSelected) Color.White.copy(alpha = 0.7f) else CleanTheme.MediumText,
                                    disabledSelectedColor = CleanTheme.LightText,
                                    disabledUnselectedColor = CleanTheme.LightText
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                                Text(
                                    text = when (option) {
                                        "Góra" -> if (hasExistingPallet) "Na istniejącej palecie" else "Paleta na górze"
                                        "Dół" -> "Paleta na dole"
                                        "Pozycja zajęta" -> "Wybierz inną pozycję"
                                        else -> "Pojedyncza paleta"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor.copy(alpha = 0.8f)
                                )
                            }
                            if (isSelected && isEnabled) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Warning messages
            if (hasExistingPallet && availableStackingOptions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = CleanTheme.WarningBackground),
                    shape = RoundedCornerShape(CleanTheme.cornerRadius)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = CleanTheme.WarningColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                hasExistingPalletOnTop -> "Na górze tej pozycji jest już paleta. Twoja paleta zostanie umieszczona na dole."
                                hasExistingPalletOnBottom -> "Na dole tej pozycji jest już paleta. Twoja paleta zostanie umieszczona na górze."
                                else -> "Na tej pozycji jest już paleta."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = CleanTheme.WarningColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (availableStackingOptions.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = CleanTheme.InvalidBackground),
                    shape = RoundedCornerShape(CleanTheme.cornerRadius)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = CleanTheme.InvalidColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ta pozycja jest całkowicie zajęta. Wybierz inną pozycję.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CleanTheme.InvalidColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanStatusCard(
    isComplete: Boolean,
    statusMessage: String,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = when {
            isComplete -> CleanTheme.ValidBackground
            statusMessage.contains("zajęta") -> CleanTheme.InvalidBackground
            else -> CleanTheme.WarningBackground
        },
        animationSpec = tween(CleanTheme.animationDuration),
        label = "statusColor"
    )

    val iconColor = when {
        isComplete -> CleanTheme.ValidColor
        statusMessage.contains("zajęta") -> CleanTheme.InvalidColor
        else -> CleanTheme.WarningColor
    }

    val icon = when {
        isComplete -> Icons.Default.CheckCircle
        statusMessage.contains("zajęta") -> Icons.Default.Error
        else -> Icons.Default.Warning
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(CleanTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = iconColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalletDetailsScreen(
    navController: NavController,
    reportViewModel: HeaderViewModel,
    selectedPalletId: String,
    selectedPosition: String
) {
    val savedPallets by reportViewModel.savedPallets.collectAsStateWithLifecycle()
    val palletPositions by reportViewModel.palletPositions.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val selectedPallet = savedPallets.find { it.id == selectedPalletId }
    val selectedPalletIndex = savedPallets.indexOfFirst { it.id == selectedPalletId } + 1

    val existingPalletsOnPosition = palletPositions.filter {
        it.positionOnVehicle == selectedPosition && it.palletId != selectedPalletId
    }

    val hasExistingPalletOnTop = existingPalletsOnPosition.any { it.stackingLevel == "A" }
    val hasExistingPalletOnBottom = existingPalletsOnPosition.any { it.stackingLevel == "B" }
    val hasExistingPalletNoStacking = existingPalletsOnPosition.any { it.stackingLevel.isBlank() }

    val hasExistingPallet = existingPalletsOnPosition.isNotEmpty()
    val availableStackingOptions = when {
        hasExistingPalletNoStacking -> emptyList()
        hasExistingPalletOnTop && hasExistingPalletOnBottom -> emptyList()
        hasExistingPalletOnTop -> listOf("Dół")
        hasExistingPalletOnBottom -> listOf("Góra")
        else -> listOf("Dół", "Góra", "Nie dotyczy")
    }

    var selectedStacking by remember(availableStackingOptions) {
        mutableStateOf(
            when {
                availableStackingOptions.isEmpty() -> ""
                availableStackingOptions.size == 1 -> availableStackingOptions.first()
                else -> "Nie dotyczy"
            }
        )
    }

    val isFormComplete = selectedStacking.isNotBlank() && availableStackingOptions.isNotEmpty()

    if (selectedPallet == null) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Wróć",
                    tint = CleanTheme.DarkText
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Szczegóły Umiejscowienia",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = CleanTheme.DarkText,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Position and pallet info
        CleanInfoCard(
            title = "Pozycja $selectedPosition",
            subtitle = "dla palety $selectedPalletIndex",
            description = if (selectedPallet.brakNumeruPalety) "Paleta bez numeru" else "Nr palety: ${selectedPallet.numerPalety.ifBlank { "Nie podano" }}",
            icon = Icons.Default.LocationOn
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Status card
        CleanStatusCard(
            isComplete = isFormComplete,
            statusMessage = when {
                availableStackingOptions.isEmpty() -> "Ta pozycja jest zajęta. Wybierz inną pozycję."
                isFormComplete -> "Gotowe do dalszego kroku - zaznacz uszkodzenia"
                else -> "Wybierz opcję piętrowania"
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stacking selector
            CleanStackingSelector(
                selectedStacking = selectedStacking,
                onStackingChange = { selectedStacking = it },
                availableStackingOptions = availableStackingOptions,
                hasExistingPallet = hasExistingPallet,
                hasExistingPalletOnTop = hasExistingPalletOnTop,
                hasExistingPalletOnBottom = hasExistingPalletOnBottom
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Bottom button
        val buttonScale by animateFloatAsState(
            targetValue = if (isFormComplete) 1f else 0.95f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "buttonScale"
        )

        Button(
            onClick = {
                if (isFormComplete) {
                    val stackingLevel = when (selectedStacking) {
                        "Góra" -> "A"
                        "Dół" -> "B"
                        else -> ""
                    }
                    // Zapisujemy pozycję
                    reportViewModel.assignPalletToPosition(
                        pallet = selectedPallet,
                        position = selectedPosition,
                        stackingLevel = stackingLevel
                    )

                    // Nawigujemy do ekranu zaznaczania uszkodzeń
                    try {
                        navController.navigate(
                            Screen.DamageLocation.createRoute(selectedPalletId, selectedPosition)
                        )
                    } catch(e: Exception) {
                        Log.e("NavigationError", "Upewnij się, że trasa Screen.DamageLocation jest poprawnie zdefiniowana i przyjmuje 2 argumenty.")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .scale(buttonScale),
            enabled = isFormComplete,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFormComplete) CleanTheme.ValidColor else CleanTheme.EmptyColor,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(CleanTheme.cornerRadius)
        ) {
            if (isFormComplete) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = when {
                    isFormComplete -> "Dalej - zaznacz uszkodzenia"
                    availableStackingOptions.isEmpty() -> "Wybierz inną pozycję"
                    else -> "Uzupełnij wszystkie pola"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}