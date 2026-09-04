package com.example.aetheraudit.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aetheraudit.data.AetherAuditDatabase
import com.example.aetheraudit.data.AuditLogEntry
import com.example.aetheraudit.data.LocalOUIEntry
import com.example.aetheraudit.network.SupabaseNetworkClient
import com.example.aetheraudit.scanner.BleSecurityScanner
import com.example.aetheraudit.scanner.DiscoveredDevice
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.core.content.edit

data class UiState(
    val isScanning: Boolean = false,
    val scannedDevices: List<DiscoveredDevice> = emptyList(),
    val localBlacklist: List<LocalOUIEntry> = emptyList(),
    val auditLogs: List<AuditLogEntry> = emptyList(),
    val statusMessage: String = "Engine Idle. Ready to audit physical perimeter.",
    val isUserAuthenticated: Boolean = false,
    val currentUserEmail: String = "",
    val currentUserId: String = "" // Holds authenticated Supabase User UUID for secure profile cascading deletions [User Query]
)

class AetherAuditViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AetherAuditDatabase.getDatabase(application)
    private val dao = database.dao()
    private val scanner = BleSecurityScanner(application)
    private val networkClient = SupabaseNetworkClient()

    // Native persistence container [13, 14]
    private val prefs = application.getSharedPreferences("aether_audit_prefs", Context.MODE_PRIVATE)

    // Temporary storage for Undo actions
    private var recentlyDeletedOUI: LocalOUIEntry? = null

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Restore session automatically upon cold-start [15]
        val savedEmail = prefs.getString("auth_email", null)
        val savedAuth = prefs.getBoolean("is_auth", false)
        val savedUserId = prefs.getString("auth_user_id", "") ?: ""
        if (savedAuth && savedEmail != null) {
            _uiState.update {
                it.copy(
                    isUserAuthenticated = true,
                    currentUserEmail = savedEmail,
                    currentUserId = savedUserId,
                    statusMessage = "Authorized operator session restored."
                )
            }
        }

        // Collect local OUI database flow and feed back to scanner rules engine
        viewModelScope.launch {
            dao.getLocalBlacklist().collect { list ->
                scanner.updateBlacklist(list)
                _uiState.update { it.copy(localBlacklist = list) }
            }
        }

        // Collect scanner discovery flow to continuously redraw dashboard UI
        viewModelScope.launch {
            scanner.scannedDevices.collect { map ->
                _uiState.update { it.copy(scannedDevices = map.values.toList()) }
            }
        }

        // Collect Audit Logs to populate History tab
        viewModelScope.launch {
            dao.getAllAuditLogs().collect { logs ->
                _uiState.update { it.copy(auditLogs = logs) }
            }
        }
    }

    fun authenticateUser(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Verifying operator credentials...") }
            val userId = networkClient.loginWithEmail(email, password)
            if (userId != null) {
                prefs.edit {
                    putString("auth_email", email)
                    putString("auth_user_id", userId)
                    putBoolean("is_auth", true)
                }
                _uiState.update {
                    it.copy(
                        isUserAuthenticated = true,
                        currentUserEmail = email,
                        currentUserId = userId,
                        statusMessage = "Authorized. Perimeter scans unlocked."
                    )
                }
            } else {
                _uiState.update { it.copy(statusMessage = "Authentication failed. Invalid security keys.") }
            }
        }
    }

    fun registerUser(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Registering operator secure profile...") }
            val success = networkClient.signUpWithEmail(email, password)
            if (success) {
                _uiState.update { it.copy(statusMessage = "Account registered! Confirm your email via the link sent to your inbox.") }
            } else {
                _uiState.update { it.copy(statusMessage = "Registration failed. Choose robust credentials.") }
            }
        }
    }

    fun logoutUser() {
        prefs.edit { clear() }
        _uiState.update {
            it.copy(
                isUserAuthenticated = false,
                currentUserEmail = "",
                currentUserId = "",
                statusMessage = "Operator logged out. Perimeter locked."
            )
        }
    }

    fun deleteOperatorAccount() {
        val userId = _uiState.value.currentUserId
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Executing account deletion protocol...") }
            val success = networkClient.deleteOperatorAccount(userId)
            if (success) {
                // Purge shared preferences session [15]
                prefs.edit { clear() }
                // Scrub local data caches
                dao.clearAuditLogs()
                _uiState.update {
                    it.copy(
                        isUserAuthenticated = false,
                        currentUserEmail = "",
                        currentUserId = "",
                        statusMessage = "Account deleted. Local storage scrubbed."
                    )
                }
            } else {
                _uiState.update { it.copy(statusMessage = "Account deletion failed. Network server busy.") }
            }
        }
    }

    fun toggleScanning() {
        val scanning = !_uiState.value.isScanning
        if (scanning) {
            val started = scanner.startScanning()
            if (started) {
                _uiState.update {
                    it.copy(
                        isScanning = true,
                        statusMessage = "Scanning active. Mapping BLE perimeter..."
                    )
                }
            } else {
                // Bluetooth off or not initialized. Guides the user beautifully [User Query]
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        statusMessage = "Scan blocked! Enable Bluetooth in device settings, then try again."
                    )
                }
            }
        } else {
            scanner.stopScanning()
            _uiState.update {
                it.copy(
                    isScanning = false,
                    statusMessage = "Scanner idle. Results buffered."
                )
            }
        }
    }

    // Sync remote Master List to Local DB without deleting manual user overrides
    fun syncOUIBlacklistFromSupabase() {
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Syncing with Supabase Vulnerability Server...") }
            try {
                val remoteList = networkClient.fetchMasterBlacklist()
                if (remoteList.isNotEmpty()) {
                    // 1. Flush old cached remote entries safely
                    dao.clearRemoteSyncedOUIs()
                    // 2. Load fresh remote entries
                    for (entry in remoteList) {
                        dao.insertOUI(entry)
                    }
                    _uiState.update { it.copy(statusMessage = "Database synchronized! Sync pulled ${remoteList.size} vulnerabilities.") }
                } else {
                    _uiState.update { it.copy(statusMessage = "Failed to synchronize master OUI database.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Sync error: Check Supabase connection.") }
            }
        }
    }

    // Save detected device to Room DB and upload resilience log to Supabase in parallel
    fun recordThreatAndUpload(device: DiscoveredDevice) {
        viewModelScope.launch {
            // 1. Save to Local Room DB (Audit Log)
            val localLog = AuditLogEntry(
                deviceName = device.name,
                macAddress = device.macAddress,
                rssi = device.rssi,
                threatLevel = device.threatLevel.name
            )
            dao.insertAuditLog(localLog)

            // 2. Push to Supabase Server
            _uiState.update { it.copy(statusMessage = "Uploading risk log to Supabase central hub...") }
            val uploaded = networkClient.uploadAuditLog(
                deviceName = device.name,
                macAddress = device.macAddress,
                rssi = device.rssi,
                threatLevel = device.threatLevel.name
            )

            if (uploaded) {
                _uiState.update { it.copy(statusMessage = "Report published to Supabase Security Dashboard.") }
            } else {
                _uiState.update { it.copy(statusMessage = "Saved offline locally. Supabase connection pending.") }
            }
        }
    }

    // Input-validated OUI entry insertion
    fun addManualOUIOverride(oui: String, vendorName: String, notes: String): Boolean {
        val cleanOUI = oui.trim().uppercase()
        val isValid = cleanOUI.matches(Regex("^[0-9A-F]{2}:[0-9A-F]{2}:[0-9A-F]{2}$"))
        if (!isValid) return false

        viewModelScope.launch {
            val entry = LocalOUIEntry(
                oui = cleanOUI,
                vendorName = vendorName,
                chipsetManufacturer = "User Override",
                vulnerabilityDetails = notes,
                isUserDefined = true
            )
            dao.insertOUI(entry)
        }
        return true
    }

    fun deleteManualOUI(entry: LocalOUIEntry) {
        recentlyDeletedOUI = entry
        viewModelScope.launch {
            dao.deleteOUI(entry)
            _uiState.update { it.copy(statusMessage = "Removed OUI Override: ${entry.oui}") }
        }
    }

    fun restoreDeletedOUI() {
        recentlyDeletedOUI?.let { entry ->
            viewModelScope.launch {
                dao.insertOUI(entry)
                _uiState.update { it.copy(statusMessage = "Restored local override: ${entry.oui}") }
                recentlyDeletedOUI = null
            }
        }
    }

    fun searchLogs(query: String) {
        viewModelScope.launch {
            dao.searchAuditLogs("%$query%").collect { logs ->
                _uiState.update { it.copy(auditLogs = logs) }
            }
        }
    }
}
