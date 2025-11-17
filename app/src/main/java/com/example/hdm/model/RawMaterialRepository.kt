package com.example.hdm.model

import java.util.Locale

// Definicja struktury przechowującej parę symbol + nazwa
private data class RawMaterial(val symbol: String, val name: String)

object RawMaterialRepository {

    // Nowa, rozbudowana i POPRAWIONA lista surowców zawierająca symbole i nazwy
    private val rawMaterials = listOf(
        RawMaterial("I-22", "921WE40D24"),
        RawMaterial("Acrylic Polimer DPF", "Acrylic Polimer"),
        RawMaterial("A-56", "Alteo ACBJ4N"),
        RawMaterial("FB-5", "Alumina Fiber FMX100NS-M"),
        RawMaterial("FB-3", "Alumina Fiber FMX50NS"),
        RawMaterial("A-81", "Aluminium Hydroxide OL-111/LE"),
        RawMaterial("A-76", "Aluminium Oxide AN31A"),
        RawMaterial("SA-4", "BEN-GEL HVP"),
        RawMaterial("DS-11", "BWD 360 (SiC)"),
        RawMaterial("DS-12", "BWD 500 (SiC)"),
        RawMaterial("DS-14", "BWD 6000"),
        RawMaterial("DS-28", "BWD 6000N"),
        RawMaterial("DS-23", "BWD 800 (SiC)"),
        RawMaterial("DS-30", "BWD 8000"),
        RawMaterial("O-68", "CATALOID SI-40"),
        RawMaterial("TS-70P", "CD-DPF TS-70P"),
        RawMaterial("TS-70P PO ODWAŻENIU", "CD-DPF TS-70P PO ODWAŻENIU"),
        RawMaterial("TS-72P", "CD-DPF TS-72P"),
        RawMaterial("TS-72P PO ODWAŻENIU", "CD-DPF TS-72P PO ODWAŻENIU"),
        RawMaterial("TS-72T", "CD-DPF TS-72T"),
        RawMaterial("TS-77P", "CD-DPF TS-77P"),
        RawMaterial("TS-79P", "CD-DPF TS-79P"),
        RawMaterial("J-14", "CR-50-2 TITANIUM OXIDE"),
        RawMaterial("CE-48", "CULMINAL MHPC 814"),
        RawMaterial("DS-25", "CUMIKLEAN Black JIS 500"),
        RawMaterial("DS-26", "CUMIKLEAN Black JIS 800"),
        RawMaterial("DS-4", "Diasic GC-1000F (SiC)"),
        RawMaterial("DS-15", "DPF -C Dark"),
        RawMaterial("DS-19", "DPF -C Green"),
        RawMaterial("DS-16", "DPF -F Dark"),
        RawMaterial("DS-27", "E-DPF-SIC D F500"),
        RawMaterial("I-21", "EMC-120WTR"),
        RawMaterial("OTHER001", "EMPTY BB FOR MS-1"),
        RawMaterial("ERRTS-70PPF - SILOS", "ERRTS-70PPF - SILOS"),
        RawMaterial("ERRTS-72PPF - SILOS", "ERRTS-72PPF - SILOS"),
        RawMaterial("ERRTS-72PPF HAC - SILOS", "ERRTS-72PPF HAC - SILOS"),
        RawMaterial("ERRTS-77PPF - SILOS", "ERRTS-77PPF - SILOS"),
        RawMaterial("ERRTS-79PPF - SILOS", "ERRTS-79PPF - SILOS"),
        RawMaterial("LG-1", "GLYCERINE"),
        RawMaterial("LG-2", "GLYCERINE"),
        RawMaterial("ID-3", "GMP"),
        RawMaterial("ID-5", "GMP-FG"),
        RawMaterial("ID-4", "GMP-FG(J)"),
        RawMaterial("GM-SE (T)", "GM-SE (T)"),
        RawMaterial("TS-71P", "GPF TS-71P"),
        RawMaterial("TS-73P", "GPF TS-73P"),
        RawMaterial("TS-75P", "GPF TS-75P"),
        RawMaterial("TS-76P", "GPF TS-76P"),
        RawMaterial("TS-76P-po odważeniu", "GPF TS-76P-po odważeniu"),
        RawMaterial("TS-80P", "GPF TS-80P"),
        RawMaterial("TS-82P", "GPF TS-82P"),
        RawMaterial("I-15", "I-15"),
        RawMaterial("I-16", "I-16"),
        RawMaterial("I-24", "I-24"),
        RawMaterial("I-28", "I-28"),
        RawMaterial("O-71", "IMISIL A15 (SILICA)"),
        RawMaterial("MW-16", "KAO POIZ 530"),
        RawMaterial("CE-47", "KELCO-VIS DG"),
        RawMaterial("KSC-2", "KSC-2"),
        RawMaterial("KSC-2D", "KSC-2D"),
        RawMaterial("KSC-2F", "KSC-2F"),
        RawMaterial("KSC-3", "KSC-3"),
        RawMaterial("KSC-4", "KSC-4"),
        RawMaterial("KSC-5", "KSC-5"),
        RawMaterial("KSC-6", "KSC-6"),
        RawMaterial("KSC-7D", "KSC-7D"),
        RawMaterial("KSC-H", "KSC-H"),
        RawMaterial("KSC-M", "KSC-M"),
        RawMaterial("SA-3", "Kunipia-F"),
        RawMaterial("TS-22P", "LSH TS-22P"),
        RawMaterial("TS-22P-po odważeniu", "LSH TS-22P-po odważeniu"),
        RawMaterial("TS-31P", "LSH TS-31P"),
        RawMaterial("TS-31P-po odważeniu", "LSH TS-31P-po odważeniu//Blokada z dnia 2025.02.12"),
        RawMaterial("RTS-31PPO - BLOKOWANIE", "Material RTS-31PPO - BLOKOWANIE"),
        RawMaterial("I-14", "MATSUMOTO MICROSPHERE F-88SE - RESIN BALLON"),
        RawMaterial("MDTS-5A", "MDTS-5A"),
        RawMaterial("MDTS-5B", "MDTS-5B"),
        RawMaterial("MS-3", "Metalic Silicon 55M"),
        RawMaterial("MS-1", "Metalic Silicon#600"),
        RawMaterial("CE-43", "Methocel 254"),
        RawMaterial("CE-41", "Metholose"),
        RawMaterial("CE-35", "Methyl Cellose SUB-04X LSH"),
        RawMaterial("CE-24", "Metolose CE-24"),
        RawMaterial("CE-9", "Metolose CE-9"),
        RawMaterial("G-20", "MODIFIED STARCH"),
        RawMaterial("MW-19", "MW-19"),
        RawMaterial("MW-20", "MW-20 - KY-30"),
        RawMaterial("MW-25", "NAA-34 OLEIC ACID"),
        RawMaterial("DS-6", "NISSORUNDUM GMF-7S"),
        RawMaterial("MS-2", "NS-20 (Silicon Metal)"),
        RawMaterial("OTHER_NGK_RAW_MATERI", "OTHER_NGK_RAW_MATERIAL"),
        RawMaterial("OTHERS_NGK", "Others"),
        RawMaterial("SA-9", "Pangel AD"),
        RawMaterial("Pine White R.", "Pine White R."),
        RawMaterial("SA-8", "Refanite YK-MI"),
        RawMaterial("RSC-100", "RSC-100"),
        RawMaterial("RSC-3S", "RSC-3S"),
        RawMaterial("RSC-4S", "RSC-4S"),
        RawMaterial("RSC-9H", "RSC-9H"),
        RawMaterial("RSC-9HX", "RSC-9HX"),
        RawMaterial("RTS-22P-RECYCLED", "RTS-22P - RECYCLED"),
        RawMaterial("RTS-31P - BLOKOWANIE", "RTS-31P - BLOKOWANIE (problem z wysokim ciśnieniem na extruderze, podejrzany materiał bazowy)"),
        RawMaterial("RTS-31PPO - BLOKOWANIE PROBLEM Z WYSOKIM CISNIENIEM", "RTS-31PPO - BLOKOWANIE (problem z wysokim ciśnieniem na extruderze, podejrzany materiał bazowy)"),
        RawMaterial("RTS-70P - RECYCLED", "RTS-70P - RECYCLED"),
        RawMaterial("RTS-71P - RECYCLED", "RTS-71P - RECYCLED"),
        RawMaterial("RTS-72P – RECYCLED", "RTS-72P – RECYCLED"),
        RawMaterial("RTS-72P HAC – RECYCLED", "RTS-72P HAC - RECYCLED"),
        RawMaterial("RTS-73P - RECYCLED", "RTS-73P - RECYCLED"),
        RawMaterial("RTS-73P T1594", "RTS-73P T1594"),
        RawMaterial("RTS-73P T1603", "RTS-73P T1603"),
        RawMaterial("RTS-75P - RECYCLED", "RTS-75P - RECYCLED"),
        RawMaterial("RTS-76P", "RTS-76P - RECYCLED"),
        RawMaterial("RTS-77P - RECYCLED", "RTS-77P - RECYCLED"),
        RawMaterial("RTS-80P -RECYCLED", "RTS-80P - RECYCLED"),
        RawMaterial("RTS-80P rev.8", "RTS-80P rev.8- RECYCLED"),
        RawMaterial("RTS-82P - RECYCLED", "RTS-82P - RECYCLED"),
        RawMaterial("DS-29", "Silicon Carbide BC-500S"),
        RawMaterial("DS-24", "Silicon carbide D800"),
        RawMaterial("CE-44", "SODIUM CARBOXYMETHYL CELLUOSE 1120"),
        RawMaterial("ID-6", "Starch GM-SE"),
        RawMaterial("ID-2", "Starch Tapioca GM-CC"),
        RawMaterial("SA-2", "Strontium_carbonate"),
        RawMaterial("PROBKI/SAMPLE", "Test samples and Analysis packed inside (MC DPF)"),
        RawMaterial("TP-01P2", "TP-01P2"),
        RawMaterial("TP-02P2", "TP-02P2"),
        RawMaterial("TP-02P3", "TP-02P3"),
        RawMaterial("TP-02P4", "TP-02P4"),
        RawMaterial("TP-02P5", "TP-02P5"),
        RawMaterial("MW-24", "Trehalose"),
        RawMaterial("TS-31P - RECYCLED", "TS-31P - RECYCLED"),
        RawMaterial("TS-72X PO ODWAŻENIU", "TS-72X PO ODWAŻENIU"),
        RawMaterial("TS-77P PO ODWAŻENIU", "TS-77P PO ODWAŻENIU")
    )

    /**
     * Wyszukuje surowce pasujące do podanego zapytania, przeszukując symbole i nazwy.
     * Zapytanie może zawierać myślniki lub być bez nich. Zawsze zwraca SYMBOL.
     *
     * @param query Zapytanie użytkownika (minimum 2 znaki)
     * @return Lista pasujących SYMBOLI surowców
     */
    fun searchRawMaterials(query: String): List<String> {
        if (query.length < 2) return emptyList()

        val normalizedQuery = query.normalizeForSearch()

        return rawMaterials
            .mapNotNull { material ->
                val symbolMatches = material.symbol.normalizeForSearch().contains(normalizedQuery)
                val nameMatches = material.name.normalizeForSearch().contains(normalizedQuery)

                if (symbolMatches || nameMatches) {
                    // Oblicz "wynik" dopasowania, aby posortować wyniki
                    val score = when {
                        material.symbol.normalizeForSearch() == normalizedQuery -> 0 // Dokładne dopasowanie symbolu
                        material.symbol.normalizeForSearch().startsWith(normalizedQuery) -> 1 // Symbol zaczyna się od zapytania
                        material.name.normalizeForSearch() == normalizedQuery -> 2 // Dokładne dopasowanie nazwy
                        material.name.normalizeForSearch().startsWith(normalizedQuery) -> 3 // Nazwa zaczyna się od zapytania
                        symbolMatches -> 4 // Symbol zawiera zapytanie
                        else -> 5 // Nazwa zawiera zapytanie
                    }
                    Pair(score, material.symbol)
                } else {
                    null
                }
            }
            .sortedBy { it.first } // Sortuj po wyniku
            .map { it.second } // Zwróć tylko symbole
            .distinct() // Usuń duplikaty, jeśli wystąpią
    }

    /**
     * Sprawdza czy podany tekst jest dokładnie jednym z SYMBOLI surowców
     * (z tolerancją na myślniki i wielkość liter).
     */
    fun isValidRawMaterial(input: String): Boolean {
        val normalizedInput = input.normalizeForSearch()
        if (normalizedInput.isBlank()) return false

        return rawMaterials.any { material ->
            material.symbol.normalizeForSearch() == normalizedInput
        }
    }

    /**
     * Zwraca pełny SYMBOL surowca na podstawie dopasowania do symbolu lub nazwy.
     * (przydatne do auto-uzupełniania)
     */
    fun getFullMaterialName(input: String): String? {
        val normalizedInput = input.normalizeForSearch()
        if (normalizedInput.isBlank()) return null

        // Najpierw szukaj dopasowania w symbolach, potem w nazwach
        return rawMaterials.find { material ->
            material.symbol.normalizeForSearch() == normalizedInput
        }?.symbol ?: rawMaterials.find { material ->
            material.name.normalizeForSearch() == normalizedInput
        }?.symbol
    }

    /**
     * Prywatna funkcja pomocnicza do normalizacji tekstu na potrzeby wyszukiwania.
     */
    private fun String.normalizeForSearch(): String {
        return this.replace("-", "")
            .replace(" ", "")
            .uppercase(Locale.getDefault())
            .trim()
    }
}