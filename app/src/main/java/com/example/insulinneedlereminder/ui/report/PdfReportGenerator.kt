package com.example.insulinneedlereminder.ui.report

import android.content.Context
import android.os.Environment
import com.example.insulinneedlereminder.data.entity.GlucoseRecord
import com.example.insulinneedlereminder.data.entity.InsulinRecord
import com.example.insulinneedlereminder.util.DateUtils
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File

object PdfReportGenerator {

    fun generate(
        context: Context,
        glucoseRecords: List<GlucoseRecord>,
        insulinRecords: List<InsulinRecord>,
        period: String
    ): File {
        val fileName = "Insu_Rapor_${period}_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        val writer = PdfWriter(file)
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc)

        // Türkçe karakter desteği için font ayarı
        val font = PdfFontFactory.createFont(StandardFonts.HELVETICA, "CP1254")
        val boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD, "CP1254")
        document.setFont(font)

        // BAŞLIK
        document.add(
            Paragraph("Insülin ve Kan Sekeri Raporu")
                .setFont(boldFont)
                .setFontSize(20f)
                .setTextAlignment(TextAlignment.CENTER)
        )

        document.add(
            Paragraph("Periyot: $period  •  Olusturma Tarihi: ${DateUtils.formatDate(System.currentTimeMillis())}")
                .setFontSize(11f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20f)
        )

        // KAN ŞEKERİ ÖZETİ VE TABLOSU
        if (glucoseRecords.isNotEmpty()) {
            document.add(Paragraph("Kan Sekeri Ozeti").setFont(boldFont).setFontSize(14f))

            val values = glucoseRecords.map { it.value }
            val statsTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f, 1f)))
                .useAllAvailableWidth().setMarginBottom(10f)

            listOf("Ort", "Min", "Max", "Kayit").forEach {
                statsTable.addCell(Cell().add(Paragraph(it).setFont(boldFont)).setBackgroundColor(ColorConstants.LIGHT_GRAY))
            }
            statsTable.addCell("${values.average().toInt()} mg/dL")
            statsTable.addCell("${values.min()} mg/dL")
            statsTable.addCell("${values.max()} mg/dL")
            statsTable.addCell("${glucoseRecords.size}")
            document.add(statsTable)

            // Detay Tablosu
            val glucoseTable = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f, 1f, 2f))).useAllAvailableWidth()
            listOf("Tarih & Saat", "Deger", "Durum", "Not").forEach {
                glucoseTable.addHeaderCell(Cell().add(Paragraph(it).setFont(boldFont)).setBackgroundColor(ColorConstants.LIGHT_GRAY))
            }

            glucoseRecords.forEach { record ->
                glucoseTable.addCell(DateUtils.formatDateTime(record.date))
                glucoseTable.addCell("${record.value} mg/dL")
                val statusText = if (record.value < 70) "Dusuk" else if (record.value > 180) "Yuksek" else "Normal"
                glucoseTable.addCell(statusText)
                glucoseTable.addCell(record.note.ifEmpty { "-" })
            }
            document.add(glucoseTable.setMarginBottom(20f))
        }

        // İNSÜLİN KAYITLARI TABLOSU
        if (insulinRecords.isNotEmpty()) {
            document.add(Paragraph("Insulin Uygulama Kayitlari").setFont(boldFont).setFontSize(14f))
            val insulinTable = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f, 1f, 2f))).useAllAvailableWidth()

            listOf("Tarih & Saat", "Ogun", "Unite", "Not").forEach {
                insulinTable.addHeaderCell(Cell().add(Paragraph(it).setFont(boldFont)).setBackgroundColor(ColorConstants.LIGHT_GRAY))
            }

            insulinRecords.forEach { record ->
                insulinTable.addCell(DateUtils.formatDateTime(record.date))
                insulinTable.addCell(record.timeLabel)
                insulinTable.addCell("${record.units}")
                insulinTable.addCell(record.note.ifEmpty { "-" })
            }
            document.add(insulinTable)
        }

        document.add(Paragraph("\nBu rapor mobil uygulama tarafindan otomatik üretilmistir.")
            .setFontSize(9f).setTextAlignment(TextAlignment.CENTER).setFontColor(ColorConstants.GRAY))

        document.close()
        return file
    }
}