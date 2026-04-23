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
                        Text(text = s.touchSettingsTitle, color = accentColor, fontSize = 36.sp, fontFamily = montserratFont, modifier = Modifier.align(Alignment.Center))
                        Text(text = s.back, color = accentColor, fontSize = 24.sp, fontFamily = robotoFont, modifier = Modifier.align(Alignment.CenterEnd).clickable { onNavigateBack() }.padding(8.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState)) {
                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = s.touchAreasTitle, subtitulo = s.touchAreasDesc, primaryColor = primaryText, secondaryColor = secondaryText,
                            conteudo = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = s.standard, color = if (currentTouchArea == "STANDARD") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, modifier = Modifier.clickable { onTouchAreaChange("STANDARD") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.large, color = if (currentTouchArea == "LARGE") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, modifier = Modifier.clickable { onTouchAreaChange("LARGE") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.extraLarge, color = if (currentTouchArea == "EXTRA LARGE") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, modifier = Modifier.clickable { onTouchAreaChange("EXTRA LARGE") })
                                }
                            }
                        )
                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = s.methodTitle, subtitulo = s.methodDesc, primaryColor = primaryText, secondaryColor = secondaryText,
                            conteudo = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = s.directTouch, color = if (currentMethod == "DIRECT TOUCH") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, modifier = Modifier.clickable { onMethodChange("DIRECT TOUCH") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.swipe, color = if (currentMethod == "SWIPE") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, modifier = Modifier.clickable { onMethodChange("SWIPE") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.joystick, color = if (currentMethod == "JOYSTICK") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, modifier = Modifier.clickable { onMethodChange("JOYSTICK") })
                                }
                            }
                        )
                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = s.responseTimeTitle, subtitulo = s.responseTimeDesc, primaryColor = primaryText, secondaryColor = secondaryText,
                            conteudo = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = s.fast, color = if (currentResponseTime == "FAST") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, modifier = Modifier.clickable { onResponseTimeChange("FAST") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.medium, color = if (currentResponseTime == "MEDIUM") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, modifier = Modifier.clickable { onResponseTimeChange("MEDIUM") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.slow, color = if (currentResponseTime == "SLOW") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, modifier = Modifier.clickable { onResponseTimeChange("SLOW") })
                                }
                            }
                        )
                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = s.errorPreventionTitle, subtitulo = s.errorPreventionDesc, primaryColor = primaryText, secondaryColor = secondaryText,
                            conteudo = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = s.on, color = if (currentErrorPrevention == "ON") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, modifier = Modifier.clickable { onErrorPreventionChange("ON") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                                    Text(text = s.off, color = if (currentErrorPrevention == "OFF") accentColor else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, modifier = Modifier.clickable { onErrorPreventionChange("OFF") })
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
    }
}
