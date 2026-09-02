package com.example.aetheraudit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aetheraudit.ui.AetherAuditDashboard
import com.example.aetheraudit.ui.theme.AetherAuditTheme
import com.example.aetheraudit.viewmodel.AetherAuditViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Instantiate the unified State ViewModel
                    val viewModel: AetherAuditViewModel = viewModel()

                    // Boot directly into your main Active Radar & Logs dashboard UI
                    AetherAuditDashboard(viewModel = viewModel)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Instantiate the unified State ViewModel
            val viewModel: AetherAuditViewModel = viewModel()

            // Boot directly into your main Active Radar & Logs dashboard UI
            AetherAuditDashboard(viewModel = viewModel)
        }
    }
}