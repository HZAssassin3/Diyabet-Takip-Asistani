package com.example.insulinneedlereminder.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.insulinneedlereminder.data.db.AppDatabase
import com.example.insulinneedlereminder.data.entity.GlucoseRecord
import com.example.insulinneedlereminder.data.repository.GlucoseRepository
import com.example.insulinneedlereminder.databinding.FragmentHistoryBinding
import com.example.insulinneedlereminder.ui.glucose.GlucoseAdapter
import com.example.insulinneedlereminder.ui.glucose.GlucoseViewModel
import com.example.insulinneedlereminder.ui.glucose.GlucoseViewModelFactory
import com.example.insulinneedlereminder.ui.widget.GlucoseWidget
import com.example.insulinneedlereminder.util.GlucoseStatus
import com.example.insulinneedlereminder.util.PrefsManager

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GlucoseViewModel by activityViewModels {
        val db = AppDatabase.getInstance(requireContext())
        GlucoseViewModelFactory(GlucoseRepository(db.glucoseDao()))
    }

    private lateinit var adapter: GlucoseAdapter
    private var allRecords: List<GlucoseRecord> = emptyList()
    private var filteredRecords: List<GlucoseRecord> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyGlucoseThresholds()

        // --- GERİ BUTONU AYARI ---
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        setupRecyclerView()
        observeRecords()
        setupFilters()
    }

    private fun setupRecyclerView() {
        adapter = GlucoseAdapter { record ->
            viewModel.delete(record)
            GlucoseWidget.sendRefreshBroadcast(requireContext())
        }
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter
    }

    private fun observeRecords() {
        viewModel.allRecords.observe(viewLifecycleOwner) { records ->
            allRecords = records
            filteredRecords = records
            adapter.submitList(records)
            binding.tvEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupFilters() {
        binding.btnAll.setOnClickListener {
            filteredRecords = allRecords
            adapter.submitList(filteredRecords)
        }
        binding.btnLow.setOnClickListener {
            filteredRecords = allRecords.filter { viewModel.getStatus(it.value) == GlucoseStatus.LOW }
            adapter.submitList(filteredRecords)
        }
        binding.btnNormal.setOnClickListener {
            filteredRecords = allRecords.filter { viewModel.getStatus(it.value) == GlucoseStatus.NORMAL }
            adapter.submitList(filteredRecords)
        }
        binding.btnHigh.setOnClickListener {
            filteredRecords = allRecords.filter { viewModel.getStatus(it.value) == GlucoseStatus.HIGH }
            adapter.submitList(filteredRecords)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun applyGlucoseThresholds() {
        val prefs = PrefsManager(requireContext())
        viewModel.setThresholds(
            low = prefs.glucoseLowThreshold,
            high = prefs.glucoseHighThreshold
        )
    }
}