package com.example.hdm.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import com.example.hdm.ui.log_monitor.NetworkEvent // NOWY IMPORT
import com.example.hdm.ui.log_monitor.NetworkEventType // NOWY IMPORT
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var previousCaps: NetworkCapabilities? = null

    private val _events = MutableStateFlow<List<NetworkEvent>>(emptyList())
    val events = _events.asStateFlow()

    fun startMonitoring() {
        if (networkCallback != null) return

        _events.value = emptyList()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                val caps = connectivityManager.getNetworkCapabilities(network)
                previousCaps = caps
                val transport = getTransportName(caps)
                addEvent(NetworkEventType.AVAILABLE, "✅ Sieć DOSTĘPNA ($transport)")
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                previousCaps = null
                addEvent(NetworkEventType.LOST, "❌ Sieć UTRACONA")
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                logDetailedChanges(networkCapabilities)
                previousCaps = networkCapabilities
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
    }

    fun stopMonitoring() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
            networkCallback = null
            previousCaps = null
        }
    }

    private fun logDetailedChanges(newCaps: NetworkCapabilities) {
        val changes = mutableListOf<String>()
        val oldCaps = previousCaps

        if (oldCaps != null) {
            val oldValidated = oldCaps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val newValidated = newCaps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            if (oldValidated && !newValidated) changes.add("utracono dostęp do internetu")
            if (!oldValidated && newValidated) changes.add("uzyskano dostęp do internetu")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (newCaps.signalStrength != oldCaps.signalStrength) {
                    changes.add("siła sygnału ${oldCaps.signalStrength} -> ${newCaps.signalStrength}")
                }
            }
        }

        if (changes.isNotEmpty()) {
            addEvent(NetworkEventType.CAPABILITIES_CHANGED, "🔄 Zmiana: ${changes.joinToString(", ")}")
        }
    }

    private fun addEvent(type: NetworkEventType, message: String) {
        val newEvent = NetworkEvent(type = type, message = message)
        val currentLogs = _events.value.toMutableList()
        currentLogs.add(0, newEvent)

        _events.value = if (currentLogs.size > 50) currentLogs.take(50) else currentLogs
    }

    private fun getTransportName(caps: NetworkCapabilities?): String {
        return when {
            caps == null -> "Nieznany"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Komórkowa"
            else -> "Inny"
        }
    }
}