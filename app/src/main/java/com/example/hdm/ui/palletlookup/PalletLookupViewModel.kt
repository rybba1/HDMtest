package com.example.hdm.ui.palletlookup

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hdm.model.DisplayableImage
import com.example.hdm.model.PalletInfoResponse
import com.example.hdm.services.PalletApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class LookupStatus { IDLE, LOADING, SUCCESS, ERROR }

data class PalletLookupUiState(
    val status: LookupStatus = LookupStatus.IDLE,
    val scannedBarcode: String? = null,
    val palletInfo: PalletInfoResponse? = null,
    val errorMessage: String? = null,
    val labelImages: List<DisplayableImage> = emptyList(),
    val damageImages: List<DisplayableImage> = emptyList(),
    val overviewImages: List<DisplayableImage> = emptyList()
)

@HiltViewModel
class PalletLookupViewModel @Inject constructor(
    private val palletApiService: PalletApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PalletLookupUiState())
    val uiState = _uiState.asStateFlow()

    fun onBarcodeScanned(barcode: String) {
        if (barcode.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                PalletLookupUiState(status = LookupStatus.LOADING, scannedBarcode = barcode)
            }

            val result = palletApiService.getPalletInfo(barcode)

            result.onSuccess { palletData ->
                val (labels, damages, overviews) = processAndCategorizeImages(palletData)
                _uiState.update {
                    it.copy(
                        status = LookupStatus.SUCCESS,
                        palletInfo = palletData,
                        labelImages = labels,
                        damageImages = damages,
                        overviewImages = overviews
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        status = LookupStatus.ERROR,
                        errorMessage = exception.message ?: "Nieznany błąd"
                    )
                }
            }
        }
    }

    private suspend fun processAndCategorizeImages(
        palletData: PalletInfoResponse
    ): Triple<List<DisplayableImage>, List<DisplayableImage>, List<DisplayableImage>> = withContext(Dispatchers.Default) {

        val imageMap = palletData.attachedImages.associateBy { it.filename }

        val labels = palletData.photoLabel.mapNotNull { filename ->
            imageMap[filename]?.data?.let { base64 ->
                val thumbnail = createThumbnailFromBase64(base64, 240)
                DisplayableImage(thumbnail, base64, filename)
            }
        }

        val damages = palletData.photosDamage.mapNotNull { filename ->
            imageMap[filename]?.data?.let { base64 ->
                val thumbnail = createThumbnailFromBase64(base64, 240)
                DisplayableImage(thumbnail, base64, filename)
            }
        }

        val overviews = palletData.photosOverview.mapNotNull { filename ->
            imageMap[filename]?.data?.let { base64 ->
                val thumbnail = createThumbnailFromBase64(base64, 240)
                DisplayableImage(thumbnail, base64, filename)
            }
        }

        return@withContext Triple(labels, damages, overviews)
    }

    private fun createThumbnailFromBase64(base64String: String, targetSize: Int): Bitmap? {
        return try {
            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

            options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize)

            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}