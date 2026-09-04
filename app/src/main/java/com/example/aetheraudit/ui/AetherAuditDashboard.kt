package com.example.aetheraudit.ui

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.example.aetheraudit.scanner.DiscoveredDevice
import com.example.aetheraudit.scanner.ThreatLevel
import com.example.aetheraudit.viewmodel.AetherAuditViewModel
import com.example.aetheraudit.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AetherAuditDashboard(viewModel: AetherAuditViewModel) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Live Radar, 1 = Audit Log History, 2 = Blacklist Manager

    // Intercept Double Back-Press to Kill Scan Engine & Exit
    val context = LocalContext.current
    var lastBackPressTime by remember { mutableStateOf(0L) }

    BackHandler(enabled = true) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000) {
            if (state.isScanning) {
                viewModel.toggleScanning()
            }
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    // Material 3 UI States
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Confirmation Alert States
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<LocalOUIEntry?>(null) }

    if (!state.isUserAuthenticated) {
        AuthGateScreen(
            statusMessage = state.statusMessage,
            onLogin = { email, pass -> viewModel.authenticateUser(email, pass) },
            onSignUp = { email, pass -> viewModel.registerUser(email, pass) }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF0F172A)) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Text("📡") },
                        label = { Text("Live Radar", color = Color.White) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Text("📁") },
                        label = { Text("History logs", color = Color.White) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Text("⚙️") },
                        label = { Text("Blacklist Editor", color = Color.White) }
                    )
                    NavigationBarItem(
                        selected = selectedTab ==3,
                        onClick = { selectedTab = 3},
                        icon = { Text("🔒️") },
                        label = { Text("Operator Panel", color = Color.White)}
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
                    0 -> LiveRadarScreen(
                        scannedDevices = state.scannedDevices,
                        isScanning = state.isScanning,
                        onToggleScan = { viewModel.toggleScanning() },
                        onSaveLog = { viewModel.recordThreatAndUpload(it) }
                    )
                    1 -> HistoryLogsScreen(state.auditLogs, { viewModel.searchLogs(it) })
                    2 -> BlacklistEditorScreen(
                        state.localBlacklist,
                        { viewModel.syncOUIBlacklistFromSupabase() },
                        { oui, vendor, note -> viewModel.addManualOUIOverride(oui, vendor, note) },
                        { entry -> entryToDelete = entry }
                    )
                    3 -> OperatorPanelScreen(
                        operatorEmail = state.currentUserEmail,
                        onLogoutClick = { showLogoutConfirm = true },
                        onDeleteAccountClick = { showDeleteAccountConfirm = true }
                    )
                }
            }
        }
    }

    // 1. CONFIRM DELETION OF LOCAL OVERRIDE DIALOG
    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete Local Override?") },
            text = { Text("Are you sure you want to delete the manual override rule for ${entry.oui}?") },
            confirmButton = {
                Button(
                    onClick = {
                        entryToDelete = null
                        // Execute Deletion with 10-Second Undo Snackbar Hook
                        scope.launch {
                            viewModel.deleteManualOUI(entry)
                            val snackbarResult = snackbarHostState.showSnackbar(
                                message = "Deleted override rule ${entry.oui}",
                                actionLabel = "UNDO",
                                duration = SnackbarDuration.Long // Material 3 standard long duration (~10s)
                            )
                            if (snackbarResult == SnackbarResult.ActionPerformed) {
                                viewModel.restoreDeletedOUI()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("DELETE", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("CANCEL")
                }
            },
            containerColor = Color(0xFF99CCFF)
        )
    }

    // 2. CONFIRM LOGOUT DIALOG
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Operator Logout") },
            text = { Text("Are you sure you want to lock the scanning console and log out?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        viewModel.logoutUser()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("LOGOUT", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("CANCEL")
                }
            },
            containerColor = Color(0xFF99CCFF)
        )
    }

    // 3. CONFIRM DELETE ACCOUNT DIALOG
    if (showDeleteAccountConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountConfirm = false },
            title = { Text("ERASE OPERATOR PROFILE?") },
            text = { Text("CRITICAL WARNING: This completely wipes your profile off the Supabase server and empties local history files. This action cannot be reversed!") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountConfirm = false
                        viewModel.deleteOperatorAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("DELETE ACCOUNT Permanently", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountConfirm = false }) {
                    Text("CANCEL")
                }
            },
            containerColor = Color(0xFF99CCFF)
        )
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
            TextButton(
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
                onValueChange = { input->
                    // Hitting strict character constraints
                    if (input.length <= 8) {
                        manualOUI = input
                        inputError = false
                    }
                },
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
                onValueChange = { input ->
                    if (input.length <= 35) manualVendor = input
                },
                label = { Text("Hardware Vendor Name", color = Color(0xFF99CCFF)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            OutlinedTextField(
                value = manualNote,
                onValueChange = { input ->
                        if (input.length <= 60) manualNote = input
                    },
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

        TextButton(onClick = {showBlacklistDialog = true}, modifier = Modifier.height(36.dp),
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.oui, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                    Text(entry.vendorName, color = Color.White, fontSize = 13.sp)
                                    Text(entry.vulnerabilityDetails, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }

                                if (entry.isUserDefined) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "LOCAL OVERRIDE", color = Color(0xFFF59E0B),
                                            fontSize = 10.sp, fontWeight = FontWeight.Bold
                                        )
                                        TextButton(
                                            onClick = { onDeleteOUI(entry) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                        ) {
                                            Text("DELETE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
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
    var isPasswordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current

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
                    onValueChange = { input ->
                        // Validation checks: Forbid using spaces, limit characters [CLO2 Quality of work]
                        if (input.length <= 35 && !input.contains(" ")) {
                            email = input
                        }
                    },
                    label = { Text("Operator Email", color = Color(0xFF99CCFF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { input ->
                        // Validation checks: Forbid using spaces, limit characters [CLO2 Quality of work]
                        if (input.length <= 20 && !input.contains(" ")) {
                            password = input
                        }
                    },
                    label = { Text("Access Key", color = Color(0xFF99CCFF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    // Secure Password Field asterisks transformation
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        // Dynamic touch hold-to-reveal password action
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "Hold down button to reveal password", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isPasswordVisible = true
                                        tryAwaitRelease()
                                        isPasswordVisible = false
                                    }
                                )
                            }
                        ) {
                            Text(if (isPasswordVisible) "🔓" else "👁️", fontSize = 16.sp)
                        }
                    }
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
fun OperatorPanelScreen(
    operatorEmail: String,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🛡️", fontSize = 50.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("AETHER AUDIT", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Version 0.1.1", color = Color(0xFF94A3B8), fontSize = 14.sp)
        }

        // Active Profile Management Module [CLO2 Development of mobile solution]
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔒 Active Operator Details", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Identity: $operatorEmail", color = Color(0xFF38BDF8), fontSize = 13.sp)
                    Text("Authority: Secure blue-team physical auditor", color = Color(0xFF94A3B8), fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onLogoutClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("LOGOUT", fontSize = 11.sp)
                        }
                        Button(
                            onClick = onDeleteAccountClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("DELETE ACCOUNT", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Academic Documentation Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📋 Cyber-Resilience Manifesto", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Aligning with UN Sustainable Development Goal 9 (Industry, Innovation, and Infrastructure), " +
                        "AetherAudit maps nearby physical space to passively find and log vulnerable legacy hardware configurations " +
                        "susceptible to unauthenticated RACE memory disclosures.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Source Code & Implicit Intent Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🧑‍💻 Academic Repository", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "This open-source Android module is distributed under the official terms of the MIT License.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            // Triggering Implicit Web Intent targeting GitHub repo [User Query]
                            val intent = Intent(Intent.ACTION_VIEW,
                                "https://github.com/coal517/AetherAudit".toUri())
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OPEN GITHUB SOURCE", color = Color.White)
                    }
                }
            }
        }
    }
}
