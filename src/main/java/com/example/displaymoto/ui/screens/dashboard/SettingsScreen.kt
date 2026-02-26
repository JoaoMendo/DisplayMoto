package com.example.displaymoto.ui.screens.dashboard

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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
fun SettingsScreen(
    velocidadeAtual: Int = 0,
    bateriaAtual: Int = 85,
    tempBateriaAtual: Int = 30,
    tempMotorAtual: Int = 80,
    marchaAtual: String = "N",
    corFundoAtual: Color = Color(0xFF0D0F26),
    onCorFundoChange: (Color) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    var showColorLibrary by remember { mutableStateOf(false) }
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

            SettingsContentSection(
                onVoltar = onNavigateBack,
                onCorSelecionada = onCorFundoChange,
                onOpenLibrary = { showColorLibrary = true },
                modifier = Modifier.weight(0.73f).fillMaxWidth()
            )

            BottomStatusSection(velocidadeAtual, bateriaAtual, tempBateriaAtual, tempMotorAtual, marchaAtual, Modifier.weight(0.15f))
        }

        if (showColorLibrary) {
            ColorLibraryDialog(
                initialColor = corFundoAtual,
                onDismiss = { showColorLibrary = false },
                onColorSelected = {
                    onCorFundoChange(it)
                    showColorLibrary = false
                }
            )
        }
    }
}

@Composable
fun TopBarSectionSettings(
    currentTime: String, currentTemp: String,
    pEsq: Boolean, pDir: Boolean, pPulso: Boolean,
    l1: Boolean, l2: Boolean, l3: Boolean, l4: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
        Row(modifier = Modifier.align(Alignment.TopStart).padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$currentTime PM  |  ☁ $currentTemp", color = Color.White, fontSize = 32.sp, fontFamily = agencyFbFont)
        }
        Box(modifier = Modifier.align(Alignment.TopCenter).width(440.dp).height(100.dp), contentAlignment = Alignment.Center) {
            Image(painter = painterResource(id = R.drawable.border_piscas), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Row(horizontalArrangement = Arrangement.spacedBy(50.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(painter = painterResource(id = R.drawable.ic_seta_dir), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(80.dp).rotate(180f).alpha(if (pEsq && pPulso) 1f else 0f))
                Icon(painter = painterResource(id = R.drawable.ic_seta_dir), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(80.dp).alpha(if (pDir && pPulso) 1f else 0f))
            }
        }
        Row(modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            val icons = listOf(R.drawable.ic_luz_verde to l1, R.drawable.ic_luz_cinza to l2, R.drawable.ic_abs to l3, R.drawable.ic_motor to l4)
            icons.forEach { (id, active) ->
                Icon(painter = painterResource(id = id), contentDescription = null, tint = if (active) Color.Unspecified else Color.Transparent, modifier = Modifier.size(48.dp).alpha(if (active) 1f else 0f))
            }
        }
    }
}

@Composable
fun SettingsContentSection(
    onVoltar: () -> Unit,
    onCorSelecionada: (Color) -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize().padding(32.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(id = R.drawable.ic_settings), null, tint = Color.White, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("SETTINGS", color = AzulClaro, fontSize = 36.sp, fontFamily = agencyFbFont)
                    }
                }
                Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.Center) {
                    var nivelBrilho by remember { mutableFloatStateOf(0.5f) }
                    LaunchedEffect(nivelBrilho) { activity?.window?.let { it.attributes = it.attributes.apply { screenBrightness = nivelBrilho } } }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.BrightnessHigh, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(16.dp))
                        Slider(value = nivelBrilho, onValueChange = { nivelBrilho = it }, modifier = Modifier.width(200.dp), colors = SliderDefaults.colors(thumbColor = AzulClaro, activeTrackColor = AzulClaro, inactiveTrackColor = Color.White))
                        Spacer(Modifier.width(16.dp))
                        Text("${(nivelBrilho * 100).toInt()}%", color = Color.White, fontSize = 20.sp, fontFamily = agencyFbFont, modifier = Modifier.width(45.dp))
                    }
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    Text("BACK", color = AzulClaro, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.clickable { onVoltar() }.padding(8.dp))
                }
            }

            Spacer(Modifier.height(32.dp))

            Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                LinhaDivisoria()
                SettingItem("BLUETOOTH", "Manage phone and intercom connections") {}
                LinhaDivisoria()
                SettingItem("CONECT MYFULGORA", "Sync your motorcycle with the official app") {}
                LinhaDivisoria()
                SettingItem("COLOUR", "Tap for full library or pick a shortcut", onClick = onOpenLibrary) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CirculoCor(Color(0xFF0D0F26)) { onCorSelecionada(Color(0xFF0D0F26)) }
                        CirculoCor(Color.Red) { onCorSelecionada(Color.Red) }
                        CirculoCor(Color.Green) { onCorSelecionada(Color.Green) }
                        Text("MORE+", color = AzulClaro, fontSize = 18.sp, modifier = Modifier.clickable { onOpenLibrary() })
                    }
                }
                LinhaDivisoria()
                SettingItem("LANGUAGE", "Select the system display language") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ENGLISH", color = Color.White, fontSize = 24.sp, fontFamily = agencyFbFont)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("▾", color = AzulClaro, fontSize = 24.sp)
                    }
                }
                LinhaDivisoria()
                SettingItem("PERSONALIZATION", "Adjust display, units and layout preferences") {}
                LinhaDivisoria()
                SettingItem("ABOUT THE MOTORCYCLE", "System information, software updates and details") {}
                LinhaDivisoria()
            }
        }
    }
}

@Composable
fun BottomStatusSection(v: Int, b: Int, tB: Int, tM: Int, m: String, modifier: Modifier) {
    Row(modifier = modifier.fillMaxWidth().background(Color(0xFF8B8E94)).padding(horizontal = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$v KM/H", color = Color.White, fontSize = 42.sp, fontFamily = agencyFbFont, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(16.dp))
            Box(modifier = Modifier.width(180.dp).height(20.dp).clip(ParallelogramShape(30f)).background(Color.White.copy(alpha = 0.5f))) {
                Box(modifier = Modifier.fillMaxWidth(v / 120f).fillMaxHeight().background(AzulClaro))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$b%", color = Color.White, fontSize = 42.sp, fontFamily = agencyFbFont, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(16.dp))
            Box(modifier = Modifier.width(180.dp).height(20.dp).clip(ParallelogramShape(30f)).background(Color.White.copy(alpha = 0.5f))) {
                Box(modifier = Modifier.fillMaxWidth(b / 100f).fillMaxHeight().background(Color(0xFF00FF7F)))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(id = R.drawable.ic_temp_bat_set), null, tint = Color.White, modifier = Modifier.size(42.dp))
                Text("${tB}º", color = Color.White, fontSize = 42.sp, fontFamily = agencyFbFont, modifier = Modifier.padding(start = 8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(id = R.drawable.ic_temp_motor_set), null, tint = Color.White, modifier = Modifier.size(42.dp))
                Text("${tM}º", color = Color.White, fontSize = 42.sp, fontFamily = agencyFbFont, modifier = Modifier.padding(start = 8.dp))
            }
            Text(m, color = Color.White, fontSize = 50.sp, fontFamily = agencyFbFont, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
        }
    }
}

@Composable
fun ColorLibraryDialog(initialColor: Color, onDismiss: () -> Unit, onColorSelected: (Color) -> Unit) {
    var selectedColor by remember { mutableStateOf(initialColor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { onColorSelected(selectedColor) }, colors = ButtonDefaults.buttonColors(containerColor = AzulClaro)) {
                Text("SELECT", fontFamily = agencyFbFont, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.Gray, fontFamily = agencyFbFont) }
        },
        title = { Text("COLOUR LIBRARY", fontFamily = agencyFbFont, fontSize = 28.sp, color = Color.White) },
        containerColor = Color(0xFF1A1C2E),
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).background(selectedColor).border(2.dp, Color.White, RoundedCornerShape(12.dp)))
                Spacer(Modifier.height(24.dp))
                HueBar(onColorChanged = { selectedColor = it })
                Spacer(Modifier.height(16.dp))
                Text("Drag to pick any colour", color = Color.LightGray, fontSize = 16.sp, fontFamily = agencyFbFont)
            }
        }
    )
}

@Composable
fun HueBar(onColorChanged: (Color) -> Unit) {
    val hueColors = remember { listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    Box(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(hueColors)).pointerInput(Unit) {
        detectDragGestures { change, _ ->
            offsetX = change.position.x.coerceIn(0f, size.width.toFloat())
            val hue = (offsetX / size.width) * 360f
            onColorChanged(Color.hsv(hue, 0.8f, 0.4f))
        }
    }) {
        Canvas(modifier = Modifier.fillMaxSize()) { drawCircle(Color.White, radius = 15f, center = Offset(offsetX, size.height / 2)) }
    }
}

@Composable
fun SettingItem(titulo: String, subtitulo: String, onClick: () -> Unit = {}, conteudo: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, color = Color.White, fontSize = 28.sp, fontFamily = agencyFbFont)
            Text(subtitulo, color = Color.Gray, fontSize = 18.sp, fontFamily = agencyFbFont)
        }
        conteudo()
    }
}

@Composable
fun CirculoCor(cor: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(cor).border(2.dp, Color.White, CircleShape).clickable { onClick() })
}

@Composable
fun LinhaDivisoria() { HorizontalDivider(color = AzulClaro, thickness = 2.dp) }

class ParallelogramShape(private val angle: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val offset = (size.height * kotlin.math.tan(Math.toRadians(angle.toDouble()))).toFloat()
            moveTo(offset, 0f); lineTo(size.width, 0f); lineTo(size.width - offset, size.height); lineTo(0f, size.height); close()
        }
        return Outline.Generic(path)
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) { if (context is Activity) return context; context = context.baseContext }
    return null
}