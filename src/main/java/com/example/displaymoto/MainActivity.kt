package com.example.displaymoto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.displaymoto.ui.screens.dashboard.*
import com.example.displaymoto.ui.screens.dashboard.settings.*

// NOVO: Criamos a variável global invisível que controla a velocidade de todas as animações
val LocalAnimationMultiplier = compositionLocalOf { 1f }

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
            var textSizeScale by rememberSaveable { mutableFloatStateOf(1f) }
            var currentColorFilter by rememberSaveable { mutableStateOf("OFF") }
            var currentTextSpacing by rememberSaveable { mutableStateOf("STANDARD") }

            // NOVO: Memória do estado das Animações
            var currentAnimations by rememberSaveable { mutableStateOf("ON") }

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

            // MATRIZ DE DALTONISMO
            val modifierComFiltro = if (currentColorFilter == "OFF") {
                Modifier.fillMaxSize()
            } else {
                Modifier.fillMaxSize().drawWithContent {
                    val colorMatrix = when (currentColorFilter) {
                        "GRAY SCALE" -> ColorMatrix().apply { setToSaturation(0f) }
                        "PROTANOPIA" -> ColorMatrix(floatArrayOf(
                            0.567f, 0.433f, 0.000f, 0f, 0f,
                            0.558f, 0.442f, 0.000f, 0f, 0f,
                            0.000f, 0.242f, 0.758f, 0f, 0f,
                            0.000f, 0.000f, 0.000f, 1f, 0f
                        ))
                        "DEUTERANOPIA" -> ColorMatrix(floatArrayOf(
                            0.625f, 0.375f, 0.000f, 0f, 0f,
                            0.700f, 0.300f, 0.000f, 0f, 0f,
                            0.000f, 0.300f, 0.700f, 0f, 0f,
                            0.000f, 0.000f, 0.000f, 1f, 0f
                        ))
                        else -> ColorMatrix()
                    }
                    val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(colorMatrix) }
                    drawIntoCanvas { canvas ->
                        canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
                        drawContent()
                        canvas.restore()
                    }
                }
            }

            // MAGIA DE ACESSIBILIDADE WCAG
            val currentDensity = LocalDensity.current
            val customDensity = Density(density = currentDensity.density, fontScale = currentDensity.fontScale * textSizeScale)

            val baseTextStyle = LocalTextStyle.current
            val customTextStyle = if (currentTextSpacing == "EXPANDED") {
                baseTextStyle.copy(letterSpacing = 0.12.em, lineHeight = 1.5.em)
            } else {
                baseTextStyle
            }

            // NOVO: Cálculo do multiplicador com base no botão escolhido
            val animationScale = when (currentAnimations) {
                "ON" -> 1f
                "REDUCED" -> 0.5f
                "OFF" -> 0.001f // Usamos 0.001f em vez de 0 para evitar erros matemáticos do Android, mas aos olhos humanos é instantâneo!
                else -> 1f
            }

            // Injetamos Tamanho, Espaçamento e Velocidade das Animações na App Inteira!
            CompositionLocalProvider(
                LocalDensity provides customDensity,
                LocalTextStyle provides customTextStyle,
                LocalAnimationMultiplier provides animationScale
            ) {
                Surface(modifier = modifierComFiltro, color = corDoFundoTema) {
                    when (currentScreen) {
                        MotoScreen.DASHBOARD -> DashboardScreen(corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast, autonomiaInicial = autonomiaGlobal, aCarregarInicial = aCarregarGlobal, onBateriaChange = { auto, carregando -> autonomiaGlobal = auto; aCarregarGlobal = carregando }, onNavigateToSettings = { currentScreenName = MotoScreen.SETTINGS.name })
                        MotoScreen.SETTINGS -> SettingsScreen(velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaMota, corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast, onCorFundoChange = { novaCor -> corPersonalizadaArgb = novaCor.toArgb(); currentContrast = "STANDARD" }, onNavigateBack = { currentScreenName = MotoScreen.DASHBOARD.name }, onNavigateToPersonalization = { currentScreenName = MotoScreen.PERSONALIZATION.name })
                        MotoScreen.PERSONALIZATION -> PersonalizationSettings(velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaMota, corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast, isIaActivated = isIaActivatedGlobal, onIaChange = { isIaActivatedGlobal = it }, onNavigateBack = { currentScreenName = MotoScreen.SETTINGS.name }, onNavigateToVisual = { currentScreenName = MotoScreen.VISUAL_PREFERENCES.name }, onNavigateToTouch = { currentScreenName = MotoScreen.TOUCH.name }, onNavigateToCognitive = { currentScreenName = MotoScreen.COGNITIVE_ASSISTANT.name }, onNavigateToAudio = { currentScreenName = MotoScreen.AUDIO_HAPTICS.name }, onNavigateToEditIcons = { currentScreenName = MotoScreen.EDIT_ICONS.name })

                        MotoScreen.VISUAL_PREFERENCES -> VisualPreferencesScreen(
                            velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaMota,
                            corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast, textSizeScale = textSizeScale, currentColorFilter = currentColorFilter,
                            currentTextSpacing = currentTextSpacing, onTextSpacingChange = { currentTextSpacing = it },
                            currentAnimations = currentAnimations, // NOVO
                            onAnimationsChange = { currentAnimations = it }, // NOVO
                            onColorFilterChange = { currentColorFilter = it }, onTextSizeChange = { textSizeScale = it }, onContrastChange = { currentContrast = it }, onNavigateBack = { currentScreenName = MotoScreen.PERSONALIZATION.name }
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
}