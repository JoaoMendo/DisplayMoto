package com.example.displaymoto.ui.screens.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

data class WazeLocation(val lat: Double, val lng: Double, val bearing: Float?)

// Style URLs do OpenFreeMap (gratuito, sem chave, sem limites)
private const val STYLE_LIBERTY     = "https://tiles.openfreemap.org/styles/liberty"
private const val STYLE_DARK_MATTER = "https://tiles.openfreemap.org/styles/dark-matter"
private const val STYLE_POSITRON    = "https://tiles.openfreemap.org/styles/positron"

@SuppressLint("MissingPermission")
@Composable
fun WazeStyleMap(
    modifier: Modifier = Modifier,
    location: WazeLocation?,
    accentColor: Color,
    follow: Boolean = true,
    zoom: Double = 17.0,
    tilt: Double = 55.0,
    darkStyle: Boolean = false,
    showMyLocation: Boolean = true,
    routePolyline: List<DoubleArray> = emptyList(), // lista de [lng, lat]
    routeColor: Color = Color(0xFF00C2FF)
) {
    val context = LocalContext.current
    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // MapLibre.getInstance precisa de ser chamado antes de criar o MapView.
    remember(context) { MapLibre.getInstance(context); true }

    val styleUrl = if (darkStyle) STYLE_DARK_MATTER else STYLE_LIBERTY

    val mapView = remember {
        MapView(context).apply {
            // O método getMapAsync devolve a referência ao MapLibreMap quando estiver pronto.
        }
    }

    // Guardamos a referência ao mapa para podermos atualizar a câmara mais tarde.
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleRef by remember { mutableStateOf<Style?>(null) }

    LaunchedEffect(mapView, styleUrl) {
        mapView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                styleRef = style
                if (showMyLocation && hasLocationPermission) {
                    enableLocationDot(context, map, style, accentColor)
                }
                installRouteLayer(style, routeColor)
            }
            map.uiSettings.apply {
                isCompassEnabled = false
                isAttributionEnabled = false
                isLogoEnabled = false
                isRotateGesturesEnabled = false
                isScrollGesturesEnabled = false
                isTiltGesturesEnabled = false
                isZoomGesturesEnabled = false
                isDoubleTapGesturesEnabled = false
                isQuickZoomGesturesEnabled = false
            }
            val initial = location?.let { LatLng(it.lat, it.lng) } ?: LatLng(38.7223, -9.1393)
            map.cameraPosition = CameraPosition.Builder()
                .target(initial)
                .zoom(zoom)
                .tilt(tilt)
                .bearing((location?.bearing ?: 0f).toDouble())
                .build()
            mapRef = map
        }
    }

    // Atualiza a polyline da rota quando muda
    LaunchedEffect(styleRef, routePolyline) {
        val style = styleRef ?: return@LaunchedEffect
        updateRouteSource(style, routePolyline)
    }

    // Câmara segue a localização atual
    LaunchedEffect(mapRef, location?.lat, location?.lng, location?.bearing, follow, zoom, tilt) {
        val map = mapRef ?: return@LaunchedEffect
        if (follow && location != null) {
            val target = LatLng(location.lat, location.lng)
            val bearing = (location.bearing ?: map.cameraPosition.bearing.toFloat()).toDouble()
            val newPos = CameraPosition.Builder()
                .target(target)
                .zoom(zoom)
                .tilt(tilt)
                .bearing(bearing)
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(newPos), 700)
        }
    }

    // Ciclo de vida — MapView nativo precisa destes callbacks
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        mapView.onCreate(null)
        mapView.onStart()
        mapView.onResume()
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME  -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE   -> mapView.onPause()
                Lifecycle.Event.ON_START   -> mapView.onStart()
                Lifecycle.Event.ON_STOP    -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            try {
                mapView.onPause()
                mapView.onStop()
                mapView.onDestroy()
            } catch (_: Exception) {}
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier.fillMaxSize())
}

private const val ROUTE_SOURCE_ID = "route-src"
private const val ROUTE_LAYER_ID  = "route-line"
private const val ROUTE_CASING_ID = "route-casing"

/** Instala a source vazia e as layers (casing + linha) onde a rota vai ser desenhada. */
private fun installRouteLayer(style: Style, routeColor: Color) {
    if (style.getSource(ROUTE_SOURCE_ID) != null) return
    style.addSource(GeoJsonSource(ROUTE_SOURCE_ID))

    val color = String.format("#%06X", 0xFFFFFF and routeColor.toArgb())

    // Casing (linha mais grossa por baixo para dar destaque)
    val casing = LineLayer(ROUTE_CASING_ID, ROUTE_SOURCE_ID).withProperties(
        PropertyFactory.lineColor("#0A2138"),
        PropertyFactory.lineWidth(11f),
        PropertyFactory.lineOpacity(0.95f),
        PropertyFactory.lineCap(org.maplibre.android.style.layers.Property.LINE_CAP_ROUND),
        PropertyFactory.lineJoin(org.maplibre.android.style.layers.Property.LINE_JOIN_ROUND)
    )
    // Linha principal (cor de destaque)
    val line = LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
        PropertyFactory.lineColor(color),
        PropertyFactory.lineWidth(7f),
        PropertyFactory.lineOpacity(1f),
        PropertyFactory.lineCap(org.maplibre.android.style.layers.Property.LINE_CAP_ROUND),
        PropertyFactory.lineJoin(org.maplibre.android.style.layers.Property.LINE_JOIN_ROUND)
    )
    style.addLayer(casing)
    style.addLayer(line)
}

/** Substitui os dados da source da rota. Lista vazia limpa a polyline. */
private fun updateRouteSource(style: Style, polyline: List<DoubleArray>) {
    val src = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID) ?: return
    if (polyline.isEmpty()) {
        src.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(emptyList())))
        return
    }
    val points = polyline.map { Point.fromLngLat(it[0], it[1]) }
    src.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(points)))
}

/**
 * Ativa o "ponto azul" do MapLibre (LocationComponent) com a cor de acento da app.
 * Usa COMPASS render mode — mostra a seta a rodar com a direção da moto.
 */
@SuppressLint("MissingPermission")
private fun enableLocationDot(
    context: Context,
    map: MapLibreMap,
    style: Style,
    accentColor: Color
) {
    try {
        val argb = accentColor.toArgb()
        val opts = LocationComponentOptions.builder(context)
            .pulseEnabled(true)
            .pulseColor(argb)
            .foregroundTintColor(argb)
            .bearingTintColor(argb)
            .accuracyColor(argb)
            .accuracyAlpha(0.15f)
            .build()
        val activation = LocationComponentActivationOptions.builder(context, style)
            .locationComponentOptions(opts)
            .useDefaultLocationEngine(true)
            .build()
        map.locationComponent.apply {
            activateLocationComponent(activation)
            isLocationComponentEnabled = true
            renderMode = RenderMode.COMPASS
        }
    } catch (_: Exception) {
        // Se faltar alguma permissão ou GPS estiver indisponível, ignoramos.
    }
}

/** Liga ao LocationManager nativo e devolve a WazeLocation atual com bearing quando há velocidade. */
@SuppressLint("MissingPermission")
@Composable
fun rememberWazeLocation(hasPermission: Boolean): WazeLocation? {
    val context = LocalContext.current
    var loc by remember { mutableStateOf<WazeLocation?>(null) }

    DisposableEffect(hasPermission) {
        var lm: LocationManager? = null
        val listener = object : LocationListener {
            override fun onLocationChanged(l: Location) {
                val bearing = if (l.hasBearing() && l.speed > 0.5f) l.bearing else null
                loc = WazeLocation(l.latitude, l.longitude, bearing)
            }
            override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }
        if (hasPermission) {
            try {
                lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
                val provider = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
                    LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
                lm.getLastKnownLocation(provider)?.let {
                    loc = WazeLocation(it.latitude, it.longitude, null)
                }
                lm.requestLocationUpdates(provider, 1000L, 1f, listener)
            } catch (_: SecurityException) {}
        }
        onDispose { lm?.removeUpdates(listener) }
    }
    return loc
}
