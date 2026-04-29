package com.example.insulinneedlereminder.ui.report

import android.content.Intent
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReportBinding.bind(view)

        setupButtons()
        observeData()
    }

    private fun observeData() {
        glucoseViewModel.allRecords.observe(viewLifecycleOwner) { records ->
            if (records.isNotEmpty()) {
                val values = records.map { it.value }
                binding.tvAvg.text = "${values.average().toInt()}"
                binding.tvMin.text = "${values.min()}"
                binding.tvMax.text = "${values.max()}"
                binding.tvTotal.text = "${records.size}"

                val lowCount = records.count { GlucoseStatus.from(it.value) == GlucoseStatus.LOW }
                val normalCount = records.count { GlucoseStatus.from(it.value) == GlucoseStatus.NORMAL }
                val highCount = records.count { GlucoseStatus.from(it.value) == GlucoseStatus.HIGH }
                binding.tvLow.text = "$lowCount"
                binding.tvNormal.text = "$normalCount"
                binding.tvHigh.text = "$highCount"

                binding.tvPeriod7.text = formatPeriodStats(records, 7)
                binding.tvPeriod14.text = formatPeriodStats(records, 14)
                binding.tvPeriod30.text = formatPeriodStats(records, 30)

                binding.layoutStats.visibility = View.VISIBLE
                binding.tvNoData.visibility = View.GONE
            } else {
                binding.layoutStats.visibility = View.GONE
                binding.tvNoData.visibility = View.VISIBLE
            }
        }
    }

    private fun formatPeriodStats(records: List<GlucoseRecord>, days: Int): String {
        val fromDate = DateUtils.daysAgo(days)
        val scoped = records.filter { it.date >= fromDate }
        if (scoped.isEmpty()) {
            return "$days gun: kayit yok"
        }
        val avg = scoped.map { it.value }.average().toInt()
        val low = scoped.count { GlucoseStatus.from(it.value) == GlucoseStatus.LOW }
        val high = scoped.count { GlucoseStatus.from(it.value) == GlucoseStatus.HIGH }
        val lowPct = (low * 100) / scoped.size
        val highPct = (high * 100) / scoped.size
        return "$days gun: ort $avg mg/dL • dusuk %$lowPct • yuksek %$highPct"
    }

    private fun setupButtons() {
        binding.btnWeeklyReport.setOnClickListener { generateAndShare("Haftalik", 7) }
        binding.btnMonthlyReport.setOnClickListener { generateAndShare("Aylik", 30) }
    }

    private fun generateAndShare(period: String, days: Int) {
        binding.progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fromDate = DateUtils.daysAgo(days)
                val filteredGlucose = glucoseViewModel.allRecords.value?.filter { it.date >= fromDate } ?: emptyList()
                val filteredInsulin = insulinViewModel.allRecords.value?.filter { it.date >= fromDate } ?: emptyList()

                val context = context ?: return@launch
                val file = PdfReportGenerator.generate(context, filteredGlucose, filteredInsulin, period)

                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext
                    binding.progressBar.visibility = View.GONE
                    sharePdf(file)
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

    private fun sharePdf(file: java.io.File) {
        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Raporu Paylas veya Görüntüle"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}