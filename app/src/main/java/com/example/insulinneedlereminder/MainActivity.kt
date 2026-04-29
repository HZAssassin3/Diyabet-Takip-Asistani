package com.example.insulinneedlereminder

import android.app.AlarmManager
import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.insulinneedlereminder.billing.BillingManager
import com.example.insulinneedlereminder.databinding.ActivityMainBinding
import com.example.insulinneedlereminder.util.PrefsManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsManager: PrefsManager
    private var billingManager: BillingManager? = null
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
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefsManager = PrefsManager(this)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigationView.setupWithNavController(navController)
        setupAds(navController)
        setupBilling()

        // Exact alarm izni iste
        requestExactAlarmPermission()
        requestNotificationPermission()
    }

    private fun setupAds(navController: androidx.navigation.NavController) {
        MobileAds.initialize(this)
        binding.adViewBanner.loadAd(AdRequest.Builder().build())

        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateBannerVisibility(destination.id)
        }
        updateBannerVisibility(navController.currentDestination?.id)
    }

    private fun setupBilling() {
        billingManager = BillingManager(this) { adsRemoved ->
            if (adsRemoved) {
                binding.adViewBanner.visibility = View.GONE
            } else {
                updateBannerVisibility(null)
            }
        }.also { it.startConnection() }
    }

    fun launchRemoveAdsPurchase(): Boolean {
        return billingManager?.launchRemoveAdsPurchase(this) == true
    }

    private fun updateBannerVisibility(destinationId: Int?) {
        if (prefsManager.adsRemoved) {
            binding.adViewBanner.visibility = View.GONE
            return
        }
        binding.adViewBanner.visibility = View.VISIBLE
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
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

    override fun onDestroy() {
        binding.adViewBanner.destroy()
        billingManager?.destroy()
        super.onDestroy()
    }
}