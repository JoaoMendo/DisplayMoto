package com.example.displaymoto.ui.screens.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.displaymoto.AppStrings
import com.example.displaymoto.R
import com.example.displaymoto.ui.screens.dashboard.TopBarSectionSettings
import com.example.displaymoto.ui.screens.dashboard.BottomStatusSection
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import java.text.SimpleDateFormat
import java.util.*
import com.example.displaymoto.IndicadoresState
import com.example.displaymoto.aMover
import kotlinx.coroutines.launch

@Composable
fun NavigationScreen(
    s: AppStrings,
    velocidadeAtual: Int,
    bateriaAtual: Int,
    aCarregarAtual: Boolean,
    tempBateriaAtual: Int,
    tempMotorAtual: Int,
    marchaAtual: String,
    corFundoAtual: Color,
    corPersonalizada: Color,
    currentContrast: String,
    onNavigateBack: () -> Unit,
    aiCorDestaque: Color?,
    aiPrimaryText: Color?,
    aiSecondaryText: Color?,
    indicadores: IndicadoresState,
    rota: RotaState,
    unidadeVelocidade: String = "km/h"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            val pedidos = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pedidos.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            launcher.launch(pedidos.toTypedArray())
        }
    }

    val currentLocation = rememberWazeLocation(hasLocationPermission)

    // Quando há rota ativa, recalcula se nos desviarmos mais de 60m da polyline.
    LaunchedEffect(currentLocation, rota.aNavegar, rota.polylineLngLat) {
        val loc = currentLocation ?: return@LaunchedEffect
        if (!rota.aNavegar || rota.polylineLngLat.isEmpty()) return@LaunchedEffect
        val maisProximo = rota.polylineLngLat.minOf { p ->
            RoutingClient.distancia(loc.lat, loc.lng, p[1], p[0])
        }
        if (maisProximo > 60.0) {
            val destLat = rota.destinoLat ?: return@LaunchedEffect
            val destLng = rota.destinoLng ?: return@LaunchedEffect
            try {
                val nova = RoutingClient.calcularRota(loc.lat, loc.lng, destLat, destLng)
                if (nova != null) {
                    rota.polylineLngLat = nova.polylineLngLat
                    rota.distanciaMetros = nova.distanciaMetros
                    rota.etaSegundos = nova.duracaoSegundos
                }
            } catch (_: Exception) {}
        }
    }

    val isLightBg = corPersonalizada.luminance() > 0.5f
    val uiElementColor = when (currentContrast) {
        "HIGH CONTRAST" -> if (corPersonalizada.luminance() < 0.35f) lerp(corPersonalizada, Color.White, 0.7f) else corPersonalizada
        "NIGHT MODE" -> lerp(corPersonalizada, Color.White, 0.35f)
        else -> if (isLightBg) Color(0xFF004466) else Color.White
    }
    val corDestaque = aiCorDestaque ?: uiElementColor
    val primaryText = aiPrimaryText ?: when (currentContrast) {
        "HIGH CONTRAST" -> if (isLightBg) Color.Black else Color.White
        "NIGHT MODE" -> Color.White
        else -> if (isLightBg) Color.Black else Color.White
    }
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

    LaunchedEffect(popupVisivel) { if (popupVisivel) { kotlinx.coroutines.delay(3000); popupVisivel = false } }

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

    LaunchedEffect(Unit) {
        while (true) { 
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(1000) 
        }
    }
    LaunchedEffect(currentLocation) {
        val loc = currentLocation ?: return@LaunchedEffect
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                currentTemp = "${org.json.JSONObject(java.net.URL("https://api.open-meteo.com/v1/forecast?latitude=${loc.lat}&longitude=${loc.lng}&current_weather=true").readText()).getJSONObject("current_weather").getInt("temperature")}ºC"
            } catch (_: Exception) {}
        }
    }

    var piscaPulso by remember { mutableStateOf(false) }
    LaunchedEffect(indicadores.piscaEsquerdo, indicadores.piscaDireito) {
        if (indicadores.piscaEsquerdo || indicadores.piscaDireito) {
            while (true) { piscaPulso = true; kotlinx.coroutines.delay(400); piscaPulso = false; kotlinx.coroutines.delay(400) }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Estado local do overlay de pesquisa de destino
    var searchVisivel by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResultados by remember { mutableStateOf<List<RoutingClient.Local>>(emptyList()) }

    // Driver-lock: se começar a andar com search aberto, fecha por segurança
    LaunchedEffect(velocidadeAtual) {
        if (aMover(velocidadeAtual) && searchVisivel) {
            searchVisivel = false
            rota.erro = "Pesquisa cancelada — moto em movimento"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(corFundoAtual).focusRequester(focusRequester).focusable().onKeyEvent { event ->
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
            // Top Bar Settings like
            TopBarSectionSettings(
                currentTime = currentTime,
                currentTemp = currentTemp,
                pEsq = indicadores.piscaEsquerdo,
                pDir = indicadores.piscaDireito,
                pPulso = piscaPulso,
                l1 = indicadores.luz1, l2 = indicadores.luz2, l3 = indicadores.luz3, l4 = indicadores.luz4,
                l5 = indicadores.luz5, l6 = indicadores.luz6, l7 = indicadores.luz7, l8 = indicadores.luz8,
                l9 = indicadores.luz9, l10 = indicadores.luz10, l11 = indicadores.luz11, l12 = indicadores.luz12,
                l13 = indicadores.luz13,
                motoLigada = indicadores.motoLigada,
                marchaAtual = marchaAtual,
                aCarregar = aCarregarAtual,
                textColor = primaryText,
                modifier = Modifier.weight(0.12f)
            )

            // Main Content (Map - Waze style)
            Box(modifier = Modifier.weight(0.73f).fillMaxWidth().padding(horizontal = 32.dp)) {
                Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)).background(Color(0xFF0a0a14))) {
                    WazeStyleMap(
                        modifier = Modifier.fillMaxSize(),
                        location = currentLocation,
                        accentColor = corDestaque,
                        follow = true,
                        zoom = 17.0,
                        tilt = 55.0,
                        darkStyle = false,
                        routePolyline = rota.polylineLngLat,
                        routeColor = corDestaque
                    )

                    // Botão de procurar destino (canto sup. esq.) — driver-lock: bloqueado em movimento
                    val emMovimento = aMover(velocidadeAtual)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .clickable(role = Role.Button) {
                                if (emMovimento) {
                                    rota.erro = "Pare a moto para procurar destino"
                                } else {
                                    searchVisivel = true
                                }
                            }
                            .background(
                                if (emMovimento) Color(0xFF1A1A2E).copy(alpha = 0.5f)
                                else Color(0xFF1A1A2E).copy(alpha = 0.85f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = corDestaque)
                            Spacer(Modifier.padding(start = 8.dp))
                            Text(
                                rota.destinoNome?.take(28) ?: "Destino",
                                color = corDestaque,
                                fontSize = 18.sp,
                                fontFamily = FontFamily(Font(R.font.roboto_regular))
                            )
                        }
                    }

                    // Return button
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .clickable(role = Role.Button) { onNavigateBack() }
                            .background(Color(0xFF1A1A2E).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(s.back, color = corDestaque, fontSize = 24.sp, fontFamily = FontFamily(Font(R.font.roboto_regular)))
                    }

                    // Barra de ETA na parte inferior do mapa quando há rota ativa
                    if (rota.aNavegar && rota.polylineLngLat.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .background(Color(0xFF0F1A2E).copy(alpha = 0.92f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.LocationOn, null, tint = corDestaque)
                            Spacer(Modifier.padding(start = 10.dp))
                            Text(formatarEta(rota.etaSegundos), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily(Font(R.font.agency_fb)))
                            Spacer(Modifier.padding(start = 16.dp))
                            Text(formatarDistancia(rota.distanciaMetros, unidadeVelocidade), color = corDestaque, fontSize = 20.sp, fontFamily = FontFamily(Font(R.font.agency_fb)))
                            Spacer(Modifier.padding(start = 16.dp))
                            Box(
                                modifier = Modifier
                                    .clickable(role = Role.Button) {
                                        rota.cancelar()
                                        LocationTrackingService.parar(context)
                                    }
                                    .background(Color(0xFF7A1C1C), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // Indicador de loading
                    if (rota.aCarregar) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color(0xFF1A1A2E).copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                                .padding(20.dp)
                        ) {
                            CircularProgressIndicator(color = corDestaque, strokeWidth = 3.dp)
                        }
                    }

                    // Mensagem de erro
                    rota.erro?.let { msg ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 80.dp)
                                .background(Color(0xFF7A1C1C).copy(alpha = 0.95f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .clickable { rota.erro = null }
                        ) {
                            Text(msg, color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }

            // Bottom Bar Settings like
            BottomStatusSection(
                v = velocidadeAtual,
                b = bateriaAtual,
                tB = tempBateriaAtual,
                tM = tempMotorAtual,
                m = marchaAtual,
                isCharging = aCarregarAtual,
                corDestaque = corDestaque,
                iconColor = corDestaque,
                textColor = primaryText,
                u = unidadeVelocidade,
                modifier = Modifier.weight(0.15f)
            )
        }

        // === Overlay de pesquisa de destino (Nominatim) ===
        if (searchVisivel) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f))
                    .clickable(role = Role.Button) { searchVisivel = false },
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 80.dp, start = 48.dp, end = 48.dp)
                        .background(Color(0xFF0F1A2E), RoundedCornerShape(16.dp))
                        .border(2.dp, corDestaque, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .clickable(enabled = false) {}
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Procurar morada ou local", color = Color.White.copy(alpha = 0.5f)) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 18.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = corDestaque,
                            unfocusedBorderColor = corDestaque.copy(alpha = 0.5f),
                            cursorColor = corDestaque
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            scope.launch {
                                rota.aCarregar = true
                                rota.erro = null
                                try {
                                    searchResultados = RoutingClient.pesquisarMorada(searchQuery)
                                    if (searchResultados.isEmpty()) rota.erro = "Sem resultados"
                                } catch (e: Exception) {
                                    rota.erro = "Erro de rede"
                                } finally {
                                    rota.aCarregar = false
                                }
                            }
                        })
                    )
                    Spacer(Modifier.padding(top = 8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(searchResultados) { local ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Button) {
                                        val origem = currentLocation
                                        if (origem == null) {
                                            rota.erro = "Sem GPS para origem"
                                            return@clickable
                                        }
                                        searchVisivel = false
                                        searchResultados = emptyList()
                                        searchQuery = ""
                                        rota.destinoLat = local.lat
                                        rota.destinoLng = local.lng
                                        rota.destinoNome = local.nome.substringBefore(",")
                                        scope.launch {
                                            rota.aCarregar = true
                                            rota.erro = null
                                            try {
                                                val r = RoutingClient.calcularRota(
                                                    origem.lat, origem.lng, local.lat, local.lng
                                                )
                                                if (r != null) {
                                                    rota.polylineLngLat = r.polylineLngLat
                                                    rota.distanciaMetros = r.distanciaMetros
                                                    rota.etaSegundos = r.duracaoSegundos
                                                    rota.aNavegar = true
                                                    LocationTrackingService.iniciar(context)
                                                } else {
                                                    rota.erro = "Sem rota disponível"
                                                }
                                            } catch (_: Exception) {
                                                rota.erro = "Erro de rede"
                                            } finally {
                                                rota.aCarregar = false
                                            }
                                        }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp)
                            ) {
                                Text(local.nome, color = Color.White, fontSize = 16.sp, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }

        // === Popup de indicador ===
        if (popupVisivel) {
            if (popupGrave) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                    Column(modifier = Modifier.background(Color(0xFF1A0A0A), shape = RoundedCornerShape(20.dp)).border(4.dp, popupCor, shape = RoundedCornerShape(20.dp)).padding(horizontal = 64.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("⚠", fontSize = 56.sp)
                        Text(text = s.warning, color = popupCor, fontSize = 48.sp, fontFamily = FontFamily(Font(R.font.agency_fb)), fontWeight = FontWeight.Bold)
                        Text(text = popupMensagem, color = Color.White, fontSize = 36.sp, fontFamily = FontFamily(Font(R.font.agency_fb)), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Row(modifier = Modifier.padding(top = 16.dp).background(Color(0xFF1A1A2E), shape = RoundedCornerShape(16.dp)).border(2.dp, popupCor, shape = RoundedCornerShape(16.dp)).padding(horizontal = 32.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = popupMensagem, color = popupCor, fontSize = 28.sp, fontFamily = FontFamily(Font(R.font.agency_fb)), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatarEta(segundos: Double): String {
    val total = segundos.toInt()
    val h = total / 3600
    val m = (total % 3600) / 60
    return if (h > 0) "${h}h ${m}min" else "${m} min"
}

private fun formatarDistancia(metros: Double, u: String): String {
    if (u == "mph") {
        val miles = metros / 1609.34
        return if (miles >= 0.1) String.format(Locale.US, "%.1f mi", miles)
        else "${(metros * 1.09361).toInt()} yd"
    } else {
        return if (metros >= 1000) String.format(Locale.US, "%.1f km", metros / 1000.0)
        else "${metros.toInt()} m"
    }
}

