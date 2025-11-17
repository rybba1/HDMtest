package com.example.hdm.services

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class PdfPrintDocumentAdapter(
    private val context: Context,
    private val filePath: String
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        // Reaguj na anulowanie
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }

        // Zbuduj informacje o dokumencie. W przypadku PDF jest to proste - 1 dokument.
        val info = PrintDocumentInfo.Builder("raport_hdm.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN) // System sam policzy strony z PDF
            .build()

        // Poinformuj system, że układ jest gotowy.
        callback.onLayoutFinished(info, newAttributes != oldAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        var input: FileInputStream? = null
        var output: FileOutputStream? = null

        try {
            val file = File(filePath)
            input = FileInputStream(file)
            output = FileOutputStream(destination.fileDescriptor)

            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (input.read(buffer).also { bytesRead = it } >= 0) {
                // Sprawdzaj, czy proces nie został anulowany
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }
                output.write(buffer, 0, bytesRead)
            }

            // Poinformuj system, że zapis został ukończony pomyślnie
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))

        } catch (e: IOException) {
            Log.e("PdfPrintAdapter", "Błąd podczas zapisu pliku PDF do druku", e)
            callback.onWriteFailed(e.toString())
        } finally {
            try {
                input?.close()
                output?.close()
            } catch (e: IOException) {
                Log.e("PdfPrintAdapter", "Błąd podczas zamykania strumieni", e)
            }
        }
    }
}