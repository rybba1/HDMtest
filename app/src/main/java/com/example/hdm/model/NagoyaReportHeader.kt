package com.example.hdm.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NagoyaReportHeader(
    val workerName: String = "",
    val orderNumber: String = "",
    val date: String = "",
    val time: String = ""
)