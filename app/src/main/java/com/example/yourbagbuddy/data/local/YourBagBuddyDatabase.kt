package com.example.yourbagbuddy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.yourbagbuddy.data.local.dao.ChecklistItemDao
import com.example.yourbagbuddy.data.local.dao.TripDao
import com.example.yourbagbuddy.data.local.entity.ChecklistItemEntity
import com.example.yourbagbuddy.data.local.entity.TripEntity

@Database(
    entities = [TripEntity::class, ChecklistItemEntity::class],
    version = 2,
    exportSchema = false
)
abstract class YourBagBuddyDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun checklistItemDao(): ChecklistItemDao
}
