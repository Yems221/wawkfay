package com.kufay.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kufay.app.ui.navigation.AppNavigation
import com.kufay.app.ui.theme.KufayTheme
import com.kufay.app.workers.NotificationCleanupWorker
import com.kufay.app.utils.NotificationPermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import com.google.android.gms.ads.MobileAds
import com.kufay.app.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.kufay.app.ui.navigation.Screen


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var userPreferences: UserPreferences

    // Launcher pour la demande de permission POST_NOTIFICATIONS
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("KUFAY_PERMISSION", "✅ Permission POST_NOTIFICATIONS accordée")
        } else {
            Log.w("KUFAY_PERMISSION", "⚠️ Permission POST_NOTIFICATIONS refusée")
            // Tu peux afficher un message à l'utilisateur ici si nécessaire
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if this is a fresh install or app data was cleared
        checkFirstLaunchAfterClear()

        // ✅ NOUVEAU : Demander la permission POST_NOTIFICATIONS
        requestNotificationPermissionIfNeeded()

        // Initialisation d'AdMob
        MobileAds.initialize(this) {}

        // Schedule periodic cleanup worker
        scheduleNotificationCleanup()

        // Determine start destination based on onboarding and PIN settings
        val startDestination = runBlocking {
            when {
                // First check if user has completed onboarding
                !userPreferences.hasCompletedOnboarding().first() -> {
                    Screen.Welcome.route
                }
                // Then check PIN status
                userPreferences.pinEnabled.first() -> {
                    Screen.Pin.route
                }
                else -> {
                    Screen.Pin.route
                }
            }
        }

        setContent {
            KufayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(startDestination = startDestination)
                }
            }
        }
    }

    private fun checkFirstLaunchAfterClear() {
        val sharedPrefs = getSharedPreferences("app_initialization", Context.MODE_PRIVATE)
        val isInitialized = sharedPrefs.getBoolean("is_initialized", false)

        if (!isInitialized) {
            // App data was cleared or this is first launch
            // Reset PIN and other initialization tasks
            lifecycleScope.launch {
                userPreferences.clearPin()
                userPreferences.setHasCompletedOnboarding(false)  // Reset onboarding on fresh install
                // Other re-initialization if needed
            }

            // Mark as initialized
            sharedPrefs.edit().putBoolean("is_initialized", true).apply()
        }
    }

    /**
     * ✅ NOUVEAU : Demande la permission POST_NOTIFICATIONS si nécessaire (Android 13+)
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (!NotificationPermissionHelper.hasNotificationPermission(this)) {
            Log.d("KUFAY_PERMISSION", "🔔 Demande de permission POST_NOTIFICATIONS...")

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            Log.d("KUFAY_PERMISSION", "✅ Permission POST_NOTIFICATIONS déjà accordée")
        }
    }

    // We've removed automatic check and redirect to notification listener settings
    // because the welcome screen will guide users to do this

    private fun scheduleNotificationCleanup() {
        val cleanupWorkRequest = PeriodicWorkRequestBuilder<NotificationCleanupWorker>(
            1, TimeUnit.DAYS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "notification_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupWorkRequest
        )
    }

    /**
     * ✅ NOUVEAU : Gestion du résultat de la demande de permission (méthode alternative)
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            NotificationPermissionHelper.NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("KUFAY_PERMISSION", "✅ Permission POST_NOTIFICATIONS accordée via onRequestPermissionsResult")
                } else {
                    Log.w("KUFAY_PERMISSION", "⚠️ Permission POST_NOTIFICATIONS refusée via onRequestPermissionsResult")
                }
            }
        }
    }
}
