package com.example.aetheraudit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.aetheraudit.scanner.DiscoveredDevice
import com.example.aetheraudit.scanner.ThreatLevel
import com.example.aetheraudit.viewmodel.AetherAuditViewModel
import com.example.aetheraudit.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AetherAuditDashboard(viewModel: AetherAuditViewModel) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Live Radar, 1 = Audit Log History, 2 = Blacklist Manager

    if (!state.isUserAuthenticated) {
        AuthGateScreen(
            statusMessage = state.statusMessage,
            onLogin = { email, pass -> viewModel.authenticateUser(email, pass) },
            onSignUp = { email, pass -> viewModel.registerUser(email, pass) }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF0F172A), contentColor = Color.White) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Text("📡") },
                        label = { Text("Live Radar") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Text("📁") },
                        label = { Text("History logs") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Text("⚙️") },
                        label = { Text("Blacklist Editor") }
                    )
                    NavigationBarItem(
                        selected = selectedTab ==3,
                        onClick = { selectedTab = 3},
                        icon = { Text("ℹ️") },
                        label = { Text("About Us")}
                    )
                }
            },
            containerColor = Color(0xFF020617)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // Status Feed Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(state.statusMessage, color = Color(0xFF94A3B8), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> LiveRadarScreen(state.scannedDevices, state.isScanning, { viewModel.toggleScanning() }, { viewModel.recordThreatAndUpload(it) })
                    1 -> HistoryLogsScreen(state.auditLogs, { viewModel.searchLogs(it) })
                    2 -> BlacklistEditorScreen(
                        state.localBlacklist,
                        { viewModel.syncOUIBlacklistFromSupabase() },
                        { oui, vendor, note -> viewModel.addManualOUIOverride(oui, vendor, note) },
                        { entry -> viewModel.deleteManualOUI(entry) }
                    )
                    3 -> AboutScreen()
                }
            }
        }
    }
}

@Composable
fun LiveRadarScreen(
    scannedDevices: List<DiscoveredDevice>,
    isScanning: Boolean,
    onToggleScan: () -> Unit,
    onSaveLog: (DiscoveredDevice) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Perimeter Scan Data", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = onToggleScan,
                colors = ButtonDefaults.buttonColors(containerColor = if (isScanning) Color.Red else Color(0xFF0284C7))
            ) {
                Text(if (isScanning) "STOP SCAN" else "START RADAR", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (scannedDevices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No devices captured. Fire up scan engine.", color = Color(0xFF475569))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(scannedDevices) { device ->
                    DeviceThreatCard(device, onSaveLog)
                }
            }
        }
    }
}

@Composable
fun DeviceThreatCard(device: DiscoveredDevice, onSaveLog: (DiscoveredDevice) -> Unit) {
    val threatColor = when (device.threatLevel) {
        ThreatLevel.SAFE -> Color(0xFF10B981)
        ThreatLevel.ELEVATED -> Color(0xFFF59E0B)
        ThreatLevel.CRITICAL -> Color(0xFFEF4444)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(device.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(device.macAddress, color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .background(threatColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .border(1.dp, threatColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(device.threatLevel.name, color = threatColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("OUI Vendor: ${device.vendorName}", color = Color(0xFF38BDF8), fontSize = 13.sp)
            Text(device.vulnerabilityDetails, color = Color(0xFF94A3B8), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Signal strength: ${device.rssi} dBm", color = Color(0xFF64748B), fontSize = 12.sp)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { onSaveLog(device) }) {
                    Text("PUBLISH THREAT LOG", color = Color(0xFF38BDF8), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun HistoryLogsScreen(auditLogs: List<AuditLogEntry>, onSearch: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }

    Column {
        TextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                onSearch(it)
            },
            placeholder = { Text("Search local security logs...", color = Color(0xFF99CCFF)) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(auditLogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(log.deviceName, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(log.threatLevel, color = if (log.threatLevel == "SAFE") Color.Green else Color.Red, fontSize = 12.sp)
                        }
                        Text("Address: ${log.macAddress} | RSSI: ${log.rssi} dBm", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun BlacklistEditorScreen(
    blacklist: List<LocalOUIEntry>,
    onSync: () -> Unit,
    onAddManual: (String, String, String) -> Boolean,
    onDeleteOUI: (LocalOUIEntry) -> Unit
) {
    var manualOUI by remember { mutableStateOf("") }
    var manualVendor by remember { mutableStateOf("") }
    var manualNote by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf(false) }
    var showBlacklistDialog by remember { mutableStateOf(false) }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Vulnerability\nDictionary", color = Color.White,
                fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = onSync,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync Icon",
                        tint = Color.White
                    )
                    Text("CLOUD SYNC", color = Color.White)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Manual Entry Card (Input Validation Demo for Rubrics)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Add Local Custom OUI Threat Override", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = manualOUI,
                onValueChange = { manualOUI = it; inputError = false },
                label = { Text("Target OUI (format: AA:BB:CC)", color = Color(0xFF99CCFF)) },
                isError = inputError,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults
                    .colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, errorTextColor = Color.Red)
            )
            if (inputError) {
                Text("Invalid OUI format! Use the exact XX:XX:XX hexadecimal format.", color = Color.Red, fontSize = 11.sp)
            }

            OutlinedTextField(
                value = manualVendor,
                onValueChange = { manualVendor = it },
                label = { Text("Hardware Vendor Name", color = Color(0xFF99CCFF)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            OutlinedTextField(
                value = manualNote,
                onValueChange = { manualNote = it },
                label = { Text("Note", color = Color(0xFF99CCFF)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val success = onAddManual(manualOUI, manualVendor, manualNote)
                    if (success) {
                        manualOUI = ""
                        manualVendor = ""
                        manualNote = ""
                    } else {
                        inputError = true
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("ADD LOCAL BLACKLIST ENTRY")
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Registered Blacklist Rules", color = Color.White, fontWeight = FontWeight.Bold)

        Button(onClick = {showBlacklistDialog = true}, modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
        ) {
            Text("VIEW (${blacklist.size})", color = Color.White)
        }
    }

    if (showBlacklistDialog) {
        Dialog(
            onDismissRequest = { showBlacklistDialog = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.8f),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                ) {
                    Text(
                        "Registered Blacklist Rules (${blacklist.size} entries)",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(blacklist) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        entry.oui,
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        entry.vendorName,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        entry.vulnerabilityDetails,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }

                                if (entry.isUserDefined) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "LOCAL OVERRIDE", color = Color(0xFFF59E0B),
                                            fontSize = 10.sp, fontWeight = FontWeight.Bold
                                        )
                                        Button(
                                            onClick = { onDeleteOUI(entry) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                        ) { Text("DELETE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    }
                                } else {
                                    Text(
                                        "SYNCED CLOUD",
                                        color = Color(0xFF10B981),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showBlacklistDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AuthGateScreen(
    statusMessage: String,
    onLogin: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF020617)).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("SECURE OPERATOR GATEWAY", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Perimeter Scout Authentication required to push remote logs.", color = Color(0xFF64748B), fontSize = 12.sp)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Operator Email", color = Color(0xFF99CCFF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Access Key", color = Color(0xFF99CCFF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(statusMessage, color = Color(0xFF94A3B8), fontSize = 11.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { onSignUp(email, password) }) {
                        Text("REGISTER PROFILE", color = Color(0xFF94A3B8))
                    }
                    Button(onClick = { onLogin(email, password) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))) {
                        Text("AUTHORIZE", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    Color(0xFF1E293B),
                    RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "🛡️",
                fontSize = 60.sp
            )
        }

        // App Name
        Text(
            "Aether Audit",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        // Version
        Text(
            "Version 0.1.0",
            color = Color(0xFF94A3B8),
            fontSize = 14.sp
        )

        // Divider
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(vertical = 8.dp), thickness = DividerDefaults.Thickness, color = Color(0xFF1E293B)
        )

        // About Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "📋 About",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Aether Audit is a sophisticated BLE security scanning application designed for perimeter security assessment and vulnerability detection.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }

        // Features Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "⚡ Key Features",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                FeatureItem("📡", "Real-time BLE device scanning")
                FeatureItem("🔍", "OUI-based vulnerability detection")
                FeatureItem("☁️", "Cloud sync with Supabase")
                FeatureItem("📊", "Threat logging and history")
                FeatureItem("⚙️", "Customizable blacklist rules")
                FeatureItem("🔐", "Secure operator authentication")
            }
        }
    }
}

@Composable
fun FeatureItem(icon: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            icon,
            fontSize = 16.sp
        )
        Text(
            text,
            color = Color(0xFFCBD5E1),
            fontSize = 13.sp
        )
    }
}
