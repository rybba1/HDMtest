package com.example.hdm.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class DnsLogRequest(
    @SerialName("device_ip")
    val deviceIp: String,
    @SerialName("device_name")
    val deviceName: String,
    @SerialName("android_id")
    val androidId: String,
    @SerialName("android_version")
    val androidVersion: String,
    @SerialName("app_version")
    val appVersion: String,
    @SerialName("battery_level")
    val batteryLevel: Int,
    @SerialName("user_login")
    val userLogin: String,
    val level: String,
    val message: String
)