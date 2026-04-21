package com.example.insulinneedlereminder.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.insulinneedlereminder.data.dao.GlucoseDao
import com.example.insulinneedlereminder.data.dao.InsulinDao
import com.example.insulinneedlereminder.data.entity.GlucoseRecord
import com.example.insulinneedlereminder.data.entity.InsulinRecord

@Database(
    entities = [InsulinRecord::class, GlucoseRecord::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun insulinDao(): InsulinDao
    abstract fun glucoseDao(): GlucoseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "insulin_health_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Schema unchanged; this prevents destructive migration on update.
            }
        }
    }
}