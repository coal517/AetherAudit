package com.example.aetheraudit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.aetheraudit.data.LocalOUIEntry

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
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync Icon",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("CLOUD SYNC", color = Color.White)
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
                    onValueChange = { input ->
                        if (input.length <= 8) {
                            manualOUI = input
                            inputError = false
                        }
                    },
                    label = { Text("Target OUI (format: AA:BB:CC)", color = Color(0xFF99CCFF)) },
                    isError = inputError,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        errorTextColor = Color.Red
                    )
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

            TextButton(
                onClick = { showBlacklistDialog = true },
                modifier = Modifier.height(36.dp),
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
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Button(
                                                onClick = { onDeleteOUI(entry) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("DELETE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
}
