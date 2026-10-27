package com.j4x.flowta.feature.settings.domain.usecase

import android.util.Log
import com.j4x.flowta.core.domain.work.WorkCanceller
import com.j4x.flowta.feature.settings.domain.repository.AppDataRepository
import com.j4x.flowta.feature.settings.domain.repository.SettingsRepository
import com.j4x.flowta.feature.types.domain.usecase.SeedDefaultTypesUseCase
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Leaves the app as freshly installed: wipes the database, clears preferences, cancels scheduled
 * work and re-seeds the default transaction types. The process stays alive.
 */
class ResetAllDataUseCase @Inject constructor(
    private val appData: AppDataRepository,
    private val settings: SettingsRepository,
    private val workCanceller: WorkCanceller,
    private val seedDefaultTypes: SeedDefaultTypesUseCase
) {
    suspend operator fun invoke(): Result<Unit> {
        // If the wipe fails nothing else is touched and the caller sees the failure.
        val wipe = try {
            appData.wipeAll()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
        wipe.onFailure { return Result.failure(it) }

        // The wipe is committed: the reset has happened, so the remaining steps are best effort and
        // never turn the result into a failure. Default types are re-seeded on the next launch
        // (FlowTaApp) if seeding fails here.
        bestEffort("seed default types") { seedDefaultTypes().getOrThrow() }
        bestEffort("clear preferences") { settings.clear() }
        bestEffort("cancel scheduled work") { workCanceller.cancelAll() }
        return Result.success(Unit)
    }

    private inline fun bestEffort(step: String, block: () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Reset-all: failed to $step after the wipe", e)
        }
    }

    private companion object {
        const val TAG = "ResetAllData"
    }
}
