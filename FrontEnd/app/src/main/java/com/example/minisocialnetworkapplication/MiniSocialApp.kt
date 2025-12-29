package com.example.minisocialnetworkapplication

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.example.minisocialnetworkapplication.core.util.AppLifecycleObserver
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MiniSocialApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var appLifecycleObserver: AppLifecycleObserver

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Register app lifecycle observer for online/offline presence tracking
        // ProcessLifecycleOwner is more reliable than Activity lifecycle
        // because it observes the entire app process
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)

        Timber.d("MiniSocialApp initialized with lifecycle observer")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

