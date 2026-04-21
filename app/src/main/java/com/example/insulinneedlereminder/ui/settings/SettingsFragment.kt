package com.example.insulinneedlereminder.ui.settings

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.insulinneedlereminder.alarm.AlarmScheduler
import com.example.insulinneedlereminder.databinding.FragmentSettingsBinding
import com.example.insulinneedlereminder.util.PrefsManager

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: PrefsManager

    private var morningHour = 8
    private var morningMinute = 0
    private var noonHour = 12
    private var noonMinute = 0
    private var eveningHour = 19
    private var eveningMinute = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PrefsManager(requireContext())
        setupToolbar()
        loadSettings()
        setupTimePickers()
        setupSaveButton()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadSettings() {
        // Sabah
        morningHour = prefs.morningHour
        morningMinute = prefs.morningMinute
        binding.switchMorning.isChecked = prefs.morningEnabled
        binding.btnMorningTime.text = formatTime(morningHour, morningMinute)
        binding.etMorningUnits.setText(if (prefs.morningUnits > 0) prefs.morningUnits.toString() else "")

        // Öğle
        noonHour = prefs.noonHour
        noonMinute = prefs.noonMinute
        binding.switchNoon.isChecked = prefs.noonEnabled
        binding.btnNoonTime.text = formatTime(noonHour, noonMinute)
        binding.etNoonUnits.setText(if (prefs.noonUnits > 0) prefs.noonUnits.toString() else "")

        // Akşam
        eveningHour = prefs.eveningHour
        eveningMinute = prefs.eveningMinute
        binding.switchEvening.isChecked = prefs.eveningEnabled
        binding.btnEveningTime.text = formatTime(eveningHour, eveningMinute)
        binding.etEveningUnits.setText(if (prefs.eveningUnits > 0) prefs.eveningUnits.toString() else "")
    }

    private fun setupTimePickers() {
        binding.btnMorningTime.setOnClickListener {
            TimePickerDialog(requireContext(), { _, hour, minute ->
                morningHour = hour
                morningMinute = minute
                binding.btnMorningTime.text = formatTime(hour, minute)
            }, morningHour, morningMinute, true).show()
        }

        binding.btnNoonTime.setOnClickListener {
            TimePickerDialog(requireContext(), { _, hour, minute ->
                noonHour = hour
                noonMinute = minute
                binding.btnNoonTime.text = formatTime(hour, minute)
            }, noonHour, noonMinute, true).show()
        }

        binding.btnEveningTime.setOnClickListener {
            TimePickerDialog(requireContext(), { _, hour, minute ->
                eveningHour = hour
                eveningMinute = minute
                binding.btnEveningTime.text = formatTime(hour, minute)
            }, eveningHour, eveningMinute, true).show()
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveSettings.setOnClickListener {
            saveSettings()
            scheduleAlarms()
            Toast.makeText(requireContext(), "✓ Ayarlar kaydedildi!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSettings() {
        // Sabah
        prefs.morningEnabled = binding.switchMorning.isChecked
        prefs.morningHour = morningHour
        prefs.morningMinute = morningMinute
        prefs.morningUnits = binding.etMorningUnits.text.toString().toIntOrNull() ?: 0

        // Öğle
        prefs.noonEnabled = binding.switchNoon.isChecked
        prefs.noonHour = noonHour
        prefs.noonMinute = noonMinute
        prefs.noonUnits = binding.etNoonUnits.text.toString().toIntOrNull() ?: 0

        // Akşam
        prefs.eveningEnabled = binding.switchEvening.isChecked
        prefs.eveningHour = eveningHour
        prefs.eveningMinute = eveningMinute
        prefs.eveningUnits = binding.etEveningUnits.text.toString().toIntOrNull() ?: 0
    }

    private fun scheduleAlarms() {
        val context = requireContext()

        if (prefs.morningEnabled) {
            AlarmScheduler.schedule(
                context, morningHour, morningMinute,
                "Sabah", prefs.morningUnits,
                AlarmScheduler.RequestCode.MORNING
            )
        } else {
            AlarmScheduler.cancel(context, AlarmScheduler.RequestCode.MORNING)
        }

        if (prefs.noonEnabled) {
            AlarmScheduler.schedule(
                context, noonHour, noonMinute,
                "Öğle", prefs.noonUnits,
                AlarmScheduler.RequestCode.NOON
            )
        } else {
            AlarmScheduler.cancel(context, AlarmScheduler.RequestCode.NOON)
        }

        if (prefs.eveningEnabled) {
            AlarmScheduler.schedule(
                context, eveningHour, eveningMinute,
                "Akşam", prefs.eveningUnits,
                AlarmScheduler.RequestCode.EVENING
            )
        } else {
            AlarmScheduler.cancel(context, AlarmScheduler.RequestCode.EVENING)
        }
    }

    private fun formatTime(hour: Int, minute: Int): String =
        String.format("%02d:%02d", hour, minute)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}