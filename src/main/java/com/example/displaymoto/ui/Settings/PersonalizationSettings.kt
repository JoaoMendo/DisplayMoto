package com.example.displaymoto.ui.screens.dashboard

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
import androidx.compose.ui.draw.scale
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
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
    unidadeVelocidade: String = "km/h",
    isIaActivated: Boolean, onIaChange: (Boolean) -> Unit, onNavigateBack: () -> Unit,
    onNavigateToVisual: () -> Unit = {}, onNavigateToTouch: () -> Unit = {}, onNavigateToCognitive: () -> Unit = {}, onNavigateToAudio: () -> Unit = {}, onNavigateToEditIcons: () -> Unit = {},
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
    val corDestaque = aiCorDestaque ?: uiElementColor
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
    val alertFeedback = com.example.displaymoto.LocalAlertFeedback.current
    fun mostrarPopup(msg: String, cor: Color, grave: Boolean) { popupMensagem = msg; popupCor = cor; popupGrave = grave; popupVisivel = true; alertFeedback(msg, grave) }
    LaunchedEffect(popupVisivel) { if (popupVisivel) { delay(3000); popupVisivel = false } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz1 }.drop(1).collect { if (it) mostrarPopup(s.indAbs, Color(0xFFFFB300), false) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz2 }.drop(1).collect { if (it) mostrarPopup(s.indMaximos, Color(0xFF42A5F5), false) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz3 }.drop(1).collect { if (it) mostrarPopup(s.indMedios, Color(0xFF00E676), false) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz4 }.drop(1).collect { if (it) mostrarPopup(s.indMil, Color(0xFFFFB300), false) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz5 }.drop(1).collect { if (it) mostrarPopup(s.indBrake, Color(0xFFE53935), true) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz6 }.drop(1).collect { if (it) mostrarPopup(s.indBattery, Color(0xFFE53935), true) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz7 }.drop(1).collect { if (it) mostrarPopup(s.indTempBat, Color(0xFFE53935), true) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz8 }.drop(1).collect { if (it) mostrarPopup(s.indMinimos, Color(0xFF00E676), false) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz9 }.drop(1).collect { if (it) mostrarPopup(s.indEsp, Color(0xFFFFB300), false) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz10 }.drop(1).collect { if (it) mostrarPopup(s.indNeblina, Color(0xFFFFB300), false) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz11 }.drop(1).collect { if (it) mostrarPopup(s.indPneu, Color(0xFFFFB300), false) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz12 }.drop(1).collect { if (it) mostrarPopup(s.indTempMotor, Color(0xFFE53935), true) } }; LaunchedEffect(Unit) { snapshotFlow { indicadores.luz13 }.drop(1).collect { if (it) mostrarPopup(s.indV2x, Color(0xFF42A5F5), false) } }

    LaunchedEffect(indicadores.piscaEsquerdo, indicadores.piscaDireito) {
        if (indicadores.piscaEsquerdo || indicadores.piscaDireito) while (true) { piscaPulso = true; delay(400); piscaPulso = false; delay(400) }
    }
    LaunchedEffect(Unit) {
        while (true) { currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()); delay(1000) }
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
            TopBarSectionSettings(currentTime = currentTime, currentTemp = currentTemp, pEsq = indicadores.piscaEsquerdo, pDir = indicadores.piscaDireito, pPulso = piscaPulso, l1 = indicadores.luz1, l2 = indicadores.luz2, l3 = indicadores.luz3, l4 = indicadores.luz4, l5 = indicadores.luz5, l6 = indicadores.luz6, l7 = indicadores.luz7, l8 = indicadores.luz8, l9 = indicadores.luz9, l10 = indicadores.luz10, l11 = indicadores.luz11, l12 = indicadores.luz12, l13 = indicadores.luz13, motoLigada = indicadores.motoLigada, marchaAtual = marchaAtual, aCarregar = aCarregarAtual, textColor = primaryText, modifier = Modifier.weight(0.12f).padding(horizontal = 32.dp))

            PersonalizationContentSection(s = s, onVoltar = onNavigateBack, onNavVisual = onNavigateToVisual, onNavTouch = onNavigateToTouch, onNavCognitive = onNavigateToCognitive, onNavAudio = onNavigateToAudio, onNavEditIcons = onNavigateToEditIcons, corDestaque = corDestaque, primaryText = primaryText, secondaryText = secondaryText, isIaActivated = isIaActivated, onIaChange = onIaChange, modifier = Modifier.weight(0.73f).fillMaxWidth())

            BottomStatusSection(v = velocidadeAtual, b = bateriaAtual, tB = tempBateriaAtual, tM = tempMotorAtual, m = marchaAtual, isCharging = aCarregarAtual, corDestaque = corDestaque, iconColor = iconColor, textColor = primaryText, u = unidadeVelocidade, modifier = Modifier.weight(0.15f))
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

@Composable
fun PersonalizationContentSection(s: AppStrings, onVoltar: () -> Unit, onNavVisual: () -> Unit, onNavTouch: () -> Unit, onNavCognitive: () -> Unit, onNavAudio: () -> Unit, onNavEditIcons: () -> Unit, corDestaque: Color, primaryText: Color, secondaryText: Color, isIaActivated: Boolean, onIaChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(text = s.persTitle, color = corDestaque, fontSize = 36.sp, fontFamily = montserratFont, modifier = Modifier.align(Alignment.Center))
                Text(text = s.back, color = corDestaque, fontSize = 24.sp, fontFamily = robotoFont, modifier = Modifier.align(Alignment.CenterEnd).clickable { onVoltar() }.padding(8.dp))
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

