package com.example.yourbagbuddy.di

import android.content.Context
import androidx.room.Room
import com.example.yourbagbuddy.data.local.YourBagBuddyDatabase
import com.example.yourbagbuddy.data.local.dao.ChecklistItemDao
import com.example.yourbagbuddy.data.local.dao.ReminderDao
import com.example.yourbagbuddy.data.local.dao.TripDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): YourBagBuddyDatabase {
        return Room.databaseBuilder(
            context,
            YourBagBuddyDatabase::class.java,
            "yourbagbuddy_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideTripDao(database: YourBagBuddyDatabase): TripDao {
        return database.tripDao()
    }
    
    @Provides
    fun provideChecklistItemDao(database: YourBagBuddyDatabase): ChecklistItemDao {
        return database.checklistItemDao()
    }

    @Provides
    fun provideReminderDao(database: YourBagBuddyDatabase): ReminderDao {
        return database.reminderDao()
    }
}
