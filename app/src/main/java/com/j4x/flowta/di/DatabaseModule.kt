package com.j4x.flowta.di

import android.content.Context
import com.j4x.flowta.core.data.db.createDatabase
import com.j4x.flowta.database.AccountQueries
import com.j4x.flowta.database.FlowTaDatabase
import com.j4x.flowta.database.GoalContributionQueries
import com.j4x.flowta.database.GoalQueries
import com.j4x.flowta.database.InternalTransferQueries
import com.j4x.flowta.database.TransactionQueries
import com.j4x.flowta.database.TransactionTypeQueries
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FlowTaDatabase =
        createDatabase(context)

    @Provides @Singleton
    fun provideAccountQueries(db: FlowTaDatabase): AccountQueries = db.accountQueries

    @Provides @Singleton
    fun provideTransactionQueries(db: FlowTaDatabase): TransactionQueries = db.transactionQueries

    @Provides @Singleton
    fun provideTransactionTypeQueries(db: FlowTaDatabase): TransactionTypeQueries = db.transactionTypeQueries

    @Provides @Singleton
    fun provideInternalTransferQueries(db: FlowTaDatabase): InternalTransferQueries = db.internalTransferQueries

    @Provides @Singleton
    fun provideGoalQueries(db: FlowTaDatabase): GoalQueries = db.goalQueries

    @Provides @Singleton
    fun provideGoalContributionQueries(db: FlowTaDatabase): GoalContributionQueries = db.goalContributionQueries
}
