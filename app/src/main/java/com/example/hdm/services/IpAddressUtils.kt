// NOWY PLIK: com/example/hdm/services/IpAddressUtils.kt
package com.example.hdm.services

import android.util.Log
import java.net.NetworkInterface

/**
 * Pomocnik do znajdowania adresu IP urządzenia w sieci lokalnej (non-loopback).
 */
object IpAddressUtils {

    fun getDeviceIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            for (intf in interfaces) {
                val addrs = intf.inetAddresses.toList()
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "Brak IP"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("IpAddressUtils", "Nie można pobrać adresu IP", e)
        }
        return "Brak IP"
    }
}