package com.example.yourbagbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.yourbagbuddy.data.local.entity.ChecklistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistItemDao {
    @Query("SELECT * FROM checklist_items WHERE tripId = :tripId ORDER BY category, name")
    fun getChecklistItems(tripId: String): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items WHERE id = :itemId LIMIT 1")
    suspend fun getItemById(itemId: String): ChecklistItemEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ChecklistItemEntity)
    
    @Update
    suspend fun updateItem(item: ChecklistItemEntity)
    
    @Delete
    suspend fun deleteItem(item: ChecklistItemEntity)
    
    @Query("DELETE FROM checklist_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: String)
    
    @Query("UPDATE checklist_items SET isPacked = :isPacked WHERE id = :itemId")
    suspend fun updatePackedStatus(itemId: String, isPacked: Boolean)
}
