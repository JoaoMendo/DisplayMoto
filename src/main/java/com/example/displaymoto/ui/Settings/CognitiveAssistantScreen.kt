package com.example.displaymoto.ui.screens.dashboard.settings

import androidx.compose.foundation.background
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
import com.example.displaymoto.AppStrings
import com.example.displaymoto.LocalAnimationMultiplier
import com.example.displaymoto.ui.screens.dashboard.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    aiCorDestaque: Color? = null, aiPrimaryText: Color? = null, aiSecondaryText: Color? = null
) {
    val isLightBg = corPersonalizada.luminance() > 0.5f

    val accentColor = aiCorDestaque ?: when (currentContrast) {
        "HIGH CONTRAST" -> if (corPersonalizada.luminance() < 0.35f) lerp(corPersonalizada, Color.White, 0.7f) else corPersonalizada
        "NIGHT MODE" -> lerp(corPersonalizada, Color.White, 0.35f)
        else -> if (isLightBg) Color(0xFF004466) else Color(0xFF00BFFF)
    }

    val primaryText = aiPrimaryText ?: when (currentContrast) {
        "HIGH CONTRAST" -> Color.White
        "NIGHT MODE" -> Color(0xFFF0F0F0)
        else -> if (isLightBg) Color.Black else Color.White
    }

    val secondaryText = aiSecondaryText ?: when (currentContrast) {
        "HIGH CONTRAST" -> Color.White
        "NIGHT MODE" -> Color(0xFFAAAAAA)
        else -> if (isLightBg) Color(0xFF4A4A4A) else Color.LightGray
    }

    var piscaEsquerdo by remember { mutableStateOf(false) }
    var piscaDireito by remember { mutableStateOf(false) }
    var piscaPulso by remember { mutableStateOf(false) }
    var luz1 by remember { mutableStateOf(false) }
    var luz2 by remember { mutableStateOf(false) }
    var luz3 by remember { mutableStateOf(false) }
    var luz4 by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf("--:--") }
    var currentTemp by remember { mutableStateOf("--ºC") }

    val animScale = LocalAnimationMultiplier.current

    LaunchedEffect(piscaEsquerdo, piscaDireito, animScale) {
        if (piscaEsquerdo || piscaDireito) {
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
                    Key.DirectionLeft -> { piscaEsquerdo = !piscaEsquerdo; if (piscaEsquerdo) piscaDireito = false; true }
                    Key.DirectionRight -> { piscaDireito = !piscaDireito; if (piscaDireito) piscaEsquerdo = false; true }
                    Key.One, Key.NumPad1 -> { luz1 = !luz1; true }
                    Key.Two, Key.NumPad2 -> { luz2 = !luz2; true }
                    Key.Three, Key.NumPad3 -> { luz3 = !luz3; true }
                    Key.Four, Key.NumPad4 -> { luz4 = !luz4; true }
                    Key.Escape, Key.Backspace, Key.B -> { onNavigateBack(); true }
                    else -> false
                }
            } else false
        }) {
            TopBarSectionSettings(currentTime = currentTime, currentTemp = currentTemp, pEsq = piscaEsquerdo, pDir = piscaDireito, pPulso = piscaPulso, l1 = luz1, l2 = luz2, l3 = luz3, l4 = luz4, textColor = primaryText, modifier = Modifier.weight(0.12f))

            val scrollState = rememberScrollState()

            Box(modifier = Modifier.weight(0.73f).fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(text = s.cognitiveTitle, color = accentColor, fontSize = 36.sp, fontFamily = agencyFbFont, modifier = Modifier.align(Alignment.Center))
                        Text(text = s.back, color = accentColor, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.align(Alignment.CenterEnd).clickable { onNavigateBack() }.padding(8.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState)) {
                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = s.langComplexityTitle, subtitulo = s.langComplexityDesc, primaryColor = primaryText, secondaryColor = secondaryText,
                            conteudo = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = s.simple, color = if (currentLanguage == "SIMPLE") accentColor else secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.clickable { onLanguageChange("SIMPLE") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont)
                                    Text(text = s.standard, color = if (currentLanguage == "STANDARD") accentColor else secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.clickable { onLanguageChange("STANDARD") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont)
                                    Text(text = s.technical, color = if (currentLanguage == "TECHNICAL") accentColor else secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.clickable { onLanguageChange("TECHNICAL") })
                                }
                            }
                        )
                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = s.infoDensityTitle, subtitulo = s.infoDensityDesc, primaryColor = primaryText, secondaryColor = secondaryText,
                            conteudo = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = s.essential, color = if (currentDensity == "ESSENTIAL") accentColor else secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.clickable { onDensityChange("ESSENTIAL") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont)
                                    Text(text = s.standard, color = if (currentDensity == "STANDARD") accentColor else secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.clickable { onDensityChange("STANDARD") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont)
                                    Text(text = s.full, color = if (currentDensity == "FULL") accentColor else secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.clickable { onDensityChange("FULL") })
                                }
                            }
                        )
                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = s.contextHelpTitle, subtitulo = s.contextHelpDesc, primaryColor = primaryText, secondaryColor = secondaryText,
                            conteudo = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = s.off, color = if (currentHelp == "OFF") accentColor else secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.clickable { onHelpChange("OFF") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont)
                                    Text(text = s.onDemand, color = if (currentHelp == "ON DEMAND") accentColor else secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.clickable { onHelpChange("ON DEMAND") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont)
                                    Text(text = s.alwaysOn, color = if (currentHelp == "ALWAYS ON") accentColor else secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.clickable { onHelpChange("ALWAYS ON") })
                                }
                            }
                        )
                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = s.alertsTitle, subtitulo = s.alertsDesc, primaryColor = primaryText, secondaryColor = secondaryText,
                            conteudo = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = s.criticalOnly, color = if (currentAlerts == "CRITICAL ONLY") accentColor else secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.clickable { onAlertsChange("CRITICAL ONLY") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont)
                                    Text(text = s.standard, color = if (currentAlerts == "STANDARD") accentColor else secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.clickable { onAlertsChange("STANDARD") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont)
                                    Text(text = s.all, color = if (currentAlerts == "ALL") accentColor else secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.clickable { onAlertsChange("ALL") })
                                }
                            }
                        )
                        LinhaDivisoria(accentColor)
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }

            BottomStatusSection(v = velocidadeAtual, b = bateriaAtual, tB = tempBateriaAtual, tM = tempMotorAtual, m = marchaAtual, isCharging = aCarregarAtual, corDestaque = accentColor, textColor = primaryText, modifier = Modifier.weight(0.15f))
        }
    }
}