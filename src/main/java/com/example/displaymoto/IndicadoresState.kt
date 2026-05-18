package com.example.displaymoto

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Limiar de velocidade acima do qual consideramos que a moto está em movimento.
 * Usar 5 km/h em vez de 0 evita falsos positivos por ruído de GPS / sensores
 * (uma moto parada num semáforo pode oscilar 1-3 km/h).
 */
const val LIMIAR_EM_MOVIMENTO_KMH = 5

/** Retorna true se a velocidade indica que a moto está em movimento — bloqueia UI complexa. */
fun aMover(velocidadeKmh: Int): Boolean = velocidadeKmh > LIMIAR_EM_MOVIMENTO_KMH

class IndicadoresState {
    var luz1 by mutableStateOf(false)
    var luz2 by mutableStateOf(false)
    var luz3 by mutableStateOf(false)
    var luz4 by mutableStateOf(false)
    var luz5 by mutableStateOf(false)
    var luz6 by mutableStateOf(false)
    var luz7 by mutableStateOf(false)
    var luz8 by mutableStateOf(false)
    var luz9 by mutableStateOf(false)
    var luz10 by mutableStateOf(false)
    var luz11 by mutableStateOf(false)
    var luz12 by mutableStateOf(false)
    var luz13 by mutableStateOf(false)
    var piscaEsquerdo by mutableStateOf(false)
    var piscaDireito by mutableStateOf(false)
    var motoLigada by mutableStateOf(false)
}
