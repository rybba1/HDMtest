package com.example.hdm.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


object ImageUtils {

    private const val TAG = "ImageUtils"
    private const val MAX_IMAGE_WIDTH = 1280
    private const val MAX_IMAGE_HEIGHT = 1280
    private const val TARGET_SIZE_KB = 250
    private const val INITIAL_QUALITY = 85

    suspend fun convertUriToBase64(context: Context, uriString: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)

                val originalOrientation = try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val exif = ExifInterface(inputStream)
                        exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    } ?: ExifInterface.ORIENTATION_NORMAL
                } catch (e: Exception) {
                    Log.w(TAG, "Nie można odczytać EXIF orientation", e)
                    ExifInterface.ORIENTATION_NORMAL
                }

                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }

                options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT)
                options.inJustDecodeBounds = false

                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                } ?: return@withContext null

                val compressedBytes = compressBitmapToTargetSize(bitmap, TARGET_SIZE_KB)
                bitmap.recycle()

                val finalBytes = addExifOrientation(context, compressedBytes, originalOrientation)

                val finalSizeKB = finalBytes.size / 1024
                Log.d(TAG, "Kompresja zakończona: $finalSizeKB KB, EXIF orientation: $originalOrientation")

                Base64.encodeToString(finalBytes, Base64.NO_WRAP)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to convert URI to Base64: $uriString", e)
                null
            }
        }
    }

    private fun addExifOrientation(context: Context, imageBytes: ByteArray, orientation: Int): ByteArray {
        var tempFile: File? = null
        return try {
            tempFile = File(context.cacheDir, "temp_exif_${System.currentTimeMillis()}.jpg")

            FileOutputStream(tempFile).use { fos ->
                fos.write(imageBytes)
            }

            val exif = ExifInterface(tempFile.absolutePath)
            exif.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                orientation.toString()
            )
            exif.saveAttributes()

            val finalBytes = tempFile.readBytes()

            Log.d(TAG, "EXIF orientation $orientation zapisany do skompresowanego obrazu")

            finalBytes
        } catch (e: Exception) {
            Log.w(TAG, "Nie można zapisać EXIF orientation, zwracam oryginalny obraz", e)
            imageBytes
        } finally {
            tempFile?.delete()
        }
    }

    private fun compressBitmapToTargetSize(bitmap: Bitmap, targetSizeKB: Int): ByteArray {
        var quality = INITIAL_QUALITY
        var compressedData: ByteArray

        do {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            compressedData = baos.toByteArray()
            val currentSizeKB = compressedData.size / 1024

            if (currentSizeKB <= targetSizeKB || quality <= 20) {
                break
            }

            quality = (quality * 0.85).toInt().coerceAtLeast(20)

        } while (true)

        return compressedData
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

    // ✅ NOWA FUNKCJA - wewnątrz obiektu ImageUtils
    fun saveBase64ToTempFile(context: Context, base64: String, prefix: String): String {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val tempFile = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        tempFile.writeBytes(bytes)
        return tempFile.absolutePath
    }
}