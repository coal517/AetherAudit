package com.example.aetheraudit.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aetheraudit.data.LocalOUIEntry
import com.example.aetheraudit.viewmodel.AetherAuditViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AetherAuditNavigation(viewModel: AetherAuditViewModel) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Live Radar, 1 = Audit Log History, 2 = Blacklist Manager, 3 = Operator Panel

    // Intercept Double Back-Press to Kill Scan Engine & Exit [User Query]
    val context = LocalContext.current
    var lastBackPressTime by remember { mutableStateOf(0L) }

    BackHandler(enabled = true) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000) {
            if (state.isScanning) {
                viewModel.toggleScanning() // Dynamical teardown saves hardware power on exit!
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

    // Confirmation Alert States for security-hardened operator confirmations [User Query]
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<LocalOUIEntry?>(null) }
    var showTutorialDialog by remember { mutableStateOf(false) }

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
                        icon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Radar", tint = Color.White) },
                        label = { Text("Live Radar", color = Color.White, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFF0284C7))
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(imageVector = Icons.Default.Menu, contentDescription = "History", tint = Color.White) },
                        label = { Text("History logs", color = Color.White, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFF0284C7))
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(imageVector = Icons.Default.Build, contentDescription = "Blacklist", tint = Color.White) },
                        label = { Text("Blacklist Editor", color = Color.White, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFF0284C7))
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Operator", tint = Color.White) },
                        label = { Text("Operator Panel", color = Color.White, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFF0284C7))
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
                        onSaveLog = { viewModel.recordThreatAndUpload(it) },
                        onTutorialClick = { showTutorialDialog = true }
                    )
                    1 -> HistoryLogsScreen(state.auditLogs, { viewModel.searchLogs(it) })
                    2 -> BlacklistEditorScreen(
                        blacklist = state.localBlacklist,
                        onSync = { viewModel.syncOUIBlacklistFromSupabase() },
                        onAddManual = { oui, vendor, note -> viewModel.addManualOUIOverride(oui, vendor, note) },
                        onDeleteOUI = { entry -> entryToDelete = entry }
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

    // 1. TUTORIAL DIALOG (Scanner Pre-requisites Help Overlay) [User Query]
    if (showTutorialDialog) {
        AlertDialog(
            onDismissRequest = { showTutorialDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pre-requisite Scanner Instructions", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Follow these critical steps to start auditing physical hardware:", color = Color.White)
                    Text("1. Turn ON your device's Bluetooth in the Android quick-settings bar.", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    Text("2. Enable GPS Location services (Android security sandboxes require location to access nearby Bluetooth beacon payloads safely).", color = Color.White)
                    Text("3. Grant the Bluetooth Scan and Location permissions when requested by the application.", color = Color.White)
                    Text("4. Tap START RADAR to initiate background scanning immediately.", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF334155)))
                    Spacer(modifier = Modifier.height(4.dp))

                    Text("⚠️ Technical Note on Initialization:", fontWeight = FontWeight.SemiBold, color = Color(0xFFF59E0B), fontSize = 11.sp)
                    Text("If Bluetooth is toggled ON while AetherAudit is already running, Android's hardware daemon requires 5-10 seconds to fully initialize the antenna. During this boot phase, the app may miss initial peripheral advertisement intervals. For maximum sensitivity, we strongly recommend enabling Bluetooth BEFORE launching the app.",
                        color = Color(0xFF94A3B8), fontSize = 11.sp, lineHeight = 16.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showTutorialDialog = false }) {
                    Text("UNDERSTOOD")
                }
            },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    // 2. CONFIRM DELETION OF LOCAL OVERRIDE DIALOG [17]
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
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    // 3. CONFIRM LOGOUT DIALOG
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
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    // 4. CONFIRM DELETE ACCOUNT DIALOG
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
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}
