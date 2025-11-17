package com.example.hdm.services

import com.example.hdm.model.JapanControlReport
import com.example.hdm.model.JapanReportHeader
import com.example.hdm.model.PalletEntry
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

class JapanReportParser {

    fun parse(xmlString: String): JapanControlReport {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlString))

        var header = JapanReportHeader()
        val pallets = mutableListOf<PalletEntry>()
        var currentPallet: MutableMap<String, String>? = null
        var pdfReportBase64: String? = null // Zmienna na dane PDF

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> when {
                    tagName.equals("Header", ignoreCase = true) -> {}
                    tagName.equals("Order", ignoreCase = true) -> header.order = readText(parser)
                    tagName.equals("Feniks", ignoreCase = true) -> header.feniks = readText(parser)
                    tagName.equals("HDL", ignoreCase = true) -> header.hdlStatus = readText(parser)
                    tagName.equals("WHWORKER", ignoreCase = true) -> header.whWorker = readText(parser)
                    tagName.equals("PDF_Report", ignoreCase = true) -> pdfReportBase64 = readText(parser) // Odczyt PDF
                    tagName.equals("PalletEntry", ignoreCase = true) -> {
                        currentPallet = mutableMapOf()
                    }
                    currentPallet != null && tagName != null -> {
                        currentPallet[tagName] = readText(parser)
                    }
                }
                XmlPullParser.END_TAG -> if (tagName.equals("PalletEntry", ignoreCase = true) && currentPallet != null) {
                    pallets.add(
                        PalletEntry(
                            carrierNumber = currentPallet["CarrierNumber"] ?: "",
                            status = currentPallet["Status"] ?: "",
                            whWorkerHalf = currentPallet["WHWORKERHALF"],
                            nokPhoto1 = currentPallet["NOKPhoto_1"],
                            nokPhoto2 = currentPallet["NOKPhoto_2"],
                            nokPhoto3 = currentPallet["NOKPhoto_3"],
                            nokPhoto4 = currentPallet["NOKPhoto_4"]
                        )
                    )
                    currentPallet = null
                }
            }
            eventType = parser.next()
        }
        return JapanControlReport(header, pallets, pdfReportBase64)
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }
}