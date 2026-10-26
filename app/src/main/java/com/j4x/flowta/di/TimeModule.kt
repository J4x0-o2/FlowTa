package com.j4x.flowta.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    /** Follows the device's zone changes: the zone is read from the system on every call. */
    @Provides @Singleton
    fun provideClock(): Clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneId.systemDefault()
        override fun withZone(zone: ZoneId): Clock = Clock.system(zone)
        override fun instant(): Instant = Instant.now()
    }
}
