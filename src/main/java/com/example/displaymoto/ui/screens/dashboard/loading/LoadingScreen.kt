package com.example.displaymoto.ui.screens.dashboard.loading

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.displaymoto.R
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(onFinished: () -> Unit) {
    // A cor de fundo oficial A-MOVeR
    val azulAmover = Color(0xFF0D0F26)

    // Lógica para mudar de ecrã após 3 segundos
    LaunchedEffect(Unit) {
        delay(3000)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(azulAmover),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Imagem do Logótipo
            // Imagem do Logótipo - FORÇADO A SER GRANDE
            Image(
                painter = painterResource(id = R.drawable.logo_amover),
                contentDescription = "Logótipo A-MOVeR",
                modifier = Modifier
                    .fillMaxWidth(0.3f), // Ocupa exatamente 60% da largura total do tablet!
                contentScale = ContentScale.FillWidth // Força a imagem a esticar para preencher esses 60%
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Indicador de progresso circular
            CircularProgressIndicator(
                color = Color(0xFF00B2FF),
                strokeWidth = 5.dp,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}