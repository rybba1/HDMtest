package com.example.hdm.ui.scanner

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.hdm.services.CodeValidator
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
@Composable
fun ScannerScreen(
    navController: NavController,
    scanType: String,
    returnKey: String,
    validationContext: String?
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor: ExecutorService = remember(key1 = Unit) { Executors.newSingleThreadExecutor() }
    var barcodeScanned by remember { mutableStateOf(false) }
    val previewView = remember { PreviewView(context).apply { layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT); scaleType = PreviewView.ScaleType.FILL_CENTER } }

    val barcodeScanner = remember(scanType) {
        val optionsBuilder = BarcodeScannerOptions.Builder()
        when (scanType) {
            // Konfiguracja TYLKO dla kodów 1D (bez zmian)
            "1D_BARCODES" -> optionsBuilder.setBarcodeFormats(
                Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39, Barcode.FORMAT_CODE_93, Barcode.FORMAT_ITF, Barcode.FORMAT_CODABAR
            )

            // Konfiguracja TYLKO dla kodów QR (bez zmian)
            "QR_CODE_ONLY" -> optionsBuilder.setBarcodeFormats(
                Barcode.FORMAT_QR_CODE
            )

            // === POCZĄTEK ZMIANY ===
            // Jawna konfiguracja dla walidacji palet: Kody 1D + Główne Kody 2D
            "VALIDATE_PALLET", "VALIDATE_PALLET_BY_TYPE" -> optionsBuilder.setBarcodeFormats(
                // Formaty 1D
                Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39, Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_ITF, Barcode.FORMAT_CODABAR,

                // Formaty 2D (w tym QR i Data Matrix)
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_PDF417
            )
            // === KONIEC ZMIANY ===

            // Domyślna konfiguracja dla reszty (np. "ALL_FORMATS")
            else -> optionsBuilder.setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        }
        BarcodeScanning.getClient(optionsBuilder.build())
    }

    LaunchedEffect(Unit) {
        try {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val imageAnalysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer(
                        scanType = scanType,
                        validationContext = validationContext,
                        barcodeScanner = barcodeScanner,
                        onBarcodeDetected = { detectedBarcodeValue ->
                            if (!barcodeScanned) {
                                barcodeScanned = true
                                Log.d("SCAN_DEBUG", "[ScannerScreen] Zeskanowano poprawny kod: '$detectedBarcodeValue'. Odsyłam pod kluczem: '$returnKey'")
                                navController.previousBackStackEntry?.savedStateHandle?.set(returnKey, detectedBarcodeValue)
                                navController.popBackStack()
                            }
                        },
                        onError = { exception -> Log.e("ScannerScreen", "Błąd analizy kodu kreskowego", exception) }
                    ))
                }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
        } catch (exc: Exception) {
            Log.e("ScannerScreen", "Use case binding failed", exc)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
        Box(modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.85f).aspectRatio(2.5f / 1f).border(2.dp, Color.White))
        Text("Umieść kod w celowniku", modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
        Button(onClick = { if (!barcodeScanned) navController.popBackStack() }, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) { Text("Anuluj") }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }
}

private class BarcodeAnalyzer(
    private val scanType: String,
    private val validationContext: String?,
    private val barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    private val onBarcodeDetected: (String) -> Unit,
    private val onError: (Exception) -> Unit
) : ImageAnalysis.Analyzer {
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        for (barcode in barcodes) {
                            val rawValue = barcode.rawValue
                            if (rawValue != null) {
                                // DODANE LOGI DEBUGOWE
                                Log.d("BarcodeAnalyzer", "═══════════════════════════════════")
                                Log.d("BarcodeAnalyzer", "Wykryto kod kreskowy:")
                                Log.d("BarcodeAnalyzer", "  Raw value: '$rawValue'")
                                Log.d("BarcodeAnalyzer", "  Długość: ${rawValue.length}")
                                Log.d("BarcodeAnalyzer", "  Format: ${barcode.format}")
                                Log.d("BarcodeAnalyzer", "  ScanType: $scanType")

                                val isValid = when (scanType) {
                                    "1D_BARCODES" -> {
                                        val result = CodeValidator.isValid(rawValue)
                                        Log.d("BarcodeAnalyzer", "  Walidacja 1D: $result")
                                        result
                                    }
                                    "VALIDATE_PALLET" -> {
                                        val result = CodeValidator.isAnyPalletNumberValid(rawValue)
                                        Log.d("BarcodeAnalyzer", "  Walidacja PALLET: $result")
                                        result
                                    }
                                    "VALIDATE_PALLET_BY_TYPE" -> {
                                        if (validationContext != null) {
                                            val result = CodeValidator.isPalletNumberValid(rawValue, validationContext)
                                            Log.d("BarcodeAnalyzer", "  Walidacja PALLET BY TYPE ($validationContext): $result")
                                            result
                                        } else {
                                            Log.w("BarcodeAnalyzer", "Próba walidacji wg typu, ale kontek jest null!")
                                            false
                                        }
                                    }

                                    // --- POCZĄTEK BEZPIECZNEJ ZMIANY ---
                                    // Dodajemy logikę walidacji (upewniamy się, że to QR kod)
                                    "QR_CODE_ONLY" -> {
                                        val result = (barcode.format == Barcode.FORMAT_QR_CODE)
                                        Log.d("BarcodeAnalyzer", "  Walidacja QR_CODE_ONLY: $result")
                                        result
                                    }
                                    // --- KONIEC BEZPIECZNEJ ZMIANY ---

                                    else -> {
                                        Log.d("BarcodeAnalyzer", "  Walidacja: TRUE (else)")
                                        true
                                    }
                                }

                                Log.d("BarcodeAnalyzer", "  WYNIK: isValid=$isValid")
                                Log.d("BarcodeAnalyzer", "═══════════════════════════════════")

                                if (isValid) {
                                    onBarcodeDetected(rawValue)
                                    break
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { onError(it) }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }
}