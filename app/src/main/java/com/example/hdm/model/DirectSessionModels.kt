// Ścieżka: com/example/hdm/model/DirectSessionModels.kt
package com.example.hdm.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Generyczna odpowiedź serwera dla wielu endpointów (update, finalize, upload).
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class DirectSessionResponse(
    val success: Boolean,
    val message: String
)

// ===== NOWY MODEL ODPOWIEDZI DLA ZDJĘĆ =====
/**
 * Dedykowana odpowiedź serwera dla endpointu /upload_image.
 * Zawiera `file_id` potrzebny do śledzenia i podmiany plików.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ImageUploadResponse(
    val success: Boolean,
    val message: String,
    @SerialName("file_id")
    val fileId: String? = null
)
// ============================================

/**
 * Model wysyłany do endpointu `.../direct_session/update`.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class SessionUpdateRequest(
    @SerialName("session_id")
    val sessionId: String,
    val data: Map<String, String>
)

/**
 * Główny obiekt odpowiedzi dla endpointu `.../direct_session/list_pending`.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class PendingSessionListResponse(
    val success: Boolean,
    val sessions: List<SessionDetailsResponse> // Używamy ponownie SessionDetailsResponse
)

/**
 * Model odpowiedzi z endpointu `.../direct_session/summary/{session_id}`.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class SessionSummaryResponse(
    @SerialName("pallet_count")
    val palletCount: Int,
    @SerialName("pallets_summary")
    val palletsSummary: List<PalletSummary>,
    @SerialName("session_id")
    val sessionId: String,
    val success: Boolean
)

/**
 * Część `SessionSummaryResponse` - reprezentuje pojedynczą paletę w podsumowaniu.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class PalletSummary(
    // ===== POPRAWKA: Dodano wartość domyślną ` = null` =====
    // To mówi parserowi, że jeśli klucz "barcode" nie istnieje w JSON,
    // ma użyć `null` zamiast rzucać błędem MissingFieldException.
    val barcode: String? = null,
    // =======================================================
    val index: Int
)

/**
 * Kompletny model danych zwracany z `.../direct_session/details/{session_id}`
 * ORAZ używany w liście z `.../list_pending`.
 *
 * ===== ZMIANA (Etap 1): Dodano pola JSON dla pełnego stanu =====
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class SessionDetailsResponse(
    @SerialName("_id")
    val _id: String = "", // ID z bazy Mongo (używane w list_pending)
    @SerialName("session_id")
    val sessionId: String? = null, // Nasze ID (jeśli inne niż _id)

    // --- Istniejące pola ---
    @SerialName("header_data")
    val headerData: SessionHeaderData? = null,
    @SerialName("pallets_data")
    val palletsData: List<SessionPalletData>? = null,

    // ===== BŁĄD POPRAWIONY =====
    // Usunięto `attachments_base64` stąd, ponieważ jest w `SessionPalletData`
    // ===========================

    // --- NOWE POLA DO PEŁNEJ SYNCHRONIZACJI (Etap 1) ---
    @SerialName("pallet_positions_json")
    val pallet_positions_json: String? = null, // Przechowuje List<PalletPosition>
    @SerialName("damage_markers_json")
    val damage_markers_json: String? = null, // Przechowuje Map<String, List<DamageMarker>>
    @SerialName("damage_heights_json")
    val damage_heights_json: String? = null, // Przechowuje Map<String, Map<String, Set<String>>>

    @SerialName("selected_vehicle_layout")
    val selected_vehicle_layout: String? = null // Przechowuje wybrany layout (np. "3x11")
)
// =======================================================

/**
 * Część `SessionDetailsResponse` - reprezentuje dane nagłówka.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class SessionHeaderData(
    // --- Istniejące pola ---
    @SerialName("report_type") val reportType: String? = null,
    @SerialName("place") val place: String? = null,
    @SerialName("feniks_ref_no") val feniksRefNo: String? = null,
    @SerialName("pic_wh") val picWh: String? = null,
    @SerialName("truck_number") val truckNumber: String? = null,
    @SerialName("container_number") val containerNumber: String? = null,
    @SerialName("responsible_party") val responsibleParty: String? = null,
    @SerialName("warehouse_location") val warehouseLocation: String? = null,
    @SerialName("truck_number_custom") val truckNumberCustom: String? = null,
    @SerialName("vehicle_type") val vehicle_type: String? = null,
    @SerialName("report_datetime") val reportDatetime: String? = null,
    @SerialName("country_of_origin") val countryOfOrigin: String? = null,
    @SerialName("comments") val comments: String? = null,
    @SerialName("report_mode_at_save") val reportModeAtSave: String? = null,
    @SerialName("attached_pdf_path_PL") val pdfPathPl: String? = null,
    @SerialName("attached_pdf_path_EN") val pdfPathEn: String? = null,

    // --- NOWE POLA DO PEŁNEJ SYNCHRONIZACJI (Etap 1) ---
    @SerialName("forklift_type")
    val forklift_type: String? = null, // Mapuje: ReportHeader.rodzajWozka
    @SerialName("report_timestamp_long")
    val report_timestamp_long: Long? = null // Mapuje: ReportHeader.dataGodzina
    // --- KONIEC NOWYCH PÓL ---
)
// =====================================================================

/**
 * Część `SessionDetailsResponse` - reprezentuje dane pojedynczej palety.
 *
 * ===== ZMIANA (Etap 1): Dodano pole dla pełnego obiektu DamagedPallet =====
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class SessionPalletData(
    // --- Istniejące pola ---
    @SerialName("pallet_number_raw") val palletNumberRaw: String? = null,
    @SerialName("lot_number") val lotNumber: String? = null,
    @SerialName("item_symbol") val itemSymbol: String? = null,
    @SerialName("batch_number") val batchNumber: String? = null,
    @SerialName("details") val details: String? = null,
    @SerialName("damage_type_key") val damageTypeKey: String? = null,
    @SerialName("report_mode_at_save") val reportModeAtSave: String? = null,
    @SerialName("damage_size") val damageSize: String? = null,

    // Listy nazw plików zdjęć na serwerze
    @SerialName("PhotoLabel") val photoLabel: List<String>? = emptyList(),
    @SerialName("PhotosDamage") val photosDamage: List<String>? = emptyList(),
    @SerialName("PhotosOverview") val photosOverview: List<String>? = emptyList(),

    // ===== POCZĄTEK POPRAWKI =====
    // Przeniesiono `attachments_base64` tutaj, aby pasowało do JSON
    @SerialName("attachments_base64")
    val attachments_base64: Map<String, List<Base64Attachment>>? = null,
    // ===== KONIEC POPRAWKI =====

    // --- NOWE POLE DO PEŁNEJ SYNCHRONIZACJI (Etap 1) ---
    @SerialName("full_pallet_model_json")
    val full_pallet_model_json: String? = null // Przechowuje zserializowany obiekt DamagedPallet
    // --- KONIEC NOWEGO POLA ---
)
// ======================================================================

/**
 * Reprezentuje pojedynczy załącznik Base64 (bez zmian).
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Base64Attachment(
    @SerialName("file_id")
    val fileId: String,
    @SerialName("data_base64")
    val dataBase64: String
)

/**
 * Klasa-wrapper dla odpowiedzi API /details (bez zmian).
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class SessionDetailsApiResponse(
    val success: Boolean,
    val session: SessionDetailsResponse
)