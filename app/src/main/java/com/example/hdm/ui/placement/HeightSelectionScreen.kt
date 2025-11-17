package com.example.hdm.ui.placement

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.hdm.Screen
import com.example.hdm.ui.header.HeaderViewModel

@Composable
private fun CleanProgressCard(
    currentMarker: Int,
    totalMarkers: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalMarkers > 0) currentMarker.toFloat() / totalMarkers.toFloat() else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "progress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CleanTheme.NeutralBackground),
        shape = RoundedCornerShape(CleanTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Znacznik $currentMarker z $totalMarkers",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = CleanTheme.DarkText
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .width(80.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (animatedProgress >= 1f) CleanTheme.ValidColor else MaterialTheme.colorScheme.primary,
                trackColor = Color(0xFFE0E0E0)
            )
        }
    }
}

@Composable
private fun CleanInstructionCard(
    instruction: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CleanTheme.InProgressBackground),
        shape = RoundedCornerShape(CleanTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = CleanTheme.InProgressColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = instruction,
                style = MaterialTheme.typography.bodySmall,
                color = CleanTheme.DarkText
            )
        }
    }
}

@Composable
private fun CleanMarkerInfoCard(
    marker: DamageMarker,
    markerIndex: Int,
    damagesForMarker: List<SelectableDamage>,
    onPhotoPreviewRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onPhotoPreviewRequest() })
            },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(CleanTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Marker indicator
            Card(
                modifier = Modifier.size(24.dp),
                colors = CardDefaults.cardColors(containerColor = marker.color),
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = markerIndex.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Znacznik $markerIndex (${damagesForMarker.size} uszkodzeń)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = CleanTheme.DarkText
                )
            }

            if (damagesForMarker.any { it.photoUri != null }) {
                Icon(
                    Icons.Default.Photo,
                    contentDescription = "Zdjęcia",
                    tint = CleanTheme.InProgressColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CleanHeightSelector(
    currentSelection: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Wybierz wysokość uszkodzeń",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CleanTheme.DarkText
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Visual pallet representation - centered
        Column(
            modifier = Modifier.width(280.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Height sections from top to bottom - larger sections
            val sections = listOf("1", "2", "3")

            sections.forEach { id ->
                CleanPalletSection(
                    sectionId = id,
                    isSelected = currentSelection.contains(id),
                    onClick = {
                        val newSelection = currentSelection.toMutableSet()
                        if (newSelection.contains(id)) {
                            if (newSelection.size > 1) newSelection.remove(id)
                        } else {
                            newSelection.add(id)
                        }
                        onSelectionChange(newSelection)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )

                if (id != "3") {
                    // Separator line between sections
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CleanTheme.NeutralBorder)
                    )
                }
            }

            // Pallet base - slightly larger
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(
                        Color(0xFF8D6E63),
                        RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)
                    )
            ) {
                // Wood texture lines
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .offset(y = (4 + index * 4).dp)
                            .background(Color(0xFF6D4C41))
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanPalletSection(
    modifier: Modifier = Modifier,
    sectionId: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) CleanTheme.InvalidColor else Color(0xFFF5F5F5),
        animationSpec = tween(CleanTheme.animationDuration),
        label = "sectionBackground"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) CleanTheme.InvalidColor else Color(0xFFE0E0E0),
        animationSpec = tween(CleanTheme.animationDuration),
        label = "sectionBorder"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "sectionScale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = when (sectionId) {
            "1" -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
            else -> RoundedCornerShape(0.dp)
        },
        border = BorderStroke(2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Minimalistic number display
            Text(
                text = sectionId,
                style = MaterialTheme.typography.displayMedium,
                color = if (isSelected) Color.White else CleanTheme.DarkText,
                fontWeight = FontWeight.Bold
            )

            // Subtle cargo texture pattern for unselected sections
            if (!isSelected) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(8) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(
                                    Color(0xFFE0E0E0),
                                    RoundedCornerShape(0.5.dp)
                                )
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
    completionMessage: String,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isComplete)
            CleanTheme.ValidBackground
        else
            CleanTheme.NeutralBackground,
        animationSpec = tween(CleanTheme.animationDuration),
        label = "statusColor"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(CleanTheme.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = if (isComplete) Icons.Default.CheckCircle else Icons.Default.Schedule
            val iconColor = if (isComplete) CleanTheme.ValidColor else CleanTheme.MediumText

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = completionMessage,
                style = MaterialTheme.typography.bodySmall,
                color = if (isComplete) CleanTheme.ValidColor else CleanTheme.DarkText,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CleanPhotoPreviewDialog(
    photoUris: List<String>,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { photoUris.size })

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(CleanTheme.cornerRadius),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Podgląd zdjęć uszkodzeń",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CleanTheme.DarkText
                )

                if (photoUris.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Zdjęcie ${pagerState.currentPage + 1} z ${photoUris.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CleanTheme.MediumText
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.height(300.dp)
                ) { page ->
                    Image(
                        painter = rememberAsyncImagePainter(model = Uri.parse(photoUris[page])),
                        contentDescription = "Podgląd uszkodzenia",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CleanTheme.cornerRadius)),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CleanTheme.InProgressColor),
                    shape = RoundedCornerShape(CleanTheme.cornerRadius)
                ) {
                    Text("Zamknij", color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HeightSelectionScreen(
    navController: NavController,
    reportViewModel: HeaderViewModel,
    selectedPalletId: String,
    markerIndex: Int,
    returnRoute: String

) {
    val damageMarkersMap by reportViewModel.damageMarkersState.collectAsStateWithLifecycle()
    val markers = damageMarkersMap[selectedPalletId] ?: emptyList()

    if (markerIndex >= markers.size) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }

    val currentMarker = markers[markerIndex]
    val isLastMarker = markerIndex == markers.size - 1

    val heightSelections by reportViewModel.damageHeightSelections.collectAsStateWithLifecycle()
    val currentSelection = heightSelections[selectedPalletId]?.get(currentMarker.id) ?: emptySet()

    var showPhotoPreview by remember { mutableStateOf(false) }

    // Get damages for current marker
    val savedPallets by reportViewModel.savedPallets.collectAsStateWithLifecycle()
    val pallet = remember { savedPallets.find { it.id == selectedPalletId } }
    val selectableDamages = remember(pallet) {
        pallet?.damageInstances?.flatMap { di -> di.details.flatMap { d -> d.types.map { t ->
            SelectableDamage(
                id = "${di.id}-${d.category}-${t.type}",
                displayText = "${d.category}: ${t.type} (${t.size} cm)",
                damageInstanceId = di.id,
                photoUri = di.photoUri
            )
        }}} ?: emptyList()
    }
    val damagesForThisMarker = selectableDamages.filter { currentMarker.assignedDamageIds.contains(it.id) }
    val photoUris = damagesForThisMarker.mapNotNull { it.photoUri }.distinct()

    if (showPhotoPreview && photoUris.isNotEmpty()) {
        CleanPhotoPreviewDialog(
            photoUris = photoUris,
            onDismiss = { showPhotoPreview = false }
        )
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
                "Wysokość uszkodzenia",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = CleanTheme.DarkText,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress indicator
        CleanProgressCard(
            currentMarker = markerIndex + 1,
            totalMarkers = markers.size
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Instruction card
        CleanInstructionCard(
            instruction = "Wybierz części palety z uszkodzeniami. Przytrzymaj znacznik by zobaczyć zdjęcia."
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Marker info card
        CleanMarkerInfoCard(
            marker = currentMarker,
            markerIndex = markerIndex + 1,
            damagesForMarker = damagesForThisMarker,
            onPhotoPreviewRequest = { showPhotoPreview = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Status card
        CleanStatusCard(
            isComplete = currentSelection.isNotEmpty(),
            completionMessage = if (currentSelection.isNotEmpty()) {
                if (isLastMarker) "Gotowe - wszystkie znaczniki skonfigurowane"
                else "Wybrano wysokość, możesz przejść dalej"
            } else {
                "Wybierz przynajmniej jedną część palety"
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Height selector - visual pallet representation - now takes most space
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            CleanHeightSelector(
                currentSelection = currentSelection,
                onSelectionChange = { newSelection ->
                    reportViewModel.updateDamageHeightSelection(selectedPalletId, currentMarker.id, newSelection)
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Continue button
        Button(
            onClick = {
                if (isLastMarker) {
                    reportViewModel.finalizeDamageParts(selectedPalletId)
                    // POPRAWKA: Wracamy do dynamicznie określonej trasy
                    navController.popBackStack(returnRoute, false)
                } else {
                    navController.navigate(Screen.HeightSelection.createRoute(selectedPalletId, markerIndex + 1))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = currentSelection.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (currentSelection.isNotEmpty()) CleanTheme.ValidColor else CleanTheme.EmptyColor
            ),
            shape = RoundedCornerShape(CleanTheme.cornerRadius)
        ) {
            if (currentSelection.isNotEmpty()) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (isLastMarker) "Zapisz i zakończ" else "Dalej",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}