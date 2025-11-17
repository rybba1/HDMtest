// Ścieżka pliku: com/example/hdm/services/PhotoDocXmlGenerator.kt

package com.example.hdm.services

import android.content.Context
import android.util.Xml
import com.example.hdm.model.AttachedImage
import com.example.hdm.ui.photodoc.PhotoDocHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoDocXmlGenerator {

    suspend fun generateXmlToStream(
        header: PhotoDocHeader,
        images: List<AttachedImage>,
        context: Context,
        outputStream: OutputStream
    ) {
        withContext(Dispatchers.IO) {
            val serializer = Xml.newSerializer()

            serializer.setOutput(outputStream, "UTF-8")
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            fun writeTag(name: String, value: String?) {
                serializer.startTag("", name)
                serializer.text(value ?: "")
                serializer.endTag("", name)
            }

            // --- Struktura XML ---
            serializer.startDocument("UTF-8", true)
            serializer.startTag("", "AttachmentSession")

            // Nagłówek
            serializer.startTag("", "ReportHeader")
            writeTag("Title", header.tytul)
            writeTag("ref_no", header.numerFeniks)
            writeTag("place", header.miejsce)
            writeTag("Dock", header.lokalizacja)

            // W XML zapisujemy pełne imię i nazwisko (bez konwersji na ID)
            writeTag("pic_wh", header.magazynier)

            writeTag("details", header.opisOgolny)

            val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val reportDateTime = iso8601Format.format(Date())
            writeTag("report_datetime", reportDateTime)

            serializer.endTag("", "ReportHeader")

            // Zdjęcia - przetwarzane jedno po drugim
            serializer.startTag("", "Photos")
            images.forEachIndexed { index, image ->
                val photoIndex = index + 1

                // Konwertujemy zdjęcie na Base64 i od razu zapisujemy do strumienia
                ImageUtils.convertUriToBase64(context, image.uri)?.let { base64 ->
                    writeTag("Photo_${photoIndex}_Base64", base64)
                }

                writeTag("Photo_${photoIndex}_Barcode", image.scannedBarcode)
                writeTag("Photo_${photoIndex}_Label", image.note)
                writeTag("Photo_${photoIndex}_Air", if (image.isAirFreight) "True" else "False")
            }
            serializer.endTag("", "Photos")

            serializer.endTag("", "AttachmentSession")
            serializer.endDocument()

            serializer.flush()
        }
    }
}