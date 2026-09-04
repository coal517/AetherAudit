package com.example.aetheraudit.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri

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
                    .size(80.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock, 
                    contentDescription = "Security Badge", 
                    modifier = Modifier.size(40.dp),
                    tint = Color(0xFF38BDF8)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("AETHER AUDIT", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Version 1.0.0 (MIT License)", color = Color(0xFF94A3B8), fontSize = 14.sp)
        }

        // Active Profile Management Module [CLO2 Development of mobile solution]
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AccountBox, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Active Operator Details", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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

        // Cyber-Resilience Manifesto [UN SDG Goal 9]
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cyber-Resilience Manifesto", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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

        // Repository & License Module [User Query]
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Academic Repository", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This open-source Android module is distributed under the official terms of the MIT License.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                // Calls an implicit web intent targeting the team's official GitHub repository [User Query]
                                val intent = Intent(Intent.ACTION_VIEW, "https://github.com/coal517/AetherAudit".toUri())
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Text("OPEN GITHUB SOURCE", color = Color.White)
                        }
                        IconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "https://github.com/coal517/AetherAudit")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Link Via"))
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share repository link", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
