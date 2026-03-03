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
import com.example.displaymoto.ui.screens.dashboard.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VisualPreferencesScreen(
    velocidadeAtual: Int = 0, bateriaAtual: Int = 85, aCarregarAtual: Boolean = false, tempBateriaAtual: Int = 30, tempMotorAtual: Int = 80, marchaAtual: String = "P",
    corFundoAtual: Color,
    corPersonalizada: Color,
    currentContrast: String,
    onContrastChange: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val isLightBg = corPersonalizada.luminance() > 0.5f

    val accentColor = when (currentContrast) {
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
            TopBarSectionSettings(currentTime, currentTemp, piscaEsquerdo, piscaDireito, piscaPulso, luz1, luz2, luz3, luz4, textColor = primaryText, modifier = Modifier.weight(0.12f))

            val scrollState = rememberScrollState()

            Box(modifier = Modifier.weight(0.73f).fillMaxWidth().padding(32.dp)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "VISUAL PREFERENCES", color = accentColor, fontSize = 36.sp, fontFamily = agencyFbFont, modifier = Modifier.align(Alignment.Center))
                        Text(text = "BACK", color = accentColor, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.align(Alignment.CenterEnd).clickable { onNavigateBack() }.padding(8.dp))
                    }

                    Spacer(Modifier.height(32.dp))

                    Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                        LinhaDivisoria(accentColor)

                        // CORREÇÃO: Usamos parâmetros explicitamente nomeados para garantir que não há trocas!
                        SettingItem(
                            titulo = "CONTRAST",
                            subtitulo = "Adjust the display contrast ratio",
                            primaryColor = primaryText,
                            secondaryColor = secondaryText,
                            conteudo = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = "STANDARD", color = if (currentContrast == "STANDARD") accentColor else secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.clickable { onContrastChange("STANDARD") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont)
                                    Text(text = "HIGH CONTRAST", color = if (currentContrast == "HIGH CONTRAST") accentColor else secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.clickable { onContrastChange("HIGH CONTRAST") })
                                    Text(text = "|", color = secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont)
                                    Text(text = "NIGHT MODE", color = if (currentContrast == "NIGHT MODE") accentColor else secondaryText, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.clickable { onContrastChange("NIGHT MODE") })
                                }
                            }
                        )

                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = "TEXT SIZE", subtitulo = "Change the system text size", primaryColor = primaryText, secondaryColor = secondaryText)

                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = "COLOR FILTERS", subtitulo = "Apply filters for color blindness or preference", primaryColor = primaryText, secondaryColor = secondaryText)

                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = "TEXT SPACING", subtitulo = "Adjust spacing between letters and lines", primaryColor = primaryText, secondaryColor = secondaryText)

                        LinhaDivisoria(accentColor)
                        SettingItem(titulo = "ANIMATIONS", subtitulo = "Reduce or disable system animations", primaryColor = primaryText, secondaryColor = secondaryText)

                        LinhaDivisoria(accentColor)
                    }
                }
            }

            BottomStatusSection(v = velocidadeAtual, b = bateriaAtual, tB = tempBateriaAtual, tM = tempMotorAtual, m = marchaAtual, isCharging = aCarregarAtual, corDestaque = accentColor, textColor = primaryText, modifier = Modifier.weight(0.15f))
        }
    }
}