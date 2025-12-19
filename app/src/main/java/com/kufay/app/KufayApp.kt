package com.kufay.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import dagger.hilt.android.HiltAndroidApp
import java.util.Arrays
import javax.inject.Inject

@HiltAndroidApp
class KufayApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Initialize the Mobile Ads SDK
        MobileAds.initialize(this) { initializationStatus ->
            // Optional: You can log or handle the initialization status here
        }

        // Optional: Set up test devices if you're testing ads
        // Remove this in production or keep your own device for testing
        val testDeviceIds = Arrays.asList("ABCDEF012345") // Replace with your test device ID
        val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        MobileAds.setRequestConfiguration(configuration)
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}