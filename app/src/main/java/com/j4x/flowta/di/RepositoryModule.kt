package com.j4x.flowta.di

import com.j4x.flowta.feature.accounts.data.repository.AccountRepositoryImpl
import com.j4x.flowta.feature.accounts.domain.repository.AccountRepository
import com.j4x.flowta.feature.goals.data.repository.GoalRepositoryImpl
import com.j4x.flowta.feature.goals.domain.repository.GoalRepository
import com.j4x.flowta.feature.history.data.repository.HistoryRepositoryImpl
import com.j4x.flowta.feature.history.domain.repository.HistoryRepository
import com.j4x.flowta.feature.metrics.data.repository.MetricsRepositoryImpl
import com.j4x.flowta.feature.metrics.domain.repository.MetricsRepository
import com.j4x.flowta.feature.profile.data.repository.ProfileRepositoryImpl
import com.j4x.flowta.feature.profile.domain.repository.ProfileRepository
import com.j4x.flowta.feature.settings.data.repository.SettingsRepositoryImpl
import com.j4x.flowta.feature.settings.domain.repository.SettingsRepository
import com.j4x.flowta.feature.transactions.data.repository.TransactionRepositoryImpl
import com.j4x.flowta.feature.transactions.domain.repository.TransactionRepository
import com.j4x.flowta.feature.transfers.data.repository.TransferRepositoryImpl
import com.j4x.flowta.feature.transfers.domain.repository.TransferRepository
import com.j4x.flowta.feature.types.data.repository.TransactionTypeRepositoryImpl
import com.j4x.flowta.feature.types.domain.repository.TransactionTypeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds @Singleton
    abstract fun bindTransferRepository(impl: TransferRepositoryImpl): TransferRepository

    @Binds @Singleton
    abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository

    @Binds @Singleton
    abstract fun bindMetricsRepository(impl: MetricsRepositoryImpl): MetricsRepository

    @Binds @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds @Singleton
    abstract fun bindTransactionTypeRepository(impl: TransactionTypeRepositoryImpl): TransactionTypeRepository

    @Binds @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository
}
