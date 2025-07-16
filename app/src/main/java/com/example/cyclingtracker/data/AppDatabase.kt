package com.example.cyclingtracker.data

import android.content.Context
import androidx.room.*
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromLatLngList(path: List<LatLng>): String {
        return Gson().toJson(path)
    }

    @TypeConverter
    fun toLatLngList(pathJson: String): List<LatLng> {
        val type = object : TypeToken<List<LatLng>>() {}.type
        return Gson().fromJson(pathJson, type)
    }
}

@Database(entities = [Ride::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rideDao(): RideDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cycling_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
