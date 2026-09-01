package com.example.aetheraudit.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_oui_blacklist")
data class LocalOUIEntry(
    @PrimaryKey val oui: String, // e.g. "00:0C:8A" (uppercase, length 8)
    val vendorName: String,
    val chipsetManufacturer: String,
    val vulnerabilityDetails: String,
    val isUserDefined: Boolean = false // If true, local database sync from remote will NOT touch this
)

@Entity(tableName = "audit_logs")
data class AuditLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deviceName: String,
    val macAddress: String,
    val rssi: Int,
    val threatLevel: String,
    val timestamp: Long = System.currentTimeMillis()
)
