package com.example.hdm.model

data class PhotoDocTemplate(
    val templateName: String, // Nazwa widoczna dla użytkownika w menu
    val title: String,        // Tytuł dokumentacji
    val place: String,        // Miejsce
    val location: String,     // Lokalizacja
    val description: String   // Opis ogólny
)

object TemplateRepository {

    private val templates = listOf(
        PhotoDocTemplate(
            templateName = "Zdjęcia do mieszanek LSH",
            title = "Zdjęcia-Mieszanki LSH",
            place = "Gaudi",
            location = "D202",
            description = "Zdjęcia kontenerów przed i po rozładunku surowca LSH."
        )
        // W przyszłości możesz tu dodać kolejne szablony, np.:
        // PhotoDocTemplate(templateName = "Inny szablon", ...),
    )

    fun getPhotoDocTemplates(): List<PhotoDocTemplate> {
        return templates
    }
}