package com.example.aetheraudit

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aetheraudit.ui.AetherAuditNavigation
import com.example.aetheraudit.viewmodel.AetherAuditViewModel

class MainActivity : ComponentActivity() {

    // Dynamic Permission Requests array matching Target SDK 33+ requirements [User Query]
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var permissionsGranted by remember { mutableStateOf(checkPermissions()) }

            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val allGranted = permissions.values.all { it }
                permissionsGranted = allGranted
            }

            var checkBluetoothTrigger by remember { mutableStateOf(0) }
            val isBluetoothEnabled = remember(permissionsGranted, checkBluetoothTrigger) {
                if (permissionsGranted) {
                    val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                    bluetoothManager?.adapter?.isEnabled == true
                } else {
                    false
                }
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF020617) // Sleek matching dark background
                ) {
                    if (permissionsGranted) {
                        if (isBluetoothEnabled) {
                            val viewModel: AetherAuditViewModel = viewModel()
                            AetherAuditNavigation(viewModel = viewModel)
                        } else {
                            // Secure Bluetooth Blocker Gate Screen [User Query]
                            Box(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.bluetooth),
                                            contentDescription = "Bluetooth Status",
                                            modifier = Modifier.size(72.dp),
                                            tint = Color(0xFF0088FF),
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "BLUETOOTH REQUIRED", 
                                            color = Color(0xFF38BDF8),
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 18.sp, 
                                            letterSpacing = 2.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "To initialize AetherAudit's passive over-the-air scanning engine safely, " +
                                            "you must enable Bluetooth on your device before running the application.\n\n" +
                                            "Turn Bluetooth on from your system's quick settings tray, then tap 'REFRESH' or restart AetherAudit.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { finish() },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("EXIT APP", fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = { checkBluetoothTrigger++ },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("REFRESH", color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Secure Permission Request Gate Screen [CLO2 Quality of work]
                        Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("📡", fontSize = 48.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("PERMISSIONS REQUIRED", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 2.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "To passively monitor physical spaces and log unpatched hardware signatures under SDG 9, " +
                                        "AetherAudit requires Bluetooth Scanning and Location telemetry access.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = { permissionLauncher.launch(requiredPermissions) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                    ) {
                                        Text("AUTHORIZE HARDWARE SCANNER", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
