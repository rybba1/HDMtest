package com.example.hdm.ui.header

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.hdm.R
import com.example.hdm.model.DamagedPallet
import com.example.hdm.model.PalletPosition
import com.example.hdm.model.ReportHeader
import com.example.hdm.ui.placement.DamageMarker
import com.example.hdm.ui.placement.SelectableDamage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
class PdfReportGenerator {

    private val TAG = "PdfGenerator_DEBUG"

    enum class Language {
        PL, EN
    }

    data class PdfConfig(
        val showVehicleSection: Boolean,
        val showFeniksNumber: Boolean,
        val showPlacementSchematic: Boolean,
        val showPositionAndDamageCode: Boolean,
        val renderPalletDamageDiagram: Boolean
    )

    companion object {
        private val COLOR_RED = Color.Red.toArgb()
        private val COLOR_BLUE = Color.Blue.toArgb()
        private val COLOR_GREEN = Color(0xFF008000).toArgb()
        private val COLOR_MAGENTA = Color.Magenta.toArgb()
        private val COLOR_BLACK = Color.Black.toArgb()

        private val COLOR_YELLOW = Color.Yellow.toArgb()
        private val COLOR_PURPLE = Color(0xFF800080).toArgb()
        private val COLOR_CYAN = Color.Cyan.toArgb()
        private val COLOR_BROWN = Color(0xFF964B00).toArgb()
        private val COLOR_GRAY = Color.Gray.toArgb()

        private fun getMarkerColorName(color: Int, language: Language): String {
            val namePl = when (color) {
                COLOR_RED -> "Czerwony"; COLOR_BLUE -> "Niebieski"; COLOR_GREEN -> "Zielony"; COLOR_MAGENTA -> "Różowy"; COLOR_BLACK -> "Czarny"
                COLOR_YELLOW -> "Żółty"; COLOR_PURPLE -> "Fioletowy"; COLOR_CYAN -> "Błękitny"; COLOR_BROWN -> "Brązowy"; COLOR_GRAY -> "Szary"
                else -> "Niestandardowy"
            }
            val nameEn = when (color) {
                COLOR_RED -> "Red"; COLOR_BLUE -> "Blue"; COLOR_GREEN -> "Green"; COLOR_MAGENTA -> "Magenta"; COLOR_BLACK -> "Black"
                COLOR_YELLOW -> "Yellow"; COLOR_PURPLE -> "Purple"; COLOR_CYAN -> "Cyan"; COLOR_BROWN -> "Brown"; COLOR_GRAY -> "Gray"
                else -> "Custom"
            }
            return if (language == Language.PL) namePl else nameEn
        }

        private val PRIMARY_BLUE = DeviceRgb(59, 130, 246)
        private val SUBTLE_BORDER = DeviceRgb(226, 232, 240)
        private val TEXT_BLACK = ColorConstants.BLACK
        private val BACKGROUND_WHITE = ColorConstants.WHITE
        private val BACKGROUND_LIGHT = DeviceRgb(250, 251, 252)
        private val TABLE_HEADER = DeviceRgb(241, 245, 249)
        private val TABLE_ROW_ALT = DeviceRgb(248, 250, 252)
        private val FONT_TITLE = 24f
        private val FONT_SUBTITLE = 12f
        private val FONT_HEADING = 10f
        private val FONT_BODY = 10f
        private val FONT_SMALL = 8f
        private val FONT_TINY = 7f
        private val SPACE_1 = 2f
        private val SPACE_2 = 4f
        private val PAGE_MARGIN = 28f
        private val SECTION_SPACING = 4f
        private val CELL_PADDING = 4f
        private val BORDER_WIDTH = 1f

        private fun getPalletLetter(index: Int): String {
            return when {
                index < 26 -> ('A' + index).toString()
                index < 52 -> "A" + ('A' + (index - 26)).toString()
                else -> "Z" + (index - 51).toString()
            }
        }

        private fun findPalletLetter(palletId: String, savedPallets: List<DamagedPallet>): String {
            val index = savedPallets.indexOfFirst { it.id == palletId }
            return if (index >= 0) getPalletLetter(index) else "?"
        }
    }

    suspend fun generatePdfReport(
        outputStream: OutputStream,
        context: Context,
        reportHeader: ReportHeader,
        savedPallets: List<DamagedPallet>,
        palletPositions: List<PalletPosition>,
        selectedVehicleLayout: String?,
        config: PdfConfig,
        damageMarkers: Map<String, List<DamageMarker>>,
        damageHeightSelections: Map<String, Map<String, Set<String>>>,
        language: Language = Language.PL,
        pdfTitle: String,
        isWyrobGotowyOverride: Boolean,
        damageTranslationMap: Map<String, String> = emptyMap()
    ): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val writer = PdfWriter(outputStream)
                val pdf = PdfDocument(writer)
                val document = Document(pdf)
                document.setMargins(PAGE_MARGIN, PAGE_MARGIN, 50f, PAGE_MARGIN)
                val fontRegular = loadRegularFont(context)
                val fontBold = loadBoldFont(context)
                document.setFont(fontRegular).setFontSize(FONT_BODY)
                addHeader(
                    document,
                    context,
                    reportHeader,
                    fontBold,
                    fontRegular,
                    language,
                    pdfTitle
                )
                addInfoSection(document, reportHeader, fontBold, fontRegular, language, config)
                addPalletsTable(
                    document,
                    savedPallets,
                    palletPositions,
                    damageMarkers,
                    damageHeightSelections,
                    fontBold,
                    fontRegular,
                    isWyrobGotowyOverride,
                    language,
                    config,
                    reportHeader.rodzajPalet
                )
                if (config.showPlacementSchematic) {
                    addVehicleSchematic(
                        document,
                        reportHeader,
                        savedPallets,
                        palletPositions,
                        fontBold,
                        fontRegular,
                        context,
                        selectedVehicleLayout,
                        language
                    )
                }
                addDamageDetails(
                    document,
                    reportHeader,
                    savedPallets,
                    palletPositions,
                    damageMarkers,
                    damageHeightSelections,
                    fontBold,
                    fontRegular,
                    context,
                    language,
                    isWyrobGotowyOverride,
                    damageTranslationMap
                )
                addEventDescription(document, reportHeader, fontBold, fontRegular, language)

                addSignatureSection(
                    document,
                    context,
                    reportHeader,
                    fontBold,
                    fontRegular,
                    language
                )

                if (config.showPlacementSchematic) {
                    addLegendSection(document, context, fontBold, language)
                }
                addPageNumbers(pdf, fontRegular, language)
                document.close()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PdfGenerator_FINAL", "Krytyczny błąd podczas generowania PDF", e)
            Result.failure(e)
        }
    }

    private fun compressBitmapForPdf(bitmap: Bitmap, maxWidth: Int = 800): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap

        val ratio = maxWidth.toFloat() / bitmap.width
        val newHeight = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
    }

    private fun loadRegularFont(context: Context): PdfFont {
        return try {
            val fontStream = context.resources.openRawResource(R.raw.roboto_regular)
            PdfFontFactory.createFont(
                fontStream.readBytes(),
                PdfEncodings.IDENTITY_H,
                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
            )
        } catch (e: Exception) {
            PdfFontFactory.createFont()
        }
    }

    private fun loadBoldFont(context: Context): PdfFont {
        return try {
            val fontBoldStream = context.resources.openRawResource(R.raw.roboto_bold)
            PdfFontFactory.createFont(
                fontBoldStream.readBytes(),
                PdfEncodings.IDENTITY_H,
                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
            )
        } catch (e: Exception) {
            PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD)
        }
    }

    @SuppressLint("ResourceType")
    private fun addHeader(
        document: Document,
        context: Context,
        reportHeader: ReportHeader,
        fontBold: PdfFont,
        fontRegular: PdfFont,
        language: Language,
        pdfTitle: String
    ) {
        val headerTable =
            Table(UnitValue.createPercentArray(floatArrayOf(1f, 2.5f, 1f))).useAllAvailableWidth()
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setBorder(SolidBorder(SUBTLE_BORDER, BORDER_WIDTH))
                .setMarginBottom(SECTION_SPACING)
        val logoCell = createCell(null, CELL_PADDING, VerticalAlignment.MIDDLE)
        try {
            val logoStream = context.resources.openRawResource(R.drawable.company_logo)
            logoCell.add(
                Image(ImageDataFactory.create(logoStream.readBytes())).setHeight(50f)
                    .setAutoScaleWidth(true)
            )
        } catch (e: Exception) {
            logoCell.add(
                Paragraph("MEIKO").setFont(fontBold).setFontSize(FONT_HEADING)
                    .setFontColor(TEXT_BLACK)
            )
        }

        // === POCZĄTEK POPRAWKI ===
        // Sprawdzamy, czy pole 'miejsce' zawiera słowo "Gaudi"
        val companyAddress = when {
            reportHeader.miejsce.contains("Gaudi", ignoreCase = true) -> "ul. Antonio Gaudiego 6D\n44-109 Gliwice"
            // Domyślnie (w tym "Leon" lub "WH1") używamy Wyczółkowskiego
            else -> "ul. Wyczółkowskiego 121\n44-109 Gliwice"
        }
        // === KONIEC POPRAWKI ===

        logoCell.add(
            Paragraph(companyAddress).setFont(fontRegular).setFontSize(FONT_SMALL)
                .setFontColor(TEXT_BLACK).setMarginTop(SPACE_2)
        )
        headerTable.addCell(logoCell)
        val titleCell = createCell(
            null,
            CELL_PADDING,
            VerticalAlignment.MIDDLE
        ).setTextAlignment(TextAlignment.CENTER)
        val reportTitle = when (language) {
            Language.PL -> "RAPORT USZKODZEŃ"; Language.EN -> "DAMAGE REPORT"
        }
        titleCell.add(
            Paragraph(reportTitle).setFont(fontBold).setFontSize(FONT_TITLE)
                .setFontColor(PRIMARY_BLUE).setTextAlignment(TextAlignment.CENTER)
        )
            .add(
                Paragraph(pdfTitle).setFont(fontRegular).setFontSize(FONT_SUBTITLE)
                    .setFontColor(TEXT_BLACK).setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(SPACE_2)
            )
        headerTable.addCell(titleCell)
        val qrCell = createCell(
            null,
            CELL_PADDING,
            VerticalAlignment.TOP
        ).setTextAlignment(TextAlignment.RIGHT)
        if (reportHeader.numerDokumentuFeniks.isNotBlank()) {
            try {
                qrCell.add(
                    Image(
                        ImageDataFactory.create(
                            bitmapToByteArray(
                                generateQRCode(
                                    reportHeader.numerDokumentuFeniks
                                )
                            )
                        )
                    ).setHeight(50f).setWidth(50f).setHorizontalAlignment(HorizontalAlignment.RIGHT)
                )
            } catch (e: Exception) {
                qrCell.add(
                    Paragraph("QR").setFont(fontBold).setFontSize(FONT_HEADING)
                        .setFontColor(TEXT_BLACK).setTextAlignment(TextAlignment.RIGHT)
                )
            }
        }
        val currentDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val generatedLabel = when (language) {
            Language.PL -> "Wygenerowano:"; Language.EN -> "Generated:"
        }
        qrCell.add(
            Paragraph("ID:").setFont(fontRegular).setFontSize(FONT_TINY).setFontColor(TEXT_BLACK)
                .setTextAlignment(TextAlignment.RIGHT).setMarginTop(SPACE_1)
        )
            .add(
                Paragraph(reportHeader.numerDokumentuFeniks).setFont(fontBold)
                    .setFontSize(FONT_TINY).setFontColor(TEXT_BLACK)
                    .setTextAlignment(TextAlignment.RIGHT)
            )
            .add(
                Paragraph(generatedLabel).setFont(fontRegular).setFontSize(FONT_TINY)
                    .setFontColor(TEXT_BLACK).setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(SPACE_2)
            )
            .add(
                Paragraph(currentDate).setFont(fontRegular).setFontSize(FONT_TINY)
                    .setFontColor(TEXT_BLACK).setTextAlignment(TextAlignment.RIGHT)
            )
        headerTable.addCell(qrCell)
        document.add(headerTable)
    }

    private fun addInfoSection(
        document: Document,
        reportHeader: ReportHeader,
        fontBold: PdfFont,
        fontRegular: PdfFont,
        language: Language,
        config: PdfConfig
    ) {
        val sectionTable =
            Table(UnitValue.createPercentArray(floatArrayOf(1f))).useAllAvailableWidth()
                .setBorder(SolidBorder(SUBTLE_BORDER, BORDER_WIDTH))
                .setMarginBottom(SECTION_SPACING)
        val headerCell = createCell(
            null,
            CELL_PADDING,
            VerticalAlignment.MIDDLE
        ).setBackgroundColor(PRIMARY_BLUE).setTextAlignment(TextAlignment.CENTER)
        val sectionTitle = when (language) {
            Language.PL -> "INFORMACJE PODSTAWOWE"; Language.EN -> "BASIC INFORMATION"
        }
        headerCell.add(
            Paragraph(sectionTitle).setFont(fontBold).setFontSize(FONT_HEADING)
                .setFontColor(ColorConstants.WHITE).setMargin(0f)
        )
        sectionTable.addCell(headerCell)
        val contentCell =
            createCell(null, 0f, VerticalAlignment.TOP).setBackgroundColor(BACKGROUND_WHITE)
        val infoTable =
            Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
                .setBorder(Border.NO_BORDER)
        val leftColumn = createCell(null, CELL_PADDING, VerticalAlignment.TOP)
        val rightColumn = createCell(null, CELL_PADDING, VerticalAlignment.TOP)

        val displayWorker = reportHeader.workerId?.let { "ID: $it" } ?: "Brak ID"

        leftColumn.add(
            createInfoRow(
                when (language) {
                    Language.PL -> "Magazynier"; Language.EN -> "Warehouse Worker"
                },
                displayWorker,
                fontBold,
                fontRegular,
                language
            )
        )
        leftColumn.add(
            createInfoRow(
                when (language) {
                    Language.PL -> "Miejsce"; Language.EN -> "Location"
                },
                "${reportHeader.miejsce} (${if (reportHeader.miejsce == "Leon") "WH1" else "WH2"})",
                fontBold,
                fontRegular,
                language
            )
        )
        leftColumn.add(
            createInfoRow(
                when (language) {
                    Language.PL -> "Lokalizacja"; Language.EN -> "Dock"
                }, reportHeader.lokalizacja, fontBold, fontRegular, language
            )
        )
        leftColumn.add(
            createInfoRow(
                when (language) {
                    Language.PL -> "Rodzaj wózka"; Language.EN -> "Forklift Type"
                }, reportHeader.rodzajWozka, fontBold, fontRegular, language
            )
        )
        val dataGodzina = SimpleDateFormat(
            "dd.MM.yyyy HH:mm",
            Locale.getDefault()
        ).format(Date(reportHeader.dataGodzina))
        rightColumn.add(
            createInfoRow(
                when (language) {
                    Language.PL -> "Data i godzina rozładunku"; Language.EN -> "Date and time of unloading"
                }, dataGodzina, fontBold, fontRegular, language
            )
        )
        if (config.showVehicleSection) {
            rightColumn.add(
                createInfoRow(
                    when (language) {
                        Language.PL -> "Rodzaj pojazdu"; Language.EN -> "Vehicle Type"
                    }, reportHeader.rodzajSamochodu, fontBold, fontRegular, language
                )
            )
            rightColumn.add(
                createInfoRow(
                    when (language) {
                        Language.PL -> "Nr rejestracyjny"; Language.EN -> "Registration No."
                    }, reportHeader.numerAuta.ifBlank { "—" }, fontBold, fontRegular, language
                )
            )
            rightColumn.add(
                createInfoRow(
                    when (language) {
                        Language.PL -> "Nr naczepy/kontenera"; Language.EN -> "Trailer/Container No."
                    },
                    reportHeader.numerNaczepyKontenera.ifBlank { "—" },
                    fontBold,
                    fontRegular,
                    language
                )
            )
        }
        infoTable.addCell(leftColumn)
        infoTable.addCell(rightColumn)
        contentCell.add(infoTable)
        sectionTable.addCell(contentCell)
        document.add(sectionTable)
    }

    private fun createInfoRow(
        label: String,
        value: String,
        fontBold: PdfFont,
        fontRegular: PdfFont,
        language: Language
    ): Paragraph {
        return Paragraph().add(Text("$label: ").setFont(fontBold).setFontSize(FONT_SMALL))
            .add(Text(value).setFont(fontRegular).setFontSize(FONT_SMALL)).setMarginBottom(SPACE_1)
    }

    private fun addPalletsTable(
        document: Document,
        savedPallets: List<DamagedPallet>,
        palletPositions: List<PalletPosition>,
        damageMarkers: Map<String, List<DamageMarker>>,
        damageHeightSelections: Map<String, Map<String, Set<String>>>,
        fontBold: PdfFont,
        fontRegular: PdfFont,
        isWyrobGotowy: Boolean,
        language: Language,
        config: PdfConfig,
        rodzajPaletPrzetlumaczony: String
    ) {
        if (savedPallets.isEmpty()) return
        val sectionTable =
            Table(UnitValue.createPercentArray(floatArrayOf(1f))).useAllAvailableWidth()
                .setBorder(SolidBorder(SUBTLE_BORDER, BORDER_WIDTH))
                .setMarginBottom(SECTION_SPACING)
        val headerCell = createCell(
            null,
            CELL_PADDING,
            VerticalAlignment.MIDDLE
        ).setBackgroundColor(PRIMARY_BLUE).setTextAlignment(TextAlignment.CENTER)
        val sectionTitle = when (language) {
            Language.PL -> "WYKAZ USZKODZONYCH PALET"; Language.EN -> "LIST OF DAMAGED PALLETS"
        }
        headerCell.add(
            Paragraph(sectionTitle).setFont(fontBold).setFontSize(FONT_HEADING)
                .setFontColor(ColorConstants.WHITE).setMargin(0f)
        )
        sectionTable.addCell(headerCell)
        val contentCell =
            createCell(null, 0f, VerticalAlignment.TOP).setBackgroundColor(BACKGROUND_WHITE)
        val headers: MutableList<String>
        val columnWidths: MutableList<Float>
        if (isWyrobGotowy) {
            headers = when (language) {
                Language.PL -> mutableListOf(
                    "ID",
                    "Nazwa Towaru",
                    "Numer palety",
                    "Numer lot",
                    "Typy uszkodzeń"
                ); Language.EN -> mutableListOf(
                    "ID",
                    "Product Name",
                    "Pallet No.",
                    "Lot No.",
                    "Damage Types"
                )
            }
            columnWidths = mutableListOf(0.4f, 1.8f, 1.4f, 1.4f, 2.8f)
        } else {
            headers = when (language) {
                Language.PL -> mutableListOf(
                    "ID",
                    "Nazwa Towaru",
                    "Numer lot",
                    "Typy uszkodzeń"
                ); Language.EN -> mutableListOf("ID", "Product Name", "Lot No.", "Damage Types")
            }
            columnWidths = mutableListOf(0.4f, 2.0f, 1.5f, 3.0f)
        }
        if (config.showPositionAndDamageCode) {
            headers.addAll(
                when (language) {
                    Language.PL -> listOf(
                        "Pozycja",
                        "Poziom",
                        "Kod uszkodzenia"
                    ); Language.EN -> listOf("Position", "Level", "Damage Code")
                }
            )
            columnWidths.addAll(listOf(0.7f, 0.7f, 1.0f))
        }
        val palletsTable =
            Table(UnitValue.createPercentArray(columnWidths.toFloatArray())).useAllAvailableWidth()
                .setBorder(Border.NO_BORDER)
        headers.forEach { header ->
            palletsTable.addCell(
                createCell(
                    SolidBorder(
                        SUBTLE_BORDER,
                        0.5f
                    ), CELL_PADDING, VerticalAlignment.MIDDLE
                ).setBackgroundColor(TABLE_HEADER).setTextAlignment(TextAlignment.CENTER).add(
                    Paragraph(header).setFont(fontBold).setFontSize(FONT_TINY)
                        .setFontColor(TEXT_BLACK)
                )
            )
        }
        savedPallets.forEachIndexed { index, pallet ->
            val position = palletPositions.find { it.palletId == pallet.id }
            val rowColor = if (index % 2 == 0) BACKGROUND_WHITE else TABLE_ROW_ALT
            val palletLetter = getPalletLetter(index)
            val nazwaTowaruText = if (isWyrobGotowy) {
                rodzajPaletPrzetlumaczony
            } else {
                "${rodzajPaletPrzetlumaczony}: ${pallet.rodzajTowaru?.takeIf { it.isNotBlank() } ?: "—"}"
            }
            val typyUszkodzenText = pallet.damageInstances.flatMap { i ->
                i.details.flatMap { d ->
                    d.types.map { t ->
                        val s = if (t.size.isNotBlank()) " (${t.size}cm)" else ""; "${t.type}$s"
                    }
                }
            }.joinToString("\n").ifBlank { "—" }
            val positionText = position?.positionOnVehicle ?: "—"
            val levelText = when (position?.stackingLevel) {
                "A" -> when (language) {
                    Language.PL -> "Góra"; Language.EN -> "Top"
                }; "B" -> when (language) {
                    Language.PL -> "Dół"; Language.EN -> "Bottom"
                }; else -> when (language) {
                    Language.PL -> "Pojedyncza"; Language.EN -> "Single"
                }
            }
            val palletHeightSelections = damageHeightSelections[pallet.id] ?: emptyMap()
            val allHeightParts =
                palletHeightSelections.values.flatten().toSet().sorted().joinToString(",")
            val damageCode = if (position != null && allHeightParts.isNotBlank()) {
                val l = if (position.stackingLevel.isBlank()) "B" else position.stackingLevel
                "${position.positionOnVehicle}${l}${allHeightParts}"
            } else "—"
            val rowData = mutableListOf(palletLetter, nazwaTowaruText)
            if (isWyrobGotowy) {
                rowData.add(
                    pallet.numerPalety.substringBefore('_', pallet.numerPalety).ifBlank { "—" })
                rowData.add(pallet.numerPalety.substringAfter('_', "—").ifBlank { "—" })
            } else {
                rowData.add(pallet.numerLotu?.takeIf { it.isNotBlank() } ?: "—")
            }
            rowData.add(typyUszkodzenText)
            if (config.showPositionAndDamageCode) {
                rowData.addAll(listOf(positionText, levelText, damageCode))
            }
            rowData.forEach { data ->
                palletsTable.addCell(
                    createCell(
                        SolidBorder(
                            SUBTLE_BORDER,
                            0.5f
                        ), CELL_PADDING, VerticalAlignment.MIDDLE
                    ).setBackgroundColor(rowColor).setTextAlignment(TextAlignment.CENTER).add(
                        Paragraph(data).setFont(fontRegular).setFontSize(FONT_TINY)
                            .setFontColor(TEXT_BLACK)
                    )
                )
            }
        }
        contentCell.add(palletsTable)
        sectionTable.addCell(contentCell)
        document.add(sectionTable)
    }

    private fun addDamageDetails(
        document: Document,
        reportHeader: ReportHeader,
        savedPallets: List<DamagedPallet>,
        palletPositions: List<PalletPosition>,
        damageMarkers: Map<String, List<DamageMarker>>,
        damageHeightSelections: Map<String, Map<String, Set<String>>>,
        fontBold: PdfFont,
        fontRegular: PdfFont,
        context: Context,
        language: Language,
        isWyrobGotowy: Boolean,
        damageTranslationMap: Map<String, String>
    ) {
        if (savedPallets.isEmpty()) return
        val sectionTable =
            Table(UnitValue.createPercentArray(floatArrayOf(1f))).useAllAvailableWidth()
                .setBorder(SolidBorder(SUBTLE_BORDER, BORDER_WIDTH))
                .setMarginBottom(SECTION_SPACING)
        val headerCell = createCell(
            null,
            CELL_PADDING,
            VerticalAlignment.MIDDLE
        ).setBackgroundColor(PRIMARY_BLUE).setTextAlignment(TextAlignment.CENTER)
        val sectionTitle = when (language) {
            Language.PL -> "SZCZEGÓŁOWE RAPORTY USZKODZEŃ"; Language.EN -> "DETAILED DAMAGE REPORTS"
        }
        headerCell.add(
            Paragraph(sectionTitle).setFont(fontBold).setFontSize(FONT_HEADING)
                .setFontColor(ColorConstants.WHITE).setMargin(0f)
        )
        sectionTable.addCell(headerCell)
        val contentCell =
            createCell(null, 0f, VerticalAlignment.TOP).setBackgroundColor(BACKGROUND_WHITE)
        val numberOfColumns = 4
        val palletLayoutTable =
            Table(UnitValue.createPercentArray(FloatArray(numberOfColumns) { 1f })).useAllAvailableWidth()
        savedPallets.forEachIndexed { index, pallet ->
            val palletCardCell =
                Cell().setPadding(CELL_PADDING).setBorder(SolidBorder(SUBTLE_BORDER, 0.5f))
            val palletHeaderText = when (language) {
                Language.PL -> "PALETA ${getPalletLetter(index)}"; Language.EN -> "PALLET ${
                    getPalletLetter(
                        index
                    )
                }"
            }
            palletCardCell.add(
                Paragraph(palletHeaderText).setFont(fontBold).setFontSize(FONT_SMALL)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(SPACE_1)
            )
            val nazwaTowaruText = if (isWyrobGotowy) {
                reportHeader.rodzajPalet
            } else {
                "${reportHeader.rodzajPalet}: ${pallet.rodzajTowaru?.takeIf { it.isNotBlank() } ?: "—"}"
            }
            palletCardCell.add(
                createInfoRow(
                    when (language) {
                        Language.PL -> "Nazwa Towaru"; Language.EN -> "Product Name"
                    }, nazwaTowaruText, fontBold, fontRegular, language
                )
            )
            if (isWyrobGotowy) {
                val numerPalety =
                    pallet.numerPalety.substringBefore('_', pallet.numerPalety).ifBlank { "—" }
                palletCardCell.add(
                    createInfoRow(
                        when (language) {
                            Language.PL -> "Numer palety"; Language.EN -> "Pallet No."
                        }, numerPalety, fontBold, fontRegular, language
                    )
                )
            } else {
                val numerLotu = pallet.numerLotu?.takeIf { it.isNotBlank() } ?: "—"
                palletCardCell.add(
                    createInfoRow(
                        when (language) {
                            Language.PL -> "Numer lot"; Language.EN -> "Lot No."
                        }, numerLotu, fontBold, fontRegular, language
                    )
                )
            }
            val position = palletPositions.find { it.palletId == pallet.id }
            val palletHeightSelections = damageHeightSelections[pallet.id] ?: emptyMap()
            val allHeightParts =
                palletHeightSelections.values.flatten().toSet().sorted().joinToString(",")
            val damageCode = if (position != null && allHeightParts.isNotBlank()) {
                val levelForCode =
                    if (position.stackingLevel.isBlank()) "B" else position.stackingLevel
                "${position.positionOnVehicle}${levelForCode}${allHeightParts}"
            } else {
                "—"
            }
            val levelText = when (position?.stackingLevel) {
                "A" -> when (language) {
                    Language.PL -> "Góra"; Language.EN -> "Top"
                }

                "B" -> when (language) {
                    Language.PL -> "Dół"; Language.EN -> "Bottom"
                }

                else -> when (language) {
                    Language.PL -> "Pojedyncza"; Language.EN -> "Single"
                }
            }
            palletCardCell.add(
                createInfoRow(
                    when (language) {
                        Language.PL -> "Poziom"; Language.EN -> "Level"
                    }, levelText, fontBold, fontRegular, language
                )
            )
            palletCardCell.add(
                createInfoRow(
                    when (language) {
                        Language.PL -> "Kod uszkodzenia"; Language.EN -> "Damage Code"
                    }, damageCode, fontBold, fontRegular, language
                )
            )
            val markersForPallet = damageMarkers[pallet.id] ?: emptyList()
            markersForPallet.forEachIndexed { markerIndex, marker ->
                val markerColorName = getMarkerColorName(marker.colorArgb, language)
                val markerText = Text(
                    "${
                        when (language) {
                            Language.PL -> "Znacznik"; Language.EN -> "Marker"
                        }
                    } ${markerIndex + 1} ($markerColorName)\n"
                ).setFont(fontBold).setFontSize(FONT_TINY)
                val detailsParagraph = Paragraph().add(markerText)
                if (language == Language.EN) {
                    marker.assignedDamageIds.forEach { originalId ->
                        val displayText =
                            damageTranslationMap[originalId] ?: "Tłumaczenie niedostępne"
                        detailsParagraph.add(
                            Text("• $displayText\n").setFont(fontRegular).setFontSize(FONT_TINY)
                        )
                    }
                } else {
                    val selectableDamages = pallet.damageInstances.flatMap { di ->
                        di.details.flatMap { d ->
                            d.types.map { t ->
                                val damageText =
                                    if (t.type == "Inne (opis)" && t.description.isNotBlank()) {
                                        t.description
                                    } else {
                                        t.type
                                    }
                                val sizeText = if (t.size.isNotBlank()) " (${t.size} cm)" else ""
                                val displayText = "${d.category}: $damageText$sizeText"
                                SelectableDamage(
                                    id = "${di.id}-${d.category}-${t.type}",
                                    displayText = displayText,
                                    damageInstanceId = di.id,
                                    photoUri = di.photoUri
                                )
                            }
                        }
                    }
                    val assignedDamages =
                        selectableDamages.filter { marker.assignedDamageIds.contains(it.id) }
                    assignedDamages.forEach {
                        detailsParagraph.add(
                            Text("• ${it.displayText}\n").setFont(
                                fontRegular
                            ).setFontSize(FONT_TINY)
                        )
                    }
                }
                palletCardCell.add(detailsParagraph.setMarginBottom(SPACE_1))
            }

            // === POPRAWIONY FRAGMENT - OBSŁUGA BASE64 I AUTOMATYCZNE GENEROWANIE ===
            // Próbuj pobrać gotową bitmapę z URI
            var damageBitmapUri = position?.damageBitmapUri ?: pallet.damageMarkingBitmapUri
            var baseBitmap: Bitmap? = null

            // Jeśli nie ma URI, ale są markery - spróbuj wygenerować z Base64 lub URI zdjęcia palety
            if (damageBitmapUri == null && markersForPallet.isNotEmpty()) {
                // Najpierw spróbuj Base64 (dla wczytanych sesji)
                if (!pallet.zdjecieCalejPaletyBase64.isNullOrBlank()) {
                    try {
                        val imageBytes = android.util.Base64.decode(
                            pallet.zdjecieCalejPaletyBase64,
                            android.util.Base64.DEFAULT
                        )
                        baseBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        Log.d("PdfReportGenerator", "✅ Wczytano zdjęcie palety z Base64 dla palety ${pallet.id}")
                    } catch (e: Exception) {
                        Log.e("PdfReportGenerator", "❌ Błąd dekodowania Base64 zdjęcia palety ${pallet.id}", e)
                    }
                }

                // Jeśli Base64 nie zadziałało, spróbuj URI
                if (baseBitmap == null) {
                    val palletPhotoUri = pallet.zdjecieCalejPaletyUri
                    if (palletPhotoUri != null) {
                        baseBitmap = uriToBitmap(context, palletPhotoUri)
                        if (baseBitmap != null) {
                            Log.d("PdfReportGenerator", "✅ Wczytano zdjęcie palety z URI dla palety ${pallet.id}")
                        }
                    }
                }
            } else if (damageBitmapUri != null) {
                baseBitmap = uriToBitmap(context, damageBitmapUri)
                if (baseBitmap != null) {
                    Log.d("PdfReportGenerator", "✅ Wczytano gotową bitmapę z markerami dla palety ${pallet.id}")
                }
            }

            // Jeśli udało się zdobyć bitmapę (gotową lub bazową do wygenerowania)
            if (baseBitmap != null) {
                val finalBitmap = drawCrossesOnBitmap(baseBitmap, markersForPallet)
                val compressedBitmap = compressBitmapForPdf(finalBitmap, 600)
                val diagramImageData =
                    ImageDataFactory.create(bitmapToByteArray(compressedBitmap))
                val diagramImage = Image(diagramImageData)
                val maxHeightInPoints = 5 * 28.35f
                if (diagramImage.imageHeight > maxHeightInPoints) {
                    val scaleRatio = maxHeightInPoints / diagramImage.imageHeight
                    diagramImage.setHeight(maxHeightInPoints)
                    diagramImage.setWidth(diagramImage.imageWidth * scaleRatio)
                }
                diagramImage.setHorizontalAlignment(HorizontalAlignment.CENTER)
                    .setMarginTop(SPACE_1)
                palletCardCell.add(diagramImage)
                Log.d("PdfReportGenerator", "✅ Dodano obrazek z markerami do PDF dla palety ${pallet.id}")
            } else {
                Log.w("PdfReportGenerator", "⚠️ Nie udało się wczytać żadnego obrazka dla palety ${pallet.id}")
            }
            // === KONIEC POPRAWIONEGO FRAGMENTU ===

            palletLayoutTable.addCell(palletCardCell)
        }
        val remainingCells = numberOfColumns - (savedPallets.size % numberOfColumns)
        if (remainingCells > 0 && remainingCells < numberOfColumns) {
            for (i in 0 until remainingCells) {
                palletLayoutTable.addCell(Cell().setBorder(Border.NO_BORDER))
            }
        }
        contentCell.add(palletLayoutTable)
        sectionTable.addCell(contentCell)
        document.add(sectionTable)
    }

    private fun drawCrossesOnBitmap(baseBitmap: Bitmap, markers: List<DamageMarker>): Bitmap {
        val mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            strokeWidth = 20f; isAntiAlias = true; style = Paint.Style.STROKE; strokeCap =
            Paint.Cap.ROUND
        }
        val crossSize = 40f
        markers.forEach { marker ->
            paint.color = marker.colorArgb
            val x = marker.coordinateX * baseBitmap.width
            val y = marker.coordinateY * baseBitmap.height
            canvas.drawLine(x - crossSize, y - crossSize, x + crossSize, y + crossSize, paint)
            canvas.drawLine(x - crossSize, y + crossSize, x + crossSize, y - crossSize, paint)
        }
        return mutableBitmap
    }

    private fun addEventDescription(
        document: Document,
        reportHeader: ReportHeader,
        fontBold: PdfFont,
        fontRegular: PdfFont,
        language: Language
    ) {
        val sectionTable =
            Table(UnitValue.createPercentArray(floatArrayOf(1f))).useAllAvailableWidth()
                .setBorder(SolidBorder(SUBTLE_BORDER, BORDER_WIDTH))
                .setMarginBottom(SECTION_SPACING)
        val headerCell = createCell(
            null,
            CELL_PADDING,
            VerticalAlignment.MIDDLE
        ).setBackgroundColor(PRIMARY_BLUE).setTextAlignment(TextAlignment.CENTER)
        val sectionTitle = when (language) {
            Language.PL -> "OPIS ZDARZENIA"; Language.EN -> "EVENT DESCRIPTION"
        }
        headerCell.add(
            Paragraph(sectionTitle).setFont(fontBold).setFontSize(FONT_HEADING)
                .setFontColor(ColorConstants.WHITE).setMargin(0f)
        )
        sectionTable.addCell(headerCell)
        val contentCell = createCell(null, CELL_PADDING, VerticalAlignment.TOP).setBackgroundColor(
            BACKGROUND_WHITE
        )
        val noDescriptionText = when (language) {
            Language.PL -> "Brak szczegółowego opisu zdarzenia."; Language.EN -> "No detailed event description."
        }
        val description = reportHeader.opisZdarzenia.ifBlank { noDescriptionText }
        contentCell.add(
            Paragraph(description).setFont(fontRegular).setFontSize(FONT_BODY)
                .setFontColor(TEXT_BLACK)
                .apply { if (reportHeader.opisZdarzenia.isBlank()) setItalic() })
        sectionTable.addCell(contentCell)
        document.add(sectionTable)
    }

    // ===== POCZĄTEK ZMIANY =====
    private fun addSignatureSection(
        document: Document,
        context: Context,
        reportHeader: ReportHeader,
        fontBold: PdfFont,
        fontRegular: PdfFont,
        language: Language
    ) {
        val sectionTable =
            Table(UnitValue.createPercentArray(floatArrayOf(1f))).useAllAvailableWidth()
                .setBorder(SolidBorder(SUBTLE_BORDER, BORDER_WIDTH))
                .setMarginBottom(SECTION_SPACING)
        val headerCell = createCell(
            null,
            CELL_PADDING,
            VerticalAlignment.MIDDLE
        ).setBackgroundColor(PRIMARY_BLUE).setTextAlignment(TextAlignment.CENTER)
        val sectionTitle = when (language) {
            Language.PL -> "POTWIERDZENIE I PODPIS"; Language.EN -> "CONFIRMATION AND SIGNATURE"
        }
        headerCell.add(
            Paragraph(sectionTitle).setFont(fontBold).setFontSize(FONT_HEADING)
                .setFontColor(ColorConstants.WHITE).setMargin(0f)
        )
        sectionTable.addCell(headerCell)
        val contentCell =
            createCell(null, 0f, VerticalAlignment.TOP).setBackgroundColor(BACKGROUND_LIGHT)
        val signatureTable =
            Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f))).useAllAvailableWidth()
                .setBorder(Border.NO_BORDER)

        // --- Komórka 1: Sporządził (Zostaje) ---
        val authorCell = createCell(null, CELL_PADDING, VerticalAlignment.TOP)
        val displayAuthor = reportHeader.workerId?.let { "ID: $it" } ?: "Brak ID"
        val preparedByLabel = when (language) {
            Language.PL -> "Sporządził:"; Language.EN -> "Prepared by:"
        }
        authorCell.add(
            Paragraph(preparedByLabel).setFont(fontBold).setFontSize(FONT_SMALL)
                .setFontColor(TEXT_BLACK).setMarginBottom(SPACE_2)
        ).add(
            Paragraph(displayAuthor).setFont(fontRegular).setFontSize(FONT_BODY)
                .setFontColor(TEXT_BLACK)
        )
        signatureTable.addCell(authorCell)

        // --- Komórka 2: Podpis (Etykieta i obrazek usunięte) ---
        val signatureCell = createCell(
            null,
            CELL_PADDING,
            VerticalAlignment.TOP
        ).setTextAlignment(TextAlignment.CENTER)

        // Usunięto etykietę "Podpis elektroniczny"
        // Usunięto logikę renderowania obrazka podpisu

        // Dodajemy pusty paragraf, aby komórka zachowała wysokość
        signatureCell.add(Paragraph(" ").setFont(fontRegular).setFontSize(FONT_TINY))

        signatureTable.addCell(signatureCell)

        // --- Komórka 3: Data (Zostaje) ---
        val dateCell = createCell(
            null,
            CELL_PADDING,
            VerticalAlignment.TOP
        ).setTextAlignment(TextAlignment.RIGHT)
        val dateText = reportHeader.podpisData?.let {
            SimpleDateFormat(
                "dd.MM.yyyy HH:mm",
                Locale.getDefault()
            ).format(Date(it))
        } ?: SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val dateTimeLabel = when (language) {
            Language.PL -> "Data i czas:"; Language.EN -> "Date and time:"
        }
        dateCell.add(
            Paragraph(dateTimeLabel).setFont(fontBold).setFontSize(FONT_SMALL)
                .setFontColor(TEXT_BLACK).setMarginBottom(SPACE_2)
        ).add(
            Paragraph(dateText).setFont(fontRegular).setFontSize(FONT_BODY).setFontColor(TEXT_BLACK)
        )
        signatureTable.addCell(dateCell)

        contentCell.add(signatureTable)
        sectionTable.addCell(contentCell)
        document.add(sectionTable)
    }
    // ===== KONIEC ZMIANY =====

    @SuppressLint("ResourceType")
    private fun addLegendSection(
        document: Document,
        context: Context,
        fontBold: PdfFont,
        language: Language
    ) {
        val sectionTable =
            Table(UnitValue.createPercentArray(floatArrayOf(1f))).useAllAvailableWidth()
                .setBorder(SolidBorder(SUBTLE_BORDER, BORDER_WIDTH)).setMarginTop(SECTION_SPACING)
        val headerCell = createCell(
            null,
            CELL_PADDING,
            VerticalAlignment.MIDDLE
        ).setBackgroundColor(PRIMARY_BLUE).setTextAlignment(TextAlignment.CENTER)
        val sectionTitle = when (language) {
            Language.PL -> "LEGENDA"; Language.EN -> "LEGEND"
        }
        headerCell.add(
            Paragraph(sectionTitle).setFont(fontBold).setFontSize(FONT_HEADING)
                .setFontColor(ColorConstants.WHITE).setMargin(0f)
        )
        sectionTable.addCell(headerCell)
        val contentCell =
            createCell(null, 0f, VerticalAlignment.TOP).setBackgroundColor(BACKGROUND_WHITE)
        try {
            contentCell.add(
                Image(
                    ImageDataFactory.create(
                        context.resources.openRawResource(R.drawable.legenda).readBytes()
                    )
                ).setWidth(UnitValue.createPercentValue(100f)).setAutoScaleHeight(true)
            )
        } catch (e: Exception) {
            val errorText = when (language) {
                Language.PL -> "Błąd ładowania obrazka legendy."; Language.EN -> "Error loading legend image."
            }
            contentCell.add(Paragraph(errorText).setPadding(CELL_PADDING))
        }
        sectionTable.addCell(contentCell)
        document.add(sectionTable)
    }

    private fun createCell(
        border: Border?,
        padding: Float,
        verticalAlignment: VerticalAlignment
    ): Cell {
        return Cell().setPadding(padding).setVerticalAlignment(verticalAlignment)
            .setBorder(border ?: Border.NO_BORDER)
    }

    private fun generateQRCode(text: String): Bitmap {
        return try {
            val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 80, 80)
            val bitmap =
                Bitmap.createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.RGB_565)
            for (x in 0 until bitMatrix.width) {
                for (y in 0 until bitMatrix.height) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) android.graphics.Color.parseColor("#1F2937") else android.graphics.Color.WHITE
                    )
                }
            }
            bitmap
        } catch (e: Exception) {
            Bitmap.createBitmap(80, 80, Bitmap.Config.RGB_565)
                .apply { eraseColor(android.graphics.Color.WHITE) }
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream)
        return stream.toByteArray()
    }

    private fun uriToBitmap(context: Context, uriString: String): Bitmap? {
        return try {
            if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                context.contentResolver.openInputStream(Uri.parse(uriString))
                    ?.use { BitmapFactory.decodeStream(it) }
            } else {
                File(uriString).takeIf { it.exists() }
                    ?.let { BitmapFactory.decodeFile(it.absolutePath) }
            }
        } catch (e: Exception) {
            Log.e("PdfReportGenerator", "Failed to convert URI/path to bitmap: $uriString", e)
            null
        }
    }

    private fun addVehicleSchematic(
        document: Document,
        reportHeader: ReportHeader,
        savedPallets: List<DamagedPallet>,
        palletPositions: List<PalletPosition>,
        fontBold: PdfFont,
        fontRegular: PdfFont,
        context: Context,
        selectedVehicleLayout: String?,
        language: Language
    ) {
        if (palletPositions.isEmpty()) return
        val sectionTable =
            Table(UnitValue.createPercentArray(floatArrayOf(1f))).useAllAvailableWidth()
                .setBorder(SolidBorder(SUBTLE_BORDER, BORDER_WIDTH))
                .setMarginBottom(SECTION_SPACING)
        val headerCell = createCell(
            null,
            CELL_PADDING,
            VerticalAlignment.MIDDLE
        ).setBackgroundColor(PRIMARY_BLUE).setTextAlignment(TextAlignment.CENTER)
        val sectionTitle = when (language) {
            Language.PL -> "SCHEMAT ROZMIESZCZENIA PALET"; Language.EN -> "PALLET PLACEMENT SCHEMATIC"
        }
        headerCell.add(
            Paragraph(sectionTitle).setFont(fontBold).setFontSize(FONT_HEADING)
                .setFontColor(ColorConstants.WHITE).setMargin(0f)
        )
        sectionTable.addCell(headerCell)
        val contentCell = createCell(null, CELL_PADDING, VerticalAlignment.TOP).setBackgroundColor(
            BACKGROUND_WHITE
        ).setTextAlignment(TextAlignment.CENTER)
        try {
            val schematicBitmap = generateVehicleSchematic(
                reportHeader,
                savedPallets,
                palletPositions,
                context,
                selectedVehicleLayout,
                language
            )
            contentCell.add(
                Image(ImageDataFactory.create(bitmapToByteArray(schematicBitmap))).setMaxWidth(
                    480f
                ).setAutoScaleHeight(true).setHorizontalAlignment(HorizontalAlignment.CENTER)
            )
        } catch (e: Exception) {
            val errorText = when (language) {
                Language.PL -> "Błąd generowania schematu pojazdu"; Language.EN -> "Error generating vehicle schematic"
            }
            contentCell.add(
                Paragraph(errorText).setFont(fontRegular).setFontSize(FONT_BODY)
                    .setFontColor(TEXT_BLACK).setItalic()
            )
        }
        sectionTable.addCell(contentCell)
        document.add(sectionTable)
    }

    private fun generateVehicleSchematic(reportHeader: ReportHeader, savedPallets: List<DamagedPallet>, palletPositions: List<PalletPosition>, context: Context, selectedVehicleLayout: String?, language: Language): Bitmap {
        val vehicleType = selectedVehicleLayout ?: reportHeader.rodzajSamochodu
        val bitmap = Bitmap.createBitmap(1440, 320, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap).apply { drawColor(android.graphics.Color.WHITE) }
        val emptySlotPaint = Paint().apply { style = Paint.Style.FILL; color = android.graphics.Color.parseColor("#F5F5F5"); isAntiAlias = true }
        val emptyBorderPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = android.graphics.Color.parseColor("#E0E0E0"); isAntiAlias = true }
        val selectedSlotPaint = Paint().apply { style = Paint.Style.FILL; color = android.graphics.Color.parseColor("#FFF3E0"); isAntiAlias = true }
        val selectedBorderPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 6f; color = android.graphics.Color.parseColor("#FF9800"); isAntiAlias = true }
        val textPaint = Paint().apply { color = android.graphics.Color.parseColor("#212121"); textSize = 30f; textAlign = Paint.Align.CENTER; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isSubpixelText = true; isLinearText = true }
        val smallTextPaint = Paint().apply { color = android.graphics.Color.parseColor("#FF9800"); textSize = 24f; textAlign = Paint.Align.CENTER; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isSubpixelText = true; isLinearText = true }

        when (vehicleType) {

            "3x11" -> drawHighQualityHorizontalGrid(canvas, 3, 33, palletPositions, savedPallets, 1440, 320, emptySlotPaint, emptyBorderPaint, selectedSlotPaint, selectedBorderPaint, textPaint, smallTextPaint, "3×11", language)
            "2x12" -> drawHighQualityHorizontalGrid(canvas, 2, 24, palletPositions, savedPallets, 1440, 320, emptySlotPaint, emptyBorderPaint, selectedSlotPaint, selectedBorderPaint, textPaint, smallTextPaint, "2×12", language)
            "15x2+1x3" -> drawHighQualityHorizontal15x2Plus3(canvas, palletPositions, savedPallets, 1440, 320, emptySlotPaint, emptyBorderPaint, selectedSlotPaint, selectedBorderPaint, textPaint, smallTextPaint, language)
            "Tandem", "Tandem 3x6+3x6" -> drawHighQualityHorizontalTandem(canvas, palletPositions, savedPallets, 1440, 320, emptySlotPaint, emptyBorderPaint, selectedSlotPaint, selectedBorderPaint, textPaint, smallTextPaint, 3, 18, language)
            "Tandem 2x6+2x6" -> drawHighQualityHorizontalTandem2x6(canvas, palletPositions, savedPallets, 1440, 320, emptySlotPaint, emptyBorderPaint, selectedSlotPaint, selectedBorderPaint, textPaint, smallTextPaint, language)
            "Solo", "Solo 3x6" -> drawHighQualityHorizontalGrid(canvas, 3, 18, palletPositions, savedPallets, 1440, 320, emptySlotPaint, emptyBorderPaint, selectedSlotPaint, selectedBorderPaint, textPaint, smallTextPaint, "SOLO", language)
            "Solo 2x6" -> drawHighQualityHorizontalGrid(canvas, 2, 12, palletPositions, savedPallets, 1440, 320, emptySlotPaint, emptyBorderPaint, selectedSlotPaint, selectedBorderPaint, textPaint, smallTextPaint, "SOLO", language)
            "Bus", "BUS", "bus" -> drawHighQualityHorizontalGrid(canvas, 2, 6, palletPositions, savedPallets, 1440, 320, emptySlotPaint, emptyBorderPaint, selectedSlotPaint, selectedBorderPaint, textPaint, smallTextPaint, "BUS", language)
            "Kontener", "KONTENER", "kontener", "Container" -> drawHighQualityHorizontalGrid(canvas, 2, 24, palletPositions, savedPallets, 1440, 320, emptySlotPaint, emptyBorderPaint, selectedSlotPaint, selectedBorderPaint, textPaint, smallTextPaint, "KONTENER", language)
            else -> drawHighQualityHorizontalGrid(canvas, 3, 18, palletPositions, savedPallets, 1440, 320, emptySlotPaint, emptyBorderPaint, selectedSlotPaint, selectedBorderPaint, textPaint, smallTextPaint, vehicleType.uppercase(), language)
        }

        return compressBitmapForPdf(bitmap, 1200)
    }

    private fun drawHighQualityPalletSlot(
        canvas: Canvas,
        positionNumber: String,
        palletPositions: List<PalletPosition>,
        savedPallets: List<DamagedPallet>,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        emptySlotPaint: Paint,
        emptyBorderPaint: Paint,
        selectedSlotPaint: Paint,
        selectedBorderPaint: Paint,
        textPaint: Paint,
        smallTextPaint: Paint
    ) {
        val positionsOnSlot =
            palletPositions.filter { it.positionOnVehicle == positionNumber || it.positionOnVehicle == "${positionNumber}a" || it.positionOnVehicle == "${positionNumber}b" }
        val rect = RectF(x, y, x + width, y + height)
        val cornerRadius = 9f
        if (positionsOnSlot.isNotEmpty()) {
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, selectedSlotPaint)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, selectedBorderPaint)
            val palletLetters = positionsOnSlot.map { p ->
                val letter = findPalletLetter(p.palletId, savedPallets); when {
                p.positionOnVehicle.endsWith("a") -> "${letter}T"; p.positionOnVehicle.endsWith("b") -> "${letter}B"; else -> letter
            }
            }
            val palletTextPaint = Paint(smallTextPaint).apply {
                textSize = when {
                    width > 90f -> 32f; width > 60f -> 28f; else -> 24f
                }
            }
            canvas.drawText(
                palletLetters.joinToString("|"),
                x + width / 2,
                y + height / 2 + (palletTextPaint.textSize / 3),
                palletTextPaint
            )
        } else {
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, emptySlotPaint)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, emptyBorderPaint)
            if (width > 45f) {
                val emptyTextPaint = Paint(textPaint).apply {
                    color = android.graphics.Color.parseColor("#666666"); typeface =
                    Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); textSize = when {
                    width > 90f -> 24f; width > 60f -> 20f; else -> 16f
                }
                }
                canvas.drawText(
                    positionNumber,
                    x + width / 2,
                    y + height / 2 + (emptyTextPaint.textSize / 3),
                    emptyTextPaint
                )
            }
        }
    }

    private fun drawHighQualityHorizontalGrid(
        canvas: Canvas,
        rows: Int,
        totalSlots: Int,
        palletPositions: List<PalletPosition>,
        savedPallets: List<DamagedPallet>,
        canvasWidth: Int,
        canvasHeight: Int,
        emptySlotPaint: Paint,
        emptyBorderPaint: Paint,
        selectedSlotPaint: Paint,
        selectedBorderPaint: Paint,
        textPaint: Paint,
        smallTextPaint: Paint,
        title: String,
        language: Language
    ) {
        val margin = 24f;
        val titleHeight = 60f;
        val slotSpacing = 6f;
        val labelHeight = 40f
        val availableWidth = canvasWidth - (2 * margin);
        val availableHeight = canvasHeight - titleHeight - (2 * margin) - labelHeight
        val columns = (totalSlots + rows - 1) / rows
        val slotWidth = (availableWidth - ((columns - 1) * slotSpacing)) / columns
        val slotHeight = (availableHeight - ((rows - 1) * slotSpacing)) / rows
        val startY = margin + titleHeight
        val titlePaint = Paint(textPaint).apply { textAlign = Paint.Align.LEFT; textSize = 36f }
        canvas.drawText(title, margin, 45f, titlePaint)

        val frontLabel = when (language) {
            Language.PL -> "PRZÓD →"; Language.EN -> "FRONT →"
        }
        val arrowPaint = Paint(textPaint).apply {
            color = android.graphics.Color.parseColor("#666666"); textSize = 30f; textAlign =
            Paint.Align.LEFT
        }
        canvas.drawText(frontLabel, margin, canvasHeight - margin, arrowPaint)

        for (col in 0 until columns) {
            for (row in 0 until rows) {
                val positionNumber = col * rows + (rows - 1 - row) + 1
                if (positionNumber <= totalSlots) {
                    val pos = positionNumber.toString()
                    val x = margin + col * (slotWidth + slotSpacing)
                    val y = startY + row * (slotHeight + slotSpacing)
                    drawHighQualityPalletSlot(
                        canvas,
                        pos,
                        palletPositions,
                        savedPallets,
                        x,
                        y,
                        slotWidth,
                        slotHeight,
                        emptySlotPaint,
                        emptyBorderPaint,
                        selectedSlotPaint,
                        selectedBorderPaint,
                        textPaint,
                        smallTextPaint
                    )
                }
            }
        }
    }

    private fun drawHighQualityHorizontal15x2Plus3(
        canvas: Canvas,
        palletPositions: List<PalletPosition>,
        savedPallets: List<DamagedPallet>,
        canvasWidth: Int,
        canvasHeight: Int,
        emptySlotPaint: Paint,
        emptyBorderPaint: Paint,
        selectedSlotPaint: Paint,
        selectedBorderPaint: Paint,
        textPaint: Paint,
        smallTextPaint: Paint,
        language: Language
    ) {
        val margin = 24f;
        val titleHeight = 60f;
        val slotSpacing = 6f;
        val sectionSpacing = 18f;
        val labelHeight = 40f
        val availableWidth = canvasWidth - (2 * margin);
        val availableHeight = canvasHeight - titleHeight - (2 * margin) - labelHeight
        val titlePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#212121"); textSize = 36f; textAlign =
            Paint.Align.LEFT; isAntiAlias = true; typeface =
            Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("15x2 + 1x3", margin, 45f, titlePaint)

        val frontLabel = when (language) {
            Language.PL -> "PRZÓD →"; Language.EN -> "FRONT →"
        }
        val arrowPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#666666"); textSize = 30f; textAlign =
            Paint.Align.LEFT; isAntiAlias = true
        }
        canvas.drawText(frontLabel, margin, canvasHeight - margin, arrowPaint)

        val startY = margin + titleHeight
        val mainColumns = 15;
        val mainRows = 2;
        val extraColumns = 1;
        val extraRows = 3
        val gridHeight = availableHeight
        val slotHeight = (gridHeight - slotSpacing) / 2;
        val extraSlotHeight = (gridHeight - ((extraRows - 1) * slotSpacing)) / extraRows
        val extraSlotWidth = slotHeight
        val mainSectionWidth = availableWidth - extraSlotWidth - sectionSpacing
        val mainSlotWidth = (mainSectionWidth - ((mainColumns - 1) * slotSpacing)) / mainColumns
        val mainGridHeight = mainRows * slotHeight + (mainRows - 1) * slotSpacing
        val mainStartY = startY + (gridHeight - mainGridHeight) / 2

        for (col in 0 until mainColumns) {
            for (row in 0 until mainRows) {
                val positionNumber = col * mainRows + (mainRows - 1 - row) + 1
                val x = margin + col * (mainSlotWidth + slotSpacing)
                val y = mainStartY + row * (slotHeight + slotSpacing)
                drawHighQualityPalletSlot(
                    canvas,
                    positionNumber.toString(),
                    palletPositions,
                    savedPallets,
                    x,
                    y,
                    mainSlotWidth,
                    slotHeight,
                    emptySlotPaint,
                    emptyBorderPaint,
                    selectedSlotPaint,
                    selectedBorderPaint,
                    textPaint,
                    smallTextPaint
                )
            }
        }

        val extraStartX = margin + mainSectionWidth + sectionSpacing
        val extraStartY = startY
        for (position in 31..33) {
            val row = position - 31
            val x = extraStartX
            val y = extraStartY + row * (extraSlotHeight + slotSpacing)
            drawHighQualityPalletSlot(
                canvas,
                position.toString(),
                palletPositions,
                savedPallets,
                x,
                y,
                extraSlotWidth,
                extraSlotHeight,
                emptySlotPaint,
                emptyBorderPaint,
                selectedSlotPaint,
                selectedBorderPaint,
                textPaint,
                smallTextPaint
            )
        }
    }

    private fun addPageNumbers(pdfDoc: PdfDocument, font: PdfFont, language: Language) {
        try {
            val totalPages = pdfDoc.numberOfPages
            Log.d(TAG, "[addPageNumbers] Dokument ma $totalPages stron.")
            for (i in 1..totalPages) {
                val page = pdfDoc.getPage(i)

                val pageSize = page.pageSize
                if (pageSize == null || pageSize.width == 0f) {
                    Log.w(
                        TAG,
                        "[addPageNumbers] Nie można pobrać rozmiaru dla strony $i. Pomijam numerację tej strony."
                    )
                    continue
                }

                val pageText = when (language) {
                    Language.PL -> "Strona $i z $totalPages"; Language.EN -> "Page $i of $totalPages"
                }
                val textWidth = font.getWidth(pageText, FONT_SMALL)
                val boxWidth = textWidth + 10f
                val boxHeight = FONT_SMALL + 10f
                val x = (pageSize.width - boxWidth) / 2
                val y = 25f

                val canvas = PdfCanvas(page.newContentStreamAfter(), page.resources, pdfDoc)
                canvas.saveState()
                    .setFillColor(BACKGROUND_WHITE)
                    .setStrokeColor(SUBTLE_BORDER)
                    .setLineWidth(BORDER_WIDTH)
                    .roundRectangle(
                        x.toDouble(),
                        y.toDouble(),
                        boxWidth.toDouble(),
                        boxHeight.toDouble(),
                        4.0
                    )
                    .fillStroke()
                    .restoreState()

                canvas.beginText()
                    .setFontAndSize(font, FONT_SMALL)
                    .moveText((x + 5f).toDouble(), (y + 5f + 1f).toDouble())
                    .showText(pageText)
                    .endText()
            }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "[addPageNumbers] Wystąpił nieoczekiwany błąd podczas dodawania numeracji stron. Numeracja może być niekompletna.",
                e
            )
        }
    }

    private fun drawHighQualityHorizontalTandem(
        canvas: Canvas, palletPositions: List<PalletPosition>, savedPallets: List<DamagedPallet>, canvasWidth: Int, canvasHeight: Int,
        emptySlotPaint: Paint, emptyBorderPaint: Paint, selectedSlotPaint: Paint, selectedBorderPaint: Paint, textPaint: Paint, smallTextPaint: Paint, columns: Int, slotsPerSection: Int, language: Language
    ) {
        val margin = 24f; val titleHeight = 60f; val sectionSpacing = 30f; val slotSpacing = 6f; val labelHeight = 40f
        val titlePaint = Paint().apply { color = android.graphics.Color.parseColor("#212121"); textSize = 36f; textAlign = Paint.Align.LEFT; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isSubpixelText = true; isLinearText = true }
        canvas.drawText("TANDEM", margin, 45f, titlePaint)

        val frontLabel = when(language) { Language.PL -> "← PRZÓD"; Language.EN -> "← FRONT" }
        val rearLabel = when(language) { Language.PL -> "TYŁ →"; Language.EN -> "REAR →" }
        val arrowPaint = Paint(textPaint).apply { color = android.graphics.Color.parseColor("#666666"); textSize = 30f }
        arrowPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(frontLabel, margin, canvasHeight - margin, arrowPaint)
        arrowPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(rearLabel, canvasWidth - margin, canvasHeight - margin, arrowPaint)

        val sectionWidth = (canvasWidth - 2 * margin - sectionSpacing) / 2
        val availableHeight = canvasHeight - titleHeight - (2 * margin) - labelHeight

        val rows = 3
        val actualColumns = 6

        val slotWidth = (sectionWidth - ((actualColumns - 1) * slotSpacing)) / actualColumns
        val slotHeight = (availableHeight - ((rows - 1) * slotSpacing)) / rows
        val startY = margin + titleHeight + 15f

        val sectionLabelPaint = Paint().apply { color = android.graphics.Color.parseColor("#1976D2"); textSize = 30f; textAlign = Paint.Align.CENTER; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isSubpixelText = true }
        val vehicleLabel = when(language) { Language.PL -> "POJAZD"; Language.EN -> "VEHICLE" }
        val trailerLabel = when(language) { Language.PL -> "PRZYCZEPA"; Language.EN -> "TRAILER" }

        val vehicleX = margin
        canvas.drawText(vehicleLabel, vehicleX + sectionWidth / 2, startY - 15f, sectionLabelPaint)

        for (col in 0 until actualColumns) {
            for (row in 0 until rows) {
                val positionNumber = col * rows + (rows - 1 - row) + 1
                val x = vehicleX + col * (slotWidth + slotSpacing)
                val y = startY + row * (slotHeight + slotSpacing)
                drawHighQualityPalletSlot(canvas, positionNumber.toString(), palletPositions, savedPallets, x, y, slotWidth, slotHeight, emptySlotPaint, emptyBorderPaint, selectedSlotPaint, selectedBorderPaint, textPaint, smallTextPaint)
            }
        }

        val trailerX = margin + sectionWidth + sectionSpacing
        canvas.drawText(trailerLabel, trailerX + sectionWidth / 2, startY - 15f, sectionLabelPaint)

        for (col in 0 until actualColumns) {
            for (row in 0 until rows) {
                val positionNumber = col * rows + (rows - 1 - row) + slotsPerSection + 1
                val x = trailerX + col * (slotWidth + slotSpacing)
                val y = startY + row * (slotHeight + slotSpacing)
                drawHighQualityPalletSlot(canvas, positionNumber.toString(), palletPositions, savedPallets, x, y, slotWidth, slotHeight, emptySlotPaint, emptyBorderPaint, selectedSlotPaint, selectedBorderPaint, textPaint, smallTextPaint)
            }
        }
    }

    private fun drawHighQualityHorizontalTandem2x6(
        canvas: Canvas, palletPositions: List<PalletPosition>, savedPallets: List<DamagedPallet>, canvasWidth: Int, canvasHeight: Int,
        emptySlotPaint: Paint, emptyBorderPaint: Paint, selectedSlotPaint: Paint, selectedBorderPaint: Paint, textPaint: Paint, smallTextPaint: Paint, language: Language
    ) {
        val margin = 24f; val titleHeight = 60f; val sectionSpacing = 30f; val slotSpacing = 6f; val labelHeight = 40f
        val titlePaint = Paint().apply { color = android.graphics.Color.parseColor("#212121"); textSize = 36f; textAlign = Paint.Align.LEFT; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isSubpixelText = true; isLinearText = true }
        canvas.drawText("TANDEM 2×6", margin, 45f, titlePaint)

        val frontLabel = when(language) { Language.PL -> "← PRZÓD"; Language.EN -> "← FRONT" }
        val rearLabel = when(language) { Language.PL -> "TYŁ →"; Language.EN -> "REAR →" }
        val arrowPaint = Paint(textPaint).apply { color = android.graphics.Color.parseColor("#666666"); textSize = 30f }
        arrowPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(frontLabel, margin, canvasHeight - margin, arrowPaint)
        arrowPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(rearLabel, canvasWidth - margin, canvasHeight - margin, arrowPaint)

        val sectionWidth = (canvasWidth - 2 * margin - sectionSpacing) / 2
        val availableHeight = canvasHeight - titleHeight - (2 * margin) - labelHeight

        val rows = 2
        val columns = 6

        val slotWidth = (sectionWidth - ((columns - 1) * slotSpacing)) / columns
        val slotHeight = (availableHeight - ((rows - 1) * slotSpacing)) / rows
        val startY = margin + titleHeight + 15f

        val sectionLabelPaint = Paint().apply { color = android.graphics.Color.parseColor("#1976D2"); textSize = 30f; textAlign = Paint.Align.CENTER; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isSubpixelText = true }
        val vehicleLabel = when(language) { Language.PL -> "POJAZD"; Language.EN -> "VEHICLE" }
        val trailerLabel = when(language) { Language.PL -> "PRZYCZEPA"; Language.EN -> "TRAILER" }

        val vehicleX = margin
        canvas.drawText(vehicleLabel, vehicleX + sectionWidth / 2, startY - 15f, sectionLabelPaint)

        for (col in 0 until columns) {
            for (row in 0 until rows) {
                val positionNumber = col * rows + (rows - 1 - row) + 1
                val x = vehicleX + col * (slotWidth + slotSpacing)
                val y = startY + row * (slotHeight + slotSpacing)
                drawHighQualityPalletSlot(canvas, positionNumber.toString(), palletPositions, savedPallets, x, y, slotWidth, slotHeight, emptySlotPaint, emptyBorderPaint, selectedSlotPaint, selectedBorderPaint, textPaint, smallTextPaint)
            }
        }

        val trailerX = margin + sectionWidth + sectionSpacing
        canvas.drawText(trailerLabel, trailerX + sectionWidth / 2, startY - 15f, sectionLabelPaint)

        for (col in 0 until columns) {
            for (row in 0 until rows) {
                val positionNumber = col * rows + (rows - 1 - row) + 13
                val x = trailerX + col * (slotWidth + slotSpacing)
                val y = startY + row * (slotHeight + slotSpacing)
                drawHighQualityPalletSlot(canvas, positionNumber.toString(), palletPositions, savedPallets, x, y, slotWidth, slotHeight, emptySlotPaint, emptyBorderPaint, selectedSlotPaint, selectedBorderPaint, textPaint, smallTextPaint)
            }
        }
    }
}