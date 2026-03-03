package com.example.displaymoto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.displaymoto.ui.screens.dashboard.*
import com.example.displaymoto.ui.screens.dashboard.settings.*

enum class MotoScreen {
    DASHBOARD, SETTINGS, PERSONALIZATION, VISUAL_PREFERENCES, TOUCH, COGNITIVE_ASSISTANT, AUDIO_HAPTICS, EDIT_ICONS
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
            var currentScreenName by rememberSaveable { mutableStateOf(MotoScreen.DASHBOARD.name) }
            val currentScreen = MotoScreen.valueOf(currentScreenName)

            var corPersonalizadaArgb by rememberSaveable { mutableIntStateOf(android.graphics.Color.parseColor("#0D0F26")) }
            val corPersonalizada = Color(corPersonalizadaArgb)

            var currentContrast by rememberSaveable { mutableStateOf("STANDARD") }
            var isIaActivatedGlobal by rememberSaveable { mutableStateOf(true) }

            var autonomiaGlobal by rememberSaveable { mutableFloatStateOf(200f) }
            var aCarregarGlobal by rememberSaveable { mutableStateOf(false) }

            val velocidadeMota = 0
            val bateriaMota = ((autonomiaGlobal / 200f) * 100f).toInt()
            val tempBatMota = 30
            val tempMotorMota = 80
            val marchaMota = "P"

            val corDoFundoTema = when (currentContrast) {
                "STANDARD" -> corPersonalizada
                "HIGH CONTRAST" -> Color.Black
                "NIGHT MODE" -> lerp(corPersonalizada, Color(0xFF121212), 0.90f)
                else -> corPersonalizada
            }

            Surface(modifier = Modifier.fillMaxSize(), color = corDoFundoTema) {
                when (currentScreen) {
                    MotoScreen.DASHBOARD -> DashboardScreen(
                        corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast,
                        autonomiaInicial = autonomiaGlobal, aCarregarInicial = aCarregarGlobal,
                        onBateriaChange = { auto, carregando -> autonomiaGlobal = auto; aCarregarGlobal = carregando },
                        onNavigateToSettings = { currentScreenName = MotoScreen.SETTINGS.name }
                    )

                    MotoScreen.SETTINGS -> SettingsScreen(
                        velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaMota,
                        corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast,
                        onCorFundoChange = { novaCor -> corPersonalizadaArgb = novaCor.toArgb(); currentContrast = "STANDARD" },
                        onNavigateBack = { currentScreenName = MotoScreen.DASHBOARD.name }, onNavigateToPersonalization = { currentScreenName = MotoScreen.PERSONALIZATION.name }
                    )

                    MotoScreen.PERSONALIZATION -> PersonalizationSettings(
                        velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaMota,
                        corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast,
                        isIaActivated = isIaActivatedGlobal, onIaChange = { isIaActivatedGlobal = it },
                        onNavigateBack = { currentScreenName = MotoScreen.SETTINGS.name }, onNavigateToVisual = { currentScreenName = MotoScreen.VISUAL_PREFERENCES.name }, onNavigateToTouch = { currentScreenName = MotoScreen.TOUCH.name }, onNavigateToCognitive = { currentScreenName = MotoScreen.COGNITIVE_ASSISTANT.name }, onNavigateToAudio = { currentScreenName = MotoScreen.AUDIO_HAPTICS.name }, onNavigateToEditIcons = { currentScreenName = MotoScreen.EDIT_ICONS.name }
                    )

                    MotoScreen.VISUAL_PREFERENCES -> VisualPreferencesScreen(
                        velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaMota,
                        corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast,
                        onContrastChange = { novoContraste -> currentContrast = novoContraste }, onNavigateBack = { currentScreenName = MotoScreen.PERSONALIZATION.name }
                    )

                    MotoScreen.TOUCH -> TouchScreen(velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaMota, corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast, onNavigateBack = { currentScreenName = MotoScreen.PERSONALIZATION.name })
                    MotoScreen.COGNITIVE_ASSISTANT -> CognitiveAssistantScreen(velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaMota, corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast, onNavigateBack = { currentScreenName = MotoScreen.PERSONALIZATION.name })
                    MotoScreen.AUDIO_HAPTICS -> AudioHapticsScreen(velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaMota, corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast, onNavigateBack = { currentScreenName = MotoScreen.PERSONALIZATION.name })
                    MotoScreen.EDIT_ICONS -> EditIconsScreen(velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaMota, corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast, onNavigateBack = { currentScreenName = MotoScreen.PERSONALIZATION.name })
                }
            }
        }
    }
}