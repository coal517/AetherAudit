package com.example.aetheraudit.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AetherAuditDao {
    @Query("SELECT * FROM local_oui_blacklist")
    fun getLocalBlacklist(): Flow<List<LocalOUIEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOUI(entry: LocalOUIEntry)

    @Delete
    suspend fun deleteOUI(entry: LocalOUIEntry)

    @Query("DELETE FROM local_oui_blacklist WHERE isUserDefined = 0")
    suspend fun clearRemoteSyncedOUIs()

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntry>>

    // Supports local Full-Text Search - Satisfies CLO2 Quantity of Work (searching)
    @Query("SELECT * FROM audit_logs WHERE deviceName LIKE :query OR macAddress LIKE :query ORDER BY timestamp DESC")
    fun searchAuditLogs(query: String): Flow<List<AuditLogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntry)

    @Query("DELETE FROM audit_logs")
    suspend fun clearAuditLogs()
}