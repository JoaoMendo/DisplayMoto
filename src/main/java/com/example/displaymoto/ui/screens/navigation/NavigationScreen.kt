package com.example.displaymoto.ui.screens.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
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
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import android.graphics.ColorMatrixColorFilter
import java.text.SimpleDateFormat
import java.util.*
import com.example.displaymoto.IndicadoresState

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
    indicadores: IndicadoresState
) {
    val context = LocalContext.current
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
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    var currentLocation by remember { mutableStateOf(GeoPoint(38.7223, -9.1393)) } // Lisboa base
    
    DisposableEffect(hasLocationPermission) {
        var locationManager: LocationManager? = null
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                currentLocation = GeoPoint(location.latitude, location.longitude)
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        
        if (hasLocationPermission) {
            try {
                locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
                val lastKnown = locationManager.getLastKnownLocation(provider)
                if (lastKnown != null) {
                    currentLocation = GeoPoint(lastKnown.latitude, lastKnown.longitude)
                }
                locationManager.requestLocationUpdates(provider, 1000L, 1f, locationListener)
            } catch (e: SecurityException) {
                // Ignore
            }
        }
        
        onDispose {
            locationManager?.removeUpdates(locationListener)
        }
    }

    val mapState = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(17.5)
            
            // Filtro escuro/high-tech
            val invertFilter = ColorMatrixColorFilter(
                floatArrayOf(
                    -0.85f,  0f,     0f,     0f,   255f, 
                     0f,    -0.85f,  0f,     0f,   255f, 
                     0f,     0f,    -0.85f,  0f,   255f, 
                     0f,     0f,     0f,     1f,   0f    
                )
            )
            overlayManager.tilesOverlay.setColorFilter(invertFilter)
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
        }
    }
    
    LaunchedEffect(currentLocation) {
        mapState.controller.animateTo(currentLocation)
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

    fun mostrarPopupSettings(msg: String, cor: Color, grave: Boolean) {
        popupMensagem = msg; popupCor = cor; popupGrave = grave; popupVisivel = true
        if (grave) { try { val tg = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100); tg.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500); android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tg.release() }, 600) } catch (_: Exception) {} }
    }

    LaunchedEffect(popupVisivel) { if (popupVisivel) { kotlinx.coroutines.delay(3000); popupVisivel = false } }

    // Watchers
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz1 }.drop(1).collect { if (it) mostrarPopupSettings(s.indAbs, Color(0xFFFFD600), false) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz2 }.drop(1).collect { if (it) mostrarPopupSettings(s.indMaximos, Color(0xFF42A5F5), false) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz3 }.drop(1).collect { if (it) mostrarPopupSettings(s.indMedios, Color(0xFF00E676), false) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz4 }.drop(1).collect { if (it) mostrarPopupSettings(s.indMil, Color(0xFFFFD600), false) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz5 }.drop(1).collect { if (it) mostrarPopupSettings(s.indBrake, Color(0xFFE53935), true) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz6 }.drop(1).collect { if (it) mostrarPopupSettings(s.indBattery, Color(0xFFE53935), true) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz7 }.drop(1).collect { if (it) mostrarPopupSettings(s.indTempBat, Color(0xFFE53935), true) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz8 }.drop(1).collect { if (it) mostrarPopupSettings(s.indMinimos, Color(0xFF00E676), false) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz9 }.drop(1).collect { if (it) mostrarPopupSettings(s.indEsp, Color(0xFFFFD600), false) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz10 }.drop(1).collect { if (it) mostrarPopupSettings(s.indNeblina, Color(0xFFFFD600), false) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz11 }.drop(1).collect { if (it) mostrarPopupSettings(s.indPneu, Color(0xFFFFD600), false) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz12 }.drop(1).collect { if (it) mostrarPopupSettings(s.indTempMotor, Color(0xFFE53935), true) } }
    LaunchedEffect(Unit) { snapshotFlow { indicadores.luz13 }.drop(1).collect { if (it) mostrarPopupSettings(s.indV2x, Color(0xFF42A5F5), false) } }

    LaunchedEffect(Unit) {
        while (true) { 
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("Europe/Lisbon") }.format(Date())
            kotlinx.coroutines.delay(1000) 
        }
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { try { currentTemp = "${org.json.JSONObject(java.net.URL("https://api.open-meteo.com/v1/forecast?latitude=41.3006&longitude=-7.7441&current_weather=true").readText()).getJSONObject("current_weather").getInt("temperature")}ºC" } catch (_: Exception) {} }
    }

    var piscaPulso by remember { mutableStateOf(false) }
    LaunchedEffect(indicadores.piscaEsquerdo, indicadores.piscaDireito) {
        if (indicadores.piscaEsquerdo || indicadores.piscaDireito) {
            while (true) { piscaPulso = true; kotlinx.coroutines.delay(400); piscaPulso = false; kotlinx.coroutines.delay(400) }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

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

            // Main Content (Map)
            Box(modifier = Modifier.weight(0.73f).fillMaxWidth().padding(horizontal = 32.dp)) {
                Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)).background(Color.DarkGray)) {
                    AndroidView(
                        factory = { mapState },
                        modifier = Modifier.fillMaxSize()
                    )
                    // Navigation Icon
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav), 
                        contentDescription = "Position",
                        tint = corDestaque,
                        modifier = Modifier.align(Alignment.Center).size(64.dp)
                    )
                    
                    // Return button inside map area or as a floating button
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
                modifier = Modifier.weight(0.15f)
            )
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
