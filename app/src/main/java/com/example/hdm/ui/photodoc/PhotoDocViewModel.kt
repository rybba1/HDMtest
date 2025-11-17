package com.example.hdm.ui.photodoc

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.hdm.model.AttachedImage
import com.example.hdm.model.PhotoDocTemplate
import com.example.hdm.model.TemplateRepository
import com.example.hdm.model.UserManager
import com.example.hdm.services.HdmLogger
import com.example.hdm.services.LogLevel
import com.example.hdm.services.PhotoDocXmlGenerator
// ===== ZMIANA IMPORTU =====
import com.example.hdm.services.PhotoDocUploadWorker
// ==========================
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@Parcelize
data class PhotoDocHeader(
    var magazynier: String = "",
    var miejsce: String = "",
    var lokalizacja: String = "",
    var tytul: String = "",
    var numerFeniks: String = "",
    var opisOgolny: String = ""
) : Parcelable

sealed class PhotoDocSubmissionState {
    object Idle : PhotoDocSubmissionState()
    data class Generating(val step: PhotoDocProcessingStep, val progress: Float) : PhotoDocSubmissionState()
    object SaveToWaitingRoomSuccess : PhotoDocSubmissionState()
    data class Error(val message: String) : PhotoDocSubmissionState()
}

enum class PhotoDocProcessingStep {
    PREPARING_DATA,
    COMPRESSING_IMAGES,
    GENERATING_XML,
    PREPARING_UPLOAD
}

@HiltViewModel
class PhotoDocViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val xmlGenerator: PhotoDocXmlGenerator,
    private val hdmLogger: HdmLogger
) : ViewModel() {

    val header: StateFlow<PhotoDocHeader> = savedStateHandle.getStateFlow("photoDocHeader", PhotoDocHeader())
    val images: StateFlow<List<AttachedImage>> = savedStateHandle.getStateFlow("photoDocImages", emptyList())
    val templates: StateFlow<List<PhotoDocTemplate>> = MutableStateFlow(TemplateRepository.getPhotoDocTemplates()).asStateFlow()
    val nameSuggestions: StateFlow<List<String>> = MutableStateFlow<List<String>>(emptyList()).asStateFlow()

    private val _submissionState = MutableStateFlow<PhotoDocSubmissionState>(PhotoDocSubmissionState.Idle)
    val submissionState: StateFlow<PhotoDocSubmissionState> = _submissionState.asStateFlow()

    private val _logoutAndNavigateToLogin = Channel<Unit>(Channel.CONFLATED)
    val logoutAndNavigateToLogin = _logoutAndNavigateToLogin.receiveAsFlow()

    fun generateAndSendXml(context: Context) {
        if (_submissionState.value !is PhotoDocSubmissionState.Idle) return

        val headerSnapshot = header.value
        val logMessage = buildString {
            append("Rozpoczęto wysyłanie dok. zdjęciowej. Tytuł='${headerSnapshot.tytul}', ")
            append("Magazynier='${headerSnapshot.magazynier}', Zdjęć=${images.value.size}")
        }
        hdmLogger.log(context, logMessage)

        viewModelScope.launch(Dispatchers.IO) {
            val reportId = "DOK_ZDJ_${System.currentTimeMillis()}"
            try {
                _submissionState.value = PhotoDocSubmissionState.Generating(PhotoDocProcessingStep.PREPARING_DATA, 0.1f)
                delay(200)

                _submissionState.value = PhotoDocSubmissionState.Generating(PhotoDocProcessingStep.COMPRESSING_IMAGES, 0.35f)
                delay(300)

                _submissionState.value = PhotoDocSubmissionState.Generating(PhotoDocProcessingStep.GENERATING_XML, 0.6f)

                val waitingRoomDir = File(context.filesDir, "waiting_room").apply { mkdirs() }
                val reportFile = File(waitingRoomDir, "$reportId.xml")

                reportFile.outputStream().use { stream ->
                    xmlGenerator.generateXmlToStream(header.value, images.value, context, stream)
                }

                _submissionState.value = PhotoDocSubmissionState.Generating(PhotoDocProcessingStep.PREPARING_UPLOAD, 0.9f)

                // ===== ZMIANA: Używamy nowego PhotoDocUploadWorker =====
                schedulePhotoDocUpload(context, reportId)
                // ====================================================

                _submissionState.value = PhotoDocSubmissionState.Generating(PhotoDocProcessingStep.PREPARING_UPLOAD, 1.0f)
                delay(300)

                _submissionState.value = PhotoDocSubmissionState.SaveToWaitingRoomSuccess
                hdmLogger.log(context, "Dok. zdjęciowa zapisana w poczekalni jako '$reportId'. Wysyłka w tle.")

                delay(2000)
                resetState()

                UserManager.logoutUser()
                _logoutAndNavigateToLogin.send(Unit)

            } catch (e: Exception) {
                val reportTitleForLog = header.value.tytul.ifBlank { "bez tytułu" }
                Log.e("PhotoDocViewModel", "Krytyczny błąd podczas generowania dok. zdjęciowej", e)
                hdmLogger.log(context, "Krytyczny błąd generowania XML dla dok. zdjęciowej '$reportTitleForLog'. Powód: ${e.message}", level = LogLevel.ERROR)
                _submissionState.value = PhotoDocSubmissionState.Error("Krytyczny błąd: ${e.message}")
            }
        }
    }

    // ===== ZMIANA: Używamy nowego PhotoDocUploadWorker =====
    private fun schedulePhotoDocUpload(context: Context, reportId: String) {
        val workManager = WorkManager.getInstance(context)
        val uploadWorkRequest = OneTimeWorkRequestBuilder<PhotoDocUploadWorker>()
            .setInputData(workDataOf(PhotoDocUploadWorker.KEY_REPORT_ID to reportId))
            .build()

        workManager.enqueueUniqueWork(
            "upload_$reportId",
            ExistingWorkPolicy.REPLACE,
            uploadWorkRequest
        )
    }
    // ====================================================

    fun resetSubmissionState() {
        _submissionState.value = PhotoDocSubmissionState.Idle
    }

    fun resetState() {
        savedStateHandle["photoDocHeader"] = PhotoDocHeader()
        savedStateHandle["photoDocImages"] = emptyList<AttachedImage>()
        _submissionState.value = PhotoDocSubmissionState.Idle
    }

    // ... (reszta pliku PhotoDocViewModel.kt bez zmian) ...
    fun applyTemplate(template: PhotoDocTemplate, context: Context) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val titleWithDate = "${template.title} - $currentDate"

        savedStateHandle["photoDocHeader"] = header.value.copy(
            tytul = titleWithDate,
            miejsce = template.place,
            lokalizacja = template.location,
            opisOgolny = template.description,
            numerFeniks = ""
        )
        clearNameSuggestions()
        hdmLogger.log(context, "Dok. zdjęciowa: Zastosowano szablon '${template.templateName}'")
    }
    fun clearHeader(context: Context) {
        val currentMagazynier = header.value.magazynier
        savedStateHandle["photoDocHeader"] = PhotoDocHeader(magazynier = currentMagazynier)
        hdmLogger.log(context, "Dok. zdjęciowa: Wyczyszczono formularz.")
    }
    fun updateTytul(title: String) {
        savedStateHandle["photoDocHeader"] = header.value.copy(tytul = title)
    }
    fun updateNumerFeniks(number: String) {
        savedStateHandle["photoDocHeader"] = header.value.copy(numerFeniks = number)
    }
    fun updateOpisOgolny(description: String) {
        savedStateHandle["photoDocHeader"] = header.value.copy(opisOgolny = description)
    }
    fun updateMiejsce(place: String, context: Context) {
        savedStateHandle["photoDocHeader"] = header.value.copy(miejsce = place, lokalizacja = "")
        hdmLogger.log(context, "Dok. zdjęciowa: Wybrano magazyn '${place}'")
    }
    fun updateLokalizacja(location: String, context: Context) {
        savedStateHandle["photoDocHeader"] = header.value.copy(lokalizacja = location)
        hdmLogger.log(context, "Dok. zdjęciowa: Wybrano lokalizację '${location}'")
    }
    fun clearNameSuggestions() {
        // Funkcja celowo pusta, brak sugestii
    }
    fun updateMagazynier(name: String, context: Context) {
        savedStateHandle["photoDocHeader"] = header.value.copy(magazynier = name)
    }
    fun addImage(uri: String, context: Context, from: String, logEvent: Boolean = true) {
        val currentImages = images.value
        if (currentImages.none { it.uri == uri }) {
            val newImage = AttachedImage(uri = uri)
            savedStateHandle["photoDocImages"] = currentImages + newImage
            if (logEvent) {
                hdmLogger.log(context, "Dok. zdjęciowa: Dodano zdjęcie z '$from'.")
            }
        }
    }
    fun addImagesFromGallery(context: Context, galleryUris: List<Uri>) {
        if (galleryUris.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val imageDirectory = File(context.filesDir, "photodoc_images").apply { mkdirs() }
            val newImages = mutableListOf<AttachedImage>()

            galleryUris.forEach { uri ->
                try {
                    val destinationFile = File(imageDirectory, "hdm_photo_${System.currentTimeMillis()}_${galleryUris.indexOf(uri)}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        FileOutputStream(destinationFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    val localUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", destinationFile)
                    newImages.add(AttachedImage(uri = localUri.toString()))
                } catch (e: Exception) {
                    Log.e("PhotoDocViewModel", "Błąd podczas kopiowania zdjęcia z galerii: $uri", e)
                }
            }

            withContext(Dispatchers.Main) {
                savedStateHandle["photoDocImages"] = images.value + newImages
                if (newImages.isNotEmpty()) {
                    hdmLogger.log(context, "Dok. zdjęciowa: Dodano ${newImages.size} zdjęć z galerii.")
                }
                if (newImages.size != galleryUris.size) {
                    Toast.makeText(context, "Nie udało się załadować wszystkich zdjęć.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    fun removeImage(image: AttachedImage) {
        savedStateHandle["photoDocImages"] = images.value - image
    }
    fun updateImageDetails(uri: String, barcode: String, note: String, isAirFreight: Boolean, context: Context) {
        var logMessage: String? = null
        val currentImages = images.value
        val updatedImages = currentImages.map { image ->
            if (image.uri == uri) {
                if (note.isNotBlank() && note != image.note) {
                    logMessage = "Dok. zdjęciowa: Dodano notatkę do zdjęcia: \"${note.take(50)}...\""
                }
                if (barcode.isNotBlank() && barcode != image.scannedBarcode) {
                    val barcodeLog = "Dok. zdjęciowa: Zapisano numer nośnika: '$barcode'"
                    logMessage = if (logMessage != null) "$logMessage | $barcodeLog" else barcodeLog
                }
                image.copy(scannedBarcode = barcode, note = note, isAirFreight = isAirFreight)
            } else {
                image
            }
        }
        savedStateHandle["photoDocImages"] = updatedImages
        logMessage?.let { hdmLogger.log(context, it) }
        hdmLogger.log(context, "Dok. zdjęciowa: Zapisano zmiany dla zdjęcia.")
    }
    fun getImageByUri(uri: String): AttachedImage? = images.value.find { it.uri == uri }
}