package com.samet.proapp.database

import android.content.Context
import com.samet.proapp.model.Group
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
//this application saved data in csv file
//so uninstall app last data in successful phone storage.
//ann preinstallation for get data in app. (not freebase)
class CSVHelper(private val context: Context) {

    private val baseDir: File = File(context.getExternalFilesDir(null), "ProApp")

    init {
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
    }
    fun getAllCSVFiles(): List<File> {
        val csvFiles = mutableListOf<File>()
        baseDir.walkTopDown().forEach { file ->
            if (file.extension == "csv") {
                csvFiles.add(file)
            }
        }
        return csvFiles
    }
    fun saveBarcode(group: Group, barcodeValue: String, title: String) {
        val groupDir = File(baseDir, "Group_${group.id}_${group.name.replace(" ", "_")}")
        if (!groupDir.exists()) {
            groupDir.mkdirs()
        }

        val fileName = "barcodes_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.csv"
        val file = File(groupDir, fileName)

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val csvLine = "$timestamp,$barcodeValue,$title\n"

        if (!file.exists()) {
            file.createNewFile()
            file.appendText("Timestamp,Barcode,Title\n")
        }
        file.appendText(csvLine)
    }

    fun getGroupCSVFiles(group: Group): List<File> {
        val groupDir = File(baseDir, "Group_${group.id}_${group.name.replace(" ", "_")}")
        return groupDir.listFiles { file -> file.extension == "csv" }?.toList() ?: emptyList()
    }

    fun getCSVContent(file: File): List<Triple<String, String, String>> {
        val content = mutableListOf<Triple<String, String, String>>()
        file.readLines().drop(1).forEach { line ->
            val parts = line.split(",")
            if (parts.size >= 3) {
                content.add(Triple(parts[0], parts[1], parts[2]))
            }
        }
        return content
    }

    fun deleteBarcodeFromCSV(group: Group, barcode: Triple<String, String, String>): Boolean {
        val groupDir = File(baseDir, "Group_${group.id}_${group.name.replace(" ", "_")}")
        if (!groupDir.exists()) return false

        var deleted = false
        groupDir.listFiles { file -> file.extension == "csv" }?.forEach { file ->
            val lines = file.readLines().toMutableList()
            val index = lines.indexOfFirst { it.contains(barcode.second) && it.contains(barcode.third) }
            if (index != -1) {
                lines.removeAt(index)
                file.writeText(lines.joinToString("\n"))
                deleted = true
                return@forEach
            }
        }
        return deleted
    }
}