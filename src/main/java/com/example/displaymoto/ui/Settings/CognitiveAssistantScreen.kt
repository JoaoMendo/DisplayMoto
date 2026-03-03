package com.example.displaymoto.ui.screens.dashboard.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun CognitiveAssistantScreen(
    velocidadeAtual: Int, bateriaAtual: Int, aCarregarAtual: Boolean, tempBateriaAtual: Int, tempMotorAtual: Int, marchaAtual: String,
    corFundoAtual: Color, corPersonalizada: Color, currentContrast: String, onNavigateBack: () -> Unit
) {
    BaseSettingsScreen(
        velocidadeAtual = velocidadeAtual, bateriaAtual = bateriaAtual, aCarregarAtual = aCarregarAtual, tempBateriaAtual = tempBateriaAtual, tempMotorAtual = tempMotorAtual, marchaAtual = marchaAtual,
        corFundoAtual = corFundoAtual, corPersonalizada = corPersonalizada, currentContrast = currentContrast, onNavigateBack = onNavigateBack,
        title = "COGNITIVE ASSISTANT"
    )
}