package com.meshai.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

@Composable
fun TaskQueueScreen() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text("TASK QUEUE", fontFamily = FontFamily.Monospace, color = Color(0xFF00D4FF), fontSize = 16.sp, letterSpacing = 4.sp)
}

@Composable
fun SettingsScreen() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text("SETTINGS", fontFamily = FontFamily.Monospace, color = Color(0xFF3DDC84), fontSize = 16.sp, letterSpacing = 4.sp)
}
