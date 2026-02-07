package com.umbrella.di

import com.umbrella.data.scheduler.AlarmSchedulerImpl
import com.umbrella.data.scheduler.NotificationScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulerModule {

    @Binds
    @Singleton
    abstract fun bindNotificationScheduler(
        impl: AlarmSchedulerImpl
    ): NotificationScheduler
}
