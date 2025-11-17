

package com.example.hdm.model

import android.graphics.Bitmap

data class DisplayableImage(
    val thumbnail: Bitmap?,
    val fullSizeBase64: String,
    val filename: String?
)