package com.example.displaymoto.ui.screens.dashboard

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun MotoButton(
    text: String,
    onAction: () -> Unit,
    accessibilityLabel: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onAction,
        modifier = modifier
            .heightIn(min = 48.dp) // WCAG: Altura mínima
            .widthIn(min = 48.dp)  // WCAG: Largura mínima
            .semantics {
                contentDescription = accessibilityLabel
            }
    ) {
        Text(text = text)
    }
}