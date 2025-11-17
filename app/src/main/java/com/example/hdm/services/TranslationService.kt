package com.example.hdm.services

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

enum class DownloadState {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

class TranslationService {

    companion object {
        private const val TAG = "TranslationService"
        private val dictionary = mapOf(
            "Rozładunek transferu" to "Unloading Transfer",
            "Rozładunek dostawy" to "Delivery Unloading",
            "Inspekcja Meiko" to "Meiko Inspection",
            "Przygotowanie wysyłki" to "Shipment Preparation",
            "Wózek nisko unoszący" to "low-lift pallet truck",
            "Wózek ręczny paletowy" to "manual pallet truck",
            "Wózek czołowy" to "Front forklift",
            "Reachtruck" to "reach truck",
            "Bus" to "Delivery truck",
            "Solo" to "Rigid truck",
            "Naczepa" to "Semi-trailer truck",
            "Tandem" to "Tandem truck",
            "Kontener" to "Container truck",
            "Wyrób gotowy" to "Finished goods",
            "Surowiec" to "Raw material",
            "Folia" to "Foil / Stretch wrap",
            "Karton" to "Cardboard / Carton",
            "Paleta" to "Pallet structure",
            "Big bag" to "Big bag",
            "Skrzynie drewniane i pojemniki metalowe" to "Wooden crates and metal containers",
            "Przecięcie / Rozerwanie" to "Cut / Tear",
            "Wilgoć" to "Moisture / Dampness",
            "Dziura" to "Hole / Puncture",
            "Otarcia" to "Abrasion / Scuffing",
            "Zgniecenie" to "Crushed / Compressed",
            "Rozerwanie/Pęknięcie" to "Tear / Crack",
            "Wgniecenie" to "Dent",
            "Pęknięcie" to "Crack / Fracture",
            "Brak elementów" to "Missing elements",
            "Wystające elementy" to "Protruding elements",
            "Pleśń/Zabrudzenia" to "Mold / Dirt / Contamination",
            "Brak/Uszkodzenie oznaczeń" to "Missing / Damaged markings",
            "Brak/Uszkodzenie slipsheet" to "Missing / Damaged slipsheet",
            "Przetarcie" to "Friction / Abrasion",
            "Przecięcie" to "Cut",
            "Zamoczenie" to "Wetness / Soaking",
            "Inne (opis)" to "Other (see description)",
            "Inne" to "Other",
            "Magazyn" to "Warehouse",
            "Dostawa" to "Delivery / Inbound",
            "Wysyłka" to "Shipment / Outbound",
            "Towar" to "Goods / Commodity",
            "Ładunek" to "Load / Cargo",
            "Przesyłka" to "Consignment / Shipment",
            "Awizo" to "Advance Shipping Notice (ASN)",
            "Folia bąbelkowa" to "Bubble wrap",
            "Taśma pakowa" to "Packing tape",
            "Etykieta" to "Label",
            "Narożnik kartonowy" to "Cardboard edge protector",
            "Przekładka" to "Layer pad / Separator sheet",
            "Wypełniacz" to "Void fill / Dunnage",
            "Wyciek" to "Leakage / Spillage",
            "Plama" to "Stain",
            "Zarysowanie" to "Scratch",
            "Odkształcenie" to "Deformation",
            "Korozja / Rdza" to "Corrosion / Rust",
            "Zanieczyszczenie" to "Contamination",
            "Niestabilny ładunek" to "Unstable load",
            "Rampa załadunkowa" to "Loading ramp / Loading bay",
            "Dok" to "Dock",
            "Strefa załadunku" to "Loading zone",
            "Strefa kwarantanny" to "Quarantine area",
            "Regał paletowy" to "Pallet rack",
            "List przewozowy" to "Bill of Lading (BOL) / Consignment Note",
            "Kontrola jakości" to "Quality check / Quality control",
            "Protokół szkody" to "Damage protocol / Damage report",
            "Sprawdzenie wizualne" to "Visual inspection"
        ).mapKeys { it.key.trim().lowercase() }
    }

    private val _downloadState = MutableStateFlow(DownloadState.IDLE)
    val downloadState: StateFlow<DownloadState> = _downloadState

    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.POLISH)
        .setTargetLanguage(TranslateLanguage.ENGLISH)
        .build()

    private val translator: Translator = Translation.getClient(options)

    init {
        checkInitialModelState()
    }

    private fun checkInitialModelState() {
        // ===== POCZĄTEK POPRAWKI =====
        // Używamy poprawnej, statycznej metody z klasy RemoteModelManager
        val modelManager = RemoteModelManager.getInstance()
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                val isPolishModelDownloaded = models.any { model -> model.language == TranslateLanguage.POLISH }
                val isEnglishModelDownloaded = models.any { model -> model.language == TranslateLanguage.ENGLISH }
                if (isPolishModelDownloaded && isEnglishModelDownloaded) {
                    Log.d(TAG, "Modele PL-EN są już pobrane. Ustawiam stan na COMPLETED.")
                    _downloadState.value = DownloadState.COMPLETED
                } else {
                    Log.d(TAG, "Modele PL-EN nie są w pełni pobrane. Stan pozostaje IDLE.")
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Nie udało się sprawdzić statusu modeli.", exception)
            }
        // ===== KONIEC POPRAWKI =====
    }

    suspend fun ensureModelsDownloaded() = withContext(Dispatchers.IO) {
        if (_downloadState.value == DownloadState.COMPLETED) return@withContext

        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        try {
            _downloadState.value = DownloadState.DOWNLOADING
            Log.d(TAG, "Rozpoczynam pobieranie modeli językowych...")
            Tasks.await(translator.downloadModelIfNeeded(conditions))
            _downloadState.value = DownloadState.COMPLETED
            Log.d(TAG, "Modele językowe są gotowe.")
        } catch (e: Exception) {
            _downloadState.value = DownloadState.FAILED
            Log.e(TAG, "Nie udało się pobrać modeli językowych.", e)
        }
    }

    suspend fun translate(text: String): String {
        if (text.isBlank()) return ""

        val fromDictionary = dictionary[text.trim().lowercase()]
        if (fromDictionary != null) {
            return fromDictionary
        }

        if (downloadState.value != DownloadState.COMPLETED) {
            ensureModelsDownloaded()
            if (downloadState.value != DownloadState.COMPLETED) {
                Log.w(TAG, "Tłumaczenie niemożliwe - modele nie są pobrane. Zwracam oryginał.")
                return text
            }
        }

        return suspendCoroutine { continuation ->
            translator.translate(text)
                .addOnSuccessListener { translatedText ->
                    continuation.resume(translatedText)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Błąd tłumaczenia tekstu przez ML Kit: $text", exception)
                    continuation.resume(text)
                }
        }
    }
}