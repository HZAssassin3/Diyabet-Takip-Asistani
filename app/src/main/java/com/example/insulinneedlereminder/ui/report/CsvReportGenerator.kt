package com.example.insulinneedlereminder.ui.report

import android.content.Context
import android.os.Environment
import com.example.insulinneedlereminder.data.entity.GlucoseRecord
import com.example.insulinneedlereminder.data.entity.InsulinRecord
import com.example.insulinneedlereminder.util.DateUtils
import com.example.insulinneedlereminder.util.GlucoseStatus
import java.io.File

object CsvReportGenerator {

    fun generate(
        context: Context,
        glucoseRecords: List<GlucoseRecord>,
        insulinRecords: List<InsulinRecord>,
        period: String
    ): File {
        val fileName = "Insu_Rapor_${period}_${System.currentTimeMillis()}.csv"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        val low = glucoseRecords.count { GlucoseStatus.from(it.value) == GlucoseStatus.LOW }
        val normal = glucoseRecords.count { GlucoseStatus.from(it.value) == GlucoseStatus.NORMAL }
        val high = glucoseRecords.count { GlucoseStatus.from(it.value) == GlucoseStatus.HIGH }
        val total = glucoseRecords.size
        val tirPercent = if (total == 0) 0 else (normal * 100) / total

        val insulinTotal = insulinRecords.sumOf { it.units }
        val morningTotal = insulinRecords.filter { it.timeLabel.contains("Sabah", ignoreCase = true) }.sumOf { it.units }
        val noonTotal = insulinRecords.filter { it.timeLabel.contains("Öğle", ignoreCase = true) || it.timeLabel.contains("Ogle", ignoreCase = true) }.sumOf { it.units }
        val eveningTotal = insulinRecords.filter { it.timeLabel.contains("Akşam", ignoreCase = true) || it.timeLabel.contains("Aksam", ignoreCase = true) }.sumOf { it.units }

        val builder = StringBuilder()
        builder.appendLine("Periyot,$period")
        builder.appendLine("Oluşturma Tarihi,${DateUtils.formatDate(System.currentTimeMillis())}")
        builder.appendLine()
        builder.appendLine("Kan Şekeri Özeti")
        builder.appendLine("Toplam Kayıt,$total")
        builder.appendLine("Ortalama mg/dL,${if (total == 0) "-" else glucoseRecords.map { it.value }.average().toInt()}")
        builder.appendLine("Min mg/dL,${if (total == 0) "-" else glucoseRecords.minOf { it.value }}")
        builder.appendLine("Max mg/dL,${if (total == 0) "-" else glucoseRecords.maxOf { it.value }}")
        builder.appendLine("TIR (70-180),%$tirPercent")
        builder.appendLine("Düşük,$low")
        builder.appendLine("Normal,$normal")
        builder.appendLine("Yüksek,$high")
        builder.appendLine()
        builder.appendLine("İnsülin Özeti")
        builder.appendLine("Toplam Ünite,$insulinTotal")
        builder.appendLine("Sabah Ünite,$morningTotal")
        builder.appendLine("Öğle Ünite,$noonTotal")
        builder.appendLine("Akşam Ünite,$eveningTotal")
        builder.appendLine()
        builder.appendLine("Kan Şekeri Kayıtları")
        builder.appendLine("TarihSaat,Değer,Durum,Not")
        glucoseRecords.forEach { record ->
            val status = when (GlucoseStatus.from(record.value)) {
                GlucoseStatus.LOW -> "Düşük"
                GlucoseStatus.NORMAL -> "Normal"
                GlucoseStatus.HIGH -> "Yüksek"
            }
            builder.appendLine("${DateUtils.formatDateTime(record.date)},${record.value},$status,${sanitize(record.note)}")
        }
        builder.appendLine()
        builder.appendLine("İnsülin Kayıtları")
        builder.appendLine("TarihSaat,Öğün,Ünite,Not")
        insulinRecords.forEach { record ->
            builder.appendLine("${DateUtils.formatDateTime(record.date)},${sanitize(record.timeLabel)},${record.units},${sanitize(record.note)}")
        }

        file.writeText(builder.toString())
        return file
    }

    private fun sanitize(value: String): String = value.replace(",", " ").replace("\n", " ").trim()
}
