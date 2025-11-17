package com.example.hdm.services

import android.util.Log
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.Protocol
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy

object LoggingEventListener : EventListener() {
    private const val TAG = "OkHttpEvents"
    private var callStartTime: Long = 0

    private fun logWithDuration(message: String) {
        val duration = System.nanoTime() - callStartTime
        Log.d(TAG, "[${duration / 1_000_000} ms] $message")
    }

    override fun callStart(call: Call) {
        callStartTime = System.nanoTime()
        Log.d(TAG, "callStart: ${call.request().method} ${call.request().url}")
    }

    override fun dnsStart(call: Call, domainName: String) {
        logWithDuration("dnsStart: $domainName")
    }

    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
        logWithDuration("dnsEnd: $domainName -> $inetAddressList")
    }

    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        logWithDuration("connectStart: Próba połączenia z $inetSocketAddress")
    }

    override fun secureConnectStart(call: Call) {
        logWithDuration("secureConnectStart: Rozpoczynanie TLS handshake")
    }

    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
        logWithDuration("secureConnectEnd: TLS handshake zakończony powodzeniem")
    }

    override fun connectEnd(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?) {
        logWithDuration("connectEnd: Połączono z $inetSocketAddress protokołem $protocol")
    }

    override fun connectFailed(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?, ioe: IOException) {
        logWithDuration("!!! connectFailed: Błąd połączenia z $inetSocketAddress")
        Log.e(TAG, "!!! connectFailed exception:", ioe)
    }

    override fun callEnd(call: Call) {
        logWithDuration("callEnd: Połączenie zakończone.")
    }

    override fun callFailed(call: Call, ioe: IOException) {
        logWithDuration("!!! callFailed: Całkowity błąd połączenia.")
        Log.e(TAG, "!!! callFailed exception:", ioe)
    }
}