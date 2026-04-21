package com.example.insulinneedlereminder.ui.glucose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.insulinneedlereminder.R
import com.example.insulinneedlereminder.data.db.AppDatabase
import com.example.insulinneedlereminder.data.repository.GlucoseRepository
import com.example.insulinneedlereminder.databinding.FragmentGlucoseBinding
import com.example.insulinneedlereminder.util.GlucoseStatus
import com.example.insulinneedlereminder.util.PrefsManager

class GlucoseFragment : Fragment() {

    private var _binding: FragmentGlucoseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GlucoseViewModel by activityViewModels {
        val db = AppDatabase.getInstance(requireContext())
        GlucoseViewModelFactory(GlucoseRepository(db.glucoseDao()))
    }

    private lateinit var adapter: GlucoseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGlucoseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyGlucoseThresholds()
        setupToolbar()
        setupRecyclerView()
        observeRecords()
        setupSaveButton()
        setupViewAllButton()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = GlucoseAdapter { record ->
            viewModel.delete(record)
            Toast.makeText(requireContext(), "Kayıt silindi", Toast.LENGTH_SHORT).show()
        }
        binding.rvGlucoseRecords.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGlucoseRecords.adapter = adapter
    }

    private fun observeRecords() {
        viewModel.allRecords.observe(viewLifecycleOwner) { records ->
            // En yeni 5 kaydı göster
            val last5 = records.sortedByDescending { it.date }.take(5)
            adapter.submitList(last5)
            binding.tvEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveGlucose.setOnClickListener {
            val valueStr = binding.etGlucoseValue.text.toString().trim()

            if (valueStr.isEmpty()) {
                binding.etGlucoseValue.error = "Değer giriniz"
                return@setOnClickListener
            }

            val value = valueStr.toIntOrNull()
            if (value == null || value < 20 || value > 600) {
                binding.etGlucoseValue.error = "Geçerli bir değer giriniz (20-600)"
                return@setOnClickListener
            }

            val mealStatus = if (binding.rbBefore.isChecked) "yemek_oncesi" else "yemek_sonrasi"
            val note = binding.etNote.text.toString().trim()

            viewModel.insert(value, mealStatus, note)

            when (viewModel.getStatus(value)) {
                GlucoseStatus.LOW -> Toast.makeText(
                    requireContext(),
                    "Düşük şeker! Lütfen bir şeyler yiyin.",
                    Toast.LENGTH_LONG
                ).show()
                GlucoseStatus.HIGH -> Toast.makeText(
                    requireContext(),
                    "Yüksek şeker! Doktorunuza danışın.",
                    Toast.LENGTH_LONG
                ).show()
                GlucoseStatus.NORMAL -> Toast.makeText(
                    requireContext(),
                    "Değer normal aralıkta kaydedildi.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Formu temizle
            binding.etGlucoseValue.text?.clear()
            binding.etNote.text?.clear()
            binding.rbAfter.isChecked = true
        }
    }

    private fun setupViewAllButton() {
        binding.btnViewAll.setOnClickListener {
            findNavController().navigate(R.id.action_glucose_to_history)
        }
    }

    private fun applyGlucoseThresholds() {
        val prefs = PrefsManager(requireContext())
        viewModel.setThresholds(
            low = prefs.glucoseLowThreshold,
            high = prefs.glucoseHighThreshold
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}