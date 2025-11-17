package com.example.hdm.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserManager {
    private const val PREFS_NAME = "HdmAppPrefs"
    private const val KEY_IDENTIFIER_URI = "identifierUriString"

    // === STARE ZMIENNE (bez zmian) ===
    private val _currentUser = MutableStateFlow("BRAK")
    val currentUser = _currentUser.asStateFlow()

    private val _identifierFileUri = MutableStateFlow<Uri?>(null)
    val identifierFileUri = _identifierFileUri.asStateFlow()

    private val _isScreenOn = MutableStateFlow(true)
    val isScreenOn = _isScreenOn.asStateFlow()

    // === NOWE ZMIENNE (dla systemu logowania) ===
    private val _loggedInUser = MutableStateFlow<LoggedInUser?>(null)
    val loggedInUser = _loggedInUser.asStateFlow()

    data class LoggedInUser(
        val login: String,
        val fullName: String,
        val workerId: Int
    )
    // ============================================

    fun setScreenState(isOn: Boolean) {
        _isScreenOn.value = isOn
    }

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriString = prefs.getString(KEY_IDENTIFIER_URI, null)
        if (uriString != null) {
            _identifierFileUri.value = Uri.parse(uriString)
        }
    }

    fun setCurrentUser(name: String?) {
        _currentUser.value = if (name.isNullOrBlank()) "BRAK" else name
    }

    fun setIdentifierFile(context: Context, uri: Uri) {
        val contentResolver = context.contentResolver
        try {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)

            _identifierFileUri.value = uri
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit {
                putString(KEY_IDENTIFIER_URI, uri.toString())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // === NOWE FUNKCJE (dla systemu logowania) ===

    /**
     * Loguje użytkownika do systemu
     *
     * @param login Login użytkownika (np. "lmazur")
     * @param fullName Pełne imię i nazwisko
     * @param workerId ID pracownika z WarehouseWorkerRepository
     */
    fun loginUser(login: String, fullName: String, workerId: Int) {
        _loggedInUser.value = LoggedInUser(login, fullName, workerId)
        _currentUser.value = fullName
    }

    /**
     * Wylogowuje użytkownika z systemu
     */
    fun logoutUser() {
        _loggedInUser.value = null
        _currentUser.value = "BRAK"
    }

    /**
     * Sprawdza czy użytkownik jest zalogowany
     */
    fun isLoggedIn(): Boolean = _loggedInUser.value != null

    /**
     * Pobiera dane zalogowanego użytkownika (null jeśli niezalogowany)
     */
    fun getLoggedInUser(): LoggedInUser? = _loggedInUser.value
    // =============================================
}