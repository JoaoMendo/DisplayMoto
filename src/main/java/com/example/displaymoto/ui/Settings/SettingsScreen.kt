package com.example.displaymoto.ui.screens.dashboard

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.displaymoto.R
import com.example.displaymoto.AppLanguage
import com.example.displaymoto.AppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

val montserratFont: FontFamily = FontFamily(androidx.compose.ui.text.font.Font(com.example.displaymoto.R.font.montserrat_bold, androidx.compose.ui.text.font.FontWeight.Bold), androidx.compose.ui.text.font.Font(com.example.displaymoto.R.font.montserrat_semibold, androidx.compose.ui.text.font.FontWeight.SemiBold))
val robotoFont: FontFamily = FontFamily(androidx.compose.ui.text.font.Font(com.example.displaymoto.R.font.roboto_medium, androidx.compose.ui.text.font.FontWeight.Medium), androidx.compose.ui.text.font.Font(com.example.displaymoto.R.font.roboto_regular, androidx.compose.ui.text.font.FontWeight.Normal))
val agencyFbFont: FontFamily = FontFamily(androidx.compose.ui.text.font.Font(com.example.displaymoto.R.font.agency_fb))
val AzulClaro = Color(0xFF00BFFF)

@Composable
fun SettingsScreen(
    s: AppStrings, currentAppLanguage: AppLanguage = AppLanguage.EN, onAppLanguageChange: (AppLanguage) -> Unit = {},
    velocidadeAtual: Int = 0, bateriaAtual: Int = 85, aCarregarAtual: Boolean = false, tempBateriaAtual: Int = 30, tempMotorAtual: Int = 80, marchaAtual: String = "N",
    corFundoAtual: Color, corPersonalizada: Color = Color(0xFF0D0F26), currentContrast: String = "STANDARD",
    unidadeVelocidade: String = "km/h",
    onUnidadeChange: (String) -> Unit = {},
    onCorFundoChange: (Color) -> Unit = {}, onCorElementosChange: (Color) -> Unit = {}, onCorTextoChange: (Color) -> Unit = {}, onCorTextoSecundarioChange: (Color) -> Unit = {}, onNavigateBack: () -> Unit = {}, onNavigateToPersonalization: () -> Unit = {},
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

    var showColorLibrary by remember { mutableStateOf(false) }
    var showElemColorLibrary by remember { mutableStateOf(false) }
    var showTextColorLibrary by remember { mutableStateOf(false) }
    var showSecTextColorLibrary by remember { mutableStateOf(false) }
    var piscaPulso by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf("--:--") }
    var currentTemp by remember { mutableStateOf("--ºC") }

    // Popup system
    var popupMensagem by remember { mutableStateOf("") }
    var popupCor by remember { mutableStateOf(Color.White) }
    var popupGrave by remember { mutableStateOf(false) }
    var popupVisivel by remember { mutableStateOf(false) }

    val alertFeedback = com.example.displaymoto.LocalAlertFeedback.current
    fun mostrarPopupSettings(msg: String, cor: Color, grave: Boolean) {
        popupMensagem = msg; popupCor = cor; popupGrave = grave; popupVisivel = true
        alertFeedback(msg, grave)
    }

    LaunchedEffect(popupVisivel) { if (popupVisivel) { delay(3000); popupVisivel = false } }

    // Watchers
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz1 }.drop(1).collect { if (it) mostrarPopupSettings(s.indAbs, Color(0xFFFFB300), false) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz2 }.drop(1).collect { if (it) mostrarPopupSettings(s.indMaximos, Color(0xFF42A5F5), false) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz3 }.drop(1).collect { if (it) mostrarPopupSettings(s.indMedios, Color(0xFF00E676), false) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz4 }.drop(1).collect { if (it) mostrarPopupSettings(s.indMil, Color(0xFFFFB300), false) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz5 }.drop(1).collect { if (it) mostrarPopupSettings(s.indBrake, Color(0xFFE53935), true) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz6 }.drop(1).collect { if (it) mostrarPopupSettings(s.indBattery, Color(0xFFE53935), true) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz7 }.drop(1).collect { if (it) mostrarPopupSettings(s.indTempBat, Color(0xFFE53935), true) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz8 }.drop(1).collect { if (it) mostrarPopupSettings(s.indMinimos, Color(0xFF00E676), false) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz9 }.drop(1).collect { if (it) mostrarPopupSettings(s.indEsp, Color(0xFFFFB300), false) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz10 }.drop(1).collect { if (it) mostrarPopupSettings(s.indNeblina, Color(0xFFFFB300), false) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz11 }.drop(1).collect { if (it) mostrarPopupSettings(s.indPneu, Color(0xFFFFB300), false) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz12 }.drop(1).collect { if (it) mostrarPopupSettings(s.indTempMotor, Color(0xFFE53935), true) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz13 }.drop(1).collect { if (it) mostrarPopupSettings(s.indV2x, Color(0xFF42A5F5), false) } }

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
            TopBarSectionSettings(currentTime = currentTime, currentTemp = currentTemp, pEsq = indicadores.piscaEsquerdo, pDir = indicadores.piscaDireito, pPulso = piscaPulso, l1 = indicadores.luz1, l2 = indicadores.luz2, l3 = indicadores.luz3, l4 = indicadores.luz4, l5 = indicadores.luz5, l6 = indicadores.luz6, l7 = indicadores.luz7, l8 = indicadores.luz8, l9 = indicadores.luz9, l10 = indicadores.luz10, l11 = indicadores.luz11, l12 = indicadores.luz12, l13 = indicadores.luz13, motoLigada = indicadores.motoLigada, marchaAtual = marchaAtual, aCarregar = aCarregarAtual, textColor = primaryText, modifier = Modifier.weight(0.12f))
            SettingsContentSection(s = s, currentAppLanguage = currentAppLanguage, onAppLanguageChange = onAppLanguageChange, onVoltar = onNavigateBack, onCorSelecionada = onCorFundoChange, onCorElementosSelecionada = onCorElementosChange, onCorTextoSelecionada = onCorTextoChange, onCorTextoSecundarioSelecionada = onCorTextoSecundarioChange, onOpenLibrary = { showColorLibrary = true }, onOpenElemLibrary = { showElemColorLibrary = true }, onOpenTextLibrary = { showTextColorLibrary = true }, onOpenSecTextLibrary = { showSecTextColorLibrary = true }, onNavigateToPersonalization = onNavigateToPersonalization, unidadeVelocidade = unidadeVelocidade, onUnidadeChange = onUnidadeChange, corDestaque = corDestaque, iconColor = iconColor, primaryText = primaryText, secondaryText = secondaryText, modifier = Modifier.weight(0.73f))
            BottomStatusSection(v = velocidadeAtual, b = bateriaAtual, tB = tempBateriaAtual, tM = tempMotorAtual, m = marchaAtual, isCharging = aCarregarAtual, corDestaque = corDestaque, iconColor = iconColor, textColor = primaryText, u = unidadeVelocidade, modifier = Modifier.weight(0.15f))
        }

        // === Popup de indicador ===
        if (popupVisivel) {
            if (popupGrave) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                    Column(modifier = Modifier.background(Color(0xFF1A0A0A), shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)).border(4.dp, popupCor, shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)).padding(horizontal = 64.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("⚠", fontSize = 56.sp)
                        Text(text = s.warning, color = popupCor, fontSize = 48.sp, fontFamily = agencyFbFont, fontWeight = FontWeight.Bold)
                        Text(text = popupMensagem, color = Color.White, fontSize = 36.sp, fontFamily = agencyFbFont, fontWeight = FontWeight.Bold)
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

        if (showColorLibrary) {
            ColorLibraryDialog(s = s, initialColor = corFundoAtual, onDismiss = { showColorLibrary = false }, onColorSelected = { onCorFundoChange(it); showColorLibrary = false })
        }
        if (showElemColorLibrary) {
            ColorLibraryDialog(s = s, initialColor = iconColor, onDismiss = { showElemColorLibrary = false }, onColorSelected = { onCorElementosChange(it); showElemColorLibrary = false })
        }
        if (showTextColorLibrary) {
            ColorLibraryDialog(s = s, initialColor = primaryText, onDismiss = { showTextColorLibrary = false }, onColorSelected = { onCorTextoChange(it); showTextColorLibrary = false })
        }
        if (showSecTextColorLibrary) {
            ColorLibraryDialog(s = s, initialColor = secondaryText, onDismiss = { showSecTextColorLibrary = false }, onColorSelected = { onCorTextoSecundarioChange(it); showSecTextColorLibrary = false })
        }
    }
}

// ==================================================================================================
// COMPONENTES DA INTERFACE (COM ACESSIBILIDADE WCAG / TALKBACK)
// ==================================================================================================

@Composable
fun TopBarSectionSettings(
    currentTime: String, currentTemp: String, pEsq: Boolean, pDir: Boolean, pPulso: Boolean,
    l1: Boolean, l2: Boolean, l3: Boolean, l4: Boolean, l5: Boolean = false, l6: Boolean = false,
    l7: Boolean = false, l8: Boolean = false, l9: Boolean = false, l10: Boolean = false,
    l11: Boolean = false, l12: Boolean = false, l13: Boolean = false,
    motoLigada: Boolean = false, marchaAtual: String = "P", aCarregar: Boolean = false,
    textColor: Color = Color.White, modifier: Modifier = Modifier
) {
    val corVerde = Color(0xFF00E676); val corAzul = Color(0xFF448AFF)
    val corAmarelo = Color(0xFFFFB300); val corVermelho = Color(0xFFFF1744)
    val readyToDrive = motoLigada; val neutralAtivo = marchaAtual == "N"; val chargingAtivo = aCarregar

    Box(modifier = modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 32.dp)) {
        Row(modifier = Modifier.align(Alignment.TopStart).padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$currentTime PM  |  ☁ $currentTemp", color = textColor, fontSize = 32.sp, fontFamily = robotoFont, modifier = Modifier.semantics { contentDescription = "Current time $currentTime PM, Weather $currentTemp" })
        }
        Box(modifier = Modifier.align(Alignment.TopCenter).width(440.dp).height(100.dp), contentAlignment = Alignment.Center) {
            Image(painter = painterResource(id = R.drawable.border_piscas), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Row(horizontalArrangement = Arrangement.spacedBy(50.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(painter = painterResource(id = R.drawable.ic_seta_dir), contentDescription = if (pEsq && pPulso) "Left turn signal active" else null, tint = Color.Unspecified, modifier = Modifier.size(80.dp).rotate(180f).alpha(if (pEsq && pPulso) 1f else 0f))
                Icon(painter = painterResource(id = R.drawable.ic_seta_dir), contentDescription = if (pDir && pPulso) "Right turn signal active" else null, tint = Color.Unspecified, modifier = Modifier.size(80.dp).alpha(if (pDir && pPulso) 1f else 0f))
            }
        }
        // === Indicadores regulamentares (2x6 grid, igual ao Dashboard) ===
        Column(modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(40.dp)) {
                    Icon(painter = painterResource(id = R.drawable.ic_ready_to_drive), contentDescription = "Ready", tint = corVerde, modifier = Modifier.fillMaxSize().alpha(if (readyToDrive && !chargingAtivo && !neutralAtivo) 1f else 0f))
                    Icon(painter = painterResource(id = R.drawable.ic_charging), contentDescription = "Charging", tint = corVerde, modifier = Modifier.fillMaxSize().alpha(if (chargingAtivo) 1f else 0f))
                    Icon(painter = painterResource(id = R.drawable.ic_neutral), contentDescription = "Neutral", tint = corVerde, modifier = Modifier.fillMaxSize().alpha(if (neutralAtivo && !chargingAtivo) 1f else 0f))
                }
                Icon(painter = painterResource(id = R.drawable.ic_battery_warning), contentDescription = "Battery", tint = corVermelho, modifier = Modifier.size(40.dp).alpha(if (l6) 1f else 0f))
                Icon(painter = painterResource(id = R.drawable.ic_temp_warning), contentDescription = "Temp Bat", tint = corVermelho, modifier = Modifier.size(40.dp).alpha(if (l7) 1f else 0f))
                Box(modifier = Modifier.size(40.dp)) {
                    Icon(painter = painterResource(id = R.drawable.ic_position_lights), contentDescription = "Mínimos", tint = corVerde, modifier = Modifier.fillMaxSize().alpha(if (l8) 1f else 0f))
                    Icon(painter = painterResource(id = R.drawable.ic_low_beam), contentDescription = "Low Beam", tint = corVerde, modifier = Modifier.fillMaxSize().alpha(if (l3) 1f else 0f))
                    Icon(painter = painterResource(id = R.drawable.ic_high_beam), contentDescription = "High Beam", tint = corAzul, modifier = Modifier.fillMaxSize().alpha(if (l2) 1f else 0f))
                }
                Icon(painter = painterResource(id = R.drawable.ic_neblina), contentDescription = "Neblina", tint = corAmarelo, modifier = Modifier.size(40.dp).alpha(if (l10) 1f else 0f))
                Icon(painter = painterResource(id = R.drawable.ic_temp_motor), contentDescription = "Temp Motor", tint = corVermelho, modifier = Modifier.size(40.dp).alpha(if (l12) 1f else 0f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(painter = painterResource(id = R.drawable.ic_brake_warning), contentDescription = "Brake", tint = corVermelho, modifier = Modifier.size(40.dp).alpha(if (l5) 1f else 0f))
                Icon(painter = painterResource(id = R.drawable.ic_mil), contentDescription = "MIL", tint = corAmarelo, modifier = Modifier.size(40.dp).alpha(if (l4) 1f else 0f))
                Icon(painter = painterResource(id = R.drawable.ic_abs), contentDescription = "ABS", tint = corAmarelo, modifier = Modifier.size(40.dp).alpha(if (l1) 1f else 0f))
                Icon(painter = painterResource(id = R.drawable.ic_estabilidade), contentDescription = "ESP", tint = corAmarelo, modifier = Modifier.size(40.dp).alpha(if (l9) 1f else 0f))
                Icon(painter = painterResource(id = R.drawable.ic_pneu_vazio), contentDescription = "Tire", tint = corAmarelo, modifier = Modifier.size(40.dp).alpha(if (l11) 1f else 0f))
                Icon(painter = painterResource(id = R.drawable.ic_v2x), contentDescription = "V2X", tint = corAzul, modifier = Modifier.size(40.dp).alpha(if (l13) 1f else 0f))
            }
        }
    }
}

@Composable
fun BottomStatusSection(v: Int, b: Int, tB: Int, tM: Int, m: String, isCharging: Boolean = false, corDestaque: Color, iconColor: Color = corDestaque, textColor: Color = Color.White, u: String = "km/h", modifier: Modifier = Modifier) {
    val corBateria = when { isCharging -> corDestaque; b <= 20 -> Color.Red; else -> Color(0xFF00FF7F) }

    Row(modifier = modifier.fillMaxWidth().background(textColor.copy(alpha = 0.08f)).padding(horizontal = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        // Velocidade (TalkBack vai ler o texto diretamente, a barra não precisa de descrição extra)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) {}) {
            val displayV = if (u == "mph") (v / 1.60934).toInt() else v
            val unitStr = if (u == "mph") "MPH" else "KM/H"
            Text("$displayV $unitStr", color = textColor, fontSize = 42.sp, fontFamily = montserratFont, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { contentDescription = "Speed $displayV $unitStr" })
            Spacer(Modifier.width(16.dp))
            Box(modifier = Modifier.width(180.dp).height(20.dp).clip(ParallelogramShape(30f)).background(Color.White.copy(alpha = 0.5f))) {
                Box(modifier = Modifier.fillMaxWidth(v / 120f).fillMaxHeight().background(corDestaque))
            }
        }
        // Bateria
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) {}) {
            val statusBateria = if (isCharging) "charging" else ""
            Text("$b%", color = textColor, fontSize = 42.sp, fontFamily = montserratFont, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { contentDescription = "Battery at $b percent $statusBateria" })
            Spacer(Modifier.width(16.dp))
            Box(modifier = Modifier.width(180.dp).height(20.dp).clip(ParallelogramShape(30f)).background(Color.White.copy(alpha = 0.5f))) {
                Box(modifier = Modifier.fillMaxWidth(b / 100f).fillMaxHeight().background(corBateria))
            }
        }
        // Temperaturas e Marcha
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // AQUI: Descrevemos o ícone para cegos saberem do que se trata a temperatura
                Icon(painterResource(id = R.drawable.ic_temp_bat_set), contentDescription = "Battery temperature", tint = iconColor, modifier = Modifier.size(42.dp))
                Text("${tB}º", color = textColor, fontSize = 42.sp, fontFamily = montserratFont, modifier = Modifier.padding(start = 8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // AQUI: Descrevemos o ícone do motor
                Icon(painterResource(id = R.drawable.ic_temp_motor_set), contentDescription = "Engine temperature", tint = iconColor, modifier = Modifier.size(42.dp))
                Text("${tM}º", color = textColor, fontSize = 42.sp, fontFamily = montserratFont, modifier = Modifier.padding(start = 8.dp))
            }
            Text(m, color = textColor, fontSize = 50.sp, fontFamily = montserratFont, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp).semantics { contentDescription = "Current gear $m" })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingItem(titulo: String, subtitulo: String, primaryColor: Color = Color.White, secondaryColor: Color = Color.Gray, onClick: (() -> Unit)? = null, conteudo: @Composable () -> Unit = {}) {
    val missClickTracker = com.example.displaymoto.LocalMissClickTracker.current
    val helpMode = com.example.displaymoto.LocalHelpMode.current

    // "ON DEMAND" → subtítulo só aparece depois de manter premido (toggle por item)
    var subtituloRevelado by remember { mutableStateOf(false) }
    val mostrarSubtitulo = when (helpMode) {
        "OFF"        -> false
        "ON DEMAND"  -> subtituloRevelado
        else         -> true   // "ALWAYS ON" / default
    }

    val baseClick: () -> Unit = { onClick?.invoke() ?: missClickTracker() }
    val baseLongClick: (() -> Unit)? = if (helpMode == "ON DEMAND") {
        { subtituloRevelado = !subtituloRevelado }
    } else null

    val modifier = Modifier.fillMaxWidth()
        .combinedClickable(
            onClickLabel = "Open $titulo setting",
            role = Role.Button,
            onLongClick = baseLongClick,
            onClick = baseClick
        )
        .padding(vertical = 12.dp, horizontal = 16.dp)

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(titulo, color = primaryColor, fontSize = 28.sp, fontFamily = montserratFont)
            if (mostrarSubtitulo) {
                Text(subtitulo, color = secondaryColor, fontSize = 18.sp, fontFamily = robotoFont)
            }
        }
        conteudo()
    }
}

@Composable
fun SettingsContentSection(s: AppStrings, currentAppLanguage: AppLanguage, onAppLanguageChange: (AppLanguage) -> Unit, onVoltar: () -> Unit, onCorSelecionada: (Color) -> Unit, onCorElementosSelecionada: (Color) -> Unit, onCorTextoSelecionada: (Color) -> Unit, onCorTextoSecundarioSelecionada: (Color) -> Unit, onOpenLibrary: () -> Unit, onOpenElemLibrary: () -> Unit, onOpenTextLibrary: () -> Unit, onOpenSecTextLibrary: () -> Unit, onNavigateToPersonalization: () -> Unit, unidadeVelocidade: String, onUnidadeChange: (String) -> Unit, corDestaque: Color, iconColor: Color, primaryText: Color, secondaryText: Color, modifier: Modifier = Modifier) {
    val activity = LocalContext.current.findActivity()
    val scrollState = rememberScrollState()
    var showLanguageDropdown by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(id = R.drawable.ic_settings), contentDescription = null, tint = iconColor, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(s.settingsTitle, color = corDestaque, fontSize = 36.sp, fontFamily = montserratFont)
                    }
                }
                Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.Center) {
                    var nivelBrilho by remember { mutableFloatStateOf(0.5f) }
                    LaunchedEffect(nivelBrilho) { activity?.window?.let { it.attributes = it.attributes.apply { screenBrightness = nivelBrilho } } }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Screen brightness slider" }) {
                        Icon(Icons.Filled.BrightnessHigh, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(16.dp))
                        Slider(value = nivelBrilho, onValueChange = { nivelBrilho = it }, modifier = Modifier.width(200.dp), colors = SliderDefaults.colors(thumbColor = corDestaque, activeTrackColor = corDestaque, inactiveTrackColor = primaryText.copy(alpha = 0.3f)))
                        Spacer(Modifier.width(16.dp))
                        Text("${(nivelBrilho * 100).toInt()}%", color = primaryText, fontSize = 20.sp, fontFamily = robotoFont, modifier = Modifier.width(45.dp))
                    }
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    Text(s.back, color = corDestaque, fontSize = 24.sp, fontFamily = robotoFont, modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Go back") { onVoltar() }.padding(8.dp))
                }
            }

            Spacer(Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = s.bluetoothTitle, subtitulo = s.bluetoothDesc, primaryColor = primaryText, secondaryColor = secondaryText)
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = s.connectTitle, subtitulo = s.connectDesc, primaryColor = primaryText, secondaryColor = secondaryText)
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = s.colorTitle, subtitulo = s.colorDesc, primaryColor = primaryText, secondaryColor = secondaryText, conteudo = {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CirculoCor(Color(0xFF0D0F26), "Default Dark Blue") { onCorSelecionada(Color(0xFF0D0F26)) }
                        CirculoCor(Color.Red, "Red") { onCorSelecionada(Color.Red) }
                        CirculoCor(Color.Green, "Green") { onCorSelecionada(Color.Green) }
                        Text(s.more, color = corDestaque, fontSize = 18.sp, modifier = Modifier.clickable(role = Role.Button) { onOpenLibrary() })
                    }
                })
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = s.elemColorTitle, subtitulo = s.elemColorDesc, primaryColor = primaryText, secondaryColor = secondaryText, conteudo = {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CirculoCor(Color(0xFF00BFFF), "Cyan") { onCorElementosSelecionada(Color(0xFF00BFFF)) }
                        CirculoCor(Color(0xFFFFB300), "Amber") { onCorElementosSelecionada(Color(0xFFFFB300)) }
                        CirculoCor(Color.White, "White") { onCorElementosSelecionada(Color.White) }
                        Text(s.more, color = corDestaque, fontSize = 18.sp, modifier = Modifier.clickable(role = Role.Button) { onOpenElemLibrary() })
                    }
                })
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = s.textColorTitle, subtitulo = s.textColorDesc, primaryColor = primaryText, secondaryColor = secondaryText, conteudo = {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CirculoCor(Color.White, "White") { onCorTextoSelecionada(Color.White) }
                        CirculoCor(Color.Black, "Black") { onCorTextoSelecionada(Color.Black) }
                        CirculoCor(Color.Gray, "Gray") { onCorTextoSelecionada(Color.Gray) }
                        Text(s.more, color = corDestaque, fontSize = 18.sp, modifier = Modifier.clickable(role = Role.Button) { onOpenTextLibrary() })
                    }
                })
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = s.secTextColorTitle, subtitulo = s.secTextColorDesc, primaryColor = primaryText, secondaryColor = secondaryText, conteudo = {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CirculoCor(Color.White, "White") { onCorTextoSecundarioSelecionada(Color.White) }
                        CirculoCor(Color.Black, "Black") { onCorTextoSecundarioSelecionada(Color.Black) }
                        CirculoCor(Color.Gray, "Gray") { onCorTextoSecundarioSelecionada(Color.Gray) }
                        Text(s.more, color = corDestaque, fontSize = 18.sp, modifier = Modifier.clickable(role = Role.Button) { onOpenSecTextLibrary() })
                    }
                })
                LinhaDivisoria(corDestaque)
                SettingItem(
                    titulo = s.vehRegUnitTitle,
                    subtitulo = s.vehRegUnitSub,
                    primaryColor = primaryText, secondaryColor = secondaryText,
                    conteudo = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "KM/H", color = if (unidadeVelocidade == "km/h") corDestaque else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (unidadeVelocidade == "km/h") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onUnidadeChange("km/h") })
                            Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = robotoFont)
                            Text(text = "MPH", color = if (unidadeVelocidade == "mph") corDestaque else secondaryText, fontSize = 24.sp, fontFamily = robotoFont, fontWeight = if (unidadeVelocidade == "mph") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { onUnidadeChange("mph") })
                        }
                    }
                )
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = s.langTitle, subtitulo = s.langDesc, primaryColor = primaryText, secondaryColor = secondaryText, onClick = { showLanguageDropdown = true }, conteudo = {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .semantics(mergeDescendants = true) {
                                    contentDescription = "${s.langTitle}: ${currentAppLanguage.displayName}"
                                    role = Role.DropdownList
                                }
                                .clickable(role = Role.Button, onClickLabel = s.langTitle) { showLanguageDropdown = true }
                        ) {
                            Text(currentAppLanguage.displayName, color = primaryText, fontSize = 24.sp, fontFamily = robotoFont)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("▾", color = corDestaque, fontSize = 24.sp, modifier = Modifier.semantics { contentDescription = "" })
                        }
                        DropdownMenu(expanded = showLanguageDropdown, onDismissRequest = { showLanguageDropdown = false }, containerColor = Color(0xFF1A1C2E)) {
                            AppLanguage.entries.forEach { lang ->
                                val isSelected = lang == currentAppLanguage
                                DropdownMenuItem(
                                    modifier = Modifier.semantics {
                                        selected = isSelected
                                        contentDescription = lang.displayName + if (isSelected) " ✓" else ""
                                    },
                                    text = { Text(lang.displayName, color = if (isSelected) corDestaque else Color.White, fontSize = 22.sp, fontFamily = robotoFont) },
                                    onClick = { onAppLanguageChange(lang); showLanguageDropdown = false }
                                )
                            }
                        }
                    }
                })
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = s.persTitle, subtitulo = s.persDesc, primaryColor = primaryText, secondaryColor = secondaryText, onClick = onNavigateToPersonalization)
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = s.aboutTitle, subtitulo = s.aboutDesc, primaryColor = primaryText, secondaryColor = secondaryText)
                LinhaDivisoria(corDestaque)

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ColorLibraryDialog(s: AppStrings, initialColor: Color, onDismiss: () -> Unit, onColorSelected: (Color) -> Unit) {
    var selectedColor by remember { mutableStateOf(initialColor) }
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(onClick = { onColorSelected(selectedColor) }, colors = ButtonDefaults.buttonColors(containerColor = AzulClaro)) { Text(s.select, fontFamily = montserratFont, color = Color.White) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel, color = Color.Gray, fontFamily = montserratFont) } }, title = { Text(s.colorLibraryTitle, fontFamily = montserratFont, fontSize = 28.sp, color = Color.White) }, containerColor = Color(0xFF1A1C2E), text = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).background(selectedColor).border(2.dp, Color.White, RoundedCornerShape(12.dp)))
            Spacer(Modifier.height(24.dp)); HueBar(onColorChanged = { selectedColor = it }); Spacer(Modifier.height(16.dp)); Text(s.dragColor, color = Color.LightGray, fontSize = 16.sp, fontFamily = robotoFont)
        }
    })
}

@Composable
fun HueBar(onColorChanged: (Color) -> Unit) {
    val hueColors = remember { listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    Box(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(hueColors)).pointerInput(Unit) {
        detectDragGestures { change, _ -> offsetX = change.position.x.coerceIn(0f, size.width.toFloat()); onColorChanged(Color.hsv((offsetX / size.width) * 360f, 0.8f, 0.4f)) }
    }) { Canvas(modifier = Modifier.fillMaxSize()) { drawCircle(Color.White, radius = 15f, center = Offset(offsetX, size.height / 2)) } }
}

@Composable
fun CirculoCor(cor: Color, colorName: String, onClick: () -> Unit) {
    // AQUI TAMBÉM: Damos nome à cor para cegos saberem no que tocam
    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(cor).border(2.dp, Color.White, CircleShape).semantics { contentDescription = "Color $colorName"; role = Role.Button }.clickable { onClick() })
}

@Composable
fun LinhaDivisoria(corDestaque: Color = AzulClaro) { HorizontalDivider(color = corDestaque, thickness = 2.dp) }

class ParallelogramShape(private val angle: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply { val offset = (size.height * kotlin.math.tan(Math.toRadians(angle.toDouble()))).toFloat(); moveTo(offset, 0f); lineTo(size.width, 0f); lineTo(size.width - offset, size.height); lineTo(0f, size.height); close() }
        return Outline.Generic(path)
    }
}

fun Context.findActivity(): Activity? {
    var context = this; while (context is ContextWrapper) { if (context is Activity) return context; context = context.baseContext }; return null
}

