package com.example.displaymoto.ui.screens.dashboard

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.displaymoto.R

val agencyFbFont: FontFamily = FontFamily(Font(R.font.agency_fb))
val AzulClaro = Color(0xFF00BFFF)

@Composable
fun SettingsContentSection(onVoltar: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context.findActivity()

    // Estado que guarda a posição do Scroll
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F26), RoundedCornerShape(24.dp))
            .padding(32.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ==========================================
            // CABEÇALHO DO MENU (TÍTULO + BRILHO + VOLTAR)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Título e Ícone
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = "Ícone Definições",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "SETTINGS",
                            color = AzulClaro,
                            fontSize = 36.sp,
                            fontFamily = agencyFbFont
                        )
                    }
                }

                // 2. Regulador de Brilho FÍSICO (Material 3)
                Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.Center) {
                    var nivelBrilho by remember { mutableFloatStateOf(0.5f) }

                    LaunchedEffect(nivelBrilho) {
                        activity?.window?.let { window ->
                            val layoutParams = window.attributes
                            layoutParams.screenBrightness = nivelBrilho
                            window.attributes = layoutParams
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.BrightnessHigh,
                            contentDescription = "Brilho",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Slider
                        Slider(
                            value = nivelBrilho,
                            onValueChange = { nivelBrilho = it },
                            modifier = Modifier.width(200.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = AzulClaro,
                                activeTrackColor = AzulClaro,
                                inactiveTrackColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "${(nivelBrilho * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontFamily = agencyFbFont,
                            modifier = Modifier.width(45.dp)
                        )
                    }
                }

                // 3. Botão de Voltar
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        text = "BACK",
                        color = AzulClaro,
                        fontSize = 24.sp,
                        fontFamily = agencyFbFont,
                        modifier = Modifier
                            .clickable { onVoltar() }
                            .padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ==========================================
            // LISTA DE DEFINIÇÕES (COM SCROLL)
            // ==========================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {

                LinhaDivisoria()

                SettingItem(
                    titulo = "BLUETOOTH",
                    subtitulo = "Manage phone and intercom connections"
                ) { }

                LinhaDivisoria()

                SettingItem(
                    titulo = "CONECT MYFULGORA",
                    subtitulo = "Sync your motorcycle with the official app"
                ) { }

                LinhaDivisoria()

                SettingItem(
                    titulo = "COLOUR",
                    subtitulo = "Customize the dashboard theme color"
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CirculoCor(Color.Magenta)
                        CirculoCor(Color.Red)
                        CirculoCor(Color(0xFFFFA500))
                        CirculoCor(Color.Yellow)
                    }
                }

                LinhaDivisoria()

                SettingItem(
                    titulo = "LANGUAGE",
                    subtitulo = "Select the system display language"
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ENGLISH", color = Color.White, fontSize = 24.sp, fontFamily = agencyFbFont)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("▾", color = AzulClaro, fontSize = 24.sp)
                    }
                }

                LinhaDivisoria()

                SettingItem(
                    titulo = "PERSONALIZATION",
                    subtitulo = "Adjust display, units and layout preferences"
                ) { }

                LinhaDivisoria()

                SettingItem(
                    titulo = "ABOUT THE MOTORCYCLE",
                    subtitulo = "System information, software updates and details"
                ) { }

                LinhaDivisoria()
            }
        }
    }
}

// ==========================================
// COMPONENTES AUXILIARES
// ==========================================

@Composable
fun LinhaDivisoria() {
    HorizontalDivider(
        color = AzulClaro,
        thickness = 2.dp
    )
}

@Composable
fun SettingItem(titulo: String, subtitulo: String, conteudoDireita: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Ação de clique */ }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                color = Color.White,
                fontSize = 28.sp,
                fontFamily = agencyFbFont
            )
            Text(
                text = subtitulo,
                color = Color.Gray,
                fontSize = 18.sp,
                fontFamily = agencyFbFont
            )
        }

        conteudoDireita()
    }
}

@Composable
fun CirculoCor(cor: Color) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(cor)
    )
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
