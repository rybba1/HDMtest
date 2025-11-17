package com.example.hdm.ui.summary

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hdm.Screen
import com.example.hdm.model.DamagedPallet
import com.example.hdm.ui.header.HeaderViewModel


@Composable
private fun CleanPalletMarkingCard(
    pallet: DamagedPallet,
    palletIndex: Int,
    isComplete: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palletValidation = if (isComplete) SummaryValidationState.VALID else SummaryValidationState.WARNING

    val borderColor by animateColorAsState(
        targetValue = when (palletValidation) {
            SummaryValidationState.VALID -> SummaryTheme.ValidBorder
            else -> SummaryTheme.WarningBorder
        },
        animationSpec = tween(SummaryTheme.animationDuration),
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when (palletValidation) {
            SummaryValidationState.VALID -> SummaryTheme.ValidBackground
            else -> SummaryTheme.WarningBackground
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
            )
            .clickable(onClick = onClick),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Numer:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SummaryTheme.MediumText
                )
                Text(
                    text = if (pallet.brakNumeruPalety) "Brak numeru" else pallet.numerPalety.ifBlank { "Nie podano" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SummaryTheme.DarkText,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isComplete) SummaryTheme.ValidColor else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(SummaryTheme.cornerRadius)
            ) {
                Icon(
                    imageVector = if (isComplete) Icons.Default.Edit else Icons.Default.AddLocation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isComplete) "Edytuj oznaczenia" else "Zaznacz uszkodzenia",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DamageMarkingSummaryScreen(
    navController: NavController,
    reportViewModel: HeaderViewModel
) {
    val savedPallets by reportViewModel.savedPallets.collectAsState()
    val allMarkingsComplete = savedPallets.isNotEmpty() && savedPallets.all { it.damageMarkingBitmapUri != null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zaznacz Uszkodzenia na Paletach") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                SummaryStatusCard(
                    validation = if (allMarkingsComplete) {
                        SummaryValidation(SummaryValidationState.VALID, "Wszystkie palety oznaczone. Możesz kontynuować.")
                    } else {
                        SummaryValidation(SummaryValidationState.WARNING, "Oznacz uszkodzenia dla wszystkich palet.")
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { navController.navigate(Screen.EventDescription.route) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = allMarkingsComplete
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Przejdź do opisu zdarzenia")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            // PalletCountCard nie jest tutaj potrzebny, bo nie jest to główny ekran podsumowania.
            // Zamiast tego mamy prosty status.

            itemsIndexed(savedPallets, key = { _, pallet -> pallet.id }) { index, pallet ->
                CleanPalletMarkingCard(
                    pallet = pallet,
                    palletIndex = index + 1,
                    isComplete = pallet.damageMarkingBitmapUri != null,
                    onClick = {
                        navController.navigate(Screen.DamageLocation.createRoute(pallet.id, null))
                    }
                )
            }
        }
    }
}