package com.example.hdm

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.hdm.services.LogSyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject
// ===== USUNIĘTY IMPORT =====
// import com.example.hdm.services.DirectSessionSyncWorker
// =========================

@HiltAndroidApp
class HdmApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        Log.d("HdmApplication", "Aplikacja startuje. Konfiguruję zadania w tle.")

        schedulePeriodicLogSync()
        // ===== USUNIĘTE WYWOŁANIE =====
        // schedulePeriodicSessionSync()
        // ============================
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.VERBOSE)
            .build()

    private fun schedulePeriodicLogSync() {
        val logSyncRequest = PeriodicWorkRequestBuilder<LogSyncWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "LogSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            logSyncRequest
        )
        Log.d("HdmApplication", "Zlecono cykliczne zadanie synchronizacji logów co 15 minut.")
    }


}