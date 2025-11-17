package com.example.hdm.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class AppLifecycleService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i("AppLifecycleService", "Główne zadanie aplikacji zostało usunięte. Zatrzymywanie HdmMonitoringService.")
        // Zatrzymujemy główną usługę monitorującą
        val serviceIntent = Intent(this, HdmMonitoringService::class.java)
        stopService(serviceIntent)
        // Zatrzymujemy samą siebie
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }
}