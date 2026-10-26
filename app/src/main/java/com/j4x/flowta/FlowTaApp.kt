package com.j4x.flowta

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import com.j4x.flowta.feature.types.domain.usecase.SeedDefaultTypesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FlowTaApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var seedDefaultTypes: SeedDefaultTypesUseCase

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        // Seed-if-empty on every launch (idempotent, never throws): covers first run, and a reset
        // whose re-seed failed, without waiting for the Profile screen to be opened.
        appScope.launch {
            seedDefaultTypes().onFailure { Log.w("FlowTaApp", "Default types seeding failed", it) }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                GOALS_CHANNEL_ID,
                "Metas de ahorro",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Recordatorios de metas próximas a vencer"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val GOALS_CHANNEL_ID = "flowta_goals_channel"
    }
}
