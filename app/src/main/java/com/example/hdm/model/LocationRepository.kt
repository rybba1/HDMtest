package com.example.hdm.model

object LocationRepository {

    val warehouses: List<String> = listOf("Leon", "Gaudi")

    // Prywatne, w pełni rozwinięte listy doków dla każdego magazynu
    private val leonDocks: List<String> = listOf(
        "D101", "D102", "D103", "D104", "D105",
        "D106", "D107", "D108", "D109", "D110","D111"
    )
    private val gaudiDocks: List<String> = listOf(
        "D201", "D202", "D203", "D204", "D205", "D206", "D207",
        "D208", "D209", "D210", "D211", "D212", "D213", "D214"
    )

    /**
     * Zwraca listę doków dla podanego magazynu.
     */
    fun getDocksFor(warehouse: String): List<String> {
        return when (warehouse) {
            "Leon" -> leonDocks
            "Gaudi" -> gaudiDocks
            else -> emptyList()
        }
    }
}