package com.example.hdm.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val TAG = "JAPAN_DEBUG"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                return Dns.SYSTEM.lookup(hostname).filter { Inet4Address::class.java.isInstance(it) }
            }
        })
        .eventListener(LoggingEventListener)
        .build()

    private const val BASE_URL = "http://10.1.0.19:8766/"
    private const val POST_REPORT_URL = "http://10.1.0.19:8765/reports"

    suspend fun getReports(): List<String> {
        Log.d(TAG, "ApiClient: Rozpoczynam pobieranie raportów...")
        val reportListXml = getReportListXml()
        if (reportListXml == null) {
            Log.e(TAG, "ApiClient: Nie udało się pobrać głównej listy raportów.")
            return emptyList()
        }

        val reportIds = parseReportIdsFromXml(reportListXml)
        if (reportIds.isEmpty()) {
            Log.w(TAG, "ApiClient: Główna lista nie zawiera żadnych raportów do pobrania.")
            return emptyList()
        }

        val reportsContent = mutableListOf<String>()
        Log.d(TAG, "ApiClient: Znaleziono ${reportIds.size} ID raportów. Pobieram ich zawartość...")
        for (reportId in reportIds) {
            getReportContent(reportId)?.let { content ->
                reportsContent.add(content)
            }
        }
        Log.d(TAG, "ApiClient: Pomyślnie pobrano zawartość ${reportsContent.size} raportów.")
        return reportsContent
    }

    private suspend fun getReportListXml(): String? = withContext(Dispatchers.IO) {
        Log.d(TAG, "ApiClient: Pobieranie listy z $BASE_URL")
        try {
            val request = Request.Builder()
                .url(BASE_URL)
                .header("Accept", "application/xml")
                .header("User-Agent", "Mozilla/5.0 (Android 15; Mobile; rv:109.0) Gecko/109.0 Firefox/115.0")
                .build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "ApiClient: Błąd pobierania listy. Kod: ${response.code}")
                return@withContext null
            }

            val xmlString = response.body?.string()
            if (xmlString.isNullOrBlank()) {
                Log.e(TAG, "ApiClient: Odpowiedź serwera jest pusta.")
                return@withContext null
            }
            Log.d(TAG, "ApiClient: Otrzymano XML z listą plików:\n$xmlString")
            xmlString

        } catch (e: Exception) {
            Log.e(TAG, "ApiClient: Wyjątek (np. brak połączenia) podczas pobierania listy.", e)
            null
        }
    }

    private suspend fun getReportContent(reportId: String): String? = withContext(Dispatchers.IO) {
        val fileUrl = "$BASE_URL$reportId"
        Log.d(TAG, "ApiClient: Pobieranie zawartości pliku $fileUrl")
        try {
            val request = Request.Builder()
                .url(fileUrl)
                .header("Accept", "application/xml")
                .header("User-Agent", "Mozilla/5.0 (Android 15; Mobile; rv:109.0) Gecko/109.0 Firefox/115.0")
                .build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "ApiClient: Błąd pobierania pliku '$reportId'. Kod: ${response.code}")
                return@withContext null
            }
            response.body?.string()
        } catch (e: Exception) {
            Log.e(TAG, "ApiClient: Wyjątek podczas pobierania pliku '$reportId'.", e)
            null
        }
    }

    private fun parseReportIdsFromXml(xmlString: String): List<String> {
        val ids = mutableListOf<String>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlString))
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "Order") {
                    if (parser.next() == XmlPullParser.TEXT) {
                        ids.add(parser.text)
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "ApiClient: Błąd parsowania XML z listą.", e)
        }
        Log.d(TAG, "ApiClient: Sparsowano następujące ID z listy: $ids")
        return ids
    }

   }