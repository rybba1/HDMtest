// VehicleSchematicScreen.kt

package com.example.hdm.ui.placement

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.hdm.Screen
import com.example.hdm.ui.header.HeaderViewModel

@Composable
private fun CleanInfoCard(
    title: String,
    subtitle: String,
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
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CleanTheme.DarkText
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CleanTheme.MediumText
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanVehicleLayoutSelector(
    selectedLayout: String?,
    onLayoutSelected: (String) -> Unit,
    layoutOptions: List<Pair<String, String>>,
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
                text = "Wybierz układ pojazdu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CleanTheme.DarkText
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                layoutOptions.forEach { (layout, description) ->
                    val isSelected = selectedLayout == layout

                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected) CleanTheme.ValidColor else Color.White,
                        animationSpec = tween(CleanTheme.animationDuration),
                        label = "layoutBackground"
                    )

                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else CleanTheme.DarkText,
                        animationSpec = tween(CleanTheme.animationDuration),
                        label = "layoutText"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = { onLayoutSelected(layout) }
                            ),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        border = if (!isSelected) CardDefaults.outlinedCardBorder() else null,
                        shape = RoundedCornerShape(CleanTheme.cornerRadius)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onLayoutSelected(layout) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = if (isSelected) Color.White else CleanTheme.ValidColor,
                                    unselectedColor = if (isSelected) Color.White.copy(alpha = 0.7f) else CleanTheme.MediumText
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = layout,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanPalletSlot(
    position: String,
    isOccupied: Boolean,
    occupiedBySelectedPallet: Boolean = false,
    slotInfo: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            occupiedBySelectedPallet -> CleanTheme.WarningColor
            isOccupied -> CleanTheme.ValidColor.copy(alpha = 0.3f)
            else -> Color.White
        },
        animationSpec = tween(CleanTheme.animationDuration),
        label = "slotBackground"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            occupiedBySelectedPallet -> CleanTheme.WarningColor
            isOccupied -> CleanTheme.ValidColor
            else -> CleanTheme.NeutralBorder
        },
        animationSpec = tween(CleanTheme.animationDuration),
        label = "slotBorder"
    )

    val scale by animateFloatAsState(
        targetValue = if (occupiedBySelectedPallet) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "slotScale"
    )

    Card(
        modifier = modifier
            .aspectRatio(1.2f)
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(
            width = if (isOccupied || occupiedBySelectedPallet) 2.dp else CleanTheme.borderWidth,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isOccupied || occupiedBySelectedPallet) 2.dp else 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isOccupied || occupiedBySelectedPallet) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (occupiedBySelectedPallet) Icons.Default.Edit else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (occupiedBySelectedPallet) Color.White else CleanTheme.ValidColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = slotInfo ?: position,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (occupiedBySelectedPallet) Color.White else CleanTheme.ValidColor,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = position,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = CleanTheme.DarkText,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CleanVehicleGrid(
    title: String,
    columns: Int,
    totalSlots: Int,
    palletPositions: List<com.example.hdm.model.PalletPosition>,
    selectedPalletId: String,
    onSlotClick: (String) -> Unit,
    slotOffset: Int = 0,
    showDirections: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(CleanTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CleanTheme.DarkText,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (showDirections) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = CleanTheme.MediumText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PRZÓD",
                        fontSize = 10.sp,
                        color = CleanTheme.MediumText,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            val rows = (totalSlots + columns - 1) / columns
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0 until rows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (col in 0 until columns) {
                            val index = row * columns + col
                            if (index < totalSlots) {
                                val positionNumber = (index + 1 + slotOffset).toString()
                                val occupiedPosition = palletPositions.find {
                                    it.positionOnVehicle == positionNumber ||
                                            it.positionOnVehicle == "${positionNumber}a" ||
                                            it.positionOnVehicle == "${positionNumber}b"
                                }
                                val isOccupied = occupiedPosition != null
                                val occupiedBySelectedPallet = occupiedPosition?.palletId == selectedPalletId

                                CleanPalletSlot(
                                    position = positionNumber,
                                    isOccupied = isOccupied,
                                    occupiedBySelectedPallet = occupiedBySelectedPallet,
                                    slotInfo = if (isOccupied) {
                                        when {
                                            occupiedPosition?.positionOnVehicle?.endsWith("a") == true -> "${positionNumber}A"
                                            occupiedPosition?.positionOnVehicle?.endsWith("b") == true -> "${positionNumber}B"
                                            else -> positionNumber
                                        }
                                    } else null,
                                    onClick = { onSlotClick(positionNumber) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if (showDirections) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = CleanTheme.MediumText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "TYŁ",
                        fontSize = 10.sp,
                        color = CleanTheme.MediumText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanTandemLayout(
    palletPositions: List<com.example.hdm.model.PalletPosition>,
    selectedPalletId: String,
    onSlotClick: (String) -> Unit,
    columns: Int = 3,
    slotsPerSection: Int = 18,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CleanVehicleGrid(
            title = "Pojazd (pozycje 1-$slotsPerSection)",
            columns = columns,
            totalSlots = slotsPerSection,
            palletPositions = palletPositions,
            selectedPalletId = selectedPalletId,
            onSlotClick = onSlotClick,
            slotOffset = 0,
            showDirections = false
        )

        CleanVehicleGrid(
            title = "Przyczepa (pozycje ${slotsPerSection + 1}-${slotsPerSection * 2})",
            columns = columns,
            totalSlots = slotsPerSection,
            palletPositions = palletPositions,
            selectedPalletId = selectedPalletId,
            onSlotClick = onSlotClick,
            slotOffset = slotsPerSection,
            showDirections = false
        )
    }
}

@Composable
private fun CleanCustomNaczepaLayout(
    palletPositions: List<com.example.hdm.model.PalletPosition>,
    selectedPalletId: String,
    onSlotClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CleanVehicleGrid(
            title = "Główne pozycje (1-30)",
            columns = 2,
            totalSlots = 30,
            palletPositions = palletPositions,
            selectedPalletId = selectedPalletId,
            onSlotClick = onSlotClick,
            slotOffset = 0
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(CleanTheme.cornerRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Dodatkowe pozycje (31-33)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CleanTheme.DarkText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (31..33).forEach { positionNumber ->
                        val occupiedPosition = palletPositions.find {
                            it.positionOnVehicle == positionNumber.toString() ||
                                    it.positionOnVehicle == "${positionNumber}a" ||
                                    it.positionOnVehicle == "${positionNumber}b"
                        }
                        val isOccupied = occupiedPosition != null
                        val occupiedBySelectedPallet = occupiedPosition?.palletId == selectedPalletId

                        CleanPalletSlot(
                            position = positionNumber.toString(),
                            isOccupied = isOccupied,
                            occupiedBySelectedPallet = occupiedBySelectedPallet,
                            slotInfo = if (isOccupied) {
                                when {
                                    occupiedPosition?.positionOnVehicle?.endsWith("a") == true -> "${positionNumber}A"
                                    occupiedPosition?.positionOnVehicle?.endsWith("b") == true -> "${positionNumber}B"
                                    else -> positionNumber.toString()
                                }
                            } else null,
                            onClick = { onSlotClick(positionNumber.toString()) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleSchematicScreen(
    navController: NavController,
    reportViewModel: HeaderViewModel,
    selectedPalletId: String
) {
    val savedPallets by reportViewModel.savedPallets.collectAsStateWithLifecycle()
    val palletPositions by reportViewModel.palletPositions.collectAsStateWithLifecycle()
    val reportHeader by reportViewModel.reportHeaderState.collectAsStateWithLifecycle()
    val selectedVehicleLayout by reportViewModel.selectedVehicleLayoutState.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    val selectedPallet = savedPallets.find { it.id == selectedPalletId }
    val selectedPalletIndex = savedPallets.indexOfFirst { it.id == selectedPalletId } + 1

    // ===== POCZĄTEK POPRAWKI =====
    val isNaczepaVehicle = reportHeader.rodzajSamochodu in listOf("Naczepa", "OKTRANS", "ROTONDO", "DSV")
    // ===== KONIEC POPRAWKI =====

    val isTandemVehicle = reportHeader.rodzajSamochodu == "Tandem"
    val isSoloVehicle = reportHeader.rodzajSamochodu == "Solo"

    val requiresLayoutSelection = isNaczepaVehicle || isTandemVehicle || isSoloVehicle

    val currentLayout = when {
        !requiresLayoutSelection -> reportHeader.rodzajSamochodu
        selectedVehicleLayout != null -> selectedVehicleLayout
        else -> null
    }

    var showLayoutChangeDialog by remember { mutableStateOf(false) }

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
                "Wybierz Pozycję",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = CleanTheme.DarkText,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        CleanInfoCard(
            title = "Paleta $selectedPalletIndex",
            subtitle = if (selectedPallet.brakNumeruPalety) "Brak numeru" else "Nr: ${selectedPallet.numerPalety.ifBlank { "Nie podano" }}",
            icon = Icons.Default.Inventory
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                requiresLayoutSelection && currentLayout == null -> {
                    val layoutOptions = when {
                        isNaczepaVehicle -> listOf(
                            "3x11" to "3×11 pozycji (33 palety)",
                            "2x12" to "2×12 pozycji (24 palety)",
                            "15x2+1x3" to "15×2 + 3 pozycje (33 palety)"
                        )
                        isTandemVehicle -> listOf(
                            "Tandem 3x6+3x6" to "3×6 + 3×6 pozycji (36 palet)",
                            "Tandem 2x6+2x6" to "2×6 + 2×6 pozycji (24 palety)"
                        )
                        isSoloVehicle -> listOf(
                            "Solo 3x6" to "3×6 pozycji (18 palet)",
                            "Solo 2x6" to "2×6 pozycji (12 palet)"
                        )
                        else -> emptyList()
                    }

                    CleanVehicleLayoutSelector(
                        selectedLayout = currentLayout,
                        onLayoutSelected = { layout ->
                            reportViewModel.updateSelectedVehicleLayout(layout)
                        },
                        layoutOptions = layoutOptions
                    )
                }
                else -> {
                    val layoutType = currentLayout ?: reportHeader.rodzajSamochodu

                    if (requiresLayoutSelection && currentLayout != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    layoutType == "3x11" -> "Układ: 3×11 (33 pozycje)"
                                    layoutType == "2x12" -> "Układ: 2×12 (24 pozycje)"
                                    layoutType == "15x2+1x3" -> "Układ: 15×2+3 (33 pozycje)"
                                    layoutType == "Tandem 3x6+3x6" -> "Układ: Tandem 3×6+3×6 (36 palet)"
                                    layoutType == "Tandem 2x6+2x6" -> "Układ: Tandem 2×6+2×6 (24 palety)"
                                    layoutType == "Solo 3x6" -> "Układ: Solo 3×6 (18 palet)"
                                    layoutType == "Solo 2x6" -> "Układ: Solo 2×6 (12 palet)"
                                    else -> "Układ: $layoutType"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CleanTheme.DarkText
                            )

                            Button(
                                onClick = { showLayoutChangeDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CleanTheme.InProgressColor
                                ),
                                shape = RoundedCornerShape(CleanTheme.cornerRadius)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Zmień układ")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    when (layoutType) {
                        "3x11" -> CleanVehicleGrid(
                            title = "",
                            columns = 3,
                            totalSlots = 33,
                            palletPositions = palletPositions,
                            selectedPalletId = selectedPalletId,
                            onSlotClick = { position ->
                                navController.navigate(
                                    Screen.PalletDetails.createRoute(selectedPalletId, position)
                                )
                            }
                        )
                        "2x12" -> CleanVehicleGrid(
                            title = "",
                            columns = 2,
                            totalSlots = 24,
                            palletPositions = palletPositions,
                            selectedPalletId = selectedPalletId,
                            onSlotClick = { position ->
                                navController.navigate(
                                    Screen.PalletDetails.createRoute(selectedPalletId, position)
                                )
                            }
                        )
                        "15x2+1x3" -> CleanCustomNaczepaLayout(
                            palletPositions = palletPositions,
                            selectedPalletId = selectedPalletId,
                            onSlotClick = { position ->
                                navController.navigate(
                                    Screen.PalletDetails.createRoute(selectedPalletId, position)
                                )
                            }
                        )
                        "Tandem 3x6+3x6" -> CleanTandemLayout(
                            palletPositions = palletPositions,
                            selectedPalletId = selectedPalletId,
                            onSlotClick = { position ->
                                navController.navigate(
                                    Screen.PalletDetails.createRoute(selectedPalletId, position)
                                )
                            },
                            columns = 3,
                            slotsPerSection = 18
                        )
                        "Tandem 2x6+2x6" -> CleanTandemLayout(
                            palletPositions = palletPositions,
                            selectedPalletId = selectedPalletId,
                            onSlotClick = { position ->
                                navController.navigate(
                                    Screen.PalletDetails.createRoute(selectedPalletId, position)
                                )
                            },
                            columns = 2,
                            slotsPerSection = 12
                        )
                        "Tandem" -> CleanTandemLayout(
                            palletPositions = palletPositions,
                            selectedPalletId = selectedPalletId,
                            onSlotClick = { position ->
                                navController.navigate(
                                    Screen.PalletDetails.createRoute(selectedPalletId, position)
                                )
                            },
                            columns = 3,
                            slotsPerSection = 18
                        )
                        "Solo 3x6" -> CleanVehicleGrid(
                            title = "",
                            columns = 3,
                            totalSlots = 18,
                            palletPositions = palletPositions,
                            selectedPalletId = selectedPalletId,
                            onSlotClick = { position ->
                                navController.navigate(
                                    Screen.PalletDetails.createRoute(selectedPalletId, position)
                                )
                            }
                        )
                        "Solo 2x6" -> CleanVehicleGrid(
                            title = "",
                            columns = 2,
                            totalSlots = 12,
                            palletPositions = palletPositions,
                            selectedPalletId = selectedPalletId,
                            onSlotClick = { position ->
                                navController.navigate(
                                    Screen.PalletDetails.createRoute(selectedPalletId, position)
                                )
                            }
                        )
                        "Solo" -> CleanVehicleGrid(
                            title = "",
                            columns = 3,
                            totalSlots = 18,
                            palletPositions = palletPositions,
                            selectedPalletId = selectedPalletId,
                            onSlotClick = { position ->
                                navController.navigate(
                                    Screen.PalletDetails.createRoute(selectedPalletId, position)
                                )
                            }
                        )
                        "Bus" -> CleanVehicleGrid(
                            title = "",
                            columns = 2,
                            totalSlots = 6,
                            palletPositions = palletPositions,
                            selectedPalletId = selectedPalletId,
                            onSlotClick = { position ->
                                navController.navigate(
                                    Screen.PalletDetails.createRoute(selectedPalletId, position)
                                )
                            }
                        )
                        "Kontener" -> CleanVehicleGrid(
                            title = "",
                            columns = 2,
                            totalSlots = 24,
                            palletPositions = palletPositions,
                            selectedPalletId = selectedPalletId,
                            onSlotClick = { position ->
                                navController.navigate(
                                    Screen.PalletDetails.createRoute(selectedPalletId, position)
                                )
                            }
                        )
                        else -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CleanTheme.NeutralBackground),
                                shape = RoundedCornerShape(CleanTheme.cornerRadius)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Nieznany typ pojazdu: $layoutType",
                                        color = CleanTheme.LightText,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLayoutChangeDialog) {
        var tempSelectedLayout by remember { mutableStateOf(currentLayout) }

        val layoutOptions = when {
            isNaczepaVehicle -> listOf(
                "3x11" to "3×11 pozycji (33 palety)",
                "2x12" to "2×12 pozycji (24 palety)",
                "15x2+1x3" to "15×2 + 3 pozycje (33 palety)"
            )
            isTandemVehicle -> listOf(
                "Tandem 3x6+3x6" to "3×6 + 3×6 pozycji (36 palet)",
                "Tandem 2x6+2x6" to "2×6 + 2×6 pozycji (24 palety)"
            )
            isSoloVehicle -> listOf(
                "Solo 3x6" to "3×6 pozycji (18 palet)",
                "Solo 2x6" to "2×6 pozycji (12 palet)"
            )
            else -> emptyList()
        }

        AlertDialog(
            onDismissRequest = { showLayoutChangeDialog = false },
            title = {
                Text(
                    "Zmień układ pojazdu",
                    fontWeight = FontWeight.Bold,
                    color = CleanTheme.DarkText
                )
            },
            text = {
                Column {
                    if (palletPositions.isNotEmpty()) {
                        Card(
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
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Zmiana układu usunie wszystkie przypisania pozycji!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CleanTheme.WarningColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(
                        text = "Wybierz nowy układ:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CleanTheme.DarkText,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    layoutOptions.forEach { (layout, description) ->
                        val isSelected = tempSelectedLayout == layout

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tempSelectedLayout = layout }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { tempSelectedLayout = layout },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CleanTheme.ValidColor
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = layout,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = CleanTheme.DarkText
                                )
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CleanTheme.MediumText
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempSelectedLayout != currentLayout && palletPositions.isNotEmpty()) {
                            reportViewModel.clearAllPalletPositions()
                        }
                        reportViewModel.updateSelectedVehicleLayout(tempSelectedLayout)
                        showLayoutChangeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CleanTheme.ValidColor
                    ),
                    shape = RoundedCornerShape(CleanTheme.cornerRadius)
                ) {
                    Text("Potwierdź", color = Color.White, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLayoutChangeDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = CleanTheme.MediumText
                    )
                ) {
                    Text("Anuluj", fontWeight = FontWeight.Medium)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(CleanTheme.cornerRadius)
        )
    }
}