package com.example.hdm.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ExternalBarcodeResponse(
    val results: Map<String, BarcodeResult>,
    val success: Boolean
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class BarcodeResult(
    @SerialName("raw_response")
    val rawResponse: String? = null,
    @SerialName("status_code")
    val statusCode: Int
)