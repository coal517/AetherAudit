package com.example.aetheraudit.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LocalOUIEntry::class, AuditLogEntry::class], version = 1, exportSchema = false)
abstract class AetherAuditDatabase : RoomDatabase() {
    abstract fun dao(): AetherAuditDao

    companion object {
        @Volatile
        private var INSTANCE: AetherAuditDatabase? = null

        fun getDatabase(context: Context): AetherAuditDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AetherAuditDatabase::class.java,
                    "aether_audit_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}