package com.example.hdm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.hdm.services.AppLifecycleService
import com.example.hdm.ui.description.EventDescriptionScreen
import com.example.hdm.ui.header.HeaderScreen
import com.example.hdm.ui.header.HeaderViewModel
import com.example.hdm.ui.log_monitor.LogMonitorScreen
import com.example.hdm.ui.modules.ModulesScreen
import com.example.hdm.ui.nagoya.JapanReportProcessingScreen
import com.example.hdm.ui.nagoya.NagoyaDashboardScreen
import com.example.hdm.ui.palletentry.PalletEntryScreen
import com.example.hdm.ui.photodoc.PhotoDocScreen
import com.example.hdm.ui.photodoc.PhotoDocViewModel
import com.example.hdm.ui.photodoc.PhotoEditScreen
import com.example.hdm.ui.placement.DamageLocationScreen
import com.example.hdm.ui.placement.HeightSelectionScreen
import com.example.hdm.ui.placement.PalletDetailsScreen
import com.example.hdm.ui.placement.PalletSelectionScreen
import com.example.hdm.ui.placement.VehicleSchematicScreen
import com.example.hdm.ui.report_type.ReportTypeScreen
import com.example.hdm.ui.scanner.ScannerScreen
import com.example.hdm.ui.signature.SignatureScreen
import com.example.hdm.ui.summary.DamageMarkingSummaryScreen
import com.example.hdm.ui.summary.PalletSummaryScreen
import com.example.hdm.ui.theme.HDMTheme
import com.example.hdm.ui.splash.SplashScreen
import dagger.hilt.android.AndroidEntryPoint
import com.example.hdm.ui.labelprinting.LabelPrintingScreen
import com.example.hdm.ui.palletlookup.PalletLookupScreen
import com.example.hdm.ui.login.LoginScreen
import com.example.hdm.ui.login.CreateUserScreen
// === DODANO IMPORT ===
import com.example.hdm.ui.bhpstats.BhpStatsScreen
// =====================

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startService(Intent(this, AppLifecycleService::class.java))

        enableEdgeToEdge()
        setContent {
            HDMTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val reportViewModel: HeaderViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) { SplashScreen(navController) }

        // === EKRANY LOGOWANIA ===
        // ===== POPRAWKA: Dodano obsługę parametru scannedData =====
        composable(
            route = Screen.Login.route,
            arguments = listOf(
                navArgument("scannedData") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val scannedData = backStackEntry.arguments?.getString("scannedData")
            LoginScreen(
                navController = navController,
                scannedData = scannedData
            )
        }
        // ===========================================================

        composable(Screen.CreateUser.route) {
            CreateUserScreen(navController = navController)
        }
        // ========================

        composable(Screen.Modules.route) { ModulesScreen(navController) }

        // --- MODUŁ JAPAN CONTROL ---
        composable(Screen.NagoyaDashboard.route) { NagoyaDashboardScreen(navController) }

        composable(
            route = Screen.JapanReportProcessing.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId")
            if (orderId != null) {
                JapanReportProcessingScreen(navController = navController, orderId = orderId)
            }
        }

        // --- GŁÓWNY PRZEPŁYW RAPORTÓW USZKODZEŃ ---
        composable(Screen.ReportType.route) { ReportTypeScreen(navController, reportViewModel) }
        composable(Screen.Header.route) { HeaderScreen(navController, reportViewModel) }
        composable(
            route = Screen.PalletEntry.route,
            arguments = listOf(navArgument("palletId") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            val palletId = backStackEntry.arguments?.getString("palletId")
            PalletEntryScreen(navController, reportViewModel, palletId)
        }
        composable(Screen.PalletSummary.route) { PalletSummaryScreen(navController, reportViewModel) }

        composable(Screen.DamageMarkingSummary.route) {
            DamageMarkingSummaryScreen(navController = navController, reportViewModel = reportViewModel)
        }

        // --- PRZEPŁYW Z UMIEJSCOWIENIEM NA POJEŹDZIE ---
        composable(Screen.PalletSelection.route) {
            PalletSelectionScreen(navController, reportViewModel)
        }
        composable(
            route = Screen.VehicleSchematic.route,
            arguments = listOf(navArgument("palletId") { type = NavType.StringType })
        ) { backStackEntry ->
            val palletId = backStackEntry.arguments?.getString("palletId") ?: ""
            VehicleSchematicScreen(navController, reportViewModel, palletId)
        }
        composable(
            route = Screen.PalletDetails.route,
            arguments = listOf(
                navArgument("palletId") { type = NavType.StringType },
                navArgument("position") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val palletId = backStackEntry.arguments?.getString("palletId") ?: ""
            val position = backStackEntry.arguments?.getString("position") ?: ""
            PalletDetailsScreen(navController, reportViewModel, palletId, position)
        }

        // --- EKRANY WSPÓLNE DLA WSZYSTKICH PRZEPŁYWÓW Z USZKODZENIAMI ---
        composable(
            route = Screen.DamageLocation.route,
            arguments = listOf(
                navArgument("palletId") { type = NavType.StringType },
                navArgument("position") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val palletId = backStackEntry.arguments?.getString("palletId") ?: ""
            val position = backStackEntry.arguments?.getString("position")
            DamageLocationScreen(navController, reportViewModel, palletId, position)
        }

        composable(
            route = Screen.HeightSelection.route,
            arguments = listOf(
                navArgument("palletId") { type = NavType.StringType },
                navArgument("markerIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val palletId = backStackEntry.arguments?.getString("palletId") ?: ""
            val markerIndex = backStackEntry.arguments?.getInt("markerIndex") ?: 0

            val strategy by reportViewModel.strategy.collectAsState()
            val returnRoute = if (strategy?.isVehicleSectionVisible == true) {
                Screen.PalletSelection.route
            } else {
                Screen.DamageMarkingSummary.route
            }

            HeightSelectionScreen(navController, reportViewModel, palletId, markerIndex, returnRoute)
        }

        // --- KOŃCOWE EKRANY PRZEPŁYWU RAPORTU USZKODZEŃ ---
        composable(Screen.EventDescription.route) { EventDescriptionScreen(navController, reportViewModel) }
        composable(Screen.Signature.route) { SignatureScreen(navController, reportViewModel) }

        // --- NARZĘDZIA ---
        composable(
            route = Screen.Scanner.route,
            arguments = listOf(
                navArgument("scanType") { type = NavType.StringType },
                navArgument("returnKey") { type = NavType.StringType },
                navArgument("validationContext") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            ScannerScreen(
                navController,
                backStackEntry.arguments?.getString("scanType") ?: "1D_BARCODES",
                backStackEntry.arguments?.getString("returnKey") ?: "scanned_value",
                backStackEntry.arguments?.getString("validationContext")
            )
        }

        // --- DOKUMENTACJA ZDJĘCIOWA ---
        composable(Screen.PhotoDoc.route) {
            PhotoDocScreen(navController = navController, viewModel = hiltViewModel())
        }
        composable(
            route = Screen.PhotoEdit.route,
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: ""
            val uri = java.net.URLDecoder.decode(encodedUri, "UTF-8")
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Screen.PhotoDoc.route) }
            val photoDocViewModel: PhotoDocViewModel = viewModel(viewModelStoreOwner = parentEntry)
            PhotoEditScreen(navController, photoDocViewModel, uri)
        }

        // --- SPRAWDZANIE USZKODZONYCH PALET ---
        composable(Screen.PalletLookup.route) {
            PalletLookupScreen(navController = navController)
        }

        // === DODANO BLOK ===
        composable(Screen.BhpStats.route) {
            BhpStatsScreen(navController = navController)
        }
        // ===================

        // --- MONITORING ---
        composable(Screen.LogMonitor.route) {
            LogMonitorScreen(
                navController = navController,
                reportViewModel = reportViewModel
            )
        }

        // --- DRUKOWANIE DOKUMENTÓW ---
        composable(Screen.LabelPrinting.route) {
            LabelPrintingScreen(navController = navController)
        }
    }
}