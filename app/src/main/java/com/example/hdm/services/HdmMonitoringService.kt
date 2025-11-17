package com.example.hdm.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.hdm.MainActivity
import com.example.hdm.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class HdmMonitoringService : Service() {

    @Inject
    lateinit var hdmLogger: HdmLogger

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "HdmMonitoringChannel"
        private const val TAG = "HdmMonitoringService"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    // private var pulseJob: Job? = null // Można usunąć, ale nie jest konieczne

    private lateinit var powerManager: PowerManager
    @Volatile
    private var isShuttingDown = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        Log.i(TAG, "Serwis monitorujący został uruchomiony (PULSE wyłączone).")

        // LOGI PULSE WYŁĄCZONE NA STAŁE
        // pulseJob?.cancel()
        // startPulseLoop()

        return START_NOT_STICKY
    }

    // Możesz usunąć całą metodę startPulseLoop() jeśli chcesz

    override fun onDestroy() {
        Log.i(TAG, "onDestroy() wywołane - rozpoczynam zatrzymywanie...")
        isShuttingDown = true

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas stopForeground", e)
        }

        // pulseJob?.cancel() // Można usunąć
        serviceJob.cancel()

        Log.i(TAG, "Serwis monitorujący został zatrzymany.")
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i(TAG, "onTaskRemoved() - aplikacja zamknięta przez użytkownika")
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HDM System Monitor")
            .setContentText("Aplikacja działa w tle, monitorując zadania.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "HDM Monitoring Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}