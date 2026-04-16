package com.meshai

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * MeshAI Application entry point.
 *
 * Initializes:
 * - Timber logging (debug only)
 * - Hilt dependency injection
 * - WorkManager with Hilt worker factory
 */
@HiltAndroidApp
class MeshAIApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for debug logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // In production, plant a crash-reporting tree (e.g., Crashlytics)
            Timber.plant(SilentTree())
        }

        Timber.i("MeshAI starting — node initializing...")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    /** Silent tree for production — swap with crash reporter */
    private class SilentTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // No-op in production; replace with Crashlytics.log() etc.
        }
    }
}
