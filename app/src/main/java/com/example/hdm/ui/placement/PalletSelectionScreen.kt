package com.example.hdm.ui.placement

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.hdm.Screen
import com.example.hdm.model.DamagedPallet
import com.example.hdm.ui.header.HeaderViewModel

@Composable
private fun CleanProgressIndicator(
    completedFields: Int,
    totalFields: Int,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = completedFields.toFloat() / totalFields.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "progress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CleanTheme.NeutralBackground
        ),
        shape = RoundedCornerShape(CleanTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Postęp rozmieszczenia",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$completedFields/$totalFields",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (completedFields == totalFields && totalFields > 0) CleanTheme.ValidColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (progress >= 1f) CleanTheme.ValidColor else MaterialTheme.colorScheme.primary,
                trackColor = Color(0xFFE0E0E0)
            )
        }
    }
}

@Composable
private fun CleanStatusCard(
    isComplete: Boolean,
    completionMessage: String,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isComplete)
            CleanTheme.ValidBackground
        else
            CleanTheme.NeutralBackground,
        animationSpec = tween(CleanTheme.animationDuration),
        label = "containerColor"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = RoundedCornerShape(CleanTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = if (isComplete) Icons.Default.CheckCircle else Icons.Default.Info
            val iconColor = if (isComplete) CleanTheme.ValidColor else MaterialTheme.colorScheme.onSurfaceVariant

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = completionMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isComplete) CleanTheme.ValidColor else CleanTheme.DarkText,
                fontWeight = if (isComplete) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun CleanPalletCard(
    pallet: DamagedPallet,
    palletIndex: Int,
    isAssigned: Boolean,
    assignedPosition: String?,
    onClick: () -> Unit,
    validationMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isAssigned -> CleanTheme.ValidBackground
            else -> Color.White
        },
        animationSpec = tween(CleanTheme.animationDuration),
        label = "backgroundColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isAssigned -> CleanTheme.ValidBorder
            else -> CleanTheme.NeutralBorder
        },
        animationSpec = tween(CleanTheme.animationDuration),
        label = "borderColor"
    )

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    Column(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .clickable(onClick = onClick)
                .border(
                    width = CleanTheme.borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(CleanTheme.cornerRadius)
                ),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(CleanTheme.cornerRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isAssigned) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isAssigned) CleanTheme.ValidColor else CleanTheme.EmptyColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Paleta $palletIndex",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CleanTheme.DarkText
                        )
                    }

                    if (isAssigned && assignedPosition != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = CleanTheme.ValidColor.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Poz. $assignedPosition",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CleanTheme.ValidColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Numer:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CleanTheme.MediumText
                    )
                    Text(
                        text = if (pallet.brakNumeruPalety) "Brak numeru" else pallet.numerPalety.ifBlank { "Nie podano" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = CleanTheme.DarkText,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (!pallet.rodzajTowaru.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Towar:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CleanTheme.MediumText
                        )
                        Text(
                            text = pallet.rodzajTowaru ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CleanTheme.DarkText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAssigned) CleanTheme.ValidColor else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(CleanTheme.cornerRadius)
                ) {
                    Icon(
                        imageVector = if (isAssigned) Icons.Default.Edit else Icons.Default.AddLocation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAssigned) "Zmień rozmieszczenie" else "Wybierz pozycję",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Warning message when validation is active and there's an issue
        if (validationMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CleanTheme.WarningBackground),
                shape = RoundedCornerShape(CleanTheme.cornerRadius)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = CleanTheme.WarningColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = validationMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = CleanTheme.WarningColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Additional info for urgent pallets
        if (pallet.brakNumeruLotu || pallet.numerLotu == "XXX") {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CleanTheme.InProgressBackground),
                shape = RoundedCornerShape(CleanTheme.cornerRadius)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = CleanTheme.InProgressColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Paleta pilna - priorytet wysyłki",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanVehicleInfoCard(
    vehicleType: String,
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
                Icons.Default.LocalShipping,
                contentDescription = null,
                tint = CleanTheme.InProgressColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Pojazd: ${
                    when (vehicleType) {
                        "OKTRANS", "ROTONDO" -> "Naczepa ($vehicleType)"
                        else -> vehicleType
                    }
                }",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = CleanTheme.DarkText
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalletSelectionScreen(
    navController: NavController,
    reportViewModel: HeaderViewModel
) {
    val savedPallets by reportViewModel.savedPallets.collectAsStateWithLifecycle()
    val palletPositions by reportViewModel.palletPositions.collectAsStateWithLifecycle()
    val reportHeader by reportViewModel.reportHeaderState.collectAsStateWithLifecycle()
    val validationActive by reportViewModel.placementValidationActive.collectAsStateWithLifecycle()
    // === DODANO LINIĘ ===
    val damageMarkers by reportViewModel.damageMarkersState.collectAsStateWithLifecycle()
    // ====================

    val totalPalletsCount = savedPallets.size
    val assignedPalletsCount = palletPositions.size

    // === ZMIENIONA WALIDACJA ===
    val allPalletsAssigned = totalPalletsCount > 0 &&
            palletPositions.size == totalPalletsCount &&
            palletPositions.all { position ->
                // Sprawdź czy pozycja ma markery ALBO ma URI bitmapy
                val hasMarkers = damageMarkers[position.palletId]?.isNotEmpty() == true
                val hasBitmapUri = position.damageBitmapUri?.isNotBlank() == true
                val hasDamagePart = position.damagePart?.isNotBlank() == true

                (hasMarkers || hasBitmapUri) && hasDamagePart
            }
    // ===========================

    // Aktywujemy walidację, gdy użytkownik wraca na ten ekran,
    // a już próbował umiejscowić jakąś paletę.
    LaunchedEffect(palletPositions.size) {
        if (palletPositions.isNotEmpty()) {
            reportViewModel.activatePlacementValidation()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Rozmieszczenie Palet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = CleanTheme.DarkText
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Progress indicator
        CleanProgressIndicator(
            completedFields = assignedPalletsCount,
            totalFields = totalPalletsCount
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Vehicle info
        CleanVehicleInfoCard(vehicleType = reportHeader.rodzajSamochodu)
        Spacer(modifier = Modifier.height(16.dp))

        // Status card
        CleanStatusCard(
            isComplete = allPalletsAssigned,
            completionMessage = if (allPalletsAssigned) {
                "Wszystkie palety zostały rozmieszczone, możesz przejść dalej"
            } else if (assignedPalletsCount == 0) {
                "Wybierz pozycje dla wszystkich palet"
            } else {
                "Pozostało ${totalPalletsCount - assignedPalletsCount} palet do rozmieszczenia"
            }
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Pallets list
        if (savedPallets.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CleanTheme.NeutralBackground),
                shape = RoundedCornerShape(CleanTheme.cornerRadius)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        tint = CleanTheme.EmptyColor,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Brak palet do rozmieszczenia",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = CleanTheme.EmptyColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Dodaj palety w sekcji wprowadzania danych",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CleanTheme.LightText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(savedPallets) { index, pallet ->
                    val assignedPosition = palletPositions.find { it.palletId == pallet.id }
                    val isAssigned = assignedPosition != null

                    // === ZMIENIONA WALIDACJA KOMUNIKATU ===
                    val incompletionMessage = assignedPosition?.let { position ->
                        val hasMarkers = damageMarkers[position.palletId]?.isNotEmpty() == true
                        val hasBitmapUri = position.damageBitmapUri?.isNotBlank() == true
                        val hasDamagePart = position.damagePart?.isNotBlank() == true

                        when {
                            !hasMarkers && !hasBitmapUri -> "Brak zaznaczenia uszkodzenia na obrazku"
                            !hasDamagePart -> "Brak wyboru wysokości uszkodzenia"
                            else -> null
                        }
                    }
                    // =======================================

                    CleanPalletCard(
                        pallet = pallet,
                        palletIndex = index + 1,
                        isAssigned = isAssigned,
                        assignedPosition = assignedPosition?.positionOnVehicle,
                        onClick = {
                            navController.navigate(
                                Screen.VehicleSchematic.createRoute(pallet.id)
                            )
                        },
                        validationMessage = if (validationActive && isAssigned && incompletionMessage != null) {
                            incompletionMessage
                        } else null
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Bottom button
        if (allPalletsAssigned) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    navController.navigate(Screen.EventDescription.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CleanTheme.ValidColor
                ),
                shape = RoundedCornerShape(CleanTheme.cornerRadius)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Przejdź do opisu zdarzenia",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}