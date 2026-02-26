package com.example.displaymoto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.displaymoto.ui.screens.dashboard.DashboardScreen
import com.example.displaymoto.ui.screens.dashboard.SettingsScreen

// Enumerador simples para gerir qual ecrã está aberto
enum class MotoScreen {
    DASHBOARD,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // BÓNUS: Esconder a barra de estado do Android (Modo Imersivo)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            // 1. Variável que controla a navegação entre os ecrãs
            var currentScreen by remember { mutableStateOf(MotoScreen.DASHBOARD) }

            // 2. O CÉREBRO DA COR: Guarda a cor atual e partilha com os dois ecrãs!
            var corDoFundoTema by remember { mutableStateOf(Color(0xFF0D0F26)) } // Começa no Azul Escuro original

            // 3. Simulação dos dados da mota (que futuramente virão de sensores reais)
            val velocidadeMota = 0
            val bateriaMota = 85
            val tempBatMota = 30
            val tempMotorMota = 80
            val marchaMota = "P"

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = corDoFundoTema
            ) {
                // Alterna entre a Dashboard e as Settings
                when (currentScreen) {
                    MotoScreen.DASHBOARD -> {
                        DashboardScreen(
                            corFundoAtual = corDoFundoTema,
                            onNavigateToSettings = { currentScreen = MotoScreen.SETTINGS }
                        )
                    }
                    MotoScreen.SETTINGS -> {
                        SettingsScreen(
                            velocidadeAtual = velocidadeMota,
                            bateriaAtual = bateriaMota,
                            tempBateriaAtual = tempBatMota,
                            tempMotorAtual = tempMotorMota,
                            marchaAtual = marchaMota,
                            corFundoAtual = corDoFundoTema,
                            // Quando clicas numa cor nas definições, esta linha atualiza o cérebro (e a app toda muda!)
                            onCorFundoChange = { novaCor -> corDoFundoTema = novaCor },
                            onNavigateBack = { currentScreen = MotoScreen.DASHBOARD }
                        )
                    }
                }
            }
        }
    }
}