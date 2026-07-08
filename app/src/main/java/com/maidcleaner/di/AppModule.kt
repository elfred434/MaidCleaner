package com.maidcleaner.di

import android.content.Context
import android.content.pm.PackageManager
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing dependencies that CANNOT use constructor injection.
 *
 * Scanners, repositories, and the RootAccessManager already have
 * @Inject constructor + @Singleton on their classes, so Hilt creates them
 * automatically. Do NOT add @Provides methods for those — it creates
 * duplicate-binding conflicts and parameter-mismatch bugs.
 *
 * Only add @Provides here for:
 *   - Framework objects (PackageManager, Context, WorkManager, etc.)
 *   - Interfaces with multiple implementations
 *   - Third-party objects you don't own
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePackageManager(@ApplicationContext context: Context): PackageManager =
        context.packageManager

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
