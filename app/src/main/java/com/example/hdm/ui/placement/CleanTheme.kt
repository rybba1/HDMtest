package com.example.hdm.ui.placement

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.UUID

object CleanTheme {
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

// Common data classes
data class SelectableDamage(
    val id: String,
    val displayText: String,
    val damageInstanceId: String,
    val photoUri: String?
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
@Parcelize
data class DamageMarker(
    val id: String = UUID.randomUUID().toString(),
    val coordinateX: Float,
    val coordinateY: Float,
    val colorArgb: Int,
    val assignedDamageIds: Set<String> = emptySet()
) : Parcelable {
    // Zwykłe gettery zamiast lazy - Gson je zignoruje automatycznie
    @IgnoredOnParcel
    val coordinates: Offset
        get() = Offset(coordinateX, coordinateY)

    @IgnoredOnParcel
    val color: Color
        get() = Color(colorArgb)
}
