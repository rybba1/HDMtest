package com.example.hdm.services

import android.content.Context
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.util.Xml
import androidx.core.content.ContextCompat
import com.example.hdm.model.UserManager
import java.io.File
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogXmlGenerator {

    private const val TAG = "LogXmlGenerator"

    fun generateLogXml(logMessage: String, context: Context, isBackgroundJob: Boolean = false): String {
        val serializer = Xml.newSerializer()
        val writer = StringWriter()

        serializer.setOutput(writer)
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

        fun writeTag(name: String, value: String?) {
            serializer.startTag("", name)
            serializer.text(value ?: "")
            serializer.endTag("", name)
        }

        serializer.startDocument("UTF-8", true)
        serializer.startTag("", "HDM_LOG")

        val macAddress = getDeviceIdentifierFromFile(context)
        val user = if (isBackgroundJob) "SYSTEM" else UserManager.currentUser.value

        writeTag("MAC", macAddress)
        writeTag("TIMESTAMP", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        writeTag("LOG", logMessage)
        writeTag("USER", user)

        serializer.endTag("", "HDM_LOG")
        serializer.endDocument()

        return writer.toString()
    }

    // --- POCZĄTEK POPRAWKI ---
    // Zmieniamy widoczność z 'internal fun' na 'fun', aby HdmLogger miał dostęp.
    fun getDeviceIdentifierFromFile(context: Context): String {
        // --- KONIEC POPRAWKI ---
        UserManager.identifierFileUri.value?.let { uri ->
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                        val fileName = cursor.getString(nameIndex)
                        Log.i(TAG, "Odczytano identyfikator z zapamiętanego URI: $fileName")
                        return fileName.substringBeforeLast('.')
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Nie udało się odczytać pliku z zapamiętanego URI. Przechodzę do skanowania.", e)
            }
        }

        val potentialRoots = ContextCompat.getExternalFilesDirs(context, null)
            .mapNotNull { it?.path?.substringBefore("/Android/data") }
            .plus(Environment.getExternalStorageDirectory().path)
            .distinct()

        for (rootPath in potentialRoots) {
            try {
                val targetDir = File(rootPath, "${File.separator}Download${File.separator}telefon")
                if (targetDir.exists() && targetDir.isDirectory) {
                    val files = targetDir.listFiles()
                    if (files != null && files.isNotEmpty()) {
                        val fileName = files[0].name.substringBeforeLast('.')
                        Log.i(TAG, "Znaleziono plik w preferowanej lokalizacji: $fileName")
                        return fileName
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Błąd podczas przeszukiwania ścieżki $rootPath", e)
                continue
            }
        }

        Log.w(TAG, "Nie znaleziono pliku identyfikacyjnego w żadnej ze sprawdzonych lokalizacji.")
        return "BRAK_PLIKU_IDENTYFIKACYJNEGO"
    }
}