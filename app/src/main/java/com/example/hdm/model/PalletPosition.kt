package com.example.hdm.model

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient // <- NOWY IMPORT

@SuppressLint("UnsafeOptInUsageError")
@Serializable
@Parcelize
data class PalletPosition(
    val palletId: String,
    val palletNumber: String,
    val positionOnVehicle: String, // np. "12", "4", "7" - bez literek a/b
    val stackingLevel: String, // "A" (góra) lub "B" (dół) lub "" (nie dotyczy)

    // --- USUNIĘTE POLE ---
    // val damageCoordinates: Pair<Float, Float>? = null, // Powodowało błąd serializacji Gson vs kotlinx
    // ---------------------

    val damagePart: String? = null, // "1" (góra), "2" (środek), "3" (dół)

    // --- ZMIENIONE POLE ---
    @Transient // Tego pola nie wysyłamy do API (to ścieżka lokalna)
    val damageBitmapUri: String? = null // zapisana bitmapa z krzyżykiem
    // ----------------------
) : Parcelable {

    /**
     * Generuje pełny kod uszkodzenia np. "12B3"
     * Pozycja + Poziom + Część
     */

    fun getDamageCode(): String? {
        val levelForCode = if (stackingLevel.isBlank()) "B" else stackingLevel

        return if (damagePart != null) { // Usunięto 'damageCoordinates' z warunku
            "${positionOnVehicle}${levelForCode}${damagePart}"
        } else {
            null
        }
    }

    /**
     * Sprawdza czy uszkodzenie jest kompletnie zdefiniowane
     */

    fun isDamageComplete(): Boolean {
        // Usunięto 'damageCoordinates' z warunku
        return damagePart != null && stackingLevel.isNotBlank()
    }

    /**
     * Zwraca czytelny opis pozycji dla UI
     */

    fun getDisplayPosition(): String {
        val basePosition = "Pozycja $positionOnVehicle"
        val levelDescription = when (stackingLevel) {
            "A" -> " (górna paleta)"
            "B" -> " (dolna paleta)"
            else -> ""
        }
        return basePosition + levelDescription
    }
    /**
     * Sprawdza, czy wszystkie wymagane dane pozycji zostały uzupełnione.
     * @return Zwraca tekst błędu, jeśli czegoś brakuje, lub null, jeśli wszystko jest kompletne.
     */

    fun getIncompletionMessage(): String? {
        if (damageBitmapUri.isNullOrBlank()) {
            return "Brak zaznaczenia uszkodzenia na obrazku"
        }
        if (damagePart.isNullOrBlank()) {
            return "Brak wyboru wysokości uszkodzenia"
        }
        return null // Wszystko jest w porządku
    }
}