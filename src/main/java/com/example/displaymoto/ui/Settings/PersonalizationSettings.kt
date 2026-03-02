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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PersonalizationSettings(
    velocidadeAtual: Int = 0,
    bateriaAtual: Int = 85,
    aCarregarAtual: Boolean = false,
    tempBateriaAtual: Int = 30,
    tempMotorAtual: Int = 80,
    marchaAtual: String = "P",
    corFundoAtual: Color,
    onNavigateBack: () -> Unit
) {
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
        if (piscaEsquerdo || piscaDireito) {
            while (true) {
                piscaPulso = true
                delay(400)
                piscaPulso = false
                delay(400)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val formatador = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("Europe/Lisbon")
            }
            currentTime = formatador.format(Date())
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=41.3006&longitude=-7.7441&current_weather=true"
                val resposta = URL(url).readText()
                val json = JSONObject(resposta)
                currentTemp = "${json.getJSONObject("current_weather").getInt("temperature")}ºC"
            } catch (_: Exception) {}
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(modifier = Modifier.fillMaxSize().background(corFundoAtual)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
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
                }
        ) {
            TopBarSectionSettings(currentTime, currentTemp, piscaEsquerdo, piscaDireito, piscaPulso, luz1, luz2, luz3, luz4, Modifier.weight(0.12f))

            PersonalizationContentSection(
                onVoltar = onNavigateBack,
                modifier = Modifier.weight(0.73f).fillMaxWidth()
            )

            BottomStatusSection(velocidadeAtual, bateriaAtual, tempBateriaAtual, tempMotorAtual, marchaAtual, aCarregarAtual, Modifier.weight(0.15f))
        }
    }
}

@Composable
fun PersonalizationContentSection(
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var isIaActivated by remember { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize().padding(32.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "PERSONALIZATION",
                    color = AzulClaro,
                    fontSize = 36.sp,
                    fontFamily = agencyFbFont,
                    modifier = Modifier.align(Alignment.Center)
                )

                Text(
                    text = "BACK",
                    color = AzulClaro,
                    fontSize = 24.sp,
                    fontFamily = agencyFbFont,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable { onVoltar() }
                        .padding(8.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {

                LinhaDivisoria()
                SettingItem(
                    titulo = "ACTIVATE ADAPTATION IA",
                    subtitulo = "Enable AI-driven interface adjustments",
                    onClick = { isIaActivated = !isIaActivated }
                ) {
                    Checkbox(
                        checked = isIaActivated,
                        onCheckedChange = { isIaActivated = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AzulClaro,
                            uncheckedColor = Color.Gray,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.scale(1.3f)
                    )
                }

                LinhaDivisoria()
                SettingItem("VISUAL PREFERENCES", "Adjust contrast, text size and layout colors")

                LinhaDivisoria()
                SettingItem("TOUCH", "Configure screen sensitivity and gestures")

                LinhaDivisoria()
                SettingItem("COGNITIVE ASSISTANT", "Simplify interface and provide guidance")

                LinhaDivisoria()
                SettingItem("AUDIO AND HAPTICS", "Manage sound feedback and vibrations")

                LinhaDivisoria()
                SettingItem("EDIT ICONS", "Customize dashboard layout and icon positions")
                LinhaDivisoria()
            }
        }
    }
}