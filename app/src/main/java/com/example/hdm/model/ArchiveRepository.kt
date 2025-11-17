// Ścieżka: com/example/hdm/model/ArchiveRepository.kt

package com.example.hdm.model

import android.content.Context
import android.util.Log
import com.example.hdm.services.ImageUtils
import com.google.gson.Gson
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ArchiveRepository {
    private const val ARCHIVE_DIR = "archived_sessions"
    private const val MAX_ARCHIVES = 3
    private const val TAG = "ArchiveRepository"
    private val gson = Gson()

    private fun loadAllArchives(context: Context): List<ArchivedSession> {
        return try {
            val dir = File(context.filesDir, ARCHIVE_DIR)
            if (!dir.exists()) return emptyList()

            dir.listFiles()
                ?.filter { it.extension == "json" }
                ?.mapNotNull { file ->
                    try {
                        gson.fromJson(file.readText(), ArchivedSession::class.java)
                    } catch (e: Exception) {
                        Log.w(TAG, "Błąd wczytywania archiwum: ${file.name}", e)
                        null
                    }
                }
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Błąd pobierania wszystkich archiwów", e)
            emptyList()
        }
    }

    suspend fun archiveSession(context: Context, archived: ArchivedSession) = withContext(Dispatchers.IO) {
        try {
            val existing = loadAllArchives(context)
            if (existing.size >= MAX_ARCHIVES) {
                existing.sortedBy { it.completionTimestamp }
                    .take(existing.size - MAX_ARCHIVES + 1)
                    .forEach { deleteArchive(context, it.archiveId) }
                Log.d(TAG, "Usunięto stare archiwa, ponieważ przekroczono limit $MAX_ARCHIVES.")
            }

            val dir = File(context.filesDir, ARCHIVE_DIR)
            dir.mkdirs()
            val file = File(dir, "${archived.archiveId}.json")

            FileOutputStream(file).use { fileStream ->
                val writer = JsonWriter(fileStream.writer())
                writer.setIndent("  ")

                writer.beginObject() // {

                writer.name("archiveId").value(archived.archiveId)
                writer.name("originalSessionId").value(archived.originalSessionId)
                writer.name("reportType").value(archived.reportType)
                writer.name("completionTimestamp").value(archived.completionTimestamp)
                writer.name("palletCount").value(archived.palletCount)

                writer.name("reportHeader").jsonValue(gson.toJson(archived.reportHeader))
                writer.name("palletPositions").jsonValue(gson.toJson(archived.palletPositions))
                writer.name("damageMarkers").jsonValue(gson.toJson(archived.damageMarkers))
                writer.name("damageHeightSelections").jsonValue(gson.toJson(archived.damageHeightSelections))

                writer.name("savedPallets").beginArray() // [
                archived.savedPallets.forEach { pallet ->
                    writer.beginObject() // {

                    // === POCZĄTEK POPRAWKI: Uzupełnienie wszystkich pól palety ===
                    writer.name("id").value(pallet.id)
                    writer.name("numerPalety").value(pallet.numerPalety)
                    writer.name("brakNumeruPalety").value(pallet.brakNumeruPalety)
                    writer.name("rodzajTowaru").value(pallet.rodzajTowaru)
                    writer.name("numerLotu").value(pallet.numerLotu)
                    writer.name("brakNumeruLotu").value(pallet.brakNumeruLotu)
                    writer.name("numerNosnika").value(pallet.numerNosnika)
                    // Uri nie zapisujemy, bo są tymczasowe

                    // Konwertuj i zapisz każde zdjęcie JEDNO PO DRUGIM
                    pallet.zdjecieDuzejEtykietyUri?.let { uri ->
                        val base64 = ImageUtils.convertUriToBase64(context, uri)
                        writer.name("zdjecieDuzejEtykietyBase64").value(base64)
                    }
                    pallet.zdjecieCalejPaletyUri?.let { uri ->
                        val base64 = ImageUtils.convertUriToBase64(context, uri)
                        writer.name("zdjecieCalejPaletyBase64").value(base64)
                    }
                    pallet.damageMarkingBitmapUri?.let { uri ->
                        val base64 = ImageUtils.convertUriToBase64(context, uri)
                        writer.name("damageMarkingBitmapBase64").value(base64)
                    }

                    writer.name("damageInstances").beginArray()
                    pallet.damageInstances.forEach { instance ->
                        writer.beginObject()
                        writer.name("id").value(instance.id)
                        writer.name("photoUri").value(instance.photoUri) // Zapisujemy URI na wszelki wypadek

                        val base64 = ImageUtils.convertUriToBase64(context, instance.photoUri)
                        writer.name("photoBase64").value(base64)

                        writer.name("details").jsonValue(gson.toJson(instance.details))
                        writer.endObject()
                    }
                    writer.endArray()
                    // === KONIEC POPRAWKI ===

                    writer.endObject() // }
                }
                writer.endArray() // ]

                writer.endObject() // }
                writer.close()
            }

            Log.d(TAG, "Strumieniowo zarchiwizowano: ${archived.reportType}")

        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas strumieniowej archiwizacji sesji", e)
        }
    }

    fun getArchivedSessions(context: Context): List<ArchivedSession> {
        return loadAllArchives(context)
            .sortedByDescending { it.completionTimestamp }
            .take(MAX_ARCHIVES)
    }

    fun deleteArchive(context: Context, archiveId: String) {
        try {
            val dir = File(context.filesDir, ARCHIVE_DIR)
            val file = File(dir, "$archiveId.json")
            if (file.delete()) {
                Log.d(TAG, "Usunięto archiwum: $archiveId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Błąd usuwania archiwum", e)
        }
    }
}