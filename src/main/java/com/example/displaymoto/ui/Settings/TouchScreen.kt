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
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TouchScreen(
    s: AppStrings,
    velocidadeAtual: Int = 0, bateriaAtual: Int = 85, aCarregarAtual: Boolean = false, tempBateriaAtual: Int = 30, tempMotorAtual: Int = 80, marchaAtual: String = "P",
    corFundoAtual: Color, corPersonalizada: Color, currentContrast: String,
    currentTouchArea: String, onTouchAreaChange: (String) -> Unit,
    currentMethod: String, onMethodChange: (String) -> Unit,
    currentResponseTime: String, onResponseTimeChange: (String) -> Unit,
    currentErrorPrevention: String, onErrorPreventionChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    aiCorDestaque: Color? = null, aiPrimaryText: Color? = null, aiSecondaryText: Color? = null
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

    var piscaEsquerdo by remember { mutableStateOf(false) }
    var piscaDireito by remember { mutableStateOf(false) }
    var piscaPulso by remember { mutableStateOf(false) }
    var luz1 by remember { mutableStateOf(false) }
    var luz2 by remember { mutableStateOf(false) }
    var luz3 by remember { mutableStateOf(false) }
    var luz4 by remember { mutableStateOf(false) }
    var luz5 by remember { mutableStateOf(false) }
    var luz6 by remember { mutableStateOf(false) }
    var luz7 by remember { mutableStateOf(false) }
    var luz8 by remember { mutableStateOf(false) }
    var luz9 by remember { mutableStateOf(false) }
    var luz10 by remember { mutableStateOf(false) }
    var luz11 by remember { mutableStateOf(false) }
    var luz12 by remember { mutableStateOf(false) }
    var luz13 by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf("--:--") }
    var currentTemp by remember { mutableStateOf("--ºC") }

    var popupMensagem by remember { mutableStateOf("") }
    var popupCor by remember { mutableStateOf(Color.White) }
    var popupGrave by remember { mutableStateOf(false) }
    var popupVisivel by remember { mutableStateOf(false) }
    fun mostrarPopup(msg: String, cor: Color, grave: Boolean) { popupMensagem = msg; popupCor = cor; popupGrave = grave; popupVisivel = true; if (grave) { try { val tg = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100); tg.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500); android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tg.release() }, 600) } catch (_: Exception) {} } }
    LaunchedEffect(popupVisivel) { if (popupVisivel) { delay(3000); popupVisivel = false } }
    LaunchedEffect(luz1) { if (luz1) mostrarPopup(s.indAbs, Color(0xFFFFD600), false) }; LaunchedEffect(luz2) { if (luz2) mostrarPopup(s.indMaximos, Color(0xFF42A5F5), false) }; LaunchedEffect(luz3) { if (luz3) mostrarPopup(s.indMedios, Color(0xFF00E676), false) }; LaunchedEffect(luz4) { if (luz4) mostrarPopup(s.indMil, Color(0xFFFFD600), false) }; LaunchedEffect(luz5) { if (luz5) mostrarPopup(s.indBrake, Color(0xFFE53935), true) }; LaunchedEffect(luz6) { if (luz6) mostrarPopup(s.indBattery, Color(0xFFE53935), true) }; LaunchedEffect(luz7) { if (luz7) mostrarPopup(s.indTempBat, Color(0xFFE53935), true) }; LaunchedEffect(luz8) { if (luz8) mostrarPopup(s.indMinimos, Color(0xFF00E676), false) }; LaunchedEffect(luz9) { if (luz9) mostrarPopup(s.indEsp, Color(0xFFFFD600), false) }; LaunchedEffect(luz10) { if (luz10) mostrarPopup(s.indNeblina, Color(0xFFFFD600), false) }; LaunchedEffect(luz11) { if (luz11) mostrarPopup(s.indPneu, Color(0xFFFFD600), false) }; LaunchedEffect(luz12) { if (luz12) mostrarPopup(s.indTempMotor, Color(0xFFE53935), true) }; LaunchedEffect(luz13) { if (luz13) mostrarPopup(s.indV2x, Color(0xFF42A5F5), false) }

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
                    Key.Two, Key.NumPad2 -> { luz2 = !luz2; if (luz2) { luz3 = false; luz8 = false }; true }
                    Key.Three, Key.NumPad3 -> { luz3 = !luz3; if (luz3) { luz2 = false; luz8 = false }; true }
                    Key.Four, Key.NumPad4 -> { luz4 = !luz4; true }
                    Key.Five, Key.NumPad5 -> { luz5 = !luz5; true }
                    Key.Six, Key.NumPad6 -> { luz6 = !luz6; true }
                    Key.Seven, Key.NumPad7 -> { luz7 = !luz7; true }
                    Key.Eight, Key.NumPad8 -> { luz8 = !luz8; if (luz8) { luz2 = false; luz3 = false }; true }
                    Key.Nine, Key.NumPad9 -> { luz9 = !luz9; true }
                    Key.Zero, Key.NumPad0 -> { luz10 = !luz10; true }
                    Key.Q -> { luz11 = !luz11; true }
                    Key.E -> { luz12 = !luz12; true }
                    Key.R -> { luz13 = !luz13; true }
                    Key.Escape, Key.Backspace, Key.B -> { onNavigateBack(); true }
                    else -> false
                }
            } else false
        }) {
            TopBarSectionSettings(currentTime = currentTime, currentTemp = currentTemp, pEsq = piscaEsquerdo, pDir = piscaDireito, pPulso = piscaPulso, l1 = luz1, l2 = luz2, l3 = luz3, l4 = luz4, l5 = luz5, l6 = luz6, l7 = luz7, l8 = luz8, l9 = luz9, l10 = luz10, l11 = luz11, l12 = luz12, l13 = luz13, textColor = primaryText, modifier = Modifier.weight(0.12f))

            val scrollState = rememberScrollState()

            Box(modifier = Modifier.weight(0.73f).fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(text = s.touchSettingsTitle, color = accentColor, fontSize = 36.sp, fontFamily = montserratFont, modifier = Modifier.align(Alignment.Center))
                        Text(text = s.back, color = accentColor, fontSize = 24.sp, fontFamily = robotoFont, modifier = Modifier.align(Alignment.CenterEnd).clickable { onNavigateBack() }.padding(8.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState)) {
                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = s.touchAreasTitle, subtitulo = s.touchAreasDesc, primaryColor = primaryText, secondaryColor = secondaryText,
                            conteudo = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = s.standard, color = if (currentTouchArea == "STANDARD") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentTouchArea == "STANDARD") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onTouchAreaChange("STANDARD") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.large, color = if (currentTouchArea == "LARGE") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentTouchArea == "LARGE") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onTouchAreaChange("LARGE") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.extraLarge, color = if (currentTouchArea == "EXTRA LARGE") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentTouchArea == "EXTRA LARGE") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onTouchAreaChange("EXTRA LARGE") })
                                }
                            }
                        )
                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = s.methodTitle, subtitulo = s.methodDesc, primaryColor = primaryText, secondaryColor = secondaryText,
                            conteudo = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = s.directTouch, color = if (currentMethod == "DIRECT TOUCH") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentMethod == "DIRECT TOUCH") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onMethodChange("DIRECT TOUCH") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.swipe, color = if (currentMethod == "SWIPE") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentMethod == "SWIPE") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onMethodChange("SWIPE") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.joystick, color = if (currentMethod == "JOYSTICK") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentMethod == "JOYSTICK") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onMethodChange("JOYSTICK") })
                                }
                            }
                        )
                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = s.responseTimeTitle, subtitulo = s.responseTimeDesc, primaryColor = primaryText, secondaryColor = secondaryText,
                            conteudo = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = s.fast, color = if (currentResponseTime == "FAST") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentResponseTime == "FAST") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onResponseTimeChange("FAST") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.medium, color = if (currentResponseTime == "MEDIUM") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentResponseTime == "MEDIUM") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onResponseTimeChange("MEDIUM") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.slow, color = if (currentResponseTime == "SLOW") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentResponseTime == "SLOW") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onResponseTimeChange("SLOW") })
                                }
                            }
                        )
                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = s.errorPreventionTitle, subtitulo = s.errorPreventionDesc, primaryColor = primaryText, secondaryColor = secondaryText,
                            conteudo = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = s.on, color = if (currentErrorPrevention == "ON") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentErrorPrevention == "ON") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onErrorPreventionChange("ON") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.off, color = if (currentErrorPrevention == "OFF") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (currentErrorPrevention == "OFF") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onErrorPreventionChange("OFF") })
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
