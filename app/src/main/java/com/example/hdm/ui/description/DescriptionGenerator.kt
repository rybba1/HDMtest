package com.example.hdm.services

import com.example.hdm.model.DamagedPallet

object DescriptionGenerator {

    /**
     * ZMODYFIKOWANA FUNKCJA
     * Teraz przyjmuje reportType, aby wygenerować poprawny nagłówek opisu.
     */
    fun generatePolishEventDescription(pallets: List<DamagedPallet>, reportType: String): String {
        if (pallets.isEmpty()) {
            return "Nie odnotowano uszkodzonych palet."
        }

        val damagedPalletPhrase = if (pallets.size == 1) "uszkodzoną paletę" else "uszkodzone palety"

        // Używamy `when` do wyboru poprawnego początku zdania
        val prefix = when (reportType) {
            "Rozładunek transferu" -> "Podczas rozładunku transferu"
            "Rozładunek dostawy" -> "Podczas rozładunku dostawy"
            "Inspekcja Meiko" -> "Podczas inspekcji Meiko"
            "Przygotowanie wysyłki" -> "Podczas przygotowania wysyłki"
            else -> "Podczas kontroli"
        }
        val header = "$prefix zauważono $damagedPalletPhrase. Szczegóły uszkodzeń:\n"

        val details = pallets.joinToString("\n") { pallet ->
            val palletIdentifier = if (pallet.brakNumeruPalety) "Paleta bez numeru" else "Paleta nr ${pallet.numerPalety}"

            val damageDetailsString = pallet.damageInstances
                .flatMap { it.details }
                .groupBy { it.category }
                .map { (category, details) ->
                    val types = details.flatMap { it.types }
                        .map { damageInfo ->
                            if (damageInfo.type == "Inne (opis)" && damageInfo.description.isNotBlank()) {
                                damageInfo.description
                            } else {
                                damageInfo.type.lowercase()
                            }
                        }
                        .distinct()
                        .joinToString(", ")

                    "$category ($types)"
                }
                .joinToString("; ")

            val itemType = when {
                !pallet.rodzajTowaru.isNullOrBlank() -> "(${pallet.rodzajTowaru})"
                else -> ""
            }

            "• $palletIdentifier: $damageDetailsString $itemType".trim()
        }

        val footer = "\nWszystkie uszkodzone palety zostały odstawione do strefy kwarantanny w celu dalszej weryfikacji."

        return header + details + footer
    }

    fun generateEnglishDamageKey(pallet: DamagedPallet): String {
        val damageCategories = pallet.damageInstances
            .flatMap { it.details }
            .map { it.category }
            .distinct().joinToString(" and ") { translateCategoryToEnglish(it) }

        return if (damageCategories.isNotBlank()) "damage to $damageCategories" else "no specific damage category"
    }

    // ===== POPRAWKA 2: Dodanie "cm" =====
    fun generateDamageSizeString(pallet: DamagedPallet): String {
        return pallet.damageInstances
            .flatMap { it.details }
            .flatMap { it.types }
            .map { it.size }
            .filter { it.isNotBlank() }.joinToString(";") { "${it}cm" }
    }
    // ===================================

    suspend fun generateEnglishDetails(
        pallet: DamagedPallet,
        reportPalletType: String,
        translationService: TranslationService,
        reportType: String
    ): String {
        val itemType = translationService.translate(reportPalletType)

        val damageDetailsString = pallet.damageInstances
            .flatMap { it.details }
            .groupBy { it.category }
            .map { (category, details) ->
                val types = details.flatMap { it.types }
                    .map { damageInfo ->
                        if (damageInfo.type == "Inne (opis)" && damageInfo.description.isNotBlank()) {
                            translationService.translate(damageInfo.description)
                        } else {
                            translationService.translate(damageInfo.type)
                        }
                    }
                    .distinct()
                    .joinToString(", ")

                val translatedCategory = translationService.translate(category)
                "$translatedCategory ($types)"
            }
            .joinToString(" and ")

        val sentenceStart = when (reportType) {
            "Rozładunek transferu" -> "During transfer unloading"
            "Rozładunek dostawy" -> "During delivery unloading"
            "Inspekcja Meiko" -> "During Meiko inspection"
            "Przygotowanie wysyłki" -> "During shipment preparation"
            else -> "During the inspection of pallets with $itemType"
        }

        return if (damageDetailsString.isNotBlank()) {
            "$sentenceStart, the warehouse worker noticed damage to the $damageDetailsString."
        } else {
            "$sentenceStart, damages were noted."
        }
    }

    suspend fun generateEnglishEventDescriptionWithTranslation(
        pallets: List<DamagedPallet>,
        translationService: TranslationService,
        reportType: String
    ): String {
        if (pallets.isEmpty()) {
            return "No damaged pallets were recorded."
        }

        val damagedPalletPhrase = if (pallets.size == 1) "a damaged pallet" else "damaged pallets"

        // --- POCZĄTEK POPRAWKI ---
        // Dodajemy logikę 'when' analogiczną do polskiej wersji, aby opis był kontekstowy.
        val prefix = when (reportType) {
            "Rozładunek transferu" -> "During transfer unloading"
            "Rozładunek dostawy" -> "During delivery unloading"
            "Inspekcja Meiko" -> "During Meiko inspection"
            "Przygotowanie wysyłki" -> "During shipment preparation"
            else -> "During the inspection"
        }
        val header = "$prefix, $damagedPalletPhrase were noticed. Damage details:\n"
        // --- KONIEC POPRAWKI ---

        val details = mutableListOf<String>()

        for (pallet in pallets) {
            val palletIdentifier = if (pallet.brakNumeruPalety) "Pallet without a number" else "Pallet no. ${pallet.numerPalety}"

            val damageDetailsList = mutableListOf<String>()

            val groupedDetails = pallet.damageInstances
                .flatMap { it.details }
                .groupBy { it.category }

            for ((category, detailsInCategory) in groupedDetails) {
                val typesList = mutableListOf<String>()

                for (detail in detailsInCategory) {
                    for (damageInfo in detail.types) {
                        val translatedType = if (damageInfo.type == "Inne (opis)" && damageInfo.description.isNotBlank()) {
                            translationService.translate(damageInfo.description)
                        } else {
                            translationService.translate(damageInfo.type).lowercase()
                        }
                        typesList.add(translatedType)
                    }
                }

                val types = typesList.distinct().joinToString(", ")
                val translatedCategory = translationService.translate(category)
                damageDetailsList.add("$translatedCategory ($types)")
            }

            val damageDetailsString = damageDetailsList.joinToString("; ")

            val itemType = when {
                !pallet.rodzajTowaru.isNullOrBlank() -> "(${translationService.translate(pallet.rodzajTowaru!!)})"
                else -> ""
            }

            details.add("• $palletIdentifier: $damageDetailsString $itemType".trim())
        }

        val footer = "\nAll damaged pallets have been moved to the quarantine area for further verification."

        return header + details.joinToString("\n") + footer
    }

    private fun translateCategoryToEnglish(category: String): String {
        return when (category) {
            "Folia" -> "Foil / Stretch wrap"
            "Karton" -> "Cardboard / Carton"
            "Paleta" -> "Pallet structure"
            "Big bag" -> "Big bag"
            "Skrzynie drewniane i pojemniki metalowe" -> "Wooden crates and metal containers"
            "Inne" -> "Other"
            else -> category.lowercase()
        }
    }
}