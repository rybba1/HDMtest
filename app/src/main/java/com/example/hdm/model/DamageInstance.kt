package com.example.hdm.model

import android.annotation.SuppressLint
import android.os.Parcelable
import com.example.hdm.model.UploadStatus // NOWY IMPORT
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient // NOWY IMPORT
import java.util.UUID

@SuppressLint("UnsafeOptInUsageError")
@Serializable
@Parcelize
data class DamageInstance(
    val id: String = UUID.randomUUID().toString(),
    val photoUri: String,
    val photoBase64: String? = null,

    // ===== BEZ ZMIAN =====
    var serverFileId: String? = null,
    // =====================

    // ===== NOWE POLE (TRANSIENT) =====
    // To pole nie będzie serializowane do JSON ani zapisywane w SavedStateHandle
    // Służy tylko do śledzenia stanu UI w bieżącej sesji.
    @Transient var uploadStatus: UploadStatus = UploadStatus.IDLE,
    // =================================

    // Zamiast pojedynczej kategorii, mamy listę szczegółów uszkodzeń, każda dla innej kategorii
    var details: MutableList<DamageDetail> = mutableListOf()
) : Parcelable