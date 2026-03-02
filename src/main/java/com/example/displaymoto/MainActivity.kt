package com.example.displaymoto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.displaymoto.ui.screens.dashboard.DashboardScreen
import com.example.displaymoto.ui.screens.dashboard.PersonalizationSettings
import com.example.displaymoto.ui.screens.dashboard.SettingsScreen

enum class MotoScreen {
    DASHBOARD,
    SETTINGS,
    PERSONALIZATION
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            var currentScreen by remember { mutableStateOf(MotoScreen.DASHBOARD) }
            var corDoFundoTema by remember { mutableStateOf(Color(0xFF0D0F26)) }

            // Variáveis globais da bateria
            var autonomiaGlobal by remember { mutableFloatStateOf(200f) }
            var aCarregarGlobal by remember { mutableStateOf(false) }

            val velocidadeMota = 0
            // A bateria agora é calculada com base na autonomia global
            val bateriaMota = ((autonomiaGlobal / 200f) * 100f).toInt()
            val tempBatMota = 30
            val tempMotorMota = 80
            val marchaMota = "P"

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = corDoFundoTema
            ) {
                when (currentScreen) {
                    MotoScreen.DASHBOARD -> {
                        DashboardScreen(
                            corFundoAtual = corDoFundoTema,
                            autonomiaInicial = autonomiaGlobal,
                            aCarregarInicial = aCarregarGlobal,
                            onBateriaChange = { auto, carregando ->
                                autonomiaGlobal = auto
                                aCarregarGlobal = carregando
                            },
                            onNavigateToSettings = { currentScreen = MotoScreen.SETTINGS }
                        )
                    }
                    MotoScreen.SETTINGS -> {
                        SettingsScreen(
                            velocidadeAtual = velocidadeMota,
                            bateriaAtual = bateriaMota,
                            aCarregarAtual = aCarregarGlobal,
                            tempBateriaAtual = tempBatMota,
                            tempMotorAtual = tempMotorMota,
                            marchaAtual = marchaMota,
                            corFundoAtual = corDoFundoTema,
                            onCorFundoChange = { novaCor -> corDoFundoTema = novaCor },
                            onNavigateBack = { currentScreen = MotoScreen.DASHBOARD },
                            onNavigateToPersonalization = { currentScreen = MotoScreen.PERSONALIZATION }
                        )
                    }
                    MotoScreen.PERSONALIZATION -> {
                        PersonalizationSettings(
                            velocidadeAtual = velocidadeMota,
                            bateriaAtual = bateriaMota,
                            aCarregarAtual = aCarregarGlobal,
                            tempBateriaAtual = tempBatMota,
                            tempMotorAtual = tempMotorMota,
                            marchaAtual = marchaMota,
                            corFundoAtual = corDoFundoTema,
                            onNavigateBack = { currentScreen = MotoScreen.SETTINGS }
                        )
                    }
                }
            }
        }
    }
}