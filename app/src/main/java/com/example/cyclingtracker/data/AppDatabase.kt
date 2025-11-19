package com.example.cyclingtracker.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Database(
    entities = [Ride::class],
    version = 2, // Incremented version
    exportSchema = true
    // Auto-migration removed in favor of manual migration
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rideDao(): RideDao

    companion object {
        // Manual migration from version 1 to 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rides ADD COLUMN caloriesBurned REAL NOT NULL DEFAULT 0.0")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cycling_database"
                )
                .addMigrations(MIGRATION_1_2) // Add manual migration
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}