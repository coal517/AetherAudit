package com.example.aetheraudit.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.example.aetheraudit.data.LocalOUIEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ThreatLevel { SAFE, ELEVATED, CRITICAL }

data class DiscoveredDevice(
    val macAddress: String,
    val name: String,
    val rssi: Int,
    val threatLevel: ThreatLevel,
    val vendorName: String = "Unknown Vendor",
    val vulnerabilityDetails: String = "No known chipset vulnerabilities."
)

class BleSecurityScanner(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    
    // Dynamic getters to re-evaluate state at runtime. Prevents null scanners when Bluetooth is toggled [User Query]
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager.adapter
    private val bleScanner: BluetoothLeScanner? get() = bluetoothAdapter?.bluetoothLeScanner

    private val _scannedDevices = MutableStateFlow<Map<String, DiscoveredDevice>>(emptyMap())
    val scannedDevices = _scannedDevices.asStateFlow()

    private var activeBlacklist: List<LocalOUIEntry> = emptyList()

    fun updateBlacklist(blacklist: List<LocalOUIEntry>) {
        this.activeBlacklist = blacklist
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val mac = device.address ?: return
            val name = device.name ?: "Unnamed BLE Beacon"
            val rssi = result.rssi

            // Extract Organizationally Unique Identifier (OUI) - First 3 bytes of MAC (length 8: e.g. "00:0C:8A")
            val OUI = if (mac.length >= 8) mac.substring(0, 8).uppercase() else ""

            // Compare MAC against loaded Blacklist (supports local overrides)
            val matchingBlacklist = activeBlacklist.find { it.oui == OUI }

            val baseThreat = if (matchingBlacklist != null) ThreatLevel.ELEVATED else ThreatLevel.SAFE

            // Proximity Threat Calculation: If a vulnerable OUI device gets too close (RSSI > -65 dBm), escalate to CRITICAL
            val dynamicThreat = if (baseThreat == ThreatLevel.ELEVATED && rssi > -65) {
                ThreatLevel.CRITICAL
            } else {
                baseThreat
            }

            val discoveredDevice = DiscoveredDevice(
                macAddress = mac,
                name = name,
                rssi = rssi,
                threatLevel = dynamicThreat,
                vendorName = matchingBlacklist?.vendorName ?: "Unknown Vendor",
                vulnerabilityDetails = matchingBlacklist?.vulnerabilityDetails ?: "No known chipset vulnerabilities found."
            )

            _scannedDevices.update { currentMap ->
                currentMap.toMutableMap().apply { put(mac, discoveredDevice) }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning(): Boolean {
        _scannedDevices.value = emptyMap()
        try {
            val adapter = bluetoothAdapter
            if (adapter == null || !adapter.isEnabled) {
                return false // Bluetooth antenna is turned OFF on device system-level
            }
            
            // Re-fetch scanner dynamically. If user turned on Bluetooth after app launch, this will now be non-null!
            val scanner = bleScanner ?: return false
            scanner.startScan(scanCallback)
            return true
        } catch (e: SecurityException) {
            // Fails if runtime Bluetooth scan permissions were denied by the user [User Query]
            return false
        } catch (e: Exception) {
            return false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        try {
            // Defensive try-catch blocks prevent crashing when stopping scan during hardware configuration changes [User Query]
            bleScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            // Ignore hardware teardown crashes
        }
    }
}
