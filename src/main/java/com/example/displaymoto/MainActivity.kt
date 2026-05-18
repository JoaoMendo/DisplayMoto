package com.example.displaymoto

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import com.example.displaymoto.ui.screens.dashboard.*
import com.example.displaymoto.ui.screens.dashboard.settings.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalAnimationMultiplier = compositionLocalOf { 1f }
val LocalMissClickTracker = compositionLocalOf<() -> Unit> { {} }

enum class MotoScreen {
    DASHBOARD, NAVIGATION, SETTINGS, PERSONALIZATION, VISUAL_PREFERENCES, TOUCH, COGNITIVE_ASSISTANT, AUDIO_HAPTICS, EDIT_ICONS
}

/** Conversão de km/h para a unidade preferida. */
fun converterVelocidade(kmh: Int, unidade: String): Int =
    if (unidade == "mph") (kmh * 0.621371).toInt() else kmh

/** Sufixo de unidade ("km/h" ou "mph"). */
fun sufixoVelocidade(unidade: String): String = if (unidade == "mph") "mph" else "km/h"

/** Conversão de metros para string formatada na unidade preferida. */
fun formatarDistanciaPref(metros: Double, unidade: String): String =
    if (unidade == "mph") {
        val mi = metros / 1609.344
        if (mi >= 0.1) String.format(java.util.Locale.US, "%.1f mi", mi)
        else "${(metros * 3.28084).toInt()} ft"
    } else {
        if (metros >= 1000) String.format(java.util.Locale.US, "%.1f km", metros / 1000.0)
        else "${metros.toInt()} m"
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
            var aiCorDestaque by remember { mutableStateOf<Color?>(Color.White) }
            var aiPrimaryText by remember { mutableStateOf<Color?>(Color.White) }
            var aiSecondaryText by remember { mutableStateOf<Color?>(Color.White) }
            
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
            val sBase = getAppStrings(appLanguage)
            val s = adaptToLanguageComplexity(sBase, currentLanguage, appLanguage)
            val context = LocalContext.current

            // === TTS para alertas falados (ABS, travão, bateria, etc.) ===
            val ttsSpeaker = remember { TtsAlertSpeaker(context) }
            DisposableEffect(Unit) { onDispose { ttsSpeaker.shutdown() } }
            LaunchedEffect(appLanguage) { ttsSpeaker.setLocale(appLanguageToLocale(appLanguage)) }
            LaunchedEffect(currentFeedback) {
                ttsSpeaker.setAudioEnabled(currentFeedback == "AUDIO" || currentFeedback == "BOTH")
            }
            val speakAlert: (String, Boolean) -> Unit = remember(ttsSpeaker) {
                { msg, grave -> ttsSpeaker.speak(msg, grave) }
            }

            // === Controlador unificado de feedback (beep + vibração + TTS + flash) ===
            val alertController = remember { AlertFeedbackController(context, speakAlert) }
            alertController.feedbackMode = currentFeedback
            alertController.errorIntensity = currentErrorFeedback
            alertController.visualAlertsOn = (currentVisualAlerts == "ON")
            alertController.alertsFilter = currentAlerts
            val alertFeedback: (String, Boolean) -> Unit = remember(alertController) {
                { msg, grave -> alertController.trigger(msg, grave) }
            }

            var globalMissedClicks by rememberSaveable { mutableIntStateOf(0) }
            val onMissClick: () -> Unit = {
                globalMissedClicks++
                if (globalMissedClicks >= 5) {
                    textSizeScale = 1.3f
                    val msg = if (appLanguage == AppLanguage.PT) "IA detetou dificuldades: Tamanho de letra aumentado globalmente." else "AI Detected Difficulty: Global text size increased."
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                    globalMissedClicks = 0
                }
            }

            var autonomiaGlobal by rememberSaveable { mutableFloatStateOf(200f) }
            var aCarregarGlobal by rememberSaveable { mutableStateOf(false) }

            val bateriaMota = ((autonomiaGlobal / 200f) * 100f).toInt()
            var marchaGlobal by rememberSaveable { mutableStateOf("P") }
            var motoLigadaGlobal by rememberSaveable { mutableStateOf(false) }
            var velocidadeGlobal by rememberSaveable { mutableFloatStateOf(0f) }
            val indicadores = remember { IndicadoresState() }
            val rota = remember { com.example.displaymoto.ui.screens.navigation.RotaState() }

            // === Settings regionais / veículo (persistidos) ===
            var unidadeVelocidade by rememberSaveable { mutableStateOf("km/h") } // "km/h" ou "mph"
            var velocidadeMaximaKmh by rememberSaveable { mutableIntStateOf(200) } // ECE R39: cobrir Vmax+20%
            var autoBrightnessAtivo by rememberSaveable { mutableStateOf(true) }

            // Auto-brilho por sensor de luz + sugestão automática de NIGHT MODE
            // O utilizador pode forçar manualmente STANDARD/HIGH CONTRAST nas settings;
            // só auto-trocamos contraste se estiver em "STANDARD" (não pisamos preferências).
            AutoBrightnessEffect(
                enabled = autoBrightnessAtivo,
                onSuggestNightMode = { noite ->
                    if (noite && currentContrast == "STANDARD") currentContrast = "NIGHT MODE"
                    else if (!noite && currentContrast == "NIGHT MODE") currentContrast = "STANDARD"
                }
            )

            val velocidadeMota = velocidadeGlobal.toInt()
            val tempBatMota = if (motoLigadaGlobal) 50 else 0
            val tempMotorMota = if (motoLigadaGlobal) 50 else 0

            val corDoFundoTema = when (currentContrast) {
                "STANDARD" -> corPersonalizada
                "HIGH CONTRAST" -> Color.Black
                "NIGHT MODE" -> lerp(corPersonalizada, Color(0xFF121212), 0.90f)
                else -> corPersonalizada
            }

            val animFundoTema by animateColorAsState(targetValue = corDoFundoTema, animationSpec = tween(durationMillis = 400))
            val animCorPersonalizada by animateColorAsState(targetValue = corPersonalizada, animationSpec = tween(durationMillis = 400))
            val animCorDestaque by animateColorAsState(targetValue = aiCorDestaque ?: Color.White, animationSpec = tween(durationMillis = 400))
            val animPrimaryText by animateColorAsState(targetValue = aiPrimaryText ?: Color.White, animationSpec = tween(durationMillis = 400))
            val animSecondaryText by animateColorAsState(targetValue = aiSecondaryText ?: Color.LightGray, animationSpec = tween(durationMillis = 400))

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
                LocalAnimationMultiplier provides animationScale,
                LocalMissClickTracker provides onMissClick,
                LocalSpeakAlert provides speakAlert,
                LocalAlertFeedback provides alertFeedback,
                LocalHelpMode provides currentHelp
            ) {
              Box(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = modifierComFiltro.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onMissClick() }
                        )
                    }, 
                    color = animFundoTema
                ) {
                    when (currentScreen) {
                            MotoScreen.DASHBOARD -> DashboardScreen(s = s, marchaAtual = marchaGlobal, onMarchaChange = { marchaGlobal = it }, motoLigadaInicial = motoLigadaGlobal, velocidadeInicial = velocidadeGlobal, onMotoLigadaChange = { motoLigadaGlobal = it }, onVelocidadeChange = { velocidadeGlobal = it }, corFundoAtual = animFundoTema, corPersonalizada = animCorPersonalizada, currentContrast = currentContrast, autonomiaInicial = autonomiaGlobal, aCarregarInicial = aCarregarGlobal, onBateriaChange = { auto, carregando -> autonomiaGlobal = auto; aCarregarGlobal = carregando }, onNavigateToSettings = { currentScreenName = MotoScreen.SETTINGS.name }, onNavigateToNavigation = { currentScreenName = MotoScreen.NAVIGATION.name }, aiCorDestaque = animCorDestaque, aiPrimaryText = animPrimaryText, aiSecondaryText = animSecondaryText, isSimplifiedMode = currentDensity == "ESSENTIAL" || velocidadeMota >= 80, densityMode = currentDensity, unidadeVelocidade = unidadeVelocidade, velocidadeMaximaKmh = velocidadeMaximaKmh, indicadores = indicadores)
                            MotoScreen.NAVIGATION -> com.example.displaymoto.ui.screens.navigation.NavigationScreen(s = s, velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaGlobal, corFundoAtual = animFundoTema, corPersonalizada = animCorPersonalizada, currentContrast = currentContrast, unidadeVelocidade = unidadeVelocidade, onNavigateBack = { currentScreenName = MotoScreen.DASHBOARD.name }, aiCorDestaque = animCorDestaque, aiPrimaryText = animPrimaryText, aiSecondaryText = animSecondaryText, indicadores = indicadores, rota = rota)
                        MotoScreen.SETTINGS -> SettingsScreen(s = s, currentAppLanguage = appLanguage, onAppLanguageChange = { appLanguageName = it.name }, velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaGlobal, corFundoAtual = animFundoTema, corPersonalizada = animCorPersonalizada, currentContrast = currentContrast, unidadeVelocidade = unidadeVelocidade, onUnidadeChange = { unidadeVelocidade = it },                         onCorFundoChange = { novaCor -> 
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
                                        originalUserPalette = CompleteAppPalette(novaCor, currentPrimary, currentSecondary, currentAccent)
                                        // Aplicar fallback local IMEDIATAMENTE (Cálculo heurístico inteligente de contraste WCAG AAA)
                                        val fallback = GeminiColorHelper.computeFallbackPalette(novaCor, GeminiColorHelper.ComponenteLock.BACKGROUND, novaCor)
                                        corPersonalizadaArgb = fallback.background.toArgb()
                                        aiCorDestaque = fallback.accentColor
                                        aiPrimaryText = fallback.primaryText
                                        aiSecondaryText = fallback.secondaryText
                                        
                                        android.widget.Toast.makeText(context, "IA a otimizar contraste do painel...", android.widget.Toast.LENGTH_SHORT).show()
                                        
                                        pendingAiJob?.cancel()
                                        pendingAiJob = coroutineScope.launch {
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
                                    val hexColor = String.format("#%06X", 0xFFFFFF and novaCor.toArgb())
                                    val localColors = GeminiColorHelper.enforceAAA(
                                        GeminiColorHelper.computeLocalContrastColors(hexColor), novaCor
                                    )
                                    aiCorDestaque = localColors.accentColor
                                    aiPrimaryText = localColors.primaryText
                                    aiSecondaryText = localColors.secondaryText
                                    
                                    android.widget.Toast.makeText(context, "IA a gerar contraste perfeito...", android.widget.Toast.LENGTH_SHORT).show()
                                    
                                    pendingAiJob?.cancel()
                                    pendingAiJob = coroutineScope.launch {
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
                                    val lastPrimary = aiPrimaryText ?: Color.White
                                    val lastSecondary = aiSecondaryText ?: Color.LightGray
                                    originalUserPalette = CompleteAppPalette(corPersonalizada, lastPrimary, lastSecondary, novaCor)
                                    // Aplicar fallback local IMEDIATAMENTE (Cálculo heurístico inteligente de contraste WCAG AAA)
                                    val fallback = GeminiColorHelper.computeFallbackPalette(novaCor, GeminiColorHelper.ComponenteLock.ACCENT, corPersonalizada)
                                    corPersonalizadaArgb = fallback.background.toArgb()
                                    aiCorDestaque = fallback.accentColor
                                    aiPrimaryText = fallback.primaryText
                                    aiSecondaryText = fallback.secondaryText
                                    
                                    android.widget.Toast.makeText(context, "IA a otimizar contraste do painel...", android.widget.Toast.LENGTH_SHORT).show()
                                    
                                    pendingAiJob?.cancel()
                                    pendingAiJob = coroutineScope.launch {
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
                                    val lastAccent = aiCorDestaque ?: Color.White
                                    val lastSecondary = aiSecondaryText ?: Color.LightGray
                                    originalUserPalette = CompleteAppPalette(corPersonalizada, novaCor, lastSecondary, lastAccent)
                                    // Aplicar fallback local IMEDIATAMENTE (Cálculo heurístico inteligente de contraste WCAG AAA)
                                    val fallback = GeminiColorHelper.computeFallbackPalette(novaCor, GeminiColorHelper.ComponenteLock.PRIMARY_TEXT, corPersonalizada)
                                    corPersonalizadaArgb = fallback.background.toArgb()
                                    aiCorDestaque = fallback.accentColor
                                    aiPrimaryText = fallback.primaryText
                                    aiSecondaryText = fallback.secondaryText
                                    
                                    android.widget.Toast.makeText(context, "IA a otimizar contraste do painel...", android.widget.Toast.LENGTH_SHORT).show()
                                    
                                    pendingAiJob?.cancel()
                                    pendingAiJob = coroutineScope.launch {
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
                                    val lastPrimary = aiPrimaryText ?: Color.White
                                    val lastAccent = aiCorDestaque ?: Color.White
                                    originalUserPalette = CompleteAppPalette(corPersonalizada, lastPrimary, novaCor, lastAccent)
                                    // Aplicar fallback local IMEDIATAMENTE (Cálculo heurístico inteligente de contraste WCAG AAA)
                                    val fallback = GeminiColorHelper.computeFallbackPalette(novaCor, GeminiColorHelper.ComponenteLock.SECONDARY_TEXT, corPersonalizada)
                                    corPersonalizadaArgb = fallback.background.toArgb()
                                    aiCorDestaque = fallback.accentColor
                                    aiPrimaryText = fallback.primaryText
                                    aiSecondaryText = fallback.secondaryText
                                    
                                    android.widget.Toast.makeText(context, "IA a otimizar contraste do painel...", android.widget.Toast.LENGTH_SHORT).show()
                                    
                                    pendingAiJob?.cancel()
                                    pendingAiJob = coroutineScope.launch {
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
                        }, onNavigateBack = { currentScreenName = MotoScreen.DASHBOARD.name }, onNavigateToPersonalization = { currentScreenName = MotoScreen.PERSONALIZATION.name }, aiCorDestaque = animCorDestaque, aiPrimaryText = animPrimaryText, aiSecondaryText = animSecondaryText, indicadores = indicadores)
                        MotoScreen.PERSONALIZATION -> PersonalizationSettings(s = s, velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaGlobal, corFundoAtual = animFundoTema, corPersonalizada = animCorPersonalizada, currentContrast = currentContrast, unidadeVelocidade = unidadeVelocidade, isIaActivated = isIaActivatedGlobal, onIaChange = { isIaActivatedGlobal = it }, onNavigateBack = { currentScreenName = MotoScreen.SETTINGS.name }, onNavigateToVisual = { currentScreenName = MotoScreen.VISUAL_PREFERENCES.name }, onNavigateToTouch = { currentScreenName = MotoScreen.TOUCH.name }, onNavigateToCognitive = { currentScreenName = MotoScreen.COGNITIVE_ASSISTANT.name }, onNavigateToAudio = { currentScreenName = MotoScreen.AUDIO_HAPTICS.name }, onNavigateToEditIcons = { currentScreenName = MotoScreen.EDIT_ICONS.name }, aiCorDestaque = animCorDestaque, aiPrimaryText = animPrimaryText, aiSecondaryText = animSecondaryText, indicadores = indicadores)
                        MotoScreen.VISUAL_PREFERENCES -> VisualPreferencesScreen(s = s, velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaGlobal, corFundoAtual = animFundoTema, corPersonalizada = animCorPersonalizada, currentContrast = currentContrast, unidadeVelocidade = unidadeVelocidade, textSizeScale = textSizeScale, currentColorFilter = currentColorFilter, currentTextSpacing = currentTextSpacing, onTextSpacingChange = { currentTextSpacing = it }, currentAnimations = currentAnimations, onAnimationsChange = { currentAnimations = it }, onColorFilterChange = { currentColorFilter = it }, onTextSizeChange = { textSizeScale = it }, onContrastChange = { currentContrast = it }, onNavigateBack = { currentScreenName = MotoScreen.PERSONALIZATION.name }, autoBrightnessAtivo = autoBrightnessAtivo, onAutoBrightnessChange = { autoBrightnessAtivo = it }, velocidadeMaximaKmh = velocidadeMaximaKmh, onVelocidadeMaximaChange = { velocidadeMaximaKmh = it }, aiCorDestaque = animCorDestaque, aiPrimaryText = animPrimaryText, aiSecondaryText = animSecondaryText, indicadores = indicadores)

                        MotoScreen.TOUCH -> TouchScreen(s = s, velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaGlobal, corFundoAtual = animFundoTema, corPersonalizada = animCorPersonalizada, currentContrast = currentContrast, unidadeVelocidade = unidadeVelocidade, currentTouchArea = currentTouchArea, onTouchAreaChange = { currentTouchArea = it }, currentMethod = currentMethod, onMethodChange = { currentMethod = it }, currentResponseTime = currentResponseTime, onResponseTimeChange = { currentResponseTime = it }, currentErrorPrevention = currentErrorPrevention, onErrorPreventionChange = { currentErrorPrevention = it }, onNavigateBack = { currentScreenName = MotoScreen.PERSONALIZATION.name }, aiCorDestaque = animCorDestaque, aiPrimaryText = animPrimaryText, aiSecondaryText = animSecondaryText, indicadores = indicadores)

                        MotoScreen.COGNITIVE_ASSISTANT -> CognitiveAssistantScreen(s = s, velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaGlobal, corFundoAtual = animFundoTema, corPersonalizada = animCorPersonalizada, currentContrast = currentContrast, unidadeVelocidade = unidadeVelocidade, currentLanguage = currentLanguage, onLanguageChange = { currentLanguage = it }, currentDensity = currentDensity, onDensityChange = { currentDensity = it }, currentHelp = currentHelp, onHelpChange = { currentHelp = it }, currentAlerts = currentAlerts, onAlertsChange = { currentAlerts = it }, onNavigateBack = { currentScreenName = MotoScreen.PERSONALIZATION.name }, aiCorDestaque = animCorDestaque, aiPrimaryText = animPrimaryText, aiSecondaryText = animSecondaryText, indicadores = indicadores)

                        MotoScreen.AUDIO_HAPTICS -> AudioHapticsScreen(s = s, velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaGlobal, corFundoAtual = animFundoTema, corPersonalizada = animCorPersonalizada, currentContrast = currentContrast, unidadeVelocidade = unidadeVelocidade, currentFeedback = currentFeedback, onFeedbackChange = { currentFeedback = it }, currentVisualAlerts = currentVisualAlerts, onVisualAlertsChange = { currentVisualAlerts = it }, currentErrorFeedback = currentErrorFeedback, onErrorFeedbackChange = { currentErrorFeedback = it }, onNavigateBack = { currentScreenName = MotoScreen.PERSONALIZATION.name }, aiCorDestaque = animCorDestaque, aiPrimaryText = animPrimaryText, aiSecondaryText = animSecondaryText, indicadores = indicadores)

                        MotoScreen.EDIT_ICONS -> EditIconsScreen(s = s, velocidadeAtual = velocidadeMota, bateriaAtual = bateriaMota, aCarregarAtual = aCarregarGlobal, tempBateriaAtual = tempBatMota, tempMotorAtual = tempMotorMota, marchaAtual = marchaGlobal, corFundoAtual = animFundoTema, corPersonalizada = animCorPersonalizada, currentContrast = currentContrast, unidadeVelocidade = unidadeVelocidade, onNavigateBack = { currentScreenName = MotoScreen.PERSONALIZATION.name }, aiCorDestaque = animCorDestaque, aiPrimaryText = animPrimaryText, aiSecondaryText = animSecondaryText, indicadores = indicadores)

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
                // Overlay full-screen para "Alertas Visuais para Áudio" (acessibilidade auditiva)
                ScreenFlashOverlay(alertController.flashTrigger)
              }
            }
        }
    }
}

/**
 * Auto-brightness controller. Lê o sensor de luz e ajusta o brilho da janela.
 * Mapeamento (lux → brilho):
 *   <10 lux (túnel/noite escura)  → 0.15
 *   <100 lux (anoitecer)          → 0.30
 *   <1000 lux (interior/nublado)  → 0.55
 *   <10000 lux (dia normal)       → 0.85
 *   ≥10000 lux (sol direto)       → 1.00
 *
 * Aplica também histerese ao trigger de NIGHT MODE (<5 lux entra, >50 lux sai)
 * via callback opcional onSuggestNightMode.
 */
@Composable
fun AutoBrightnessEffect(
    enabled: Boolean = true,
    onSuggestNightMode: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose { }

        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (sm == null || sensor == null) {
            return@DisposableEffect onDispose { }
        }

        // Suavizar oscilações: média móvel curta + atualização só se diferença > 5%
        var ultimoBrilho = -1f
        var ultimoEstadoNoite = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val lux = event.values.firstOrNull() ?: return
                val novoBrilho = mapearLuxParaBrilho(lux)
                if (kotlin.math.abs(novoBrilho - ultimoBrilho) > 0.05f) {
                    ultimoBrilho = novoBrilho
                    val attrs = activity.window.attributes
                    attrs.screenBrightness = novoBrilho
                    activity.window.attributes = attrs
                }

                // Histerese p/ NIGHT MODE: entra <5 lux, sai >50 lux
                val sugerirNoite = when {
                    lux < 5f  -> true
                    lux > 50f -> false
                    else -> ultimoEstadoNoite
                }
                if (sugerirNoite != ultimoEstadoNoite) {
                    ultimoEstadoNoite = sugerirNoite
                    onSuggestNightMode?.invoke(sugerirNoite)
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sm.unregisterListener(listener) }
    }
}

private fun mapearLuxParaBrilho(lux: Float): Float = when {
    lux < 10f     -> 0.15f
    lux < 100f    -> 0.30f
    lux < 1000f   -> 0.55f
    lux < 10000f  -> 0.85f
    else          -> 1.00f
}