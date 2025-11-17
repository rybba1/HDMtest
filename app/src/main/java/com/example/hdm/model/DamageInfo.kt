package com.example.hdm.model

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
@Parcelize
data class DamageInfo(
    val type: String, // np. "Dziura", "Przecięcie / Rozerwanie"
    var size: String = "",
    var description: String = "" // Dla opcji "Inne (opis)"
) : Parcelable