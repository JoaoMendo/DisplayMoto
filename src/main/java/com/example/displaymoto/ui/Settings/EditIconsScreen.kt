package com.example.displaymoto.ui.screens.dashboard.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
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
import com.example.displaymoto.ui.screens.dashboard.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EditIconsScreen(s: AppStrings, velocidadeAtual: Int, bateriaAtual: Int, aCarregarAtual: Boolean, tempBateriaAtual: Int, tempMotorAtual: Int, marchaAtual: String, corFundoAtual: Color, corPersonalizada: Color, currentContrast: String, onNavigateBack: () -> Unit, aiCorDestaque: Color? = null, aiPrimaryText: Color? = null, aiSecondaryText: Color? = null) {
    BaseSettingsScreen(s = s, velocidadeAtual = velocidadeAtual, bateriaAtual = bateriaAtual, aCarregarAtual = aCarregarAtual, tempBateriaAtual = tempBateriaAtual, tempMotorAtual = tempMotorAtual, marchaAtual = marchaAtual, corFundoAtual = corFundoAtual, corPersonalizada = corPersonalizada, currentContrast = currentContrast, onNavigateBack = onNavigateBack, title = s.editIconsTitle, aiCorDestaque = aiCorDestaque, aiPrimaryText = aiPrimaryText, aiSecondaryText = aiSecondaryText)
}

@Composable
fun BaseSettingsScreen(
    s: AppStrings,
    velocidadeAtual: Int, bateriaAtual: Int, aCarregarAtual: Boolean, tempBateriaAtual: Int, tempMotorAtual: Int, marchaAtual: String,
    corFundoAtual: Color, corPersonalizada: Color, currentContrast: String,
    onNavigateBack: () -> Unit, title: String,
    aiCorDestaque: Color? = null, aiPrimaryText: Color? = null, aiSecondaryText: Color? = null
) {
    val isLightBg = corPersonalizada.luminance() > 0.5f
    val corDestaque = aiCorDestaque ?: when (currentContrast) {
        "HIGH CONTRAST" -> if (corPersonalizada.luminance() < 0.35f) lerp(corPersonalizada, Color.White, 0.7f) else corPersonalizada
        "NIGHT MODE" -> lerp(corPersonalizada, Color.White, 0.35f)
        else -> if (isLightBg) Color(0xFF004466) else Color(0xFF00BFFF)
    }
    val primaryText = aiPrimaryText ?: when (currentContrast) {
        "HIGH CONTRAST" -> Color.White
        "NIGHT MODE" -> Color(0xFFF0F0F0)
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

    LaunchedEffect(piscaEsquerdo, piscaDireito) {
        if (piscaEsquerdo || piscaDireito) while (true) { piscaPulso = true; delay(400); piscaPulso = false; delay(400) }
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
            TopBarSectionSettings(currentTime = currentTime, currentTemp = currentTemp, pEsq = piscaEsquerdo, pDir = piscaDireito, pPulso = piscaPulso, l1 = luz1, l2 = luz2, l3 = luz3, l4 = luz4, textColor = primaryText, modifier = Modifier.wrapContentHeight())

            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(title, color = corDestaque, fontSize = 36.sp, fontFamily = agencyFbFont, modifier = Modifier.align(Alignment.Center))
                        Text(s.back, color = corDestaque, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.align(Alignment.CenterEnd).clickable { onNavigateBack() }.padding(8.dp))
                    }
                    Spacer(Modifier.height(32.dp))
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(s.workInProgress, color = primaryText, fontSize = 32.sp, fontFamily = agencyFbFont)
                    }
                }
            }

            BottomStatusSection(
                v = velocidadeAtual, b = bateriaAtual, tB = tempBateriaAtual, tM = tempMotorAtual, m = marchaAtual, isCharging = aCarregarAtual,
                corDestaque = corDestaque, textColor = primaryText, modifier = Modifier.wrapContentHeight()
            )
        }
    }
}