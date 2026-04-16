package com.meshai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// MeshAI brand colors — cyberpunk teal on dark
private val MeshTeal = Color(0xFF00E5CC)
private val MeshTealDark = Color(0xFF00B5A0)
private val MeshBackground = Color(0xFF0A0E1A)
private val MeshSurface = Color(0xFF141928)

private val DarkColorScheme = darkColorScheme(
    primary = MeshTeal,
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF004F47),
    onPrimaryContainer = Color(0xFF72F8E9),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color(0xFF00201D),
    background = MeshBackground,
    onBackground = Color(0xFFE0F7FA),
    surface = MeshSurface,
    onSurface = Color(0xFFE0F7FA),
    surfaceVariant = Color(0xFF1E2535),
    onSurfaceVariant = Color(0xFF9EAAB5),
    outline = Color(0xFF4A5568),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF1A0000)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006B60),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF72F8E9),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = Color(0xFF4A6360),
    onSecondary = Color.White,
    background = Color(0xFFF5FBFA),
    onBackground = Color(0xFF161D1C),
    surface = Color.White,
    onSurface = Color(0xFF161D1C)
)

@Composable
fun MeshAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MeshAITypography,
        content = content
    )
}
