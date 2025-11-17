package com.example.hdm.services

import android.util.Log

object CodeValidator {

    // Wagi dla PZ (tylko cyfry, 10 pozycji)
    private val WEIGHTS_PZ = intArrayOf(1, 1, 3, 1, 1, 3, 1, 1, 3, 1)

    /** Główna metoda – zwraca true, gdy numer ma poprawny format i cyfrę kontrolną. */
    fun isValid(raw: String): Boolean {
        val code = raw.trim().uppercase()

        return when {
            code.startsWith("TR")  -> validatePzcEan13Ascii(code)  // FIX: TR używa tego samego algorytmu co PZC!
            code.startsWith("WZ")  -> validateEan13Ascii(code)
            code.startsWith("PZC") -> validatePzcEan13Ascii(code)
            code.startsWith("PZ")  -> validatePz(code)
            code.startsWith("LZ")  -> validateEan13Ascii(code)
            code.startsWith("ZT")  -> validateEan13Ascii(code)
            else                   -> false
        }
    }

    private val PALLET_PREDEFINED_VALUES = setOf("BRAK")

    private val PALLET_PATTERNS = mapOf(
        "FP" to listOf(
            Regex("^\\w{6}_\\w{11}$"),
            Regex("^\\w{6}_\\w{10}$")
        ),
        "RM_INNE" to listOf(
            Regex("^[0-2]229999\\d{11}\\Z"),
            Regex("^[56]\\d{7}\\Z"),
            Regex("^CACE2\\d{3}-\\d{2}(B|)-\\d{2}\\Z"),
            Regex("^99022\\d{4}P\\d{1,2}\\Z"),
            Regex("^CACE\\d{4}-\\d{2}-\\d{2}\\Z")
        )
    )

    /**
     * Sprawdza poprawność numeru palety na podstawie podanego typu palety.
     * @param palletNo Numer palety do walidacji.
     * @param palletType Typ palety ("Wyrób gotowy", "Surowiec", "Inne").
     * @return Zwraca true, jeśli numer jest poprawny, w przeciwnym razie false.
     */
    fun isPalletNumberValid(palletNo: String, palletType: String): Boolean {
        Log.d("CodeValidator", "═══ isPalletNumberValid ═══")
        Log.d("CodeValidator", "Input: '$palletNo' (len=${palletNo.length})")
        Log.d("CodeValidator", "Type: '$palletType'")

        if (palletNo.isBlank()) {
            Log.d("CodeValidator", "REJECTED: blank")
            return false
        }
        if (palletNo in PALLET_PREDEFINED_VALUES) {
            Log.d("CodeValidator", "ACCEPTED: predefined value")
            return true
        }

        val mode = when (palletType) {
            "Wyrób gotowy" -> "FP"
            "Surowiec", "Inne" -> "RM_INNE"
            else -> {
                Log.d("CodeValidator", "REJECTED: unknown type")
                return false // Nieznany typ, nie walidujemy
            }
        }

        val patternsToCheck = PALLET_PATTERNS[mode] ?: return false

        patternsToCheck.forEachIndexed { index, pattern ->
            val matches = pattern.matches(palletNo)
            Log.d("CodeValidator", "Pattern[$index] ($mode): ${pattern.pattern}")
            Log.d("CodeValidator", "  -> matches: $matches")
        }

        val result = patternsToCheck.any { it.matches(palletNo) }
        Log.d("CodeValidator", "FINAL RESULT: $result")
        Log.d("CodeValidator", "═══════════════════════════════")

        return result
    }

    /**
     * Sprawdza poprawność numeru palety pod kątem WSZYSTKICH zdefiniowanych formatów.
     * @param palletNo Numer palety do walidacji.
     * @return Zwraca true, jeśli numer pasuje do któregokolwiek formatu.
     */
    fun isAnyPalletNumberValid(palletNo: String): Boolean {
        Log.d("CodeValidator", "═══ isAnyPalletNumberValid ═══")
        Log.d("CodeValidator", "Input: '$palletNo' (len=${palletNo.length})")

        if (palletNo.isBlank()) {
            Log.d("CodeValidator", "REJECTED: blank")
            return false
        }
        if (palletNo in PALLET_PREDEFINED_VALUES) {
            Log.d("CodeValidator", "ACCEPTED: predefined value")
            return true
        }

        // Bierzemy wszystkie wzorce z mapy i łączymy w jedną listę
        val allPatterns = PALLET_PATTERNS.values.flatten()

        allPatterns.forEachIndexed { index, pattern ->
            val matches = pattern.matches(palletNo)
            Log.d("CodeValidator", "Pattern[$index]: ${pattern.pattern}")
            Log.d("CodeValidator", "  -> matches: $matches")
        }

        val result = allPatterns.any { it.matches(palletNo) }
        Log.d("CodeValidator", "FINAL RESULT: $result")
        Log.d("CodeValidator", "═══════════════════════════════")

        return result
    }


    /* ---------- helpers ---------- */

    private fun validatePz(code: String): Boolean {
        val (core, cd) = splitCoreAndCdPz(code) ?: return false
        val checksum = calcCd(core, WEIGHTS_PZ, 9)
        return checksum == cd
    }

    private fun validateEan13Ascii(code: String): Boolean {
        if (code.length < 2) return false

        val codeWithoutChecksum = code.dropLast(1)
        val checksumDigit = code.last().digitToIntOrNull() ?: return false

        val calculatedChecksum = calcEan13Ascii(codeWithoutChecksum)
        return calculatedChecksum == checksumDigit
    }

    private fun validatePzcEan13Ascii(code: String): Boolean {
        if (code.length < 2) return false

        val codeWithoutChecksum = code.dropLast(1)
        val checksumDigit = code.last().digitToIntOrNull() ?: return false

        val calculatedChecksum = calcPzcEan13Ascii(codeWithoutChecksum)
        return calculatedChecksum == checksumDigit
    }

    private fun calcCd(digits: String, weights: IntArray, base: Int): Int {
        val sum = digits.mapIndexed { i, c -> (c - '0') * weights[i] }.sum()
        val result = (base - (sum % 10)) % 10
        return if (result < 0) result + 10 else result
    }

    private fun calcEan13Ascii(code: String): Int {
        val sum = code.mapIndexed { i, c ->
            val asciiValue = c.code
            val weight = if (i % 2 == 0) 3 else 1
            asciiValue * weight
        }.sum()
        return (10 - (sum % 10)) % 10
    }

    private fun calcPzcEan13Ascii(code: String): Int {
        val sum = code.mapIndexed { i, c ->
            val asciiValue = c.code
            val weight = if (i % 2 == 0) 1 else 3
            asciiValue * weight
        }.sum()
        return (10 - (sum % 10)) % 10
    }

    /**
     * Sprawdza poprawność numeru kontenera morskiego według standardu ISO 6346.
     * Format: 4 litery (prefiks właściciela) + 6 cyfr (numer seryjny) + 1 cyfra kontrolna
     * @param containerNo Numer kontenera do walidacji (np. "CSQU3054383")
     * @return Zwraca true, jeśli numer kontenera jest poprawny
     */
    fun isContainerNumberValid(containerNo: String): Boolean {
        if (containerNo.isBlank()) {
            return false
        }

        val cleaned = containerNo.trim().uppercase()

        // Sprawdź długość (4 litery + 6 cyfr + 1 cyfra kontrolna = 11 znaków)
        if (cleaned.length != 11) {
            return false
        }

        // Sprawdź format: 4 litery, 6 cyfr, 1 cyfra
        val prefixMatch = Regex("^[A-Z]{4}").find(cleaned)
        val serialMatch = Regex("^[A-Z]{4}(\\d{6})").find(cleaned)
        val checkDigitMatch = Regex("^[A-Z]{4}\\d{6}(\\d)$").find(cleaned)

        if (prefixMatch == null || serialMatch == null || checkDigitMatch == null) {
            return false
        }

        val prefix = cleaned.substring(0, 4)
        val serialNumber = cleaned.substring(4, 10)
        val providedCheckDigit = cleaned[10].digitToIntOrNull() ?: return false

        // Oblicz cyfrę kontrolną
        val calculatedCheckDigit = calculateContainerCheckDigit(prefix, serialNumber)

        return calculatedCheckDigit == providedCheckDigit
    }

    /**
     * Oblicza cyfrę kontrolną dla numeru kontenera morskiego.
     * Algorytm zgodny z ISO 6346.
     */
    private fun calculateContainerCheckDigit(prefix: String, serialNumber: String): Int {
        // Tablica wartości dla liter A-Z (pomijamy I, O, Q które nie są używane w standardzie,
        // ale dla uproszczenia mapujemy wszystkie litery)
        val letterValues = mapOf(
            'A' to 10, 'B' to 12, 'C' to 13, 'D' to 14, 'E' to 15, 'F' to 16, 'G' to 17,
            'H' to 18, 'I' to 19, 'J' to 20, 'K' to 21, 'L' to 23, 'M' to 24, 'N' to 25,
            'O' to 26, 'P' to 27, 'Q' to 28, 'R' to 29, 'S' to 30, 'T' to 31, 'U' to 32,
            'V' to 34, 'W' to 35, 'X' to 36, 'Y' to 37, 'Z' to 38
        )

        val fullNumber = prefix + serialNumber
        var sum = 0

        // Dla każdej pozycji (0-9):
        for (i in 0 until 10) {
            val value = if (i < 4) {
                // Litery prefiksu - pobierz wartość z mapy
                letterValues[fullNumber[i]] ?: return -1
            } else {
                // Cyfry numeru seryjnego
                fullNumber[i].digitToIntOrNull() ?: return -1
            }

            // Mnożnik to 2^i
            val multiplier = 1 shl i  // To samo co Math.pow(2, i).toInt()
            sum += value * multiplier
        }

        // Cyfra kontrolna to reszta z dzielenia przez 11
        val checkDigit = sum % 11

        // Jeśli reszta wynosi 10, cyfra kontrolna to 0
        return if (checkDigit == 10) 0 else checkDigit
    }

    private fun splitCoreAndCdPz(code: String): Pair<String, Int>? {
        val m = Regex("""^PZ(\d{4})-(\d{6})(\d)$""").matchEntire(code) ?: return null
        val core = m.groupValues[1] + m.groupValues[2]
        val cd   = m.groupValues[3].toInt()
        return core to cd
    }
}