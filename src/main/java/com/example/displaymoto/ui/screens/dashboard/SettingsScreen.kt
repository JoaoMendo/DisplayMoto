package com.example.displaymoto.ui.screens.dashboard

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.displaymoto.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

val agencyFbFont: FontFamily = FontFamily(Font(R.font.agency_fb))
val AzulClaro = Color(0xFF00BFFF)

@Composable
fun SettingsScreen(onNavigateBack: () -> Unit = {}) {
    val azulFundo = Color(0xFF0D0F26)

    // Estados dos piscas
    var piscaEsquerdo by remember { mutableStateOf(false) }
    var piscaDireito by remember { mutableStateOf(false) }
    var piscaPulso by remember { mutableStateOf(false) }

    // Estados das Luzes (ABS, Motor, etc...)
    var luz1 by remember { mutableStateOf(false) }
    var luz2 by remember { mutableStateOf(false) }
    var luz3 by remember { mutableStateOf(false) }
    var luz4 by remember { mutableStateOf(false) }

    // Estados da Hora e Temperatura
    var currentTime by remember { mutableStateOf("--:--") }
    var currentTemp by remember { mutableStateOf("--ºC") }

    // Animação intermitente dos piscas
    LaunchedEffect(piscaEsquerdo, piscaDireito) {
        if (piscaEsquerdo || piscaDireito) {
            while (true) {
                piscaPulso = true
                delay(400)
                piscaPulso = false
                delay(400)
            }
        } else {
            piscaPulso = false
        }
    }

    // Atualizador da Hora
    LaunchedEffect(Unit) {
        while (true) {
            val formatador = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("Europe/Lisbon") }
            currentTime = formatador.format(Date())
            delay(1000)
        }
    }

    // Atualizador da Temperatura (API)
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

    // Pedido de focus para intercetar o teclado/comando
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(azulFundo)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            piscaEsquerdo = !piscaEsquerdo
                            if (piscaEsquerdo) piscaDireito = false
                            true
                        }
                        Key.DirectionRight -> {
                            piscaDireito = !piscaDireito
                            if (piscaDireito) piscaEsquerdo = false
                            true
                        }
                        // Teclas para ligar/desligar as luzes (Idêntico ao Dashboard)
                        Key.One, Key.NumPad1 -> { luz1 = !luz1; true }
                        Key.Two, Key.NumPad2 -> { luz2 = !luz2; true }
                        Key.Three, Key.NumPad3 -> { luz3 = !luz3; true }
                        Key.Four, Key.NumPad4 -> { luz4 = !luz4; true }

                        Key.Escape, Key.Backspace, Key.B -> {
                            onNavigateBack()
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {

        // 1. BARRA DE TOPO (0.12f de peso, exato igual à Dashboard)
        Box(
            modifier = Modifier
                .weight(0.12f)
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, top = 0.dp, bottom = 0.dp)
        ) {

            // HORA E TEMPERATURA (Esquerda)
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$currentTime PM  |  ☁ $currentTemp",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontFamily = agencyFbFont
                )
            }

            // UI DOS PISCAS (Centro)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(440.dp)
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.border_piscas),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(50.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_seta_dir),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(80.dp)
                            .rotate(180f)
                            .alpha(if (piscaEsquerdo && piscaPulso) 1f else 0f)
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.ic_seta_dir),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(80.dp)
                            .alpha(if (piscaDireito && piscaPulso) 1f else 0f)
                    )
                }
            }

            // ÍCONES DAS LUZES (Canto superior direito)
            Row(modifier = Modifier.align(Alignment.CenterEnd).padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                val luzes = listOf(
                    R.drawable.ic_luz_verde to luz1,
                    R.drawable.ic_luz_cinza to luz2,
                    R.drawable.ic_abs to luz3,
                    R.drawable.ic_motor to luz4
                )
                luzes.forEach { (id, ativa) ->
                    Icon(
                        painter = painterResource(id = id),
                        contentDescription = null,
                        tint = if (ativa) Color.Unspecified else Color.Transparent,
                        modifier = Modifier.size(48.dp).alpha(if (ativa) 1f else 0f)
                    )
                }
            }
        }

        // 2. CONTEÚDO DAS DEFINIÇÕES (0.88f do peso restante)
        SettingsContentSection(
            onVoltar = onNavigateBack,
            modifier = Modifier
                .weight(0.88f)
                .fillMaxWidth()
        )
    }
}

// ==========================================
// COMPONENTES AUXILIARES DAS DEFINIÇÕES
// ==========================================

@Composable
fun SettingsContentSection(onVoltar: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context.findActivity()

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F26), RoundedCornerShape(24.dp))
            .padding(32.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // CABEÇALHO DO MENU
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Título e Ícone
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = "Ícone Definições",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "SETTINGS",
                            color = AzulClaro,
                            fontSize = 36.sp,
                            fontFamily = agencyFbFont
                        )
                    }
                }

                // 2. Regulador de Brilho FÍSICO
                Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.Center) {
                    var nivelBrilho by remember { mutableFloatStateOf(0.5f) }

                    LaunchedEffect(nivelBrilho) {
                        activity?.window?.let { window ->
                            val layoutParams = window.attributes
                            layoutParams.screenBrightness = nivelBrilho
                            window.attributes = layoutParams
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.BrightnessHigh,
                            contentDescription = "Brilho",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Slider(
                            value = nivelBrilho,
                            onValueChange = { nivelBrilho = it },
                            modifier = Modifier.width(200.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = AzulClaro,
                                activeTrackColor = AzulClaro,
                                inactiveTrackColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "${(nivelBrilho * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontFamily = agencyFbFont,
                            modifier = Modifier.width(45.dp)
                        )
                    }
                }

                // 3. Botão de Voltar
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        text = "BACK",
                        color = AzulClaro,
                        fontSize = 24.sp,
                        fontFamily = agencyFbFont,
                        modifier = Modifier
                            .clickable { onVoltar() }
                            .padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // LISTA DE DEFINIÇÕES
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                LinhaDivisoria()

                SettingItem(
                    titulo = "BLUETOOTH",
                    subtitulo = "Manage phone and intercom connections"
                ) { }

                LinhaDivisoria()

                SettingItem(
                    titulo = "CONECT MYFULGORA",
                    subtitulo = "Sync your motorcycle with the official app"
                ) { }

                LinhaDivisoria()

                SettingItem(
                    titulo = "COLOUR",
                    subtitulo = "Customize the dashboard theme color"
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CirculoCor(Color.Magenta)
                        CirculoCor(Color.Red)
                        CirculoCor(Color(0xFFFFA500))
                        CirculoCor(Color.Yellow)
                    }
                }

                LinhaDivisoria()

                SettingItem(
                    titulo = "LANGUAGE",
                    subtitulo = "Select the system display language"
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ENGLISH", color = Color.White, fontSize = 24.sp, fontFamily = agencyFbFont)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("▾", color = AzulClaro, fontSize = 24.sp)
                    }
                }

                LinhaDivisoria()

                SettingItem(
                    titulo = "PERSONALIZATION",
                    subtitulo = "Adjust display, units and layout preferences"
                ) { }

                LinhaDivisoria()

                SettingItem(
                    titulo = "ABOUT THE MOTORCYCLE",
                    subtitulo = "System information, software updates and details"
                ) { }

                LinhaDivisoria()
            }
        }
    }
}

@Composable
fun LinhaDivisoria() {
    HorizontalDivider(color = AzulClaro, thickness = 2.dp)
}

@Composable
fun SettingItem(titulo: String, subtitulo: String, conteudoDireita: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Ação de clique */ }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titulo, color = Color.White, fontSize = 28.sp, fontFamily = agencyFbFont)
            Text(text = subtitulo, color = Color.Gray, fontSize = 18.sp, fontFamily = agencyFbFont)
        }
        conteudoDireita()
    }
}

@Composable
fun CirculoCor(cor: Color) {
    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(cor))
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}