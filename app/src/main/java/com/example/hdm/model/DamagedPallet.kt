package com.example.hdm.model

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient // NOWY IMPORT
import java.util.UUID

// ===== NOWY ENUM (WSPÓŁDZIELONY) =====
// Definiujemy stan wysyłania
enum class UploadStatus { IDLE, UPLOADING, SUCCESS, FAILED }
// =====================================

@SuppressLint("UnsafeOptInUsageError")
@Serializable
@Parcelize
data class DamagedPallet(
    var id: String = UUID.randomUUID().toString(),

    // ===== BEZ ZMIAN =====
    var serverIndex: Int? = null,
    // =====================

    var numerPalety: String = "",
    var brakNumeruPalety: Boolean = false,
    var rodzajTowaru: String? = null,
    var numerLotu: String? = null,
    var brakNumeruLotu: Boolean = false,

    var zdjecieDuzejEtykietyUri: String? = null,
    // ===== NOWE POLE (TRANSIENT) =====
    @Transient var zdjecieDuzejEtykietyUploadStatus: UploadStatus = UploadStatus.IDLE,
    // =================================

    var damageInstances: MutableList<DamageInstance> = mutableListOf(),

    var zdjecieCalejPaletyUri: String? = null,
    // ===== NOWE POLE (TRANSIENT) =====
    @Transient var zdjecieCalejPaletyUploadStatus: UploadStatus = UploadStatus.IDLE,
    // =================================

    var damageMarkingBitmapUri: String? = null,
    var numerNosnika: String? = null,

    // ===== BEZ ZMIAN =====
    var zdjecieDuzejEtykietyFileId: String? = null,
    var zdjecieCalejPaletyFileId: String? = null,
    // ==============================================

    // NOWE POLA dla archiwum Base64
    var zdjecieDuzejEtykietyBase64: String? = null,
    var zdjecieCalejPaletyBase64: String? = null,
    var damageMarkingBitmapBase64: String? = null
): Parcelable {

    // ===== NOWE FUNKCJE POMOCNICZE =====

    /** Sprawdza, czy jakiekolwiek zdjęcie dla tej palety jest W TRAKCIE wysyłania. */
    fun isAnyPhotoUploading(): Boolean {
        return zdjecieDuzejEtykietyUploadStatus == UploadStatus.UPLOADING ||
                zdjecieCalejPaletyUploadStatus == UploadStatus.UPLOADING ||
                damageInstances.any { it.uploadStatus == UploadStatus.UPLOADING }
    }

    /** Zwraca liczbę zdjęć, które się nie wysłały. */
    fun getFailedPhotoCount(): Int {
        var count = 0
        if (zdjecieDuzejEtykietyUploadStatus == UploadStatus.FAILED) count++
        if (zdjecieCalejPaletyUploadStatus == UploadStatus.FAILED) count++
        count += damageInstances.count { it.uploadStatus == UploadStatus.FAILED }
        return count
    }

    /** Sprawdza, czy wszystkie wymagane zdjęcia zostały pomyślnie wysłane (mają status SUCCESS). */
    fun areAllPhotosUploadedSuccessfully(isPalletComplete: Boolean): Boolean {
        if (!isPalletComplete) return false // Jeśli forma nieważna, to nie ma znaczenia

        // Jeśli zdjęcie etykiety istnieje, musi mieć status SUCCESS
        if (zdjecieDuzejEtykietyUri != null && zdjecieDuzejEtykietyUploadStatus != UploadStatus.SUCCESS) return false
        // Jeśli zdjęcie całej palety istnieje, musi mieć status SUCCESS
        if (zdjecieCalejPaletyUri != null && zdjecieCalejPaletyUploadStatus != UploadStatus.SUCCESS) return false
        // Wszystkie zdjęcia uszkodzeń muszą mieć status SUCCESS
        if (damageInstances.any { it.uploadStatus != UploadStatus.SUCCESS }) return false

        return true // Wszystkie warunki spełnione
    }
    // ===================================

    // Funkcja isPristine bez zmian
    fun isPristine(): Boolean {
        return this.serverIndex == null &&
                this.numerPalety.isBlank() &&
                !this.brakNumeruPalety &&
                this.rodzajTowaru.isNullOrBlank() &&
                this.numerLotu.isNullOrBlank() &&
                !this.brakNumeruLotu &&
                this.zdjecieDuzejEtykietyUri.isNullOrBlank() &&
                this.damageInstances.isEmpty() &&
                this.zdjecieCalejPaletyUri.isNullOrBlank()
    }
}