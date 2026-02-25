package com.example.displaymoto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.displaymoto.ui.screens.dashboard.DashboardScreen
import com.example.displaymoto.ui.screens.dashboard.SettingsScreen // <-- Import da janela de definições
import com.example.displaymoto.ui.screens.dashboard.loading.LoadingScreen
import com.example.displaymoto.ui.theme.DisplayMotoTheme

// Lista dos ecrãs que a app tem
enum class MotoScreen {
    LOADING, DASHBOARD, SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DisplayMotoTheme {
                // A app começa no Loading
                var currentScreen by remember { mutableStateOf(MotoScreen.LOADING) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                    ) {
                        // O "cérebro" que decide qual ecrã mostrar
                        when (currentScreen) {
                            MotoScreen.LOADING -> {
                                LoadingScreen(
                                    onFinished = { currentScreen = MotoScreen.DASHBOARD }
                                )
                            }
                            MotoScreen.DASHBOARD -> {
                                DashboardScreen(
                                    onNavigateToSettings = { currentScreen = MotoScreen.SETTINGS }
                                )
                            }
                            MotoScreen.SETTINGS -> {
                                SettingsScreen(
                                    onNavigateBack = { currentScreen = MotoScreen.DASHBOARD }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}