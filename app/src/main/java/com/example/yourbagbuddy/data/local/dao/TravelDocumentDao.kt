package com.example.yourbagbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.yourbagbuddy.data.local.entity.TravelDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelDocumentDao {
    @Query("SELECT * FROM travel_documents WHERE tripId = :tripId ORDER BY type")
    fun getDocumentsForTrip(tripId: String): Flow<List<TravelDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(documents: List<TravelDocumentEntity>)

    @Query("UPDATE travel_documents SET isChecked = :isChecked WHERE id = :id")
    suspend fun setChecked(id: String, isChecked: Boolean)

    @Update
    suspend fun update(document: TravelDocumentEntity)

    @Query("DELETE FROM travel_documents WHERE tripId = :tripId")
    suspend fun deleteAllForTrip(tripId: String)
}
