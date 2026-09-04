package com.example.aetheraudit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aetheraudit.scanner.DiscoveredDevice
import com.example.aetheraudit.scanner.ThreatLevel

@Composable
fun LiveRadarScreen(
    scannedDevices: List<DiscoveredDevice>,
    isScanning: Boolean,
    onToggleScan: () -> Unit,
    onSaveLog: (DiscoveredDevice) -> Unit,
    onTutorialClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Perimeter Scan Data", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onTutorialClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info, 
                        contentDescription = "Show Scanner Guide", 
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
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
