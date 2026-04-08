package com.example.displaymoto.ui.screens.dashboard

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.displaymoto.AppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PersonalizationSettings(
    s: AppStrings,
    velocidadeAtual: Int = 0, bateriaAtual: Int = 85, aCarregarAtual: Boolean = false, tempBateriaAtual: Int = 30, tempMotorAtual: Int = 80, marchaAtual: String = "P",
    corFundoAtual: Color, corPersonalizada: Color, currentContrast: String = "STANDARD",
    isIaActivated: Boolean, onIaChange: (Boolean) -> Unit, onNavigateBack: () -> Unit,
    onNavigateToVisual: () -> Unit = {}, onNavigateToTouch: () -> Unit = {}, onNavigateToCognitive: () -> Unit = {}, onNavigateToAudio: () -> Unit = {}, onNavigateToEditIcons: () -> Unit = {}
) {
    val isLightBg = corPersonalizada.luminance() > 0.5f
    val corDestaque = when (currentContrast) {
        "HIGH CONTRAST" -> if (corPersonalizada.luminance() < 0.35f) lerp(corPersonalizada, Color.White, 0.7f) else corPersonalizada
        "NIGHT MODE" -> lerp(corPersonalizada, Color.White, 0.35f)
        else -> if (isLightBg) Color(0xFF004466) else Color(0xFF00BFFF)
    }
    val primaryText = when (currentContrast) {
        "HIGH CONTRAST" -> Color.White
        "NIGHT MODE" -> Color(0xFFF0F0F0)
        else -> if (isLightBg) Color.Black else Color.White
    }
    val secondaryText = when (currentContrast) {
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
            // PROPORÇÕES 100% CORRIGIDAS! (12% Topo, 73% Meio, 15% Base)
            TopBarSectionSettings(currentTime = currentTime, currentTemp = currentTemp, pEsq = piscaEsquerdo, pDir = piscaDireito, pPulso = piscaPulso, l1 = luz1, l2 = luz2, l3 = luz3, l4 = luz4, textColor = primaryText, modifier = Modifier.weight(0.12f).padding(horizontal = 32.dp))

            PersonalizationContentSection(s = s, onVoltar = onNavigateBack, onNavVisual = onNavigateToVisual, onNavTouch = onNavigateToTouch, onNavCognitive = onNavigateToCognitive, onNavAudio = onNavigateToAudio, onNavEditIcons = onNavigateToEditIcons, corDestaque = corDestaque, primaryText = primaryText, secondaryText = secondaryText, isIaActivated = isIaActivated, onIaChange = onIaChange, modifier = Modifier.weight(0.73f).fillMaxWidth())

            BottomStatusSection(v = velocidadeAtual, b = bateriaAtual, tB = tempBateriaAtual, tM = tempMotorAtual, m = marchaAtual, isCharging = aCarregarAtual, corDestaque = corDestaque, textColor = primaryText, modifier = Modifier.weight(0.15f))
        }
    }
}

@Composable
fun PersonalizationContentSection(s: AppStrings, onVoltar: () -> Unit, onNavVisual: () -> Unit, onNavTouch: () -> Unit, onNavCognitive: () -> Unit, onNavAudio: () -> Unit, onNavEditIcons: () -> Unit, corDestaque: Color, primaryText: Color, secondaryText: Color, isIaActivated: Boolean, onIaChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(text = s.persTitle, color = corDestaque, fontSize = 36.sp, fontFamily = agencyFbFont, modifier = Modifier.align(Alignment.Center))
                Text(text = s.back, color = corDestaque, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.align(Alignment.CenterEnd).clickable { onVoltar() }.padding(8.dp))
            }

            Spacer(Modifier.height(16.dp))

            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = s.activateIaTitle, subtitulo = s.activateIaDesc, primaryColor = primaryText, secondaryColor = secondaryText, onClick = { onIaChange(!isIaActivated) }, conteudo = {
                    Checkbox(checked = isIaActivated, onCheckedChange = { onIaChange(it) }, colors = CheckboxDefaults.colors(checkedColor = corDestaque, uncheckedColor = secondaryText, checkmarkColor = Color.Black), modifier = Modifier.scale(1.3f))
                })
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = s.visualPrefTitle, subtitulo = s.visualPrefDesc, primaryColor = primaryText, secondaryColor = secondaryText, onClick = onNavVisual)
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = s.touchTitle, subtitulo = s.touchDesc, primaryColor = primaryText, secondaryColor = secondaryText, onClick = onNavTouch)
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = s.cognitiveTitle, subtitulo = s.cognitiveDesc, primaryColor = primaryText, secondaryColor = secondaryText, onClick = onNavCognitive)
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = s.audioHapticsTitle, subtitulo = s.audioHapticsDesc, primaryColor = primaryText, secondaryColor = secondaryText, onClick = onNavAudio)
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = s.editIconsTitle, subtitulo = s.editIconsDesc, primaryColor = primaryText, secondaryColor = secondaryText, onClick = onNavEditIcons)
                LinhaDivisoria(corDestaque)

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}