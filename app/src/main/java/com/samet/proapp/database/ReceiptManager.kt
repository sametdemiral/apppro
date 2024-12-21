// ReceiptManager.kt
package com.samet.proapp.database

import android.content.Context
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object ReceiptManager {
    private const val RECEIPTS_FILE = "receipts.xml"

    // Varolan yöntemler...

    // Yeni eklenen updateReceipts metodu
    fun updateReceipts(context: Context, receipts: List<Receipt>) {
        val serializer = Xml.newSerializer()
        context.openFileOutput(RECEIPTS_FILE, Context.MODE_PRIVATE).use { outputStream ->
            serializer.run {
                setOutput(outputStream, "UTF-8")
                startDocument("UTF-8", true)
                startTag("", "receipts")

                for (receipt in receipts) {
                    startTag("", "receipt")

                    startTag("", "name")
                    text(receipt.name)
                    endTag("", "name")

                    startTag("", "imagePath")
                    text(receipt.imagePath)
                    endTag("", "imagePath")

                    startTag("", "date")
                    text(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(receipt.date))
                    endTag("", "date")

                    endTag("", "receipt")
                }

                endTag("", "receipts")
                endDocument()
            }
        }
    }

    fun saveReceipt(context: Context, receipt: Receipt) {
        val receipts = loadReceipts(context).toMutableList()
        receipts.add(receipt)
        updateReceipts(context, receipts)
    }

    fun deleteReceipt(context: Context, receipt: Receipt) {
        val receipts = loadReceipts(context).toMutableList()
        receipts.remove(receipt)
        updateReceipts(context, receipts)

        // Fiş fotoğrafını da sil
        File(receipt.imagePath).delete()
    }

    fun loadReceipts(context: Context): List<Receipt> {
        if (!File(context.filesDir, RECEIPTS_FILE).exists()) {
            return emptyList()
        }

        val receipts = mutableListOf<Receipt>()
        context.openFileInput(RECEIPTS_FILE).use { inputStream ->
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var currentName = ""
            var currentImagePath = ""
            var currentDate = Date()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "name" -> currentName = parser.nextText()
                            "imagePath" -> currentImagePath = parser.nextText()
                            "date" -> currentDate = SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss",
                                Locale.getDefault()
                            ).parse(parser.nextText()) ?: Date()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "receipt") {
                            receipts.add(Receipt(currentName, currentImagePath, currentDate))
                        }
                    }
                }
                eventType = parser.next()
            }
        }
        return receipts
    }
}