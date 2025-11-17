package com.example.hdm.di

import android.content.Context
import androidx.work.WorkManager
import com.example.hdm.model.FileLogService
import com.example.hdm.network.NetworkService
import com.example.hdm.services.FileService
import com.example.hdm.services.HdmLogger
import com.example.hdm.services.JapanReportSerializer
import com.example.hdm.services.LoggingEventListener
import com.example.hdm.services.PalletApiService
import com.example.hdm.services.PhotoDocXmlGenerator
import com.example.hdm.services.TranslationService
import com.example.hdm.ui.header.PdfReportGenerator
import com.example.hdm.ui.labelprinting.LabelPrintingService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import com.example.hdm.services.LoginApiService
import com.example.hdm.services.ExternalBarcodeApiService
import com.example.hdm.services.BhpApiService
import com.example.hdm.model.UserManager
import com.example.hdm.repository.QuizStatisticsRepository
import com.example.hdm.services.UpdateManager
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.hdm.services.DirectSessionApiService
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // ===== POPRAWKA: WŁĄCZONY CONNECTION POOL =====
        // Connection Pool pozwala reużywać połączenia TCP między requestami,
        // co znacząco redukuje latencję (brak konieczności TCP handshake przy każdym requeście)
        val connectionPool = okhttp3.ConnectionPool(
            maxIdleConnections = 5,      // Maksymalnie 5 bezczynnych połączeń w puli
            keepAliveDuration = 5,       // Trzymaj połączenia przez 5 minut
            timeUnit = TimeUnit.MINUTES
        )
        // =============================================

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            // ===== DODANO: Używamy connection pool =====
            .connectionPool(connectionPool)
            // ===========================================
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    return try {
                        Dns.SYSTEM.lookup(hostname).filter { Inet4Address::class.java.isInstance(it) }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            })
            .eventListener(LoggingEventListener)
            .build()
    }

    @Provides
    @Singleton
    fun provideNetworkService(okHttpClient: OkHttpClient): NetworkService = NetworkService(okHttpClient)

    @Provides
    @Singleton
    fun provideFileService(): FileService = FileService

    @Provides
    @Singleton
    fun provideFileLogService(): FileLogService = FileLogService

    @Provides
    @Singleton
    fun provideHdmLogger(networkService: NetworkService, fileLogService: FileLogService): HdmLogger {
        return HdmLogger(networkService, fileLogService)
    }

    @Provides
    @Singleton
    fun provideTranslationService(): TranslationService = TranslationService()

    @Provides
    @Singleton
    fun providePhotoDocXmlGenerator(): PhotoDocXmlGenerator = PhotoDocXmlGenerator()

    @Provides
    @Singleton
    fun provideJapanReportSerializer(): JapanReportSerializer = JapanReportSerializer()

    @Provides
    @Singleton
    fun providePdfReportGenerator(): PdfReportGenerator = PdfReportGenerator()

    @Provides
    @Singleton
    fun provideLabelPrintingService(okHttpClient: OkHttpClient): LabelPrintingService = LabelPrintingService(okHttpClient)

    @Provides
    @Singleton
    fun providePalletApiService(okHttpClient: OkHttpClient): PalletApiService {
        return PalletApiService(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideDirectSessionApiService(okHttpClient: OkHttpClient, json: Json): DirectSessionApiService {
        return DirectSessionApiService(okHttpClient, json)
    }

    @Provides
    @Singleton
    fun provideLoginApiService(okHttpClient: OkHttpClient): LoginApiService {
        return LoginApiService(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideBhpApiService(okHttpClient: OkHttpClient): BhpApiService {
        return BhpApiService(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideUserManager(): UserManager {
        return UserManager
    }

    @Provides
    @Singleton
    fun provideQuizStatisticsRepository(): QuizStatisticsRepository {
        return QuizStatisticsRepository
    }

    @Provides
    @Singleton
    fun provideExternalBarcodeApiService(okHttpClient: OkHttpClient): ExternalBarcodeApiService {
        return ExternalBarcodeApiService(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideUpdateManager(
        @ApplicationContext context: Context,
        loginApiService: LoginApiService,
        workManager: WorkManager
    ): UpdateManager {
        return UpdateManager(context, loginApiService, workManager)
    }
}