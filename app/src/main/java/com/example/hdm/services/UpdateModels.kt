// ZASTĄP CAŁY PLIK: com/example/hdm/services/UpdateModels.kt
package com.example.hdm.services

import android.annotation.SuppressLint
import android.net.Uri // <-- NOWY IMPORT
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Modele API ---

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UpdateCheckRequest(
    @SerialName("device_name") val deviceName: String,
    @SerialName("current_version") val currentVersion: String?,
    @SerialName("device_ip") val deviceIp: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UpdateCheckResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("update_available") val updateAvailable: Boolean,
    @SerialName("latest_version") val latestVersion: String,
    @SerialName("update_filename") val updateFilename: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ChangelogResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("changelog") val changelog: List<String> = emptyList()
)

// --- Modele Stanu UI ---

/**
 * Przechowuje kompletne informacje o dostępnej aktualizacji.
 */
data class UpdateInfo(
    val latestVersion: String,
    val filename: String,
    val changelog: List<String>
)

/**
 * Reprezentuje stan procesu aktualizacji w całej aplikacji.
 */
sealed class UpdateState {
    /** Stan początkowy, nic się nie dzieje. */
    object Idle : UpdateState()

    /**
     * Aktualizacja jest dostępna, przechowuje jej dane.
     * @param info Szczegóły aktualizacji (wersja, nazwa pliku, changelog).
     */
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState()

    /**
     * Aktualizacja jest w trakcie pobierania.
     * @param progress Postęp pobierania (0-100).
     */
    data class Downloading(val progress: Int) : UpdateState()

    /** Wystąpił błąd podczas sprawdzania lub pobierania. */
    data class Error(val message: String) : UpdateState()

    // --- POCZĄTEK ZMIANY ---
    /**
     * Pobieranie zakończone pomyślnie. Plik jest gotowy do instalacji.
     * @param apkUri Bezpieczne URI do pobranego pliku APK.
     */
    data class DownloadReadyToInstall(val apkUri: Uri) : UpdateState()
    // --- KONIEC ZMIANY ---
}