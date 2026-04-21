package com.example.insulinneedlereminder.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.insulinneedlereminder.R
import com.example.insulinneedlereminder.data.db.AppDatabase
import com.example.insulinneedlereminder.data.repository.GlucoseRepository
import com.example.insulinneedlereminder.data.repository.InsulinRepository
import com.example.insulinneedlereminder.databinding.FragmentHomeBinding
import com.example.insulinneedlereminder.ui.glucose.GlucoseViewModel
import com.example.insulinneedlereminder.ui.glucose.GlucoseViewModelFactory
import com.example.insulinneedlereminder.ui.insulin.InsulinViewModel
import com.example.insulinneedlereminder.ui.insulin.InsulinViewModelFactory
import com.example.insulinneedlereminder.util.DateUtils
import com.example.insulinneedlereminder.util.GlucoseStatus
import com.example.insulinneedlereminder.util.PrefsManager
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val insulinViewModel: InsulinViewModel by activityViewModels {
        val db = AppDatabase.getInstance(requireContext())
        InsulinViewModelFactory(InsulinRepository(db.insulinDao()))
    }

    private val glucoseViewModel: GlucoseViewModel by activityViewModels {
        val db = AppDatabase.getInstance(requireContext())
        GlucoseViewModelFactory(GlucoseRepository(db.glucoseDao()))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyGlucoseThresholds()
        setupDate()
        observeInsulin()
        observeGlucose()
        setupButtons()
        setupDarkMode()
    }

    private fun setupDate() {
        val today = DateUtils.formatDate(System.currentTimeMillis())
        val dayName = getDayName()
        binding.tvDate.text = "$dayName, $today"
    }

    private fun getDayName(): String {
        val days = arrayOf(
            "Pazar", "Pazartesi", "Salı",
            "Çarşamba", "Perşembe", "Cuma", "Cumartesi"
        )
        return days[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]
    }

    private fun observeInsulin() {
        insulinViewModel.todayRecords.observe(viewLifecycleOwner) { records ->
            val done = records.count { it.isDone }
            val total = records.size
            binding.tvInsulinStatus.text = "Bugün: $done / $total iğne yapıldı"

            val sabah = records.find { it.timeLabel == "Sabah" }
            val ogle = records.find { it.timeLabel == "Öğle" }
            val aksam = records.find { it.timeLabel == "Akşam" }

            binding.ivMorningCheck.visibility =
                if (sabah?.isDone == true) View.VISIBLE else View.GONE
            binding.tvMorningPending.visibility =
                if (sabah?.isDone == true) View.GONE else View.VISIBLE

            binding.ivNoonCheck.visibility =
                if (ogle?.isDone == true) View.VISIBLE else View.GONE
            binding.tvNoonPending.visibility =
                if (ogle?.isDone == true) View.GONE else View.VISIBLE

            binding.ivEveningCheck.visibility =
                if (aksam?.isDone == true) View.VISIBLE else View.GONE
            binding.tvEveningPending.visibility =
                if (aksam?.isDone == true) View.GONE else View.VISIBLE
        }
    }

    private fun observeGlucose() {
        glucoseViewModel.todayRecords.observe(viewLifecycleOwner) { records ->
            if (records.isEmpty()) {
                binding.tvLastGlucose.text = "Bugün ölçüm yapılmadı"
                binding.tvGlucoseWarning.visibility = View.GONE
                return@observe
            }
            val latest = records.first()
            binding.tvLastGlucose.text =
                "Son ölçüm: ${latest.value} mg/dL  •  ${DateUtils.formatTime(latest.date)}"

            when (glucoseViewModel.getStatus(latest.value)) {
                GlucoseStatus.LOW -> {
                    binding.tvGlucoseWarning.text = "⚠️ Düşük şeker! (${latest.value} mg/dL)"
                    binding.tvGlucoseWarning.setTextColor(
                        resources.getColor(R.color.glucose_low, null)
                    )
                    binding.tvGlucoseWarning.visibility = View.VISIBLE
                }
                GlucoseStatus.HIGH -> {
                    binding.tvGlucoseWarning.text = "⚠️ Yüksek şeker! (${latest.value} mg/dL)"
                    binding.tvGlucoseWarning.setTextColor(
                        resources.getColor(R.color.glucose_high, null)
                    )
                    binding.tvGlucoseWarning.visibility = View.VISIBLE
                }
                GlucoseStatus.NORMAL -> {
                    binding.tvGlucoseWarning.visibility = View.GONE
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnAddGlucose.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_glucose)
        }
        binding.btnAddInsulin.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_insulin)
        }
    }

    private fun setupDarkMode() {
        val prefs = requireContext().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("is_dark", false)

        // Animasyonsuz başlangıç
        updateThemeButtonInstant(isDark)

        binding.layoutThemeToggle.setOnClickListener {
            val newDark = !prefs.getBoolean("is_dark", false)
            prefs.edit().putBoolean("is_dark", newDark).apply()

            // Animasyonlu geçiş
            updateThemeButton(newDark)

            // Animasyon bittikten sonra temayı değiştir
            binding.layoutThemeToggle.postDelayed({
                if (newDark) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }, 350)
        }
    }

    // Animasyonlu - sadece tıklamada kullan
    private fun updateThemeButton(isDark: Boolean) {
        binding.layoutThemeToggle.post {
            val toggleWidth = binding.layoutThemeToggle.width.toFloat()
            val circleWidth = binding.viewCircle.width.toFloat()
            val moveDistance = toggleWidth - circleWidth - 6f

            if (isDark) {
                binding.layoutThemeToggle.setBackgroundResource(R.drawable.bg_toggle_night)
                binding.tvMoon.text = "🌙"
                binding.viewCircle.animate().translationX(0f).setDuration(300).start()
                binding.tvMoon.animate().translationX(0f).setDuration(300).start()
            } else {
                binding.layoutThemeToggle.setBackgroundResource(R.drawable.bg_toggle_day)
                binding.tvMoon.text = "☀️"
                binding.viewCircle.animate().translationX(moveDistance).setDuration(300).start()
                binding.tvMoon.animate().translationX(-moveDistance).setDuration(300).start()
            }
        }
    }

    // Animasyonsuz - sadece başlangıçta kullan
    private fun updateThemeButtonInstant(isDark: Boolean) {
        binding.layoutThemeToggle.post {
            val toggleWidth = binding.layoutThemeToggle.width.toFloat()
            val circleWidth = binding.viewCircle.width.toFloat()
            val moveDistance = toggleWidth - circleWidth - 6f

            if (isDark) {
                binding.layoutThemeToggle.setBackgroundResource(R.drawable.bg_toggle_night)
                binding.tvMoon.text = "🌙"
                binding.viewCircle.translationX = 0f
                binding.tvMoon.translationX = 0f
            } else {
                binding.layoutThemeToggle.setBackgroundResource(R.drawable.bg_toggle_day)
                binding.tvMoon.text = "☀️"
                binding.viewCircle.translationX = moveDistance
                binding.tvMoon.translationX = -moveDistance
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun applyGlucoseThresholds() {
        val prefs = PrefsManager(requireContext())
        glucoseViewModel.setThresholds(
            low = prefs.glucoseLowThreshold,
            high = prefs.glucoseHighThreshold
        )
    }
}