package com.j4x.flowta.feature.settings.domain.usecase

import com.j4x.flowta.core.domain.work.WorkCanceller
import com.j4x.flowta.feature.settings.domain.repository.AppDataRepository
import com.j4x.flowta.feature.settings.domain.repository.SettingsRepository
import com.j4x.flowta.feature.types.domain.usecase.SeedDefaultTypesUseCase
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
    suspend operator fun invoke(): Result<Unit> = runCatching {
        // If the wipe fails nothing else is touched. Seeding runs right after it so the app is
        // never left without default categories, even if a later step fails.
        appData.wipeAll().getOrThrow()
        seedDefaultTypes()
        settings.clear()
        workCanceller.cancelAll()
    }
}
