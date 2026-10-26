package com.j4x.flowta.core.data.work

import android.content.Context
import androidx.work.WorkManager
import com.j4x.flowta.core.domain.work.WorkCanceller
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WorkManagerCanceller @Inject constructor(
    @ApplicationContext private val context: Context
) : WorkCanceller {
    override fun cancelAll() {
        WorkManager.getInstance(context).cancelAllWork()
    }
}
