package com.example.displaymoto.ui.screens.dashboard.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.displaymoto.AppStrings
import com.example.displaymoto.LocalAnimationMultiplier
import com.example.displaymoto.ui.screens.dashboard.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CognitiveAssistantScreen(
    s: AppStrings,
    velocidadeAtual: Int = 0, bateriaAtual: Int = 85, aCarregarAtual: Boolean = false, tempBateriaAtual: Int = 30, tempMotorAtual: Int = 80, marchaAtual: String = "P",
    corFundoAtual: Color, corPersonalizada: Color, currentContrast: String,
    currentLanguage: String, onLanguageChange: (String) -> Unit,
    currentDensity: String, onDensityChange: (String) -> Unit,
    currentHelp: String, onHelpChange: (String) -> Unit,
    currentAlerts: String, onAlertsChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    aiCorDestaque: Color? = null, aiPrimaryText: Color? = null, aiSecondaryText: Color? = null,
    indicadores: com.example.displaymoto.IndicadoresState = remember { com.example.displaymoto.IndicadoresState() }
) {
    val isLightBg = corPersonalizada.luminance() > 0.5f

    val uiElementColor = when (currentContrast) {
        "HIGH CONTRAST" -> if (corPersonalizada.luminance() < 0.35f) lerp(corPersonalizada, Color.White, 0.7f) else corPersonalizada
        "NIGHT MODE" -> lerp(corPersonalizada, Color.White, 0.35f)
        else -> if (isLightBg) Color(0xFF004466) else Color.White
    }
    val iconColor = aiCorDestaque ?: uiElementColor
    val accentColor = aiCorDestaque ?: uiElementColor

    val primaryText = aiPrimaryText ?: when (currentContrast) {
        "HIGH CONTRAST" -> if (isLightBg) Color.Black else Color.White
        "NIGHT MODE" -> Color.White
        else -> if (isLightBg) Color.Black else Color.White
    }

    val secondaryText = aiSecondaryText ?: when (currentContrast) {
        "HIGH CONTRAST" -> if (isLightBg) Color.Black else Color.White
        "NIGHT MODE" -> Color.White
        else -> if (isLightBg) Color.Black else Color.White
    }

    var piscaPulso by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf("--:--") }
    var currentTemp by remember { mutableStateOf("--ºC") }

    var popupMensagem by remember { mutableStateOf("") }
    var popupCor by remember { mutableStateOf(Color.White) }
    var popupGrave by remember { mutableStateOf(false) }
    var popupVisivel by remember { mutableStateOf(false) }
    fun mostrarPopup(msg: String, cor: Color, grave: Boolean) { popupMensagem = msg; popupCor = cor; popupGrave = grave; popupVisivel = true; if (grave) { try { val tg = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100); tg.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500); android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tg.release() }, 600) } catch (_: Exception) {} } }
    LaunchedEffect(popupVisivel) { if (popupVisivel) { delay(3000); popupVisivel = false } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz1 }.drop(1).collect { if (it) mostrarPopup(s.indAbs, Color(0xFFFFD600), false) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz2 }.drop(1).collect { if (it) mostrarPopup(s.indMaximos, Color(0xFF42A5F5), false) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz3 }.drop(1).collect { if (it) mostrarPopup(s.indMedios, Color(0xFF00E676), false) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz4 }.drop(1).collect { if (it) mostrarPopup(s.indMil, Color(0xFFFFD600), false) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz5 }.drop(1).collect { if (it) mostrarPopup(s.indBrake, Color(0xFFE53935), true) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz6 }.drop(1).collect { if (it) mostrarPopup(s.indBattery, Color(0xFFE53935), true) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz7 }.drop(1).collect { if (it) mostrarPopup(s.indTempBat, Color(0xFFE53935), true) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz8 }.drop(1).collect { if (it) mostrarPopup(s.indMinimos, Color(0xFF00E676), false) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz9 }.drop(1).collect { if (it) mostrarPopup(s.indEsp, Color(0xFFFFD600), false) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz10 }.drop(1).collect { if (it) mostrarPopup(s.indNeblina, Color(0xFFFFD600), false) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz11 }.drop(1).collect { if (it) mostrarPopup(s.indPneu, Color(0xFFFFD600), false) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz12 }.drop(1).collect { if (it) mostrarPopup(s.indTempMotor, Color(0xFFE53935), true) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz13 }.drop(1).collect { if (it) mostrarPopup(s.indV2x, Color(0xFF42A5F5), false) } }

    val animScale = LocalAnimationMultiplier.current

    LaunchedEffect(indicadores.piscaEsquerdo, indicadores.piscaDireito, animScale) {
        if (indicadores.piscaEsquerdo || indicadores.piscaDireito) {
            while (true) {
                piscaPulso = true
                val delayTime = (400 * (if (animScale < 0.1f) 1000f else animScale)).toLong()
                if (animScale < 0.1f) {
                    delay(100000L)
                } else {
                    delay(delayTime)
                    piscaPulso = false
                    delay(delayTime)
                }
            }
        } else {
            piscaPulso = false
        }
    }

    LaunchedEffect(Unit) {
        while (true) { currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("Europe/Lisbon") }.format(Date()); delay(1000) }
    }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { try { currentTemp = "${JSONObject(URL("https://api.open-meteo.com/v1/forecast?latitude=41.3006&longitude=-7.7441&current_weather=true").readText()).getJSONObject("current_weather").getInt("temperature")}ºC" } catch (_: Exception) {} }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(modifier = Modifier.fillMaxSize().background(corFundoAtual)) {
        Column(modifier = Modifier.fillMaxSize().focusRequester(focusRequester).focusable().onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                when (event.key) {
                    Key.DirectionLeft -> { indicadores.piscaEsquerdo = !indicadores.piscaEsquerdo; if (indicadores.piscaEsquerdo) indicadores.piscaDireito = false; true }
                    Key.DirectionRight -> { indicadores.piscaDireito = !indicadores.piscaDireito; if (indicadores.piscaDireito) indicadores.piscaEsquerdo = false; true }
                    Key.One, Key.NumPad1 -> { indicadores.luz1 = !indicadores.luz1; true }
                    Key.Two, Key.NumPad2 -> { indicadores.luz2 = !indicadores.luz2; if (indicadores.luz2) { indicadores.luz3 = false; indicadores.luz8 = false }; true }
                    Key.Three, Key.NumPad3 -> { indicadores.luz3 = !indicadores.luz3; if (indicadores.luz3) { indicadores.luz2 = false; indicadores.luz8 = false }; true }
                    Key.Four, Key.NumPad4 -> { indicadores.luz4 = !indicadores.luz4; true }
                    Key.Five, Key.NumPad5 -> { indicadores.luz5 = !indicadores.luz5; true }
                    Key.Six, Key.NumPad6 -> { indicadores.luz6 = !indicadores.luz6; true }
                    Key.Seven, Key.NumPad7 -> { indicadores.luz7 = !indicadores.luz7; true }
                    Key.Eight, Key.NumPad8 -> { indicadores.luz8 = !indicadores.luz8; if (indicadores.luz8) { indicadores.luz2 = false; indicadores.luz3 = false }; true }
                    Key.Nine, Key.NumPad9 -> { indicadores.luz9 = !indicadores.luz9; true }
                    Key.Zero, Key.NumPad0 -> { indicadores.luz10 = !indicadores.luz10; true }
                    Key.Q -> { indicadores.luz11 = !indicadores.luz11; true }
                    Key.E -> { indicadores.luz12 = !indicadores.luz12; true }
                    Key.R -> { indicadores.luz13 = !indicadores.luz13; true }
                    Key.Escape, Key.Backspace, Key.B -> { onNavigateBack(); true }
                    else -> false
                }
            } else false
        }) {
            TopBarSectionSettings(currentTime = currentTime, currentTemp = currentTemp, pEsq = indicadores.piscaEsquerdo, pDir = indicadores.piscaDireito, pPulso = piscaPulso, l1 = indicadores.luz1, l2 = indicadores.luz2, l3 = indicadores.luz3, l4 = indicadores.luz4, l5 = indicadores.luz5, l6 = indicadores.luz6, l7 = indicadores.luz7, l8 = indicadores.luz8, l9 = indicadores.luz9, l10 = indicadores.luz10, l11 = indicadores.luz11, l12 = indicadores.luz12, l13 = indicadores.luz13, motoLigada = indicadores.motoLigada, marchaAtual = marchaAtual, aCarregar = aCarregarAtual, textColor = primaryText, modifier = Modifier.weight(0.12f))

            val scrollState = rememberScrollState()

            Box(modifier = Modifier.weight(0.73f).fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(text = s.cognitiveTitle, color = accentColor, fontSize = 36.sp, fontFamily = montserratFont, modifier = Modifier.align(Alignment.Center))
                        Text(text = s.back, color = accentColor, fontSize = 24.sp, fontFamily = robotoFont, modifier = Modifier.align(Alignment.CenterEnd).clickable { onNavigateBack() }.padding(8.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState)) {
                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = s.langComplexityTitle, subtitulo = s.langComplexityDesc, primaryColor = primaryText, secondaryColor = secondaryText,
                            conteudo = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = s.simple, color = if (currentLanguage == "SIMPLE") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentLanguage == "SIMPLE") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onLanguageChange("SIMPLE") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.standard, color = if (currentLanguage == "STANDARD") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentLanguage == "STANDARD") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onLanguageChange("STANDARD") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.technical, color = if (currentLanguage == "TECHNICAL") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentLanguage == "TECHNICAL") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onLanguageChange("TECHNICAL") })
                                }
                            }
                        )
                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = s.infoDensityTitle, subtitulo = s.infoDensityDesc, primaryColor = primaryText, secondaryColor = secondaryText,
                            conteudo = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = s.essential, color = if (currentDensity == "ESSENTIAL") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentDensity == "ESSENTIAL") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onDensityChange("ESSENTIAL") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.standard, color = if (currentDensity == "STANDARD") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentDensity == "STANDARD") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onDensityChange("STANDARD") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.full, color = if (currentDensity == "FULL") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentDensity == "FULL") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onDensityChange("FULL") })
                                }
                            }
                        )
                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = s.contextHelpTitle, subtitulo = s.contextHelpDesc, primaryColor = primaryText, secondaryColor = secondaryText,
                            conteudo = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = s.off, color = if (currentHelp == "OFF") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentHelp == "OFF") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onHelpChange("OFF") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.onDemand, color = if (currentHelp == "ON DEMAND") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentHelp == "ON DEMAND") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onHelpChange("ON DEMAND") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.alwaysOn, color = if (currentHelp == "ALWAYS ON") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentHelp == "ALWAYS ON") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onHelpChange("ALWAYS ON") })
                                }
                            }
                        )
                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = s.alertsTitle, subtitulo = s.alertsDesc, primaryColor = primaryText, secondaryColor = secondaryText,
                            conteudo = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = s.criticalOnly, color = if (currentAlerts == "CRITICAL ONLY") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentAlerts == "CRITICAL ONLY") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onAlertsChange("CRITICAL ONLY") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.standard, color = if (currentAlerts == "STANDARD") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentAlerts == "STANDARD") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onAlertsChange("STANDARD") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.all, color = if (currentAlerts == "ALL") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentAlerts == "ALL") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onAlertsChange("ALL") })
                                }
                            }
                        )
                        LinhaDivisoria(accentColor)
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }

            BottomStatusSection(v = velocidadeAtual, b = bateriaAtual, tB = tempBateriaAtual, tM = tempMotorAtual, m = marchaAtual, isCharging = aCarregarAtual, corDestaque = accentColor, iconColor = iconColor, textColor = primaryText, modifier = Modifier.weight(0.15f))
        }

        if (popupVisivel) {
            if (popupGrave) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                    Column(modifier = Modifier.background(Color(0xFF1A0A0A), shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)).border(4.dp, popupCor, shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)).padding(horizontal = 64.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("⚠", fontSize = 56.sp); Text(text = s.warning, color = popupCor, fontSize = 48.sp, fontFamily = agencyFbFont, fontWeight = FontWeight.Bold); Text(text = popupMensagem, color = Color.White, fontSize = 36.sp, fontFamily = agencyFbFont, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Row(modifier = Modifier.padding(top = 16.dp).background(Color(0xFF1A1A2E), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).border(2.dp, popupCor, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(horizontal = 32.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = popupMensagem, color = popupCor, fontSize = 28.sp, fontFamily = agencyFbFont, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
