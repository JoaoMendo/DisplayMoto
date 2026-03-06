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
    velocidadeAtual: Int = 0, bateriaAtual: Int = 85, aCarregarAtual: Boolean = false, tempBateriaAtual: Int = 30, tempMotorAtual: Int = 80, marchaAtual: String = "N",
    corFundoAtual: Color, corPersonalizada: Color = Color(0xFF0D0F26), currentContrast: String = "STANDARD",
    onCorFundoChange: (Color) -> Unit = {}, onNavigateBack: () -> Unit = {}, onNavigateToPersonalization: () -> Unit = {}
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
            TopBarSectionSettings(currentTime = currentTime, currentTemp = currentTemp, pEsq = piscaEsquerdo, pDir = piscaDireito, pPulso = piscaPulso, l1 = luz1, l2 = luz2, l3 = luz3, l4 = luz4, textColor = primaryText, modifier = Modifier.weight(0.12f))
            SettingsContentSection(onVoltar = onNavigateBack, onCorSelecionada = onCorFundoChange, onOpenLibrary = { showColorLibrary = true }, onNavigateToPersonalization = onNavigateToPersonalization, corDestaque = corDestaque, primaryText = primaryText, secondaryText = secondaryText, modifier = Modifier.weight(0.73f))
            BottomStatusSection(v = velocidadeAtual, b = bateriaAtual, tB = tempBateriaAtual, tM = tempMotorAtual, m = marchaAtual, isCharging = aCarregarAtual, corDestaque = corDestaque, textColor = primaryText, modifier = Modifier.weight(0.15f))
        }

        if (showColorLibrary) {
            ColorLibraryDialog(initialColor = corFundoAtual, onDismiss = { showColorLibrary = false }, onColorSelected = { onCorFundoChange(it); showColorLibrary = false })
        }
    }
}

// ==================================================================================================
// COMPONENTES DA INTERFACE (COM ACESSIBILIDADE WCAG / TALKBACK)
// ==================================================================================================

@Composable
fun TopBarSectionSettings(currentTime: String, currentTemp: String, pEsq: Boolean, pDir: Boolean, pPulso: Boolean, l1: Boolean, l2: Boolean, l3: Boolean, l4: Boolean, textColor: Color = Color.White, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 32.dp)) {

        // HORAS E TEMPERATURA
        // O TalkBack vai ler isto automaticamente como um bloco de texto.
        Row(modifier = Modifier.align(Alignment.TopStart).padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$currentTime PM  |  ☁ $currentTemp", color = textColor, fontSize = 32.sp, fontFamily = agencyFbFont, modifier = Modifier.semantics { contentDescription = "Current time $currentTime PM, Weather $currentTemp" })
        }

        // PISCAS
        Box(modifier = Modifier.align(Alignment.TopCenter).width(440.dp).height(100.dp), contentAlignment = Alignment.Center) {
            // A moldura é decorativa, por isso mantemos null
            Image(painter = painterResource(id = R.drawable.border_piscas), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Row(horizontalArrangement = Arrangement.spacedBy(50.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                // AQUI TEMOS ACESSIBILIDADE: Se o pisca estiver ativo, o TalkBack avisa! Se não estiver, o TalkBack ignora.
                Icon(painter = painterResource(id = R.drawable.ic_seta_dir), contentDescription = if (pEsq && pPulso) "Left turn signal active" else null, tint = Color.Unspecified, modifier = Modifier.size(80.dp).rotate(180f).alpha(if (pEsq && pPulso) 1f else 0f))
                Icon(painter = painterResource(id = R.drawable.ic_seta_dir), contentDescription = if (pDir && pPulso) "Right turn signal active" else null, tint = Color.Unspecified, modifier = Modifier.size(80.dp).alpha(if (pDir && pPulso) 1f else 0f))
            }
        }

        // LUZES DE AVISO (Motor, ABS, etc)
        Row(modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            // Mapeamos cada luz para a sua descrição de acessibilidade
            val iconesAcessiveis = listOf(
                Triple(R.drawable.ic_luz_verde, l1, "Green indicator light"),
                Triple(R.drawable.ic_luz_cinza, l2, "Neutral gear light"),
                Triple(R.drawable.ic_abs, l3, "A B S warning light"),
                Triple(R.drawable.ic_motor, l4, "Engine warning light")
            )

            iconesAcessiveis.forEach { (id, active, descricao) ->
                Icon(
                    painter = painterResource(id = id),
                    // Se estiver acesa, o TalkBack avisa o condutor. Se estiver apagada, fica invisível para o leitor.
                    contentDescription = if (active) "$descricao is on" else null,
                    tint = if (active) Color.Unspecified else Color.Transparent,
                    modifier = Modifier.size(48.dp).alpha(if (active) 1f else 0f)
                )
            }
        }
    }
}

@Composable
fun BottomStatusSection(v: Int, b: Int, tB: Int, tM: Int, m: String, isCharging: Boolean = false, corDestaque: Color, textColor: Color = Color.White, modifier: Modifier = Modifier) {
    val corBateria = when { isCharging -> corDestaque; b <= 20 -> Color.Red; else -> Color(0xFF00FF7F) }

    Row(modifier = modifier.fillMaxWidth().background(Color(0xFF8B8E94)).padding(horizontal = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        // Velocidade (TalkBack vai ler o texto diretamente, a barra não precisa de descrição extra)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) {}) {
            Text("$v KM/H", color = textColor, fontSize = 42.sp, fontFamily = agencyFbFont, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { contentDescription = "Speed $v kilometers per hour" })
            Spacer(Modifier.width(16.dp))
            Box(modifier = Modifier.width(180.dp).height(20.dp).clip(ParallelogramShape(30f)).background(Color.White.copy(alpha = 0.5f))) {
                Box(modifier = Modifier.fillMaxWidth(v / 120f).fillMaxHeight().background(corDestaque))
            }
        }
        // Bateria
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) {}) {
            val statusBateria = if (isCharging) "charging" else ""
            Text("$b%", color = textColor, fontSize = 42.sp, fontFamily = agencyFbFont, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { contentDescription = "Battery at $b percent $statusBateria" })
            Spacer(Modifier.width(16.dp))
            Box(modifier = Modifier.width(180.dp).height(20.dp).clip(ParallelogramShape(30f)).background(Color.White.copy(alpha = 0.5f))) {
                Box(modifier = Modifier.fillMaxWidth(b / 100f).fillMaxHeight().background(corBateria))
            }
        }
        // Temperaturas e Marcha
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // AQUI: Descrevemos o ícone para cegos saberem do que se trata a temperatura
                Icon(painterResource(id = R.drawable.ic_temp_bat_set), contentDescription = "Battery temperature", tint = textColor, modifier = Modifier.size(42.dp))
                Text("${tB}º", color = textColor, fontSize = 42.sp, fontFamily = agencyFbFont, modifier = Modifier.padding(start = 8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // AQUI: Descrevemos o ícone do motor
                Icon(painterResource(id = R.drawable.ic_temp_motor_set), contentDescription = "Engine temperature", tint = textColor, modifier = Modifier.size(42.dp))
                Text("${tM}º", color = textColor, fontSize = 42.sp, fontFamily = agencyFbFont, modifier = Modifier.padding(start = 8.dp))
            }
            Text(m, color = textColor, fontSize = 50.sp, fontFamily = agencyFbFont, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp).semantics { contentDescription = "Current gear $m" })
        }
    }
}

@Composable
fun SettingItem(titulo: String, subtitulo: String, primaryColor: Color = Color.White, secondaryColor: Color = Color.Gray, onClick: () -> Unit = {}, conteudo: @Composable () -> Unit = {}) {
    // Adicionamos Role.Button para o Android saber que isto é clicável e dar o som de feedback correto
    Row(modifier = Modifier.fillMaxWidth().clickable(onClickLabel = "Open $titulo setting", role = Role.Button) { onClick() }.padding(vertical = 12.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(titulo, color = primaryColor, fontSize = 28.sp, fontFamily = agencyFbFont)
            Text(subtitulo, color = secondaryColor, fontSize = 18.sp, fontFamily = agencyFbFont)
        }
        conteudo()
    }
}

@Composable
fun SettingsContentSection(onVoltar: () -> Unit, onCorSelecionada: (Color) -> Unit, onOpenLibrary: () -> Unit, onNavigateToPersonalization: () -> Unit, corDestaque: Color, primaryText: Color, secondaryText: Color, modifier: Modifier = Modifier) {
    val activity = LocalContext.current.findActivity()
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    // Aqui mantemos null porque a palavra "SETTINGS" está logo ao lado. Dizer "Settings Icon, Settings" seria erro.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(id = R.drawable.ic_settings), contentDescription = null, tint = primaryText, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("SETTINGS", color = corDestaque, fontSize = 36.sp, fontFamily = agencyFbFont)
                    }
                }
                Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.Center) {
                    var nivelBrilho by remember { mutableFloatStateOf(0.5f) }
                    LaunchedEffect(nivelBrilho) { activity?.window?.let { it.attributes = it.attributes.apply { screenBrightness = nivelBrilho } } }
                    // A barra de brilho ganha semântica para ser anunciada como controlo de ecrã
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Screen brightness slider" }) {
                        Icon(Icons.Filled.BrightnessHigh, contentDescription = null, tint = primaryText, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(16.dp))
                        Slider(value = nivelBrilho, onValueChange = { nivelBrilho = it }, modifier = Modifier.width(200.dp), colors = SliderDefaults.colors(thumbColor = corDestaque, activeTrackColor = corDestaque, inactiveTrackColor = primaryText.copy(alpha = 0.3f)))
                        Spacer(Modifier.width(16.dp))
                        Text("${(nivelBrilho * 100).toInt()}%", color = primaryText, fontSize = 20.sp, fontFamily = agencyFbFont, modifier = Modifier.width(45.dp))
                    }
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    // Botão BACK agora tem a Role certa
                    Text("BACK", color = corDestaque, fontSize = 24.sp, fontFamily = agencyFbFont, modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Go back") { onVoltar() }.padding(8.dp))
                }
            }

            Spacer(Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = "BLUETOOTH", subtitulo = "Manage phone and intercom connections", primaryColor = primaryText, secondaryColor = secondaryText)
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = "CONECT MYFULGORA", subtitulo = "Sync your motorcycle with the official app", primaryColor = primaryText, secondaryColor = secondaryText)
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = "COLOUR", subtitulo = "Tap for full library or pick a shortcut", primaryColor = primaryText, secondaryColor = secondaryText, onClick = onOpenLibrary, conteudo = {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CirculoCor(Color(0xFF0D0F26), "Default Dark Blue") { onCorSelecionada(Color(0xFF0D0F26)) }
                        CirculoCor(Color.Red, "Red") { onCorSelecionada(Color.Red) }
                        CirculoCor(Color.Green, "Green") { onCorSelecionada(Color.Green) }
                        Text("MORE+", color = corDestaque, fontSize = 18.sp, modifier = Modifier.clickable(role = Role.Button) { onOpenLibrary() })
                    }
                })
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = "LANGUAGE", subtitulo = "Select the system display language", primaryColor = primaryText, secondaryColor = secondaryText, conteudo = {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("ENGLISH", color = primaryText, fontSize = 24.sp, fontFamily = agencyFbFont); Spacer(modifier = Modifier.width(4.dp)); Text("▾", color = corDestaque, fontSize = 24.sp) }
                })
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = "PERSONALIZATION", subtitulo = "Adjust display, units and layout preferences", primaryColor = primaryText, secondaryColor = secondaryText, onClick = onNavigateToPersonalization)
                LinhaDivisoria(corDestaque)
                SettingItem(titulo = "ABOUT THE MOTORCYCLE", subtitulo = "System information, software updates and details", primaryColor = primaryText, secondaryColor = secondaryText)
                LinhaDivisoria(corDestaque)

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ColorLibraryDialog(initialColor: Color, onDismiss: () -> Unit, onColorSelected: (Color) -> Unit) {
    var selectedColor by remember { mutableStateOf(initialColor) }
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(onClick = { onColorSelected(selectedColor) }, colors = ButtonDefaults.buttonColors(containerColor = AzulClaro)) { Text("SELECT", fontFamily = agencyFbFont, color = Color.White) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.Gray, fontFamily = agencyFbFont) } }, title = { Text("COLOUR LIBRARY", fontFamily = agencyFbFont, fontSize = 28.sp, color = Color.White) }, containerColor = Color(0xFF1A1C2E), text = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).background(selectedColor).border(2.dp, Color.White, RoundedCornerShape(12.dp)))
            Spacer(Modifier.height(24.dp)); HueBar(onColorChanged = { selectedColor = it }); Spacer(Modifier.height(16.dp)); Text("Drag to pick any colour", color = Color.LightGray, fontSize = 16.sp, fontFamily = agencyFbFont)
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