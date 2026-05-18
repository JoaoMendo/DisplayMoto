package com.example.displaymoto.ui.screens.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Estado global da navegação por rota. Segue o mesmo padrão de IndicadoresState:
 * holder mutável com `mutableStateOf`, instanciado uma vez em MainActivity e
 * partilhado pelas screens que precisam de ler/escrever rota ativa.
 */
class RotaState {
    /** Destino selecionado pelo utilizador (lat/lng). Null = sem destino. */
    var destinoLat by mutableStateOf<Double?>(null)
    var destinoLng by mutableStateOf<Double?>(null)
    var destinoNome by mutableStateOf<String?>(null)

    /** Polyline da rota: lista de [lng, lat] (formato GeoJSON). */
    var polylineLngLat by mutableStateOf<List<DoubleArray>>(emptyList())

    /** Distância total da rota em metros. */
    var distanciaMetros by mutableStateOf(0.0)

    /** Tempo estimado em segundos. */
    var etaSegundos by mutableStateOf(0.0)

    /** True enquanto há uma rota ativa (utilizador está a navegar). */
    var aNavegar by mutableStateOf(false)

    /** True enquanto o cliente HTTP está a calcular rota / a pesquisar morada. */
    var aCarregar by mutableStateOf(false)

    /** Mensagem de erro a apresentar ao utilizador (null = sem erro). */
    var erro by mutableStateOf<String?>(null)

    /** Limpa o destino e a rota — usado ao cancelar navegação. */
    fun cancelar() {
        destinoLat = null
        destinoLng = null
        destinoNome = null
        polylineLngLat = emptyList()
        distanciaMetros = 0.0
        etaSegundos = 0.0
        aNavegar = false
        aCarregar = false
        erro = null
    }
}
