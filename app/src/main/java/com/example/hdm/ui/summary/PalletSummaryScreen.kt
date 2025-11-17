package com.example.hdm.ui.summary

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.hdm.model.DamagedPallet
import com.example.hdm.ui.header.HeaderViewModel

@Composable
private fun CleanPalletSummaryCard(
    pallet: DamagedPallet,
    palletIndex: Int,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palletValidation = when {
        pallet.damageInstances.isEmpty() -> SummaryValidationState.INVALID
        pallet.zdjecieDuzejEtykietyUri == null -> SummaryValidationState.WARNING
        pallet.zdjecieCalejPaletyUri == null -> SummaryValidationState.WARNING
        else -> SummaryValidationState.VALID
    }

    val borderColor by animateColorAsState(
        targetValue = when (palletValidation) {
            SummaryValidationState.VALID -> SummaryTheme.ValidBorder
            SummaryValidationState.INVALID -> SummaryTheme.InvalidBorder
            SummaryValidationState.WARNING -> SummaryTheme.WarningBorder
            SummaryValidationState.EMPTY -> SummaryTheme.NeutralBorder
        },
        animationSpec = tween(SummaryTheme.animationDuration),
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when (palletValidation) {
            SummaryValidationState.VALID -> SummaryTheme.ValidBackground
            SummaryValidationState.INVALID -> SummaryTheme.InvalidBackground
            SummaryValidationState.WARNING -> SummaryTheme.WarningBackground
            SummaryValidationState.EMPTY -> Color.White
        },
        animationSpec = tween(SummaryTheme.animationDuration),
        label = "backgroundColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = SummaryTheme.borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(SummaryTheme.cornerRadius)
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(SummaryTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Paleta $palletIndex",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SummaryTheme.DarkText
                )
                SummaryValidationIcon(validationState = palletValidation)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Numer:", style = MaterialTheme.typography.bodyMedium, color = SummaryTheme.MediumText)
                    Text(text = if (pallet.brakNumeruPalety) "Brak numeru" else pallet.numerPalety.ifBlank { "Nie podano" }, style = MaterialTheme.typography.bodyMedium, color = SummaryTheme.DarkText, fontWeight = FontWeight.Medium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Uszkodzenia:", style = MaterialTheme.typography.bodyMedium, color = SummaryTheme.MediumText)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "${pallet.damageInstances.size}", style = MaterialTheme.typography.bodyMedium, color = if (pallet.damageInstances.isEmpty()) SummaryTheme.InvalidColor else SummaryTheme.DarkText, fontWeight = FontWeight.Medium)
                        if (pallet.damageInstances.isEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Error, contentDescription = null, tint = SummaryTheme.InvalidColor, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Zdjęcia:", style = MaterialTheme.typography.bodyMedium, color = SummaryTheme.MediumText)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (pallet.zdjecieDuzejEtykietyUri != null) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = if (pallet.zdjecieDuzejEtykietyUri != null) SummaryTheme.ValidColor else SummaryTheme.LightText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(if (pallet.zdjecieCalejPaletyUri != null) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = if (pallet.zdjecieCalejPaletyUri != null) SummaryTheme.ValidColor else SummaryTheme.LightText, modifier = Modifier.size(16.dp))
                    }
                }
            }
            if (palletValidation != SummaryValidationState.VALID) {
                Spacer(modifier = Modifier.height(12.dp))
                val statusMessage = when {
                    pallet.damageInstances.isEmpty() -> "Brak zdefiniowanych uszkodzeń"
                    pallet.zdjecieDuzejEtykietyUri == null && pallet.zdjecieCalejPaletyUri == null -> "Brak zdjęć etykiety i całej palety"
                    pallet.zdjecieDuzejEtykietyUri == null -> "Brak zdjęcia etykiety"
                    pallet.zdjecieCalejPaletyUri == null -> "Brak zdjęcia całej palety"
                    else -> ""
                }
                if (statusMessage.isNotEmpty()) {
                    Text(text = statusMessage, style = MaterialTheme.typography.bodySmall, color = when (palletValidation) { SummaryValidationState.INVALID -> SummaryTheme.InvalidColor; SummaryValidationState.WARNING -> SummaryTheme.WarningColor; else -> SummaryTheme.LightText })
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onEditClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(SummaryTheme.cornerRadius), border = BorderStroke(1.dp, SummaryTheme.NeutralBorder)) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edytuj paletę")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalletSummaryScreen(
    navController: NavController,
    reportViewModel: HeaderViewModel
) {
    val savedPallets by reportViewModel.savedPallets.collectAsState()
    val isNagoyaFlow by reportViewModel.isNagoyaFlow.collectAsState()
    val strategy by reportViewModel.strategy.collectAsStateWithLifecycle()

    val summaryValidation = remember(savedPallets) {
        when {
            savedPallets.isEmpty() -> SummaryValidation(state = SummaryValidationState.EMPTY, message = "Dodaj przynajmniej jedną paletę aby kontynuować")
            savedPallets.any { it.damageInstances.isEmpty() } -> SummaryValidation(state = SummaryValidationState.INVALID, message = "Niektóre palety nie mają zdefiniowanych uszkodzeń")
            savedPallets.any { it.zdjecieDuzejEtykietyUri == null || it.zdjecieCalejPaletyUri == null } -> SummaryValidation(state = SummaryValidationState.WARNING, message = "Niektóre palety nie mają wszystkich zdjęć")
            else -> SummaryValidation(state = SummaryValidationState.VALID, message = "Wszystkie palety są kompletne")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Podsumowanie Palet", color = SummaryTheme.DarkText) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (summaryValidation.state != SummaryValidationState.VALID) {
                    SummaryStatusCard(validation = summaryValidation)
                }
                val addButtonScale by animateFloatAsState(targetValue = 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "addButtonScale")
                Button(
                    onClick = {
                        reportViewModel.resetCurrentPallet()
                        navController.navigate(Screen.PalletEntry.createRoute()) {
                            popUpTo(Screen.PalletSummary.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).scale(addButtonScale),
                    colors = ButtonDefaults.buttonColors(containerColor = SummaryTheme.ValidColor),
                    shape = RoundedCornerShape(SummaryTheme.cornerRadius)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Dodaj kolejną paletę", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                }
                val continueButtonScale by animateFloatAsState(targetValue = if (savedPallets.isNotEmpty()) 1f else 0.95f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "continueButtonScale")
                Button(
                    onClick = {
                        if (isNagoyaFlow) {
                            navController.navigate(Screen.Signature.route)
                        } else {
                            val flow = strategy?.getScreenFlow()
                            val nextScreen = flow?.getOrNull((flow.indexOf(Screen.PalletSummary) ?: -1) + 1)
                            nextScreen?.let { navController.navigate(it.route) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).scale(continueButtonScale),
                    enabled = savedPallets.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (savedPallets.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (savedPallets.isNotEmpty()) Color.White else SummaryTheme.LightText),
                    shape = RoundedCornerShape(SummaryTheme.cornerRadius)
                ) {
                    if (savedPallets.isNotEmpty()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    val buttonText = if (isNagoyaFlow) "Przejdź do podpisu" else {
                        val flow = strategy?.getScreenFlow()
                        val nextScreen = flow?.getOrNull((flow.indexOf(Screen.PalletSummary) ?: -1) + 1)
                        when (nextScreen) {
                            is Screen.PalletSelection -> "Przejdź do umiejscowienia palet"
                            is Screen.DamageMarkingSummary -> "Przejdź do zaznaczania uszkodzeń"
                            else -> "Kontynuuj"
                        }
                    }
                    Text(text = if (savedPallets.isNotEmpty()) buttonText else "Dodaj palety aby kontynuować", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            PalletCountCard(palletCount = savedPallets.size)

            if (savedPallets.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SummaryTheme.NeutralBackground),
                    shape = RoundedCornerShape(SummaryTheme.cornerRadius),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Inventory, contentDescription = null, tint = SummaryTheme.LightText, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Brak zapisanych palet", style = MaterialTheme.typography.titleMedium, color = SummaryTheme.DarkText, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Dodaj przynajmniej jedną paletę z uszkodzeniami aby kontynuować", style = MaterialTheme.typography.bodyMedium, color = SummaryTheme.LightText)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(savedPallets) { index, pallet ->
                        CleanPalletSummaryCard(
                            pallet = pallet,
                            palletIndex = index + 1,
                            onEditClick = { navController.navigate(Screen.PalletEntry.createRoute(pallet.id)) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}