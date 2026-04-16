package com.meshai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.meshai.ui.viewmodel.DashboardViewModel

// -----------------------------------------------------------------------
// Goal Input Screen
// -----------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalInputScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    var goalText by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Goal") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Describe your goal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "The agent will break it down into tasks and execute them autonomously.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = goalText,
                onValueChange = { goalText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                label = { Text("e.g., Monitor front door and alert me if motion detected") },
                maxLines = 6
            )

            // Example goals
            Text("Examples:", style = MaterialTheme.typography.labelMedium)
            listOf(
                "Send daily weather summary to my phone number",
                "Monitor battery and alert me if below 20%",
                "Respond to incoming messages with 'I'm busy, will reply soon' when I'm in DND mode"
            ).forEach { example ->
                SuggestionChip(
                    onClick = { goalText = example },
                    label = { Text(example, maxLines = 1) }
                )
            }

            Spacer(Modifier.weight(1f))

            if (submitted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Goal submitted! Tasks are being created.")
                    }
                }
            }

            Button(
                onClick = {
                    if (goalText.isNotBlank()) {
                        viewModel.submitGoal(goalText)
                        submitted = true
                        goalText = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = goalText.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Submit Goal")
            }
        }
    }
}

// -----------------------------------------------------------------------
// Task Queue Screen
// -----------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskQueueScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Queue") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            if (state.recentTasks.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No tasks yet. Add a goal to get started.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(state.recentTasks) { task ->
                    TaskCard(task = task)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------
// Mesh Map Screen
// -----------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshMapScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesh Network") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Hub,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Text("Mesh Map", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Visual mesh topology coming soon.\nNodes are listed on the Dashboard.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// -----------------------------------------------------------------------
// Settings Screen
// -----------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    var agentModeEnabled by remember { mutableStateOf(false) }
    var bleEnabled by remember { mutableStateOf(true) }
    var nearbyEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Agent Mode Override") },
                    supportingContent = { Text("Force agent mode even when owner is present") },
                    leadingContent = { Icon(Icons.Default.SmartToy, null) },
                    trailingContent = {
                        Switch(checked = agentModeEnabled, onCheckedChange = { agentModeEnabled = it })
                    }
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("BLE Discovery") },
                    supportingContent = { Text("Advertise and scan via Bluetooth LE") },
                    leadingContent = { Icon(Icons.Default.Bluetooth, null) },
                    trailingContent = {
                        Switch(checked = bleEnabled, onCheckedChange = { bleEnabled = it })
                    }
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("Nearby Connections") },
                    supportingContent = { Text("Use Google Nearby for peer discovery") },
                    leadingContent = { Icon(Icons.Default.Wifi, null) },
                    trailingContent = {
                        Switch(checked = nearbyEnabled, onCheckedChange = { nearbyEnabled = it })
                    }
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("About MeshAI") },
                    supportingContent = { Text("v0.1.0 · MIT License · github.com/yourusername/MeshAI") },
                    leadingContent = { Icon(Icons.Default.Info, null) }
                )
            }
        }
    }
}
