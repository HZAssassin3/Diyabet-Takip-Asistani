package com.example.insulinneedlereminder.ui.insulin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.insulinneedlereminder.R
import com.example.insulinneedlereminder.data.db.AppDatabase
import com.example.insulinneedlereminder.data.repository.InsulinRepository
import com.example.insulinneedlereminder.databinding.FragmentInsulinBinding

class InsulinFragment : Fragment() {

    private var _binding: FragmentInsulinBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InsulinViewModel by activityViewModels {
        val db = AppDatabase.getInstance(requireContext())
        InsulinViewModelFactory(InsulinRepository(db.insulinDao()))
    }

    private lateinit var adapter: InsulinAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsulinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeRecords()
        setupFab()
    }

    private fun setupRecyclerView() {
        adapter = InsulinAdapter(
            onCheckChanged = { record, isChecked ->
                if (isChecked) viewModel.markDone(record)
                else viewModel.markUndone(record)
            },
            onDelete = { record ->
                viewModel.delete(record)
                Toast.makeText(requireContext(), "Kayıt silindi", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvInsulinRecords.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInsulinRecords.adapter = adapter
    }

    private fun observeRecords() {
        viewModel.allRecords.observe(viewLifecycleOwner) { records ->
            adapter.submitList(records)
            binding.tvEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupFab() {
        binding.fabAddInsulin.setOnClickListener {
            showAddDialog()
        }
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_insulin, null)

        val rgTimeLabel = dialogView.findViewById<RadioGroup>(R.id.rgTimeLabel)
        val etUnits = dialogView.findViewById<EditText>(R.id.etUnits)
        val etNote = dialogView.findViewById<EditText>(R.id.etDialogNote)

        AlertDialog.Builder(requireContext())
            .setTitle("💉 İnsülin Kaydı Ekle")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                val timeLabel = when (rgTimeLabel.checkedRadioButtonId) {
                    R.id.rbSabah  -> "Sabah"
                    R.id.rbOgle   -> "Öğle"
                    R.id.rbAksam  -> "Akşam"
                    else          -> "Sabah"
                }
                val unitsStr = etUnits.text.toString().trim()
                if (unitsStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Ünite giriniz", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val units = unitsStr.toIntOrNull() ?: 0
                val note = etNote.text.toString().trim()
                viewModel.addRecord(timeLabel, units, note)
                Toast.makeText(requireContext(), "İğne kaydedildi ✓", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}