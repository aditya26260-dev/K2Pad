package com.k2pad.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.k2pad.app.nativebridge.NativeBridge
import com.k2pad.app.ui.theme.K2PadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            K2PadTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Phase1Screen()
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun Phase1Screen() {
    var nativeStatus by remember { mutableStateOf("(not checked yet)") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("K2Pad") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Phase 1: Repository Foundation",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "This build only proves the toolchain — Gradle, Kotlin, " +
                    "Compose/Material3, and the CMake/NDK/JNI bridge — compiles and " +
                    "runs together. No input capture, mapping, Shizuku, or uinput " +
                    "code exists yet; those arrive in Phases 2 through 6.",
                style = MaterialTheme.typography.bodyLarge
            )

            Button(onClick = { nativeStatus = NativeBridge.getNativeStatus() }) {
                Text("Call native layer")
            }

            Text(
                text = "Native status: $nativeStatus",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Phase1ScreenPreview() {
    K2PadTheme {
        Phase1Screen()
    }
}
