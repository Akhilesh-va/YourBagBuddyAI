package com.example.yourbagbuddy.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.yourbagbuddy.data.local.YourBagBuddyDatabase
import com.example.yourbagbuddy.data.local.dao.ChecklistItemDao
import com.example.yourbagbuddy.data.local.dao.ReminderDao
import com.example.yourbagbuddy.data.local.dao.TravelDocumentDao
import com.example.yourbagbuddy.data.local.dao.TripDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS travel_documents (
                id TEXT NOT NULL PRIMARY KEY,
                tripId TEXT NOT NULL,
                type TEXT NOT NULL,
                displayName TEXT NOT NULL,
                reminderText TEXT,
                resourceUrl TEXT,
                isChecked INTEGER NOT NULL,
                FOREIGN KEY(tripId) REFERENCES trips(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_travel_documents_tripId ON travel_documents(tripId)")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trips ADD COLUMN sharedListId TEXT")
    }
}

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
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
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

    @Provides
    fun provideTravelDocumentDao(database: YourBagBuddyDatabase): TravelDocumentDao {
        return database.travelDocumentDao()
    }
}
