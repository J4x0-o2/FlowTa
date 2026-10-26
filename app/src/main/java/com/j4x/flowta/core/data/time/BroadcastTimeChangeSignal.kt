package com.j4x.flowta.core.data.time

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.j4x.flowta.core.domain.time.TimeChangeSignal
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.TimeZone

/**
 * Listens to the system time zone / time / date change broadcasts while collected. The receiver is
 * registered with the application context so it never leaks an Activity.
 */
class BroadcastTimeChangeSignal(context: Context) : TimeChangeSignal {

    private val appContext = context.applicationContext

    override val changes: Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Make sure ZoneId.systemDefault() reads the new zone before anyone re-evaluates.
                if (intent.action == Intent.ACTION_TIMEZONE_CHANGED) TimeZone.setDefault(null)
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        appContext.registerReceiver(receiver, filter)
        awaitClose { appContext.unregisterReceiver(receiver) }
    }
}
