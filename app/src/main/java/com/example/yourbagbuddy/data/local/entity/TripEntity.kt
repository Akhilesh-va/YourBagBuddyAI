package com.example.yourbagbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.yourbagbuddy.domain.model.Trip
import com.example.yourbagbuddy.domain.model.TripType
import java.util.Date

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val destination: String,
    val startDate: Long,
    val endDate: Long,
    val numberOfPeople: Int,
    val tripType: String,
    val createdDate: Long,
    val userId: String? = null,
    val lastSyncedAt: Long? = null
) {
    fun toDomain(): Trip {
        return Trip(
            id = id,
            name = name,
            destination = destination,
            startDate = Date(startDate),
            endDate = Date(endDate),
            numberOfPeople = numberOfPeople,
            tripType = TripType.valueOf(tripType),
            createdDate = Date(createdDate),
            userId = userId
        )
    }
    
    companion object {
        fun fromDomain(trip: Trip): TripEntity {
            return TripEntity(
                id = trip.id,
                name = trip.name,
                destination = trip.destination,
                startDate = trip.startDate.time,
                endDate = trip.endDate.time,
                numberOfPeople = trip.numberOfPeople,
                tripType = trip.tripType.name,
                createdDate = trip.createdDate.time,
                userId = trip.userId,
                lastSyncedAt = null
            )
        }
    }
}
