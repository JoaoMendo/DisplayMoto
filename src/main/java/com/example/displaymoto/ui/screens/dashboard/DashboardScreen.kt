@file:Suppress("UNUSED_VALUE", "SpellCheckingInspection", "UnusedImport")
package com.example.displaymoto.ui.screens.dashboard

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.displaymoto.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import com.example.displaymoto.AppStrings
import java.text.SimpleDateFormat
import java.util.*

val agencyFb: FontFamily = FontFamily(Font(R.font.agency_fb))

@Composable
fun DashboardScreen(
    s: AppStrings,
    velocidadeAtual: Int = 0,
    bateriaAtual: Int = 85,
    tempBateriaAtual: Int = 30,
    tempMotorAtual: Int = 80,
    marchaAtual: String = "P",
    autonomiaKm: Int = 200,
    corFundoAtual: Color = Color(0xFF0D0F26),
    corPersonalizada: Color = Color(0xFF0D0F26),
    currentContrast: String = "STANDARD",
    autonomiaInicial: Float = 200f,
    aCarregarInicial: Boolean = false,
    onBateriaChange: (Float, Boolean) -> Unit = { _, _ -> },
    onNavigateToSettings: () -> Unit,
    aiCorDestaque: Color? = null,
    aiPrimaryText: Color? = null,
    aiSecondaryText: Color? = null,
    onMarchaChange: (String) -> Unit = {}
) {
    val isLightBg = corPersonalizada.luminance() > 0.5f

    val uiElementColor = when (currentContrast) {
        "HIGH CONTRAST" -> if (corPersonalizada.luminance() < 0.35f) lerp(corPersonalizada, Color.White, 0.7f) else corPersonalizada
        "NIGHT MODE" -> lerp(corPersonalizada, Color.White, 0.35f)
        else -> if (isLightBg) Color(0xFF004466) else Color.White
    }

    val iconColor = aiCorDestaque ?: uiElementColor

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

    // NOVO: O Texto fica BOLD se for Alto Contraste!
    val contrastWeight = if (currentContrast == "HIGH CONTRAST") FontWeight.Bold else FontWeight.Normal

    var luz1 by remember { mutableStateOf(false) }
    var luz2 by remember { mutableStateOf(false) }
    var luz3 by remember { mutableStateOf(false) }
    var luz4 by remember { mutableStateOf(false) }
    var piscaEsquerdo by remember { mutableStateOf(false) }
    var piscaDireito by remember { mutableStateOf(false) }
    var motoLigada by remember { mutableStateOf(false) }
    var velocidadeTarget by remember { mutableFloatStateOf(0f) }
    var modeIdx by remember { mutableIntStateOf(1) }

    val velocidadeAnimadaState = animateFloatAsState(targetValue = if (motoLigada) velocidadeTarget else 0f, animationSpec = tween(1500, easing = FastOutSlowInEasing), label = "")
    val tempAnimadaState = animateFloatAsState(targetValue = if (motoLigada) 0.50f else 0f, animationSpec = tween(4000, easing = FastOutSlowInEasing), label = "")

    var odometro by remember { mutableFloatStateOf(0f) }
    var autonomia by remember { mutableFloatStateOf(autonomiaInicial) }
    var consumo by remember { mutableFloatStateOf(0f) }
    var aCarregar by remember { mutableStateOf(aCarregarInicial) }

    LaunchedEffect(autonomia, aCarregar) { onBateriaChange(autonomia, aCarregar) }

    val bateriaPercentagem = (autonomia / 200f) * 100f
    var avisoBateriaDispensado by remember { mutableStateOf(false) }

    LaunchedEffect(bateriaPercentagem) { if (bateriaPercentagem > 20f) avisoBateriaDispensado = false }

    val mostrarAviso = bateriaPercentagem <= 20f && !avisoBateriaDispensado && !aCarregar
    LaunchedEffect(mostrarAviso) { if (mostrarAviso) { delay(5000); avisoBateriaDispensado = true } }

    LaunchedEffect(Unit) {
        while (true) {
            if (aCarregar) { autonomia = minOf(200f, autonomia + 2.5f); if (autonomia >= 200f) aCarregar = false }
            val velReal = velocidadeAnimadaState.value
            val multiplicadorConsumo = when(modeIdx) { 0 -> 0.75f; 2 -> 1.40f; else -> 1.0f }
            if (velReal > 0.1f) {
                val deltaDistancia = (velReal / 3600f) * 0.1f
                val distVisualTeste = deltaDistancia * 150f
                odometro += distVisualTeste
                autonomia = maxOf(0f, autonomia - (distVisualTeste * multiplicadorConsumo))
                consumo = (8.4f + (velReal * 0.05f)) * multiplicadorConsumo
            } else {
                consumo = if (motoLigada && bateriaPercentagem > 0f) 0.8f * multiplicadorConsumo else 0.0f
            }
            if (autonomia <= 0f && velocidadeTarget > 0f) velocidadeTarget = 0f
            delay(100)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(corFundoAtual)) {
            TopBarSection(luz1, luz2, luz3, luz4, piscaEsquerdo, piscaDireito, primaryText, contrastWeight, Modifier.weight(0.12f).padding(horizontal = 32.dp))
            MainContentSection(s, velocidadeAnimadaState.value, tempAnimadaState.value, marchaAtual, motoLigada, velocidadeTarget, bateriaPercentagem, aCarregar, primaryText, secondaryText, uiElementColor, contrastWeight, { motoLigada = it }, onMarchaChange, { velocidadeTarget = it }, { luz1 = !luz1 }, { luz2 = !luz2 }, { luz3 = !luz3 }, { luz4 = !luz4 }, { piscaEsquerdo = !piscaEsquerdo; if (piscaEsquerdo) piscaDireito = false }, { piscaDireito = !piscaDireito; if (piscaDireito) piscaEsquerdo = false }, { aCarregar = !aCarregar }, Modifier.weight(0.63f).padding(horizontal = 32.dp))
            InfoBarSection(odometro, autonomia, consumo, primaryText, iconColor, contrastWeight, Modifier.weight(0.1f).padding(horizontal = 32.dp))
            BottomBarSection(modeIdx, primaryText, secondaryText, uiElementColor, iconColor, contrastWeight, { modeIdx = it }, onNavigateToSettings, Modifier.weight(0.15f))
        }

        if (mostrarAviso) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                Column(modifier = Modifier.border(4.dp, Color(0xFFE53935)).background(Color(0xFF1A1A1A)).padding(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.warning, color = Color(0xFFE53935), fontSize = 72.sp, fontFamily = agencyFb, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(s.lowBattery, color = Color.White, fontSize = 56.sp, fontFamily = agencyFb, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(s.critRange1, color = Color.LightGray, fontSize = 36.sp, fontFamily = agencyFb)
                    Text(s.critRange2, color = Color.LightGray, fontSize = 36.sp, fontFamily = agencyFb)
                }
            }
        }
    }
}

@Composable
private fun TopBarSection(luz1: Boolean, luz2: Boolean, luz3: Boolean, luz4: Boolean, piscaEsqLigado: Boolean, piscaDirLigado: Boolean, primaryText: Color, contrastWeight: FontWeight, modifier: Modifier = Modifier) {
    var currentTime by remember { mutableStateOf("--:--") }
    var currentTemp by remember { mutableStateOf("--ºC") }
    var piscaPulso by remember { mutableStateOf(false) }

    LaunchedEffect(piscaEsqLigado, piscaDirLigado) {
        if (piscaEsqLigado || piscaDirLigado) { while (true) { piscaPulso = true; delay(400); piscaPulso = false; delay(400) } } else { piscaPulso = false }
    }
    LaunchedEffect(Unit) {
        while (true) { currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("Europe/Lisbon") }.format(Date()); delay(1000) }
    }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { try { currentTemp = "${JSONObject(URL("https://api.open-meteo.com/v1/forecast?latitude=41.3006&longitude=-7.7441&current_weather=true").readText()).getJSONObject("current_weather").getInt("temperature")}ºC" } catch (_: Exception) {} }
    }

    Box(modifier = modifier.fillMaxWidth().fillMaxHeight()) {
        Row(modifier = Modifier.align(Alignment.TopStart).padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$currentTime PM  |  ☁ $currentTemp", color = primaryText, fontSize = 32.sp, fontFamily = agencyFb, fontWeight = contrastWeight)
        }
        Box(modifier = Modifier.align(Alignment.TopCenter).width(440.dp).height(100.dp), contentAlignment = Alignment.Center) {
            Image(painter = painterResource(id = R.drawable.border_piscas), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
            Row(horizontalArrangement = Arrangement.spacedBy(50.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(painter = painterResource(id = R.drawable.ic_seta_dir), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(80.dp).rotate(180f).alpha(if (piscaEsqLigado && piscaPulso) 1f else 0f))
                Icon(painter = painterResource(id = R.drawable.ic_seta_dir), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(80.dp).alpha(if (piscaDirLigado && piscaPulso) 1f else 0f))
            }
        }
        Row(modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            listOf(R.drawable.ic_luz_verde to luz1, R.drawable.ic_luz_cinza to luz2, R.drawable.ic_abs to luz3, R.drawable.ic_motor to luz4).forEach { (id, ativa) ->
                Icon(painter = painterResource(id = id), contentDescription = null, tint = if (ativa) Color.Unspecified else Color.Transparent, modifier = Modifier.size(48.dp).alpha(if (ativa) 1f else 0f))
            }
        }
    }
}

@Composable
private fun MainContentSection(s: AppStrings, velocidadeAnimada: Float, tempAnimada: Float, marcha: String, motoLigada: Boolean, velocidadeTarget: Float, bateriaPercentagem: Float, aCarregar: Boolean, primaryText: Color, secondaryText: Color, accentColor: Color, contrastWeight: FontWeight, onMotoLigadaChange: (Boolean) -> Unit, onMarchaChange: (String) -> Unit, onVelocidadeTargetChange: (Float) -> Unit, onToggleLuz1: () -> Unit, onToggleLuz2: () -> Unit, onToggleLuz3: () -> Unit, onToggleLuz4: () -> Unit, onTogglePiscaEsq: () -> Unit, onTogglePiscaDir: () -> Unit, onToggleCarga: () -> Unit, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(modifier = modifier.fillMaxWidth().focusRequester(focusRequester).focusable().onKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown) {
            when (event.key) {
                Key.Enter, Key.NumPadEnter -> { val proximoEstado = !motoLigada; onMotoLigadaChange(proximoEstado); if (proximoEstado) onMarchaChange("D") else { onMarchaChange("P"); onVelocidadeTargetChange(0f) }; true }
                Key.C -> { if (velocidadeAnimada < 1f) onToggleCarga(); true }
                Key.W -> { if (motoLigada && bateriaPercentagem > 0f && !aCarregar) onVelocidadeTargetChange(minOf(120f, velocidadeTarget + 5f)); true }
                Key.S -> { if (motoLigada && !aCarregar) onVelocidadeTargetChange(maxOf(0f, velocidadeTarget - 8f)); true }
                Key.D -> { if (motoLigada) onMarchaChange("D"); true }
                Key.N -> { if (motoLigada) onMarchaChange("N"); true }
                Key.P -> { if (motoLigada) onMarchaChange("P"); true }
                Key.One, Key.NumPad1 -> { onToggleLuz1(); true }
                Key.Two, Key.NumPad2 -> { onToggleLuz2(); true }
                Key.Three, Key.NumPad3 -> { onToggleLuz3(); true }
                Key.Four, Key.NumPad4 -> { onToggleLuz4(); true }
                Key.DirectionLeft -> { onTogglePiscaEsq(); true }
                Key.DirectionRight -> { onTogglePiscaDir(); true }
                else -> false
            }
        } else false
    }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Box(modifier = Modifier.size(280.dp, 360.dp).align(Alignment.Center)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val espessura = 15.dp.toPx(); val gap = 8.dp.toPx()
                    val pBaixo = Offset(size.width - 20.dp.toPx(), size.height - 20.dp.toPx())
                    val pMeioBaixo = Offset(60.dp.toPx(), size.height / 2f + gap)
                    val pMeioCima = Offset(60.dp.toPx(), size.height / 2f - gap)
                    val pCima = Offset(size.width - 20.dp.toPx(), 20.dp.toPx())
                    drawLine(accentColor.copy(alpha = 0.2f), pBaixo, pMeioBaixo, espessura, StrokeCap.Butt)
                    drawLine(accentColor.copy(alpha = 0.2f), pMeioCima, pCima, espessura, StrokeCap.Butt)
                    clipRect(top = (size.height - 20.dp.toPx()) - ((size.height - 40.dp.toPx()) * (velocidadeAnimada / 120f))) {
                        drawLine(accentColor, pBaixo, pMeioBaixo, espessura, StrokeCap.Butt)
                        drawLine(accentColor, pMeioCima, pCima, espessura, StrokeCap.Butt)
                    }
                }
                EscalaTexto("120", 175.dp, 0.dp, primaryText, contrastWeight); EscalaTexto("100", 115.dp, 55.dp, primaryText, contrastWeight); EscalaTexto("80", 55.dp, 110.dp, primaryText, contrastWeight); EscalaTexto("60", 15.dp, 165.dp, primaryText, contrastWeight); EscalaTexto("40", 55.dp, 220.dp, primaryText, contrastWeight); EscalaTexto("20", 115.dp, 275.dp, primaryText, contrastWeight); EscalaTexto("0", 175.dp, 330.dp, primaryText, contrastWeight)
                BarraTermicaSVG(modifier = Modifier.align(Alignment.BottomStart).offset(x = 20.dp, y = 10.dp).rotate(-3f).size(125.dp), progresso = tempAnimada, svgId = R.drawable.ic_temp_motor_dash)
                EscalaTempTexto("100", (-15).dp, 250.dp, primaryText, contrastWeight); EscalaTempTexto("50", 15.dp, 290.dp, primaryText, contrastWeight); EscalaTempTexto("20", 45.dp, 330.dp, primaryText, contrastWeight)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center).offset(x = 85.dp)) {
                Text(marcha, color = primaryText, fontSize = 42.sp, fontFamily = agencyFb, fontWeight = contrastWeight)
                Text("${velocidadeAnimada.toInt()}", color = primaryText, fontSize = 120.sp, fontFamily = agencyFb, fontWeight = FontWeight.Bold)
                Text("km/h", color = secondaryText, fontSize = 24.sp, fontFamily = agencyFb, fontWeight = contrastWeight)
            }
        }
        Box(modifier = Modifier.weight(1.3f), contentAlignment = Alignment.Center) { Text(s.road3d, color = secondaryText, fontSize = 24.sp, fontFamily = agencyFb, fontWeight = contrastWeight) }
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Box(modifier = Modifier.size(280.dp, 360.dp).align(Alignment.Center)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val espessura = 15.dp.toPx(); val gap = 8.dp.toPx()
                    val pBaixo = Offset(20.dp.toPx(), size.height - 20.dp.toPx())
                    val pMeioBaixo = Offset(size.width - 60.dp.toPx(), size.height / 2f + gap)
                    val pMeioCima = Offset(size.width - 60.dp.toPx(), size.height / 2f - gap)
                    val pCima = Offset(20.dp.toPx(), 20.dp.toPx())
                    drawLine(accentColor.copy(alpha = 0.2f), pBaixo, pMeioBaixo, espessura, StrokeCap.Butt)
                    drawLine(accentColor.copy(alpha = 0.2f), pMeioCima, pCima, espessura, StrokeCap.Butt)
                    clipRect(top = (size.height - 20.dp.toPx()) - ((size.height - 40.dp.toPx()) * (bateriaPercentagem / 100f))) {
                        val corBateria = when { aCarregar -> accentColor; bateriaPercentagem <= 20f -> Color.Red; else -> Color.Green }
                        drawLine(corBateria, pBaixo, pMeioBaixo, espessura, StrokeCap.Butt)
                        drawLine(corBateria, pMeioCima, pCima, espessura, StrokeCap.Butt)
                    }
                }
                EscalaTexto("100", 65.dp, 0.dp, primaryText, contrastWeight); EscalaTexto("75", 145.dp, 75.dp, primaryText, contrastWeight); EscalaTexto("50", 235.dp, 165.dp, primaryText, contrastWeight); EscalaTexto("25", 155.dp, 248.dp, primaryText, contrastWeight); EscalaTexto("0", 50.dp, 330.dp, primaryText, contrastWeight)
                BarraTermicaSVG(modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-20).dp, y = 10.dp).rotate(3f).size(125.dp), progresso = tempAnimada, svgId = R.drawable.ic_temp_bat_dash)
                EscalaTempTexto("100", 275.dp, 250.dp, primaryText, contrastWeight); EscalaTempTexto("50", 235.dp, 290.dp, primaryText, contrastWeight); EscalaTempTexto("20", 195.dp, 330.dp, primaryText, contrastWeight)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center).offset(x = (-85).dp)) {
                Text("%", color = primaryText, fontSize = 42.sp, fontFamily = agencyFb, fontWeight = contrastWeight)
                Text("${bateriaPercentagem.toInt()}", color = primaryText, fontSize = 120.sp, fontFamily = agencyFb, fontWeight = FontWeight.Bold)
                Text("BAT", color = Color.Transparent, fontSize = 24.sp, fontFamily = agencyFb)
            }
        }
    }
}

@Composable
fun BarraTermicaSVG(modifier: Modifier = Modifier, progresso: Float, svgId: Int) {
    Box(modifier = modifier) {
        Image(painter = painterResource(id = svgId), contentDescription = null, modifier = Modifier.fillMaxSize(), alpha = 0.2f, contentScale = ContentScale.Fit)
        Image(painter = painterResource(id = svgId), contentDescription = null, modifier = Modifier.fillMaxSize().drawWithContent { clipRect(top = size.height * (1f - progresso)) { this@drawWithContent.drawContent() } }, contentScale = ContentScale.Fit)
    }
}

@Composable
fun EscalaTexto(texto: String, offsetX: Dp, offsetY: Dp, color: Color, weight: FontWeight) { Text(texto, color = color, fontSize = 32.sp, fontFamily = agencyFb, fontWeight = weight, modifier = Modifier.offset(x = offsetX, y = offsetY)) }

@Composable
fun EscalaTempTexto(texto: String, offsetX: Dp, offsetY: Dp, color: Color, weight: FontWeight) { Text(texto, color = color, fontSize = 20.sp, fontFamily = agencyFb, fontWeight = weight, modifier = Modifier.offset(x = offsetX, y = offsetY)) }

@Composable
private fun InfoBarSection(odometro: Float, autonomia: Float, consumo: Float, primaryText: Color, iconColor: Color, contrastWeight: FontWeight, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(painter = painterResource(id = R.drawable.ic_odometro), contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(String.format(Locale.US, "%05dkm", odometro.toInt()), color = primaryText, fontSize = 22.sp, fontFamily = agencyFb, fontWeight = contrastWeight)
        Spacer(modifier = Modifier.width(16.dp)); Text("|", color = primaryText, fontSize = 22.sp, fontFamily = agencyFb, fontWeight = contrastWeight); Spacer(modifier = Modifier.width(16.dp))
        Icon(painter = painterResource(id = R.drawable.ic_autonomia), contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(String.format(Locale.US, "%dkm", autonomia.toInt()), color = primaryText, fontSize = 22.sp, fontFamily = agencyFb, fontWeight = contrastWeight)
        Spacer(modifier = Modifier.width(16.dp)); Text("|", color = primaryText, fontSize = 22.sp, fontFamily = agencyFb, fontWeight = contrastWeight); Spacer(modifier = Modifier.width(16.dp))
        Icon(painter = painterResource(id = R.drawable.ic_consumo), contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(String.format(Locale.US, "%.1fkwh", consumo), color = primaryText, fontSize = 22.sp, fontFamily = agencyFb, fontWeight = contrastWeight)
    }
}

@Composable
private fun BottomBarSection(modeIdx: Int, primaryText: Color, secondaryText: Color, uiElementColor: Color, iconColor: Color, contrastWeight: FontWeight, onModeChange: (Int) -> Unit, onNavigateToSettings: () -> Unit = {}, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val modes = listOf("Eco", "Standard", "Sport")
    var isPlaying by remember { mutableStateOf(true) }
    var musicProgress by remember { mutableFloatStateOf(0.3f) }

    LaunchedEffect(isPlaying) { while (isPlaying) { delay(1000); musicProgress += 0.005f; if (musicProgress > 1f) musicProgress = 0f } }

    Box(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.align(Alignment.BottomStart).size(430.dp, 80.dp), contentAlignment = Alignment.CenterStart) {
            Box(modifier = Modifier.matchParentSize().graphicsLayer { scaleX = -1f }.paint(painterResource(id = R.drawable.fundo_menu), contentScale = ContentScale.FillBounds))
            Row(modifier = Modifier.fillMaxSize().padding(start = 32.dp, end = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(56.dp).background(Color.DarkGray, CircleShape).clip(CircleShape), contentAlignment = Alignment.Center) {
                    Image(painter = painterResource(id = R.drawable.ic_musica_fundo), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text("In The End", color = primaryText, fontSize = 20.sp, fontFamily = agencyFb, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Linkin Park", color = secondaryText, fontSize = 14.sp, fontFamily = agencyFb, fontWeight = contrastWeight, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { musicProgress }, modifier = Modifier.fillMaxWidth().height(2.dp), color = uiElementColor, trackColor = secondaryText.copy(alpha = 0.3f))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { musicProgress = 0f }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.SkipPrevious, null, tint = iconColor) }
                    IconButton(onClick = { isPlaying = !isPlaying }, modifier = Modifier.size(40.dp)) { Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = iconColor, modifier = Modifier.size(32.dp)) }
                    IconButton(onClick = { musicProgress = 0f }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.SkipNext, null, tint = iconColor) }
                }
            }
        }
        Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onModeChange(if (modeIdx - 1 < 0) modes.size - 1 else modeIdx - 1) }) { Icon(painterResource(id = R.drawable.ic_seta_esquerda), null, tint = iconColor, modifier = Modifier.size(36.dp)) }
            Text(modes[modeIdx], color = primaryText, fontSize = 32.sp, fontFamily = agencyFb, fontWeight = contrastWeight)
            IconButton(onClick = { onModeChange((modeIdx + 1) % modes.size) }) { Icon(painterResource(id = R.drawable.ic_seta_direita), null, tint = iconColor, modifier = Modifier.size(36.dp)) }
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd).size(430.dp, 80.dp).paint(painterResource(id = R.drawable.fundo_menu), contentScale = ContentScale.FillBounds), contentAlignment = Alignment.Center) {
            Row(modifier = Modifier.padding(start = 80.dp, end = 32.dp), horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(R.drawable.ic_bluetooth, R.drawable.ic_settings, R.drawable.ic_phone, R.drawable.ic_nav).forEach { icon ->
                    IconButton(onClick = {
                        when (icon) {
                            R.drawable.ic_settings -> onNavigateToSettings()
                            R.drawable.ic_bluetooth -> try { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) } catch (e: Exception) {}
                        }
                    }) { Icon(painterResource(id = icon), null, tint = iconColor, modifier = Modifier.size(36.dp)) }
                }
            }
        }
    }
}