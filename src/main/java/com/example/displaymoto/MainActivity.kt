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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

            // Memória Visual
            var currentContrast by rememberSaveable { mutableStateOf("STANDARD") }
            var isIaActivatedGlobal by rememberSaveable { mutableStateOf(true) }
            var textSizeScale by rememberSaveable { mutableFloatStateOf(1f) }
            var currentColorFilter by rememberSaveable { mutableStateOf("OFF") }
            var currentTextSpacing by rememberSaveable { mutableStateOf("STANDARD") }
            var currentAnimations by rememberSaveable { mutableStateOf("ON") }

            // AI Color States
            var aiCorDestaque by remember { mutableStateOf<Color?>(null) }
            var aiPrimaryText by remember { mutableStateOf<Color?>(null) }
            var aiSecondaryText by remember { mutableStateOf<Color?>(null) }
            
            // Pop-up de Confirmação da IA
            var originalUserPalette by remember { mutableStateOf<CompleteAppPalette?>(null) }
            var showAiColorPopup by remember { mutableStateOf(false) }
            
            val coroutineScope = rememberCoroutineScope()
            // Debounce: cancelar pedido anterior quando o utilizador muda rapidamente
            var pendingAiJob by remember { mutableStateOf<Job?>(null) }

            // Memória Touch (NOVO)
            var currentTouchArea by rememberSaveable { mutableStateOf("STANDARD") }
            var currentMethod by rememberSaveable { mutableStateOf("DIRECT TOUCH") }
            var currentResponseTime by rememberSaveable { mutableStateOf("MEDIUM") }
            var currentErrorPrevention by rememberSaveable { mutableStateOf("ON") }

            // Memória Cognitiva (NOVO)
            var currentLanguage by rememberSaveable { mutableStateOf("STANDARD") }
            var currentDensity by rememberSaveable { mutableStateOf("STANDARD") }
            var currentHelp by rememberSaveable { mutableStateOf("ON DEMAND") }
            var currentAlerts by rememberSaveable { mutableStateOf("STANDARD") }

            // Memória Audio & Haptics (NOVO)
            var currentFeedback by rememberSaveable { mutableStateOf("BOTH") }
            var currentVisualAlerts by rememberSaveable { mutableStateOf("ON") }
            var currentErrorFeedback by rememberSaveable { mutableStateOf("HAPTIC BOOST") }

            // Língua Global
            var appLanguageName by rememberSaveable { mutableStateOf(AppLanguage.EN.name) }
            val appLanguage = AppLanguage.valueOf(appLanguageName)
            val s = getAppStrings(appLanguage)

            var autonomiaGlobal by rememberSaveable { mutableFloatStateOf(200f) }
            var aCarregarGlobal by rememberSaveable { mutableStateOf(false) }

            val velocidadeMota = 82
            val bateriaMota = ((autonomiaGlobal / 200f) * 100f).toInt()
            val tempBatMota = 30
            val tempMotorMota = 80
            val marchaMota = "D"

            val corDoFundoTema = when (currentContrast) {
                "STANDARD" -> corPersonalizada
                "HIGH CONTRAST" -> Color.Black
                "NIGHT MODE" -> lerp(corPersonalizada, Color(0xFF121212), 0.90f)
                else -> corPersonalizada
            }

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

            val currentDensityVal = LocalDensity.current
            val customDensity = Density(density = currentDensityVal.density, fontScale = currentDensityVal.fontScale * textSizeScale)

            val baseTextStyle = LocalTextStyle.current
            val customTextStyle = if (currentTextSpacing == "EXPANDED") {
                baseTextStyle.copy(letterSpacing = 0.12.em, lineHeight = 1.5.em)
            } else {
                baseTextStyle
            }

            val animationScale = when (currentAnimations) {
                "ON" -> 1f
                "REDUCED" -> 0.5f
                "OFF" -> 0.001f
                else -> 1f
            }

            CompositionLocalProvider(
                LocalDensity provides customDensity,
                LocalTextStyle provides customTextStyle,
                LocalAnimationMultiplier provides animationScale
            ) {
                Surface(modifier = modifierComFiltro, color = corDoFundoTema) {
                    when (currentScreen) {
                        MotoScreen.DASHBOARD -> DashboardScreen(s = s, corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast, autonomiaInicial = autonomiaGlobal, aCarregarInicial = aCarregarGlobal, onBateriaChange = { auto, carregando -> autonomiaGlobal = auto; aCarregarGlobal = carregando }, onNavigateToSettings = { currentScreenName = MotoScreen.SETTINGS.name }, aiCorDestaque = aiCorDestaque, aiPrimaryText = aiPrimaryText, aiSecondaryText = aiSecondaryText)
                        MotoScreen.SETTINGS -> SettingsScreen(s = s, currentAppLanguage = appLanguage, onAppLanguageChange = { appLanguageName = it.name }, velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaMota, corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast, onCorFundoChange = { novaCor -> 
                            corPersonalizadaArgb = novaCor.toArgb()
                            currentContrast = "STANDARD"
                            // Verificar se as cores atuais dos elementos ainda têm contraste suficiente
                            if (isIaActivatedGlobal) {
                                val currentAccent = aiCorDestaque
                                val currentPrimary = aiPrimaryText
                                val currentSecondary = aiSecondaryText
                                // Se o utilizador já tem cores definidas, verificar se têm contraste
                                if (currentAccent != null && currentPrimary != null && currentSecondary != null) {
                                    val accentOk = GeminiColorHelper.verificarContraste(currentAccent, novaCor, GeminiColorHelper.TipoComponente.INTERATIVO).aprovado
                                    val primaryOk = GeminiColorHelper.verificarContraste(currentPrimary, novaCor, GeminiColorHelper.TipoComponente.TEXTO_GRANDE).aprovado
                                    val secondaryOk = GeminiColorHelper.verificarContraste(currentSecondary, novaCor, GeminiColorHelper.TipoComponente.TEXTO_NORMAL).aprovado
                                    if (!accentOk || !primaryOk || !secondaryOk) {
                                        pendingAiJob?.cancel()
                                        pendingAiJob = coroutineScope.launch {
                                            originalUserPalette = CompleteAppPalette(novaCor, currentPrimary, currentSecondary, currentAccent)
                                            // Aplicar fallback local IMEDIATAMENTE
                                            val fallback = GeminiColorHelper.computeFallbackPalette(novaCor, GeminiColorHelper.ComponenteLock.BACKGROUND, novaCor)
                                            corPersonalizadaArgb = fallback.background.toArgb()
                                            aiCorDestaque = fallback.accentColor
                                            aiPrimaryText = fallback.primaryText
                                            aiSecondaryText = fallback.secondaryText
                                            
                                            // Debounce: esperar antes de chamar a API
                                            delay(500)
                                            
                                            val adaptadas = GeminiColorHelper.adaptPaletteToUserChoice(novaCor, GeminiColorHelper.ComponenteLock.BACKGROUND)
                                            if (adaptadas != null) {
                                                corPersonalizadaArgb = adaptadas.background.toArgb()
                                                aiCorDestaque = adaptadas.accentColor
                                                aiPrimaryText = adaptadas.primaryText
                                                aiSecondaryText = adaptadas.secondaryText
                                            }
                                            
                                            showAiColorPopup = true
                                        }
                                    }
                                    // Se tudo tem contraste → não mexer! Respeitar personalização.
                                } else {
                                    // Sem cores personalizadas ainda — aplicação completa com GUARDIÃO AAA
                                    val hexColor = String.format("#%06X", 0xFFFFFF and novaCor.toArgb())
                                    val localColors = GeminiColorHelper.enforceAAA(
                                        GeminiColorHelper.computeLocalContrastColors(hexColor), novaCor
                                    )
                                    aiCorDestaque = localColors.accentColor
                                    aiPrimaryText = localColors.primaryText
                                    aiSecondaryText = localColors.secondaryText
                                    pendingAiJob?.cancel()
                                    pendingAiJob = coroutineScope.launch {
                                        delay(500)
                                        val adaptadas = GeminiColorHelper.adaptColorsToBackground(hexColor)
                                        if (adaptadas != null) {
                                            val safe = GeminiColorHelper.enforceAAA(adaptadas, novaCor)
                                            aiCorDestaque = safe.accentColor
                                            aiPrimaryText = safe.primaryText
                                            aiSecondaryText = safe.secondaryText
                                        }
                                    }
                                }
                            }
                        }, onCorElementosChange = { novaCor ->
                            aiCorDestaque = novaCor
                            if (isIaActivatedGlobal) {
                                val check = GeminiColorHelper.verificarContraste(novaCor, corPersonalizada, GeminiColorHelper.TipoComponente.INTERATIVO)
                                if (!check.aprovado) {
                                    pendingAiJob?.cancel()
                                    pendingAiJob = coroutineScope.launch {
                                        val lastPrimary = aiPrimaryText ?: Color.White
                                        val lastSecondary = aiSecondaryText ?: Color.LightGray
                                        originalUserPalette = CompleteAppPalette(corPersonalizada, lastPrimary, lastSecondary, novaCor)
                                        // Aplicar fallback local IMEDIATAMENTE
                                        val fallback = GeminiColorHelper.computeFallbackPalette(novaCor, GeminiColorHelper.ComponenteLock.ACCENT, corPersonalizada)
                                        corPersonalizadaArgb = fallback.background.toArgb()
                                        aiCorDestaque = fallback.accentColor
                                        aiPrimaryText = fallback.primaryText
                                        aiSecondaryText = fallback.secondaryText
                                        
                                        delay(500)
                                        
                                        val adaptadas = GeminiColorHelper.adaptPaletteToUserChoice(novaCor, GeminiColorHelper.ComponenteLock.ACCENT)
                                        if (adaptadas != null) {
                                            corPersonalizadaArgb = adaptadas.background.toArgb()
                                            aiCorDestaque = adaptadas.accentColor
                                            aiPrimaryText = adaptadas.primaryText
                                            aiSecondaryText = adaptadas.secondaryText
                                        }
                                        
                                        showAiColorPopup = true
                                    }
                                }
                            }
                        }, onCorTextoChange = { novaCor ->
                            aiPrimaryText = novaCor
                            if (isIaActivatedGlobal) {
                                val check = GeminiColorHelper.verificarContraste(novaCor, corPersonalizada, GeminiColorHelper.TipoComponente.TEXTO_GRANDE)
                                if (!check.aprovado) {
                                    pendingAiJob?.cancel()
                                    pendingAiJob = coroutineScope.launch {
                                        val lastAccent = aiCorDestaque ?: Color.White
                                        val lastSecondary = aiSecondaryText ?: Color.LightGray
                                        originalUserPalette = CompleteAppPalette(corPersonalizada, novaCor, lastSecondary, lastAccent)
                                        val fallback = GeminiColorHelper.computeFallbackPalette(novaCor, GeminiColorHelper.ComponenteLock.PRIMARY_TEXT, corPersonalizada)
                                        corPersonalizadaArgb = fallback.background.toArgb()
                                        aiCorDestaque = fallback.accentColor
                                        aiPrimaryText = fallback.primaryText
                                        aiSecondaryText = fallback.secondaryText
                                        
                                        delay(500)
                                        
                                        val adaptadas = GeminiColorHelper.adaptPaletteToUserChoice(novaCor, GeminiColorHelper.ComponenteLock.PRIMARY_TEXT)
                                        if (adaptadas != null) {
                                            corPersonalizadaArgb = adaptadas.background.toArgb()
                                            aiCorDestaque = adaptadas.accentColor
                                            aiPrimaryText = adaptadas.primaryText
                                            aiSecondaryText = adaptadas.secondaryText
                                        }
                                        
                                        showAiColorPopup = true
                                    }
                                }
                            }
                        }, onCorTextoSecundarioChange = { novaCor ->
                            aiSecondaryText = novaCor
                            if (isIaActivatedGlobal) {
                                val check = GeminiColorHelper.verificarContraste(novaCor, corPersonalizada, GeminiColorHelper.TipoComponente.TEXTO_NORMAL)
                                if (!check.aprovado) {
                                    pendingAiJob?.cancel()
                                    pendingAiJob = coroutineScope.launch {
                                        val lastPrimary = aiPrimaryText ?: Color.White
                                        val lastAccent = aiCorDestaque ?: Color.White
                                        originalUserPalette = CompleteAppPalette(corPersonalizada, lastPrimary, novaCor, lastAccent)
                                        val fallback = GeminiColorHelper.computeFallbackPalette(novaCor, GeminiColorHelper.ComponenteLock.SECONDARY_TEXT, corPersonalizada)
                                        corPersonalizadaArgb = fallback.background.toArgb()
                                        aiCorDestaque = fallback.accentColor
                                        aiPrimaryText = fallback.primaryText
                                        aiSecondaryText = fallback.secondaryText
                                        
                                        delay(500)
                                        
                                        val adaptadas = GeminiColorHelper.adaptPaletteToUserChoice(novaCor, GeminiColorHelper.ComponenteLock.SECONDARY_TEXT)
                                        if (adaptadas != null) {
                                            corPersonalizadaArgb = adaptadas.background.toArgb()
                                            aiCorDestaque = adaptadas.accentColor
                                            aiPrimaryText = adaptadas.primaryText
                                            aiSecondaryText = adaptadas.secondaryText
                                        }
                                        
                                        showAiColorPopup = true
                                    }
                                }
                            }
                        }, onNavigateBack = { currentScreenName = MotoScreen.DASHBOARD.name }, onNavigateToPersonalization = { currentScreenName = MotoScreen.PERSONALIZATION.name }, aiCorDestaque = aiCorDestaque, aiPrimaryText = aiPrimaryText, aiSecondaryText = aiSecondaryText)
                        MotoScreen.PERSONALIZATION -> PersonalizationSettings(s = s, velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaMota, corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast, isIaActivated = isIaActivatedGlobal, onIaChange = { isIaActivatedGlobal = it }, onNavigateBack = { currentScreenName = MotoScreen.SETTINGS.name }, onNavigateToVisual = { currentScreenName = MotoScreen.VISUAL_PREFERENCES.name }, onNavigateToTouch = { currentScreenName = MotoScreen.TOUCH.name }, onNavigateToCognitive = { currentScreenName = MotoScreen.COGNITIVE_ASSISTANT.name }, onNavigateToAudio = { currentScreenName = MotoScreen.AUDIO_HAPTICS.name }, onNavigateToEditIcons = { currentScreenName = MotoScreen.EDIT_ICONS.name }, aiCorDestaque = aiCorDestaque, aiPrimaryText = aiPrimaryText, aiSecondaryText = aiSecondaryText)
                        MotoScreen.VISUAL_PREFERENCES -> VisualPreferencesScreen(s = s, velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaMota, corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast, textSizeScale = textSizeScale, currentColorFilter = currentColorFilter, currentTextSpacing = currentTextSpacing, onTextSpacingChange = { currentTextSpacing = it }, currentAnimations = currentAnimations, onAnimationsChange = { currentAnimations = it }, onColorFilterChange = { currentColorFilter = it }, onTextSizeChange = { textSizeScale = it }, onContrastChange = { currentContrast = it }, onNavigateBack = { currentScreenName = MotoScreen.PERSONALIZATION.name }, aiCorDestaque = aiCorDestaque, aiPrimaryText = aiPrimaryText, aiSecondaryText = aiSecondaryText)

                        MotoScreen.TOUCH -> TouchScreen(s = s, velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaMota, corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast, currentTouchArea = currentTouchArea, onTouchAreaChange = { currentTouchArea = it }, currentMethod = currentMethod, onMethodChange = { currentMethod = it }, currentResponseTime = currentResponseTime, onResponseTimeChange = { currentResponseTime = it }, currentErrorPrevention = currentErrorPrevention, onErrorPreventionChange = { currentErrorPrevention = it }, onNavigateBack = { currentScreenName = MotoScreen.PERSONALIZATION.name }, aiCorDestaque = aiCorDestaque, aiPrimaryText = aiPrimaryText, aiSecondaryText = aiSecondaryText)

                        MotoScreen.COGNITIVE_ASSISTANT -> CognitiveAssistantScreen(s = s, velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaMota, corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast, currentLanguage = currentLanguage, onLanguageChange = { currentLanguage = it }, currentDensity = currentDensity, onDensityChange = { currentDensity = it }, currentHelp = currentHelp, onHelpChange = { currentHelp = it }, currentAlerts = currentAlerts, onAlertsChange = { currentAlerts = it }, onNavigateBack = { currentScreenName = MotoScreen.PERSONALIZATION.name }, aiCorDestaque = aiCorDestaque, aiPrimaryText = aiPrimaryText, aiSecondaryText = aiSecondaryText)

                        MotoScreen.AUDIO_HAPTICS -> AudioHapticsScreen(s = s, velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaMota, corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast, currentFeedback = currentFeedback, onFeedbackChange = { currentFeedback = it }, currentVisualAlerts = currentVisualAlerts, onVisualAlertsChange = { currentVisualAlerts = it }, currentErrorFeedback = currentErrorFeedback, onErrorFeedbackChange = { currentErrorFeedback = it }, onNavigateBack = { currentScreenName = MotoScreen.PERSONALIZATION.name }, aiCorDestaque = aiCorDestaque, aiPrimaryText = aiPrimaryText, aiSecondaryText = aiSecondaryText)

                        MotoScreen.EDIT_ICONS -> EditIconsScreen(s = s, velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaMota, corFundoAtual = corDoFundoTema, corPersonalizada = corPersonalizada, currentContrast = currentContrast, onNavigateBack = { currentScreenName = MotoScreen.PERSONALIZATION.name }, aiCorDestaque = aiCorDestaque, aiPrimaryText = aiPrimaryText, aiSecondaryText = aiSecondaryText)
                    }

                    if (showAiColorPopup && originalUserPalette != null) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showAiColorPopup = false },
                            title = { androidx.compose.material3.Text("Aviso de Acessibilidade") },
                            text = { androidx.compose.material3.Text("A combinação de cores que escolheu dificulta a leitura do painel. A IA já ajustou automaticamente as cores para garantir a sua segurança e legibilidade. Deseja manter estes ajustes?") },
                            confirmButton = {
                                androidx.compose.material3.TextButton(onClick = {
                                    // Manter as alterações da IA, basta fechar o popup
                                    showAiColorPopup = false
                                }) {
                                    androidx.compose.material3.Text("Manter Ajustes", color = androidx.compose.ui.graphics.Color.White)
                                }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(onClick = {
                                    // Reverter para as alterações sem contraste do utilizador
                                    val manual = originalUserPalette!!
                                    corPersonalizadaArgb = manual.background.toArgb()
                                    aiCorDestaque = manual.accentColor
                                    aiPrimaryText = manual.primaryText
                                    aiSecondaryText = manual.secondaryText
                                    showAiColorPopup = false
                                }) {
                                    androidx.compose.material3.Text("Reverter Alterações", color = androidx.compose.ui.graphics.Color.Gray)
                                }
                            },
                            containerColor = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
                            titleContentColor = androidx.compose.ui.graphics.Color.White,
                            textContentColor = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        }
    }
}