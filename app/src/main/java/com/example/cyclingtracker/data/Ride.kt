package com.example.cyclingtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity(tableName = "rides")
data class Ride(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val durationInMillis: Long,
    val distanceInMeters: Float,
    val path: List<LatLng>,
    val caloriesBurned: Float = 0f // Added calories
)
