package com.meshai.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.meshai.service.AgentForegroundService
import com.meshai.ui.navigation.MeshAINavGraph
import com.meshai.ui.theme.MeshAITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start the agent service on first launch
        ContextCompat.startForegroundService(
            this,
            AgentForegroundService.startIntent(this)
        )

        setContent {
            MeshAITheme {
                MeshAINavGraph()
            }
        }
    }
}
