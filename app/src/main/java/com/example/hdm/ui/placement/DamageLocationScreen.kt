package com.example.hdm.ui.placement

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.hdm.R
import com.example.hdm.Screen
import com.example.hdm.ui.header.HeaderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private val markerColors = listOf(
    Color.Red,
    Color.Blue,
    Color(0xFF008000),
    Color.Magenta,
    Color.Black,
    Color.Yellow,
    Color(0xFF800080),
    Color.Cyan,
    Color(0xFF964B00),
    Color.Gray
)

private suspend fun saveBitmapWithMarkersToFile(context: Context, markers: List<DamageMarker>): String? = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.resources.openRawResource(R.drawable.pallets)
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext null

        val mutableBitmap = originalBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        markers.forEachIndexed { index, marker ->
            val textPaint = Paint().apply {
                color = marker.color.toArgb()
                textSize = 50f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = 2f
            }
            val backgroundPaint = Paint().apply {
                color = Color.White.copy(alpha = 0.8f).toArgb()
                style = Paint.Style.FILL
            }

            val x = marker.coordinates.x * originalBitmap.width
            val y = marker.coordinates.y * originalBitmap.height

            canvas.drawCircle(x, y, 30f, backgroundPaint)
            canvas.drawText((index + 1).toString(), x, y - ((textPaint.descent() + textPaint.ascent()) / 2), textPaint)
        }

        val fileName = "damage_locations_${System.currentTimeMillis()}.png"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            mutableBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        return@withContext file.absolutePath
    } catch (e: Exception) {
        Log.e("DamageLocationScreen", "Błąd podczas zapisu bitmapy", e)
        return@withContext null
    }
}

@Composable
private fun CleanDamageAssignmentDialog(
    allSelectableDamages: List<SelectableDamage>,
    markers: List<DamageMarker>,
    editingMarkerId: String,
    onDismiss: () -> Unit,
    onConfirm: (List<DamageMarker>) -> Unit
) {
    val editingMarker = markers.find { it.id == editingMarkerId }
    if (editingMarker == null) {
        onDismiss()
        return
    }

    var selectedDamageIds by remember { mutableStateOf(editingMarker.assignedDamageIds) }

    val damagesAssignedToOthers = remember(markers, editingMarkerId) {
        markers
            .filter { it.id != editingMarkerId }
            .flatMap { it.assignedDamageIds }
            .toSet()
    }

    var showPhotoPreview by remember { mutableStateOf(false) }
    var previewPhotoUri by remember { mutableStateOf<String?>(null) }

    if (showPhotoPreview && previewPhotoUri != null) {
        Dialog(onDismissRequest = {
            showPhotoPreview = false
            previewPhotoUri = null
        }) {
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
                        text = "Podgląd uszkodzenia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CleanTheme.DarkText
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Image(
                        painter = rememberAsyncImagePainter(model = Uri.parse(previewPhotoUri)),
                        contentDescription = "Podgląd uszkodzenia",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(CleanTheme.cornerRadius)),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showPhotoPreview = false
                            previewPhotoUri = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CleanTheme.InProgressColor),
                        shape = RoundedCornerShape(CleanTheme.cornerRadius)
                    ) {
                        Text("Zamknij", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Przypisz uszkodzenia do znacznika",
                fontWeight = FontWeight.Bold,
                color = CleanTheme.DarkText
            )
        },
        text = {
            Column {
                CleanInstructionCard(
                    instruction = "Przytrzymaj wiersz, aby zobaczyć podgląd zdjęcia uszkodzenia."
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(
                        items = allSelectableDamages,
                        key = { it.id }
                    ) { damage ->
                        DamageListItem(
                            damage = damage,
                            isSelected = selectedDamageIds.contains(damage.id),
                            isEnabled = !damagesAssignedToOthers.contains(damage.id) || selectedDamageIds.contains(damage.id),
                            onToggle = { damageId ->
                                selectedDamageIds = if (selectedDamageIds.contains(damageId)) {
                                    selectedDamageIds - damageId
                                } else {
                                    selectedDamageIds + damageId
                                }
                            },
                            onShowPhoto = { uri ->
                                previewPhotoUri = uri
                                showPhotoPreview = true
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedMarkers = markers.map { marker ->
                        if (marker.id == editingMarkerId) {
                            marker.copy(assignedDamageIds = selectedDamageIds)
                        } else {
                            marker
                        }
                    }
                    onConfirm(updatedMarkers)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CleanTheme.ValidColor),
                shape = RoundedCornerShape(CleanTheme.cornerRadius)
            ) {
                Text("Potwierdź", color = Color.White, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = CleanTheme.MediumText)
            ) {
                Text("Anuluj", fontWeight = FontWeight.Medium)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(CleanTheme.cornerRadius)
    )
}

@Composable
private fun DamageListItem(
    damage: SelectableDamage,
    isSelected: Boolean,
    isEnabled: Boolean,
    onToggle: (String) -> Unit,
    onShowPhoto: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CleanTheme.ValidBackground else Color.White
        ),
        border = if (isSelected) BorderStroke(2.dp, CleanTheme.ValidBorder) else CardDefaults.outlinedCardBorder(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isEnabled) {
                        Modifier.pointerInput(damage.id) {
                            detectTapGestures(
                                onTap = {
                                    onToggle(damage.id)
                                },
                                onLongPress = {
                                    damage.photoUri?.let { uri ->
                                        onShowPhoto(uri)
                                    }
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                enabled = isEnabled,
                colors = CheckboxDefaults.colors(
                    checkedColor = CleanTheme.ValidColor,
                    uncheckedColor = if (isEnabled) CleanTheme.MediumText else CleanTheme.LightText,
                    disabledCheckedColor = CleanTheme.ValidColor.copy(alpha = 0.5f),
                    disabledUncheckedColor = CleanTheme.LightText.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = damage.displayText,
                color = when {
                    !isEnabled -> CleanTheme.LightText
                    isSelected -> CleanTheme.ValidColor
                    else -> CleanTheme.DarkText
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )

            if (damage.photoUri != null) {
                Icon(
                    Icons.Default.Photo,
                    contentDescription = "Ma zdjęcie",
                    tint = if (isEnabled) CleanTheme.InProgressColor else CleanTheme.LightText,
                    modifier = Modifier.size(16.dp)
                )
            }
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
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = CleanTheme.InProgressColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = instruction,
                style = MaterialTheme.typography.bodyMedium,
                color = CleanTheme.DarkText,
                fontWeight = FontWeight.Medium
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
            val icon = if (isComplete) Icons.Default.CheckCircle else Icons.Default.Schedule
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
private fun CleanDeleteTrashCan(
    visible: Boolean,
    isMarkerOverTrash: Boolean,
    defaultColor: Color,
    onBoundsMeasured: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(CleanTheme.animationDuration)),
        exit = fadeOut(animationSpec = tween(150))
    ) {
        val scale by animateFloatAsState(
            targetValue = if (isMarkerOverTrash) 1.2f else 1.0f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
            label = "trashScale"
        )

        val backgroundColor by animateColorAsState(
            targetValue = if (isMarkerOverTrash) CleanTheme.InvalidBackground else CleanTheme.NeutralBackground,
            animationSpec = tween(CleanTheme.animationDuration),
            label = "trashBackground"
        )

        val iconColor = if (isMarkerOverTrash) CleanTheme.InvalidColor else defaultColor

        Card(
            modifier = Modifier
                .onGloballyPositioned { layoutCoordinates ->
                    val boundsInRoot = layoutCoordinates.boundsInRoot()
                    onBoundsMeasured(boundsInRoot)
                }
                .scale(scale),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(CleanTheme.cornerRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isMarkerOverTrash) 4.dp else 2.dp)
        ) {
            Box(
                modifier = Modifier.padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Usuń znacznik",
                    tint = iconColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DamageLocationScreen(
    navController: NavController,
    reportViewModel: HeaderViewModel,
    selectedPalletId: String,
    selectedPosition: String?
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val savedPallets by reportViewModel.savedPallets.collectAsStateWithLifecycle()
    val pallet = remember { savedPallets.find { it.id == selectedPalletId } }

    val selectableDamages = remember(pallet) {
        pallet?.damageInstances?.flatMap { damageInstance ->
            damageInstance.details.flatMap { detail ->
                detail.types.map { type ->
                    SelectableDamage(
                        id = "${damageInstance.id}-${detail.category}-${type.type}",
                        displayText = "${detail.category}: ${type.type} (${type.size} cm)",
                        damageInstanceId = damageInstance.id,
                        photoUri = damageInstance.photoUri
                    )
                }
            }
        } ?: emptyList()
    }

    val markersFromViewModel by reportViewModel.damageMarkersState.collectAsStateWithLifecycle()
    val markers = markersFromViewModel[selectedPalletId] ?: emptyList()

    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var showDamageDialog by remember { mutableStateOf(false) }
    var currentlyEditingMarkerId by remember { mutableStateOf<String?>(null) }
    var heldMarkerId by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var markerToDeleteId by remember { mutableStateOf<String?>(null) }
    var imageLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var trashCanRect by remember { mutableStateOf(Rect.Zero) }
    var isMarkerOverTrash by remember { mutableStateOf(false) }

    val allDamageIds = remember(selectableDamages) { selectableDamages.map { it.id }.toSet() }
    val assignedDamageIds = markers.flatMap { it.assignedDamageIds }.toSet()
    val isEverythingAssigned = allDamageIds.isNotEmpty() && allDamageIds == assignedDamageIds
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    if (showDamageDialog && currentlyEditingMarkerId != null) {
        CleanDamageAssignmentDialog(
            allSelectableDamages = selectableDamages,
            markers = markers,
            editingMarkerId = currentlyEditingMarkerId!!,
            onDismiss = { showDamageDialog = false },
            onConfirm = { newMarkers ->
                reportViewModel.updateDamageMarkers(selectedPalletId, newMarkers)
                showDamageDialog = false
                currentlyEditingMarkerId = null
            }
        )
    }

    if (showDeleteDialog && markerToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    "Potwierdź usunięcie",
                    fontWeight = FontWeight.Bold,
                    color = CleanTheme.DarkText
                )
            },
            text = {
                Text(
                    "Czy na pewno chcesz usunąć ten znacznik? Przypisane do niego uszkodzenia zostaną zwolnione.",
                    color = CleanTheme.MediumText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedMarkers = markers.filterNot { it.id == markerToDeleteId }
                        reportViewModel.updateDamageMarkers(selectedPalletId, updatedMarkers)
                        showDeleteDialog = false
                        markerToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CleanTheme.InvalidColor),
                    shape = RoundedCornerShape(CleanTheme.cornerRadius)
                ) {
                    Text("Tak, usuń", color = Color.White, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = CleanTheme.MediumText)
                ) {
                    Text("Anuluj", fontWeight = FontWeight.Medium)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(CleanTheme.cornerRadius)
        )
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
                "Zaznacz miejsca uszkodzeń",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = CleanTheme.DarkText,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        CleanInstructionCard(
            instruction = "Kliknij znacznik, aby przypisać uszkodzenia. Przytrzymaj, aby przesunąć lub usunąć."
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (markers.size < selectableDamages.size) {
                    val newMarker = DamageMarker(
                        coordinateX = 0.5f,
                        coordinateY = 0.5f,
                        colorArgb = markerColors[markers.size % markerColors.size].toArgb()
                    )
                    reportViewModel.updateDamageMarkers(selectedPalletId, markers + newMarker)
                }
            },
            enabled = !isEverythingAssigned && markers.size < selectableDamages.size,
            colors = ButtonDefaults.buttonColors(
                containerColor = CleanTheme.InProgressColor,
                disabledContainerColor = CleanTheme.EmptyColor
            ),
            shape = RoundedCornerShape(CleanTheme.cornerRadius),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Dodaj miejsce uszkodzenia", fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(16.dp))
        CleanStatusCard(
            isComplete = isEverythingAssigned,
            completionMessage = if (isEverythingAssigned) {
                "Wszystkie uszkodzenia zostały przypisane, możesz przejść dalej"
            } else if (markers.isEmpty()) {
                "Dodaj znaczniki dla miejsc uszkodzeń"
            } else {
                "Przypisz wszystkie uszkodzenia do znaczników"
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(CleanTheme.cornerRadius),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .onGloballyPositioned {
                            imageSize = it.size
                            imageLayoutCoordinates = it
                        }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.pallets),
                        contentDescription = "Schemat palety",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    val updatedMarkers by rememberUpdatedState(markers)

                    updatedMarkers.forEachIndexed { index, marker ->
                        val scale by animateFloatAsState(
                            targetValue = if (heldMarkerId == marker.id) 1.3f else 1.0f,
                            animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
                            label = "markerScale"
                        )

                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (marker.coordinates.x * imageSize.width / density.density).dp - (20.dp * scale),
                                    y = (marker.coordinates.y * imageSize.height / density.density).dp - (20.dp * scale)
                                )
                                .scale(scale)
                                .size(40.dp)
                                .pointerInput(marker.id) {
                                    detectTapGestures(
                                        onTap = {
                                            currentlyEditingMarkerId = marker.id
                                            showDamageDialog = true
                                        },
                                        onLongPress = { }
                                    )
                                }
                                .pointerInput(marker.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { heldMarkerId = marker.id },
                                        onDragEnd = {
                                            if (isMarkerOverTrash) {
                                                markerToDeleteId = heldMarkerId
                                                showDeleteDialog = true
                                            }
                                            heldMarkerId = null
                                            isMarkerOverTrash = false
                                        },
                                        onDragCancel = { heldMarkerId = null },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val currentMarker = updatedMarkers.find { it.id == heldMarkerId } ?: return@detectDragGesturesAfterLongPress
                                            val newNormalizedX = (currentMarker.coordinates.x + (dragAmount.x / imageSize.width)).coerceIn(0f, 1f)
                                            val newNormalizedY = (currentMarker.coordinates.y + (dragAmount.y / imageSize.height)).coerceIn(0f, 1f)
                                            val newCoords = Offset(newNormalizedX, newNormalizedY)

                                            val nextMarkers = updatedMarkers.map {
                                                if (it.id == heldMarkerId) it.copy(coordinateX = newCoords.x, coordinateY = newCoords.y) else it
                                            }
                                            reportViewModel.updateDamageMarkers(selectedPalletId, nextMarkers)

                                            val markerPixelOffset = Offset(newCoords.x * imageSize.width, newCoords.y * imageSize.height)
                                            val markerGlobalOffset = imageLayoutCoordinates?.localToRoot(markerPixelOffset)

                                            if (markerGlobalOffset != null) {
                                                isMarkerOverTrash = trashCanRect.contains(markerGlobalOffset)
                                            }
                                        }
                                    )
                                }
                        ) {
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = CircleShape,
                                border = BorderStroke(3.dp, marker.color),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = marker.color,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            CleanDeleteTrashCan(
                visible = heldMarkerId != null,
                isMarkerOverTrash = isMarkerOverTrash,
                defaultColor = onSurfaceColor,
                onBoundsMeasured = { trashCanRect = it },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isEverythingAssigned) {
                    coroutineScope.launch {
                        val bitmapPath = saveBitmapWithMarkersToFile(context, markers)
                        if (bitmapPath != null) {
                            if (selectedPosition != null) {
                                reportViewModel.updateDamageCoordinates(
                                    palletId = selectedPalletId,
                                    bitmapUri = bitmapPath
                                )
                            } else {
                                reportViewModel.updatePalletDamageMarkingBitmap(selectedPalletId, bitmapPath)
                            }
                        }
                        navController.navigate(Screen.HeightSelection.createRoute(selectedPalletId, 0))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isEverythingAssigned,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isEverythingAssigned) CleanTheme.ValidColor else CleanTheme.EmptyColor
            ),
            shape = RoundedCornerShape(CleanTheme.cornerRadius)
        ) {
            if (isEverythingAssigned) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (isEverythingAssigned) "Dalej - wybór wysokości" else "Przypisz wszystkie uszkodzenia",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}