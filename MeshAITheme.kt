package com.meshai.feature.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MeshColorScheme = darkColorScheme(
    primary         = Color(0xFF3DDC84),
    secondary       = Color(0xFF00D4FF),
    background      = Color(0xFF06101E),
    surface         = Color(0xFF0B1D35),
    onPrimary       = Color.Black,
    onSecondary     = Color.Black,
    onBackground    = Color.White,
    onSurface       = Color.White
)

@Composable
fun MeshAITheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = MeshColorScheme,
    content     = content
)
