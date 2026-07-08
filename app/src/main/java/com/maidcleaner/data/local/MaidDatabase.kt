package com.maidcleaner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.maidcleaner.data.local.dao.ScheduledScanDao
import com.maidcleaner.data.local.dao.ScanHistoryDao
import com.maidcleaner.data.local.dao.WhitelistDao
import com.maidcleaner.data.local.entity.ScheduledScanEntity
import com.maidcleaner.data.local.entity.ScanHistoryEntity
import com.maidcleaner.data.local.entity.WhitelistEntity
import com.maidcleaner.data.model.ListType
import com.maidcleaner.data.model.ScanFrequency
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

class Converters {
    @androidx.room.TypeConverter
    fun fromListType(value: ListType) = value.name

    @androidx.room.TypeConverter
    fun toListType(value: String) = ListType.valueOf(value)

    @androidx.room.TypeConverter
    fun fromScanFrequency(value: ScanFrequency) = value.name

    @androidx.room.TypeConverter
    fun toScanFrequency(value: String) = ScanFrequency.valueOf(value)
}

@Database(
    entities = [
        WhitelistEntity::class,
        ScheduledScanEntity::class,
        ScanHistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MaidDatabase : RoomDatabase() {
    abstract fun whitelistDao(): WhitelistDao
    abstract fun scheduledScanDao(): ScheduledScanDao
    abstract fun scanHistoryDao(): ScanHistoryDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): MaidDatabase = Room.databaseBuilder(
        context,
        MaidDatabase::class.java,
        "maid_cleaner.db"
    ).build()

    @Provides
    fun provideWhitelistDao(db: MaidDatabase) = db.whitelistDao()

    @Provides
    fun provideScheduledScanDao(db: MaidDatabase) = db.scheduledScanDao()

    @Provides
    fun provideScanHistoryDao(db: MaidDatabase) = db.scanHistoryDao()
}
