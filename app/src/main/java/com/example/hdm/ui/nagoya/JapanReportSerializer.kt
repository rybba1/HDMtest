package com.example.hdm.services

import android.content.Context
import com.example.hdm.model.JapanControlReport
import org.xmlpull.v1.XmlSerializer
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.StringWriter

class JapanReportSerializer {

    suspend fun serialize(context: Context, report: JapanControlReport): String {
        return withContext(Dispatchers.IO) {
            val serializer: XmlSerializer = Xml.newSerializer()
            val writer = StringWriter()

            serializer.setOutput(writer)
            serializer.startDocument("utf-8", true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag(null, "JapanControl")

            serializer.startTag(null, "Header")
            serializer.startTag(null, "Order").text(report.header.order).endTag(null, "Order")
            serializer.startTag(null, "Feniks").text(report.header.feniks).endTag(null, "Feniks")
            serializer.startTag(null, "HDL").text(report.header.hdlStatus).endTag(null, "HDL")
            serializer.startTag(null, "WHWORKER").text(report.header.whWorker).endTag(null, "WHWORKER")
            serializer.endTag(null, "Header")

            serializer.startTag(null, "Pallets")
            report.pallets.forEach { pallet ->
                serializer.startTag(null, "PalletEntry")
                serializer.startTag(null, "CarrierNumber").text(pallet.carrierNumber).endTag(null, "CarrierNumber")
                serializer.startTag(null, "Status").text(pallet.status).endTag(null, "Status")

                pallet.whWorkerHalf?.let { serializer.startTag(null, "WHWORKERHALF").text(it).endTag(null, "WHWORKERHALF") }

                pallet.nokPhoto1?.let { uriString ->
                    ImageUtils.convertUriToBase64(context, uriString)?.let { base64 ->
                        serializer.startTag(null, "NOKPhoto_1").text(base64).endTag(null, "NOKPhoto_1")
                    }
                }
                pallet.nokPhoto2?.let { uriString ->
                    ImageUtils.convertUriToBase64(context, uriString)?.let { base64 ->
                        serializer.startTag(null, "NOKPhoto_2").text(base64).endTag(null, "NOKPhoto_2")
                    }
                }
                pallet.nokPhoto3?.let { uriString ->
                    ImageUtils.convertUriToBase64(context, uriString)?.let { base64 ->
                        serializer.startTag(null, "NOKPhoto_3").text(base64).endTag(null, "NOKPhoto_3")
                    }
                }
                pallet.nokPhoto4?.let { uriString ->
                    ImageUtils.convertUriToBase64(context, uriString)?.let { base64 ->
                        serializer.startTag(null, "NOKPhoto_4").text(base64).endTag(null, "NOKPhoto_4")
                    }
                }

                serializer.endTag(null, "PalletEntry")
            }
            serializer.endTag(null, "Pallets")

            serializer.endTag(null, "JapanControl")
            serializer.endDocument()

            writer.toString()
        }
    }
}