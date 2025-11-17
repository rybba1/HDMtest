package com.example.hdm.services

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object FileService {

    private const val TAG = "FileService"
    private const val WAITING_ROOM_DIR = "waiting_room"
    private const val REPORTS_SUBDIR = "RaportyHDM"

    suspend fun saveTextToDownloads(context: Context, textContent: String, fileName: String): Result<String> {
        return saveFileToDownloads(context, textContent.toByteArray(), fileName, "text/plain")
    }

    suspend fun saveXmlToDownloads(context: Context, xmlBytes: ByteArray, fileName: String): Result<String> {
        return saveFileToDownloads(context, xmlBytes, fileName, "text/xml")
    }

    suspend fun savePdfToDownloads(context: Context, pdfBytes: ByteArray, fileName: String): Result<String> {
        return saveFileToDownloads(context, pdfBytes, fileName, "application/pdf")
    }

    private suspend fun saveFileToDownloads(context: Context, bytes: ByteArray, fileName: String, mimeType: String): Result<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveFileToDownloadsModern(context, bytes, fileName, mimeType)
        } else {
            saveFileToDownloadsLegacy(bytes, fileName)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun saveFileToDownloadsModern(context: Context, bytes: ByteArray, fileName: String, mimeType: String): Result<String> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + REPORTS_SUBDIR)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri == null) {
            return@withContext Result.failure(Exception("Nie udało się utworzyć pliku w MediaStore."))
        }

        try {
            resolver.openOutputStream(uri)?.use { it.write(bytes) }

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            val successMessage = "Pobrane/$REPORTS_SUBDIR/$fileName"
            Result.success(successMessage)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            Result.failure(e)
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun saveFileToDownloadsLegacy(bytes: ByteArray, fileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val reportsDir = File(downloadsDir, REPORTS_SUBDIR)
            if (!reportsDir.exists() && !reportsDir.mkdirs()) {
                throw Exception("Nie można utworzyć folderu $REPORTS_SUBDIR")
            }
            val file = File(reportsDir, fileName)
            FileOutputStream(file).use { it.write(bytes) }
            val successMessage = "Pobrane/$REPORTS_SUBDIR/$fileName"
            Result.success(successMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReportXmlContentById(context: Context, reportId: String): String? = withContext(Dispatchers.IO) {
        try {
            val waitingRoom = File(context.filesDir, WAITING_ROOM_DIR)
            val reportFile = File(waitingRoom, "$reportId.xml")
            if (reportFile.exists()) {
                return@withContext reportFile.readText()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Błąd odczytu raportu '$reportId' z poczekalni", e)
        }
        return@withContext null
    }

    fun listReportsInWaitingRoom(context: Context): List<String> {
        return try {
            val waitingRoom = File(context.filesDir, WAITING_ROOM_DIR)
            if (!waitingRoom.exists()) return emptyList()
            waitingRoom.listFiles { _, name -> name.endsWith(".xml") }
                ?.map { it.nameWithoutExtension }
                ?.sortedDescending()
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas listowania raportów w poczekalni", e)
            emptyList()
        }
    }

    fun deleteReportFromWaitingRoom(context: Context, reportId: String) {
        try {
            val waitingRoom = File(context.filesDir, WAITING_ROOM_DIR)
            val reportFile = File(waitingRoom, "$reportId.xml")
            if (reportFile.exists()) {
                reportFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas usuwania raportu '$reportId' z poczekalni", e)
        }
    }
}