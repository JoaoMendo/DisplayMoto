package com.example.displaymoto.ui.screens.navigation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Cliente HTTP para Nominatim (geocoding) + OSRM (routing). Sem dependências externas
 * (segue o padrão da app: java.net.URL + JSONObject).
 *
 * NOTA: O servidor demo do OSRM (router.project-osrm.org) e o Nominatim público
 * são gratuitos mas têm Terms of Use de uso pessoal / dev. Para uso comercial
 * intensivo deves hospedar o teu próprio OSRM/Nominatim ou usar GraphHopper/ORS.
 */
object RoutingClient {

    private const val USER_AGENT = "DisplayMoto/1.0 (Android Kotlin app)"
    private const val NOMINATIM = "https://nominatim.openstreetmap.org"
    private const val OSRM = "https://router.project-osrm.org"

    data class Local(val nome: String, val lat: Double, val lng: Double)

    data class Rota(
        val polylineLngLat: List<DoubleArray>,
        val distanciaMetros: Double,
        val duracaoSegundos: Double
    )

    /**
     * Pesquisa morada/local via Nominatim. Devolve até 5 resultados.
     * Throttle: Nominatim limita a 1 req/segundo — caller deve respeitar.
     */
    suspend fun pesquisarMorada(query: String): List<Local> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val q = URLEncoder.encode(query, "UTF-8")
        val url = URL("$NOMINATIM/search?q=$q&format=json&limit=5&addressdetails=0")
        val body = url.openConnectionWithUA().getInputStream().bufferedReader().use { it.readText() }
        val arr = org.json.JSONArray(body)
        val out = mutableListOf<Local>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Local(
                    nome = o.optString("display_name"),
                    lat = o.getDouble("lat"),
                    lng = o.getDouble("lon")
                )
            )
        }
        out
    }

    /**
     * Calcula rota entre dois pontos via OSRM. Devolve polyline densa em GeoJSON
     * (lista de [lng, lat]) + distância e duração.
     */
    suspend fun calcularRota(
        origemLat: Double, origemLng: Double,
        destinoLat: Double, destinoLng: Double
    ): Rota? = withContext(Dispatchers.IO) {
        // OSRM espera coordenadas no formato lng,lat (não lat,lng)
        val path = "$origemLng,$origemLat;$destinoLng,$destinoLat"
        val url = URL("$OSRM/route/v1/driving/$path?overview=full&geometries=geojson&steps=false")
        val body = url.openConnectionWithUA().getInputStream().bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        if (json.optString("code") != "Ok") return@withContext null
        val routes = json.getJSONArray("routes")
        if (routes.length() == 0) return@withContext null
        val route = routes.getJSONObject(0)
        val coords = route.getJSONObject("geometry").getJSONArray("coordinates")
        val polyline = ArrayList<DoubleArray>(coords.length())
        for (i in 0 until coords.length()) {
            val c = coords.getJSONArray(i)
            polyline.add(doubleArrayOf(c.getDouble(0), c.getDouble(1)))
        }
        Rota(
            polylineLngLat = polyline,
            distanciaMetros = route.getDouble("distance"),
            duracaoSegundos = route.getDouble("duration")
        )
    }

    /** Helper: distância entre dois pontos GPS (metros, Haversine). */
    fun distancia(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2).let { it * it } +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2).let { it * it }
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    private fun URL.openConnectionWithUA(): HttpURLConnection {
        val c = openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", USER_AGENT)
        c.setRequestProperty("Accept", "application/json")
        c.connectTimeout = 8000
        c.readTimeout = 12000
        return c
    }
}
