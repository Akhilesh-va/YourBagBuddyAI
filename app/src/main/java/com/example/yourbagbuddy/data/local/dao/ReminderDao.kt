package com.example.yourbagbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.yourbagbuddy.data.local.entity.ReminderEntity

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE checklistId = :checklistId LIMIT 1")
    suspend fun getByChecklistId(checklistId: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity)

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE checklistId = :checklistId")
    suspend fun deleteByChecklistId(checklistId: String)

    /**
     * Used by Worker: load reminder by checklistId to check stop conditions and show notification.
     */
    @Query("SELECT * FROM reminders WHERE checklistId = :checklistId AND isEnabled = 1 LIMIT 1")
    suspend fun getEnabledByChecklistId(checklistId: String): ReminderEntity?
}
