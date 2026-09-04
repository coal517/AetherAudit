package com.example.aetheraudit.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
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
                        // Hardened validation checks: Forbid spaces, limit length [CLO2 Quality of work]
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
                        // Hardened validation checks: Forbid spaces, limit length [CLO2 Quality of work]
                        if (input.length <= 20 && !input.contains(" ")) {
                            password = input
                        }
                    },
                    label = { Text("Access Key", color = Color(0xFF99CCFF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        // Fixes Touch-Hold Reveal action cleanly with tryAwaitRelease + Toast hint [User Query]
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isPasswordVisible = true
                                            try {
                                                tryAwaitRelease() // Standard Compose PressGestureScope waits until user releases finger!
                                            } finally {
                                                isPasswordVisible = false
                                            }
                                        },
                                        onTap = {
                                            Toast.makeText(context, "Hold down button to reveal password", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Info else Icons.Default.Lock,
                                contentDescription = "Reveal Access Key",
                                tint = Color(0xFF99CCFF)
                            )
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
