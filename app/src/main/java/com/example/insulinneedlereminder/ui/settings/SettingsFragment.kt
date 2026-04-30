package com.example.insulinneedlereminder.ui.settings

import android.app.TimePickerDialog
import androidx.appcompat.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.room.withTransaction
import com.example.insulinneedlereminder.alarm.AlarmScheduler
import com.example.insulinneedlereminder.data.db.AppDatabase
import com.example.insulinneedlereminder.data.entity.GlucoseRecord
import com.example.insulinneedlereminder.data.entity.InsulinRecord
import com.example.insulinneedlereminder.databinding.FragmentSettingsBinding
import com.example.insulinneedlereminder.util.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: PrefsManager
    private lateinit var db: AppDatabase

    private var morningHour = 8
    private var morningMinute = 0
    private var noonHour = 12
    private var noonMinute = 0
    private var eveningHour = 19
    private var eveningMinute = 0
    private var replaceExistingOnImport = true

    private val createBackupLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                exportBackupToUri(uri)
            }
        }

    private val importBackupLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                importBackupFromUri(uri, replaceExistingOnImport)
            }
        }

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
        db = AppDatabase.getInstance(requireContext())
        loadSettings()
        setupTimePickers()
        setupSaveButton()
        setupBackupButtons()
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

    private fun setupBackupButtons() {
        binding.btnExportBackup.setOnClickListener {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            createBackupLauncher.launch("insulin_backup_$stamp.json")
        }
        binding.btnImportBackup.setOnClickListener {
            showImportModeDialog()
        }
        binding.btnOpenReports.setOnClickListener {
            findNavController().navigate(com.example.insulinneedlereminder.R.id.action_settings_to_report)
        }
    }

    private fun showImportModeDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Yedek geri yükleme")
            .setMessage("Yükleme türünü seçin")
            .setPositiveButton("Sil ve yükle") { _, _ ->
                replaceExistingOnImport = true
                importBackupLauncher.launch(arrayOf("application/json", "text/plain"))
            }
            .setNegativeButton("Üstüne ekle") { _, _ ->
                replaceExistingOnImport = false
                importBackupLauncher.launch(arrayOf("application/json", "text/plain"))
            }
            .setNeutralButton("İptal", null)
            .show()
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

    private fun exportBackupToUri(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val insulin = db.insulinDao().getAllDirect()
                val glucose = db.glucoseDao().getAllDirect()
                val payload = JSONObject().apply {
                    put("version", 1)
                    put("exportedAt", System.currentTimeMillis())
                    put("insulinRecords", insulin.toInsulinJsonArray())
                    put("glucoseRecords", glucose.toGlucoseJsonArray())
                }.toString()

                val resolver = context?.contentResolver ?: return@launch
                resolver.openOutputStream(uri)?.use { output ->
                    output.write(payload.toByteArray(Charsets.UTF_8))
                } ?: throw IllegalStateException("Dosya yazılamadı")

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Yedek oluşturuldu (${insulin.size + glucose.size} kayıt)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Yedek alınırken hata: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun importBackupFromUri(uri: Uri, replaceExisting: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resolver = context?.contentResolver ?: return@launch
                val raw = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("Dosya okunamadı")
                val json = JSONObject(raw)
                val insulin = json.optJSONArray("insulinRecords").toInsulinRecords()
                val glucose = json.optJSONArray("glucoseRecords").toGlucoseRecords()

                db.withTransaction {
                    if (replaceExisting) {
                        db.insulinDao().clearAll()
                        db.glucoseDao().clearAll()
                    }
                    if (insulin.isNotEmpty()) db.insulinDao().insertAll(insulin)
                    if (glucose.isNotEmpty()) db.glucoseDao().insertAll(glucose)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Yedek yüklendi (${insulin.size + glucose.size} kayıt, ${
                            if (replaceExisting) "silip yükleme" else "üstüne ekleme"
                        })",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Yedek yüklenirken hata: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun List<InsulinRecord>.toInsulinJsonArray(): JSONArray = JSONArray().apply {
        forEach { record ->
            put(
                JSONObject().apply {
                    put("id", record.id)
                    put("date", record.date)
                    put("timeLabel", record.timeLabel)
                    put("units", record.units)
                    put("isDone", record.isDone)
                    put("note", record.note)
                }
            )
        }
    }

    private fun List<GlucoseRecord>.toGlucoseJsonArray(): JSONArray = JSONArray().apply {
        forEach { record ->
            put(
                JSONObject().apply {
                    put("id", record.id)
                    put("date", record.date)
                    put("value", record.value)
                    put("mealStatus", record.mealStatus)
                    put("note", record.note)
                }
            )
        }
    }

    private fun JSONArray?.toInsulinRecords(): List<InsulinRecord> {
        if (this == null) return emptyList()
        val list = mutableListOf<InsulinRecord>()
        for (i in 0 until length()) {
            val item = optJSONObject(i) ?: continue
            list.add(
                InsulinRecord(
                    id = item.optInt("id", 0),
                    date = item.optLong("date", System.currentTimeMillis()),
                    timeLabel = item.optString("timeLabel", "Sabah"),
                    units = item.optInt("units", 0),
                    isDone = item.optBoolean("isDone", false),
                    note = item.optString("note", "")
                )
            )
        }
        return list
    }

    private fun JSONArray?.toGlucoseRecords(): List<GlucoseRecord> {
        if (this == null) return emptyList()
        val list = mutableListOf<GlucoseRecord>()
        for (i in 0 until length()) {
            val item = optJSONObject(i) ?: continue
            list.add(
                GlucoseRecord(
                    id = item.optInt("id", 0),
                    date = item.optLong("date", System.currentTimeMillis()),
                    value = item.optInt("value", 0),
                    mealStatus = item.optString("mealStatus", "yemek_sonrasi"),
                    note = item.optString("note", "")
                )
            )
        }
        return list
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}