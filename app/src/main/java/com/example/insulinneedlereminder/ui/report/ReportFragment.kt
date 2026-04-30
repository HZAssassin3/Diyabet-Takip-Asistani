package com.example.insulinneedlereminder.ui.report

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.insulinneedlereminder.R
import com.example.insulinneedlereminder.databinding.FragmentReportBinding
import com.example.insulinneedlereminder.data.entity.GlucoseRecord
import com.example.insulinneedlereminder.ui.glucose.GlucoseViewModel
import com.example.insulinneedlereminder.ui.insulin.InsulinViewModel
import com.example.insulinneedlereminder.util.DateUtils
import com.example.insulinneedlereminder.util.GlucoseStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReportFragment : Fragment(R.layout.fragment_report) {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    private val glucoseViewModel: GlucoseViewModel by activityViewModels()
    private val insulinViewModel: InsulinViewModel by activityViewModels()
    private var latestGlucoseRecords: List<GlucoseRecord> = emptyList()
    private var latestInsulinRecords: List<com.example.insulinneedlereminder.data.entity.InsulinRecord> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReportBinding.bind(view)

        setupButtons()
        observeData()
    }

    private fun observeData() {
        glucoseViewModel.allRecords.observe(viewLifecycleOwner) { records ->
            latestGlucoseRecords = records
            renderStats()
        }
        insulinViewModel.allRecords.observe(viewLifecycleOwner) { records ->
            latestInsulinRecords = records
            renderStats()
        }
    }

    private fun renderStats() {
        val glucoseRecords = latestGlucoseRecords
        val insulinRecords = latestInsulinRecords
        if (glucoseRecords.isEmpty()) {
            binding.layoutStats.visibility = View.GONE
            binding.tvNoData.visibility = View.VISIBLE
            return
        }

        val values = glucoseRecords.map { it.value }
        binding.tvAvg.text = "${values.average().toInt()}"
        binding.tvMin.text = "${values.minOrNull() ?: "-"}"
        binding.tvMax.text = "${values.maxOrNull() ?: "-"}"
        binding.tvTotal.text = "${glucoseRecords.size}"

        val lowCount = glucoseRecords.count { GlucoseStatus.from(it.value) == GlucoseStatus.LOW }
        val normalCount = glucoseRecords.count { GlucoseStatus.from(it.value) == GlucoseStatus.NORMAL }
        val highCount = glucoseRecords.count { GlucoseStatus.from(it.value) == GlucoseStatus.HIGH }
        binding.tvLow.text = "$lowCount"
        binding.tvNormal.text = "$normalCount"
        binding.tvHigh.text = "$highCount"

        val tirPercent = (normalCount * 100) / glucoseRecords.size
        binding.tvTimeInRange.text = "%$tirPercent  •  Düşük: $lowCount  Normal: $normalCount  Yüksek: $highCount"

        val insulinTotal = insulinRecords.sumOf { it.units }
        val morning = insulinRecords.filter { it.timeLabel.contains("Sabah", ignoreCase = true) }.sumOf { it.units }
        val noon = insulinRecords.filter { it.timeLabel.contains("Öğle", ignoreCase = true) || it.timeLabel.contains("Ogle", ignoreCase = true) }.sumOf { it.units }
        val evening = insulinRecords.filter { it.timeLabel.contains("Akşam", ignoreCase = true) || it.timeLabel.contains("Aksam", ignoreCase = true) }.sumOf { it.units }
        binding.tvInsulinSummary.text = "Toplam: $insulinTotal u  •  Sabah: $morning u  •  Öğle: $noon u  •  Akşam: $evening u"

        binding.tvPeriod7.text = formatPeriodStats(glucoseRecords, 7)
        binding.tvPeriod14.text = formatPeriodStats(glucoseRecords, 14)
        binding.tvPeriod30.text = formatPeriodStats(glucoseRecords, 30)

        binding.layoutStats.visibility = View.VISIBLE
        binding.tvNoData.visibility = View.GONE
    }

    private fun formatPeriodStats(records: List<GlucoseRecord>, days: Int): String {
        val fromDate = DateUtils.daysAgo(days)
        val scoped = records.filter { it.date >= fromDate }
        if (scoped.isEmpty()) {
            return "$days gün: kayıt yok"
        }
        val avg = scoped.map { it.value }.average().toInt()
        val low = scoped.count { GlucoseStatus.from(it.value) == GlucoseStatus.LOW }
        val high = scoped.count { GlucoseStatus.from(it.value) == GlucoseStatus.HIGH }
        val lowPct = (low * 100) / scoped.size
        val highPct = (high * 100) / scoped.size
        return "$days gün: ort $avg mg/dL • düşük %$lowPct • yüksek %$highPct"
    }

    private fun setupButtons() {
        binding.btnWeeklyReport.setOnClickListener { generateAndShare("Haftalık", 7) }
        binding.btnMonthlyReport.setOnClickListener { generateAndShare("Aylık", 30) }
    }

    private fun generateAndShare(period: String, days: Int) {
        binding.progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fromDate = DateUtils.daysAgo(days)
                val filteredGlucose = glucoseViewModel.allRecords.value?.filter { it.date >= fromDate } ?: emptyList()
                val filteredInsulin = insulinViewModel.allRecords.value?.filter { it.date >= fromDate } ?: emptyList()

                val context = context ?: return@launch
                val pdfFile = PdfReportGenerator.generate(context, filteredGlucose, filteredInsulin, period)
                val csvFile = CsvReportGenerator.generate(context, filteredGlucose, filteredInsulin, period)

                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext
                    binding.progressBar.visibility = View.GONE
                    shareReports(pdfFile, csvFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun shareReports(pdfFile: java.io.File, csvFile: java.io.File) {
        val pdfUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", pdfFile)
        val csvUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", csvFile)
        val uris = arrayListOf<Uri>(pdfUri, csvUri)
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.report_share_chooser)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}