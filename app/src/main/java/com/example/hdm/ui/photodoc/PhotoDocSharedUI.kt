package com.example.hdm.ui.photodoc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// VALIDATION SYSTEM
enum class ValidationState {
    EMPTY, VALID, INVALID
}

data class FieldValidation(
    val state: ValidationState,
    val message: String = ""
)

object CleanTheme {
    val ValidColor = Color(0xFF2E7D32)
    val InvalidColor = Color(0xFFD32F2F)
    val EmptyColor = Color(0xFF757575)
    val ValidBackground = Color(0xFFF1F8F2)
    val InvalidBackground = Color(0xFFFFF3F3)
    val NeutralBackground = Color(0xFFFAFAFA)
    val ValidBorder = Color(0xFFE8F5E8)
    val InvalidBorder = Color(0xFFFFEBEE)
    val NeutralBorder = Color(0xFFE0E0E0)
    val DarkText = Color(0xFF212121)
    val MediumText = Color(0xFF424242)
    val LightText = Color(0xFF757575)
    val cornerRadius = 12.dp
    val borderWidth = 1.dp
    val animationDuration = 250
}

@Composable
fun SubtleValidationIcon(
    validationState: ValidationState,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (validationState) {
        ValidationState.VALID -> Icons.Default.Check to CleanTheme.ValidColor
        ValidationState.INVALID -> Icons.Default.ErrorOutline to CleanTheme.InvalidColor
        ValidationState.EMPTY -> return
    }

    AnimatedVisibility(
        visible = validationState != ValidationState.EMPTY,
        enter = fadeIn(animationSpec = tween(CleanTheme.animationDuration)),
        exit = fadeOut(animationSpec = tween(150))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = modifier.size(18.dp)
        )
    }
}

@Composable
fun CleanTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    validationState: ValidationState,
    modifier: Modifier = Modifier,
    textFieldModifier: Modifier = Modifier,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    minLines: Int = 1
) {
    val borderColor by animateColorAsState(
        targetValue = when (validationState) {
            ValidationState.VALID -> CleanTheme.ValidBorder
            ValidationState.INVALID -> CleanTheme.InvalidBorder
            ValidationState.EMPTY -> CleanTheme.NeutralBorder
        },
        animationSpec = tween(CleanTheme.animationDuration),
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when (validationState) {
            ValidationState.VALID -> CleanTheme.ValidBackground
            ValidationState.INVALID -> CleanTheme.InvalidBackground
            ValidationState.EMPTY -> Color.White
        },
        animationSpec = tween(CleanTheme.animationDuration),
        label = "backgroundColor"
    )

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(CleanTheme.cornerRadius)
                )
                .border(
                    width = CleanTheme.borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(CleanTheme.cornerRadius)
                )
                .clip(RoundedCornerShape(CleanTheme.cornerRadius))
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label, color = CleanTheme.MediumText) },
                modifier = textFieldModifier.fillMaxWidth(),
                singleLine = singleLine,
                readOnly = readOnly,
                enabled = enabled,
                minLines = minLines,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    errorBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedTextColor = CleanTheme.DarkText,
                    unfocusedTextColor = CleanTheme.DarkText,
                    disabledTextColor = CleanTheme.LightText,
                    focusedLabelColor = CleanTheme.MediumText,
                    unfocusedLabelColor = CleanTheme.LightText
                ),
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SubtleValidationIcon(validationState = validationState)
                        trailingIcon?.invoke()
                    }
                }
            )
        }
        // === POPRAWKA 2: Tekst pomocniczy musi być wywołany w kontekście Column ===
        supportingText?.invoke()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanDropdownField(
    value: String,
    options: List<String>,
    label: String,
    validationState: ValidationState,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Wybierz opcję...",
    // === POPRAWKA 1: Dodano brakujący parametr `trailingIcon` ===
    trailingIcon: @Composable (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = when (validationState) {
            ValidationState.VALID -> CleanTheme.ValidBorder
            ValidationState.INVALID -> CleanTheme.InvalidBorder
            ValidationState.EMPTY -> CleanTheme.NeutralBorder
        },
        animationSpec = tween(CleanTheme.animationDuration),
        label = "borderColor"
    )
    val backgroundColor by animateColorAsState(
        targetValue = when (validationState) {
            ValidationState.VALID -> CleanTheme.ValidBackground
            ValidationState.INVALID -> CleanTheme.InvalidBackground
            ValidationState.EMPTY -> Color.White
        },
        animationSpec = tween(CleanTheme.animationDuration),
        label = "backgroundColor"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
            .fillMaxWidth()
            .background(color = backgroundColor, shape = RoundedCornerShape(CleanTheme.cornerRadius))
            .border(
                width = CleanTheme.borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(CleanTheme.cornerRadius)
            )
            .clip(RoundedCornerShape(CleanTheme.cornerRadius))
    ) {
        OutlinedTextField(
            value = if (value.isBlank()) placeholder else value,
            onValueChange = {},
            label = { Text(label, color = CleanTheme.MediumText) },
            readOnly = true,
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                errorBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                focusedTextColor = if (value.isBlank()) CleanTheme.LightText else CleanTheme.DarkText,
                unfocusedTextColor = if (value.isBlank()) CleanTheme.LightText else CleanTheme.DarkText,
                disabledTextColor = CleanTheme.LightText,
                focusedLabelColor = CleanTheme.MediumText,
                unfocusedLabelColor = CleanTheme.LightText
            ),
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Wywołanie przekazanej ikony
                    trailingIcon?.invoke()
                    SubtleValidationIcon(validationState = validationState)
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (options.isEmpty()) {
                DropdownMenuItem(text = { Text("Brak opcji do wyboru") }, onClick = { })
            } else {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CleanProgressIndicator(
    completedFields: Int,
    totalFields: Int,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (totalFields > 0) completedFields.toFloat() / totalFields.toFloat() else 0f,
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
                    text = "Postęp wypełniania",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$completedFields/$totalFields",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (completedFields == totalFields) CleanTheme.ValidColor else MaterialTheme.colorScheme.onSurfaceVariant
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