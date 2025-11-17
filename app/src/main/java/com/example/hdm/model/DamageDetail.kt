package com.example.hdm.model

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
@Parcelize
data class DamageDetail(
    val category: String, // np. "Folia", "Karton"
    var types: MutableList<DamageInfo> = mutableListOf() // Lista zaznaczonych typów uszkodzeń dla tej kategorii
) : Parcelable