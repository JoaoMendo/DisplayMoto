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
import com.example.displaymoto.ui.screens.dashboard.loading.LoadingScreen
import com.example.displaymoto.ui.theme.DisplayMotoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DisplayMotoTheme {
                // O "cérebro" que controla se mostramos o loading ou o dashboard
                var mostrarLoading by remember { mutableStateOf(true) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // O Box usa o espaço seguro do ecrã (innerPadding) para nada ficar cortado
                    Box(modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                    ) {
                        if (mostrarLoading) {
                            // Passa a ordem para o LoadingScreen: "Quando acabares, avisa!"
                            LoadingScreen(onFinished = { mostrarLoading = false })
                        } else {
                            // O loading acabou, mostra o ecrã principal da mota
                            DashboardScreen()
                        }
                    }
                }
            }
        }
    }
}