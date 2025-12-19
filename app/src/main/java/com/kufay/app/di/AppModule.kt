package com.kufay.app.di

import android.content.Context
import com.kufay.app.data.db.KufayDatabase
import com.kufay.app.data.db.dao.NotificationDao
import com.kufay.app.data.repository.NotificationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KufayDatabase {
        return KufayDatabase.buildDatabase(context)
    }

    @Provides
    fun provideNotificationDao(database: KufayDatabase): NotificationDao {
        return database.notificationDao()
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(
        notificationDao: NotificationDao
    ): NotificationRepository {
        return NotificationRepository(notificationDao)
    }
}