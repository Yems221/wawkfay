package com.kufay.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if this is a fresh install or app data was cleared
        checkFirstLaunchAfterClear()

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
}