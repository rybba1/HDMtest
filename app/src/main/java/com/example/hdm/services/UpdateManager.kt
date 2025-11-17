// ZASTĄP CAŁY PLIK: com/example/hdm/services/UpdateManager.kt
package com.example.hdm.services

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loginApiService: LoginApiService,
    private val workManager: WorkManager
) {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState = _updateState.asStateFlow()

    private var checkJob: Job? = null
    private var downloadObserveJob: Job? = null

    companion object {
        const val UPDATE_WORK_NAME = "HdmUpdateDownloadWork"
        private const val TAG = "UpdateManager"
    }

    /**
     * Sprawdza dostępność nowej aktualizacji na serwerze.
     */
    fun checkForUpdates() {
        if (checkJob?.isActive == true || _updateState.value is UpdateState.Downloading) {
            return
        }

        checkJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
                val deviceName = Build.MODEL
                val deviceIp = IpAddressUtils.getDeviceIpAddress()

                val request = UpdateCheckRequest(
                    deviceName = deviceName,
                    currentVersion = currentVersion,
                    deviceIp = deviceIp
                )

                Log.d(TAG, "Sprawdzanie aktualizacji: $request")
                val response = loginApiService.checkUpdate(request).getOrThrow()

                if (response.success && response.updateAvailable) {
                    Log.i(TAG, "Dostępna nowa wersja: ${response.latestVersion}")
                    val changelogResponse = loginApiService.getChangelog().getOrDefault(ChangelogResponse(false))
                    val info = UpdateInfo(
                        latestVersion = response.latestVersion,
                        filename = response.updateFilename,
                        changelog = changelogResponse.changelog
                    )
                    _updateState.value = UpdateState.UpdateAvailable(info)
                } else {
                    Log.i(TAG, "Aplikacja jest aktualna (v${currentVersion})")
                    _updateState.value = UpdateState.Idle
                }

            } catch (e: Exception) {
                Log.e(TAG, "Błąd podczas sprawdzania aktualizacji", e)
                // === POPRAWKA ===
                // Poprawiona obsługa null
                val errorMsg = e.message ?: "Nieznany błąd"
                _updateState.value = UpdateState.Error("Błąd sprawdzania aktualizacji: $errorMsg")
                // ================
            }
        }
    }

    /**
     * Rozpoczyna pobieranie aktualizacji w tle przy użyciu WorkManagera.
     */
    fun startDownload(filename: String) {
        if (filename.isBlank()) {
            _updateState.value = UpdateState.Error("Brak nazwy pliku do pobrania.")
            return
        }

        val workData = Data.Builder()
            .putString(UpdateDownloadWorker.KEY_FILENAME, filename)
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
            .setInputData(workData)
            .build()

        workManager.enqueueUniqueWork(
            UPDATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            downloadRequest
        )

        observeDownloadWork(downloadRequest.id)
    }

    private fun observeDownloadWork(workId: java.util.UUID) {
        downloadObserveJob?.cancel()
        downloadObserveJob = CoroutineScope(Dispatchers.Main).launch {
            workManager.getWorkInfoByIdFlow(workId).collectLatest { workInfo ->
                if (workInfo == null) return@collectLatest

                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        downloadObserveJob?.cancel()
                        val uriString = workInfo.outputData.getString(UpdateDownloadWorker.KEY_APK_URI)
                        if (uriString.isNullOrBlank()) {
                            Log.e(TAG, "Worker SUCCEEDED, ale nie zwrócił APK_URI")
                            _updateState.value = UpdateState.Error("Błąd: Pusta ścieżka pliku po pobraniu.")
                        } else {
                            Log.i(TAG, "Pobieranie zakończone, URI: $uriString")
                            _updateState.value = UpdateState.DownloadReadyToInstall(Uri.parse(uriString))
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        downloadObserveJob?.cancel()
                        // === POPRAWKA ===
                        // Poprawiona obsługa null
                        val error = workInfo.outputData.getString("ERROR") ?: "Nieznany błąd pobierania"
                        _updateState.value = UpdateState.Error(error)
                        // ================
                        launch {
                            delay(5000)
                            if (_updateState.value is UpdateState.Error) {
                                resetState()
                            }
                        }
                    }
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt(UpdateDownloadWorker.KEY_PROGRESS, 0)
                        _updateState.value = UpdateState.Downloading(progress)
                    }
                    WorkInfo.State.ENQUEUED -> {
                        _updateState.value = UpdateState.Downloading(0) // Stan "Oczekuje"
                    }
                    else -> {
                        downloadObserveJob?.cancel()
                        resetState()
                    }
                }
            }
        }
    }

    /**
     * Resetuje stan menedżera do Idle.
     */
    fun resetState() {
        if (_updateState.value is UpdateState.Downloading) {
            return
        }
        _updateState.value = UpdateState.Idle
    }
}