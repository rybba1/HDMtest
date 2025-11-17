package com.example.hdm

sealed class Screen(val route: String) {

    object Splash : Screen("splash_screen")

    // === NOWE EKRANY LOGOWANIA ===
    object Login : Screen("login_screen?scannedData={scannedData}") {
        fun createRoute(scannedData: String? = null): String {
            return if (scannedData != null) {
                "login_screen?scannedData=$scannedData"
            } else {
                "login_screen"
            }
        }
    }
    object CreateUser : Screen("create_user_screen")
    // =============================

    object Modules : Screen("modules_screen")

    // ... reszta ekranów bez zmian ...
    object NagoyaDashboard : Screen("nagoya_dashboard_screen")
    object JapanReportProcessing : Screen("japan_report_processing_screen/{orderId}") {
        fun createRoute(orderId: String): String = "japan_report_processing_screen/$orderId"
    }

    object ReportType : Screen("report_type_screen")
    object Header : Screen("header_screen")
    object PalletEntry : Screen("pallet_entry_screen?palletId={palletId}") {
        fun createRoute(palletId: String? = null): String {
            return if (palletId != null) "pallet_entry_screen?palletId=$palletId" else "pallet_entry_screen"
        }
    }
    object PalletSummary : Screen("pallet_summary_screen")

    object DamageMarkingSummary : Screen("damage_marking_summary_screen")
    object PalletSelection : Screen("pallet_selection_screen")
    object VehicleSchematic : Screen("vehicle_schematic_screen/{palletId}") {
        fun createRoute(palletId: String): String = "vehicle_schematic_screen/$palletId"
    }
    object PalletDetails : Screen("pallet_details_screen/{palletId}/{position}") {
        fun createRoute(palletId: String, position: String): String = "pallet_details_screen/$palletId/$position"
    }

    object DamageLocation : Screen("damage_location_screen?palletId={palletId}&position={position}") {
        fun createRoute(palletId: String, position: String?): String {
            val baseRoute = "damage_location_screen?palletId=$palletId"
            return if (position != null) "$baseRoute&position=$position" else baseRoute
        }
    }

    object HeightSelection : Screen("height_selection_screen/{palletId}/{markerIndex}") {
        fun createRoute(palletId: String, markerIndex: Int): String = "height_selection_screen/$palletId/$markerIndex"
    }
    object EventDescription : Screen("event_description_screen")
    object Signature : Screen("signature_screen")

    object PhotoDoc : Screen("photo_doc_screen")
    object PhotoEdit : Screen("photo_edit_screen/{imageUri}") {
        fun createRoute(imageUri: String): String {
            val encodedUri = java.net.URLEncoder.encode(imageUri, "UTF-8")
            return "photo_edit_screen/$encodedUri"
        }
    }

    object Scanner : Screen("scanner_screen/{scanType}/{returnKey}?validationContext={validationContext}") {
        fun withArgs(scanType: String, returnKey: String, validationContext: String? = null): String {
            val baseRoute = "scanner_screen/$scanType/$returnKey"
            return if (validationContext != null) {
                "$baseRoute?validationContext=$validationContext"
            } else {
                baseRoute
            }
        }
    }

    object LogMonitor : Screen("log_monitor_screen")
    object LabelPrinting : Screen("label_printing_screen")
    object PalletLookup : Screen("pallet_lookup_screen")

    // === DODANO EKRAN STATYSTYK ===
    object BhpStats : Screen("bhp_stats_screen")
    // ==============================
}