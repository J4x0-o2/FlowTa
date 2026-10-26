package com.j4x.flowta.di

import com.j4x.flowta.core.data.work.WorkManagerCanceller
import com.j4x.flowta.core.domain.work.WorkCanceller
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkModule {

    @Binds @Singleton
    abstract fun bindWorkCanceller(impl: WorkManagerCanceller): WorkCanceller
}
