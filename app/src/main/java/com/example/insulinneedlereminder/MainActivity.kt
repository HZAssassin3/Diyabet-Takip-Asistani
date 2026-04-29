package com.example.insulinneedlereminder

import android.app.AlarmManager
import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.NavHostFragment
import com.example.insulinneedlereminder.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val uiHandler = Handler(Looper.getMainLooper())
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled by system UI */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val systemDark = (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        if (!prefs.contains("is_dark")) {
            prefs.edit().putBoolean("is_dark", systemDark).apply()
        }
        val isDark = prefs.getBoolean("is_dark", false)
        val targetNightMode = if (isDark) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        if (AppCompatDelegate.getDefaultNightMode() != targetNightMode) {
            AppCompatDelegate.setDefaultNightMode(targetNightMode)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            if (navController.currentDestination?.id == item.itemId) {
                return@setOnItemSelectedListener true
            }
            navController.navigate(
                item.itemId,
                null,
                androidx.navigation.NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .setPopUpTo(
                        navController.graph.findStartDestination().id,
                        inclusive = false,
                        saveState = true
                    )
                    .build()
            )
            true
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigationView.menu.findItem(destination.id)?.isChecked = true
        }
        setupAds()
        deferPermissionPrompts()
    }

    private fun setupAds() {
        MobileAds.initialize(this)
        binding.adViewBanner.loadAd(AdRequest.Builder().build())
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            val permissionPrefs = getSharedPreferences("permission_prefs", MODE_PRIVATE)
            val prompted = permissionPrefs.getBoolean("exact_alarm_prompted", false)
            if (!alarmManager.canScheduleExactAlarms() && !prompted) {
                permissionPrefs.edit().putBoolean("exact_alarm_prompted", true).apply()
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun deferPermissionPrompts() {
        // İlk frame çizildikten sonra istemler açılırsa açılış daha akıcı olur.
        uiHandler.postDelayed({
            requestExactAlarmPermission()
            requestNotificationPermission()
        }, 450)
    }

    override fun onDestroy() {
        uiHandler.removeCallbacksAndMessages(null)
        binding.adViewBanner.destroy()
        super.onDestroy()
    }
}