// Ścieżka: com/example/hdm/model/ArchivedSession.kt

package com.example.hdm.model

import android.os.Parcelable
import com.example.hdm.ui.placement.DamageMarker
import kotlinx.parcelize.Parcelize

@Parcelize
data class ArchivedSession(
    val archiveId: String,
    val originalSessionId: String,
    val reportType: String,
    val completionTimestamp: Long,
    val reportHeader: ReportHeader,
    val savedPallets: List<DamagedPallet>,
    val palletPositions: List<PalletPosition>,
    val damageMarkers: Map<String, List<DamageMarker>>,
    val damageHeightSelections: Map<String, Map<String, Set<String>>>,
    val palletCount: Int
) : Parcelable