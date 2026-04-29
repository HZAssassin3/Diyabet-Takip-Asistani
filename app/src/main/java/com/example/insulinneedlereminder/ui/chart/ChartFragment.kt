package com.example.insulinneedlereminder.ui.chart

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.insulinneedlereminder.data.db.AppDatabase
import com.example.insulinneedlereminder.data.entity.GlucoseRecord
import com.example.insulinneedlereminder.data.entity.InsulinRecord
import com.example.insulinneedlereminder.data.repository.GlucoseRepository
import com.example.insulinneedlereminder.data.repository.InsulinRepository
import com.example.insulinneedlereminder.databinding.FragmentChartBinding
import com.example.insulinneedlereminder.ui.glucose.GlucoseViewModel
import com.example.insulinneedlereminder.ui.glucose.GlucoseViewModelFactory
import com.example.insulinneedlereminder.ui.insulin.InsulinViewModel
import com.example.insulinneedlereminder.ui.insulin.InsulinViewModelFactory
import com.example.insulinneedlereminder.util.DateUtils
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter

class ChartFragment : Fragment() {

    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!

    private val glucoseViewModel: GlucoseViewModel by activityViewModels {
        val db = AppDatabase.getInstance(requireContext())
        GlucoseViewModelFactory(GlucoseRepository(db.glucoseDao()))
    }

    private val insulinViewModel: InsulinViewModel by activityViewModels {
        val db = AppDatabase.getInstance(requireContext())
        InsulinViewModelFactory(InsulinRepository(db.insulinDao()))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNoDataTexts()
        observeGlucose()
        observeInsulin()
    }

    private fun setupNoDataTexts() {
        binding.glucoseChart.setNoDataText("Grafik verisi bulunamadı")
        binding.insulinChart.setNoDataText("Grafik verisi bulunamadı")
    }

    private fun observeGlucose() {
        glucoseViewModel.lastRecords.observe(viewLifecycleOwner) { records ->
            if (records.isEmpty()) {
                binding.glucoseChart.clear()
                binding.glucoseChart.invalidate()
                binding.tvAverage.text = "-"
                binding.tvMin.text = "-"
                binding.tvMax.text = "-"
                return@observe
            }
            // Grafik tamamen çizildikten sonra setup et
            binding.glucoseChart.post {
                setupGlucoseChart(records)
                setupStats(records)
            }
        }
    }

    private fun observeInsulin() {
        insulinViewModel.allRecords.observe(viewLifecycleOwner) { records ->
            if (records.isEmpty()) {
                binding.insulinChart.clear()
                binding.insulinChart.invalidate()
                return@observe
            }
            binding.insulinChart.post {
                setupInsulinChart(records)
            }
        }
    }

    private fun setupGlucoseChart(records: List<GlucoseRecord>) {
        val entries = records.reversed().mapIndexed { index, record ->
            Entry(index.toFloat(), record.value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Kan Şekeri (mg/dL)").apply {
            color = Color.parseColor("#2196F3")
            valueTextColor = Color.parseColor("#212121")
            lineWidth = 2f
            circleRadius = 4f
            setCircleColor(Color.parseColor("#2196F3"))
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        binding.glucoseChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = true
            setTouchEnabled(true)
            setPinchZoom(true)

            axisLeft.removeAllLimitLines()
            axisLeft.addLimitLine(
                LimitLine(70f, "Düşük").apply {
                    lineColor = Color.parseColor("#F44336")
                    lineWidth = 1f
                    textColor = Color.parseColor("#F44336")
                }
            )
            axisLeft.addLimitLine(
                LimitLine(180f, "Yüksek").apply {
                    lineColor = Color.parseColor("#FF9800")
                    lineWidth = 1f
                    textColor = Color.parseColor("#FF9800")
                }
            )

            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index >= 0 && index < records.reversed().size)
                        DateUtils.formatDate(records.reversed()[index].date)
                    else ""
                }
            }
            xAxis.labelRotationAngle = -45f
            axisRight.isEnabled = false
            notifyDataSetChanged()
            invalidate()
        }
    }

    private fun setupStats(records: List<GlucoseRecord>) {
        if (records.isEmpty()) return
        val values = records.map { it.value }
        val avg = values.average().toInt()
        val min = values.min()
        val max = values.max()

        binding.tvAverage.text = avg.toString()
        binding.tvMin.text = min.toString()
        binding.tvMax.text = max.toString()
    }

    private fun setupInsulinChart(records: List<InsulinRecord>) {
        val grouped = records.reversed()
            .takeLast(14)
            .groupBy { DateUtils.formatDate(it.date) }

        val entries = grouped.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.sumOf { it.units }.toFloat())
        }

        val labels = grouped.keys.toList()

        val dataSet = BarDataSet(entries, "Günlük İnsülin (ünite)").apply {
            color = Color.parseColor("#00BCD4")
            valueTextColor = Color.parseColor("#212121")
        }

        binding.insulinChart.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            legend.isEnabled = true
            setTouchEnabled(true)

            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index >= 0 && index < labels.size) labels[index] else ""
                }
            }
            xAxis.labelRotationAngle = -45f
            axisRight.isEnabled = false
            notifyDataSetChanged()
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}