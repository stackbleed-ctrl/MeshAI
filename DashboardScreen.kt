package com.meshai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.meshai.agent.AgentTask
import com.meshai.agent.NodeStatus
import com.meshai.agent.TaskStatus
import com.meshai.mesh.NetworkStatus
import com.meshai.ui.navigation.Screen
import com.meshai.ui.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("MeshAI", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    // Network status indicator
                    NetworkStatusChip(status = state.networkStatus)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.GoalInput.route) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Goal") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Agent Mode Banner
            item {
                AgentModeBanner(
                    isAgentMode = state.isAgentModeActive,
                    onToggle = { viewModel.toggleAgentMode() }
                )
            }

            // Stats row
            item {
                StatsRow(
                    nodeCount = state.meshNodes.size,
                    pendingTasks = state.pendingCount,
                    onMeshMapClick = { navController.navigate(Screen.MeshMap.route) },
                    onTasksClick = { navController.navigate(Screen.TaskQueue.route) }
                )
            }

            // Mesh nodes section
            if (state.meshNodes.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Mesh Nodes",
                        icon = Icons.Default.Wifi,
                        action = "View map",
                        onAction = { navController.navigate(Screen.MeshMap.route) }
                    )
                }
                items(state.meshNodes.take(3)) { node ->
                    NodeCard(node = node)
                }
            }

            // Recent tasks section
            item {
                SectionHeader(
                    title = "Recent Tasks",
                    icon = Icons.Default.Assignment,
                    action = "View all",
                    onAction = { navController.navigate(Screen.TaskQueue.route) }
                )
            }

            if (state.recentTasks.isEmpty()) {
                item {
                    EmptyTasksCard(onAddGoal = { navController.navigate(Screen.GoalInput.route) })
                }
            } else {
                items(state.recentTasks) { task ->
                    TaskCard(task = task)
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// -----------------------------------------------------------------------
// Sub-composables
// -----------------------------------------------------------------------

@Composable
private fun AgentModeBanner(isAgentMode: Boolean, onToggle: () -> Unit) {
    val containerColor = if (isAgentMode)
        MaterialTheme.colorScheme.errorContainer
    else
        MaterialTheme.colorScheme.primaryContainer

    val contentColor = if (isAgentMode)
        MaterialTheme.colorScheme.onErrorContainer
    else
        MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isAgentMode) Icons.Default.SmartToy else Icons.Default.Person,
                contentDescription = null,
                tint = contentColor
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isAgentMode) "Agent Mode Active" else "Owner Present",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = if (isAgentMode) "AI is handling tasks autonomously"
                    else "AI is on standby",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
            }
            Switch(
                checked = isAgentMode,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
private fun StatsRow(
    nodeCount: Int,
    pendingTasks: Int,
    onMeshMapClick: () -> Unit,
    onTasksClick: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = "Mesh Nodes",
            value = nodeCount.toString(),
            icon = Icons.Default.Hub,
            onClick = onMeshMapClick
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "Pending Tasks",
            value = pendingTasks.toString(),
            icon = Icons.Default.Pending,
            onClick = onTasksClick
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun NodeCard(node: com.meshai.agent.AgentNode) {
    val statusColor = when (node.status) {
        NodeStatus.ACTIVE, NodeStatus.AGENT_MODE -> MaterialTheme.colorScheme.primary
        NodeStatus.IDLE -> Color(0xFF4CAF50)
        NodeStatus.SLEEPING -> Color(0xFFFF9800)
        NodeStatus.OFFLINE -> MaterialTheme.colorScheme.outline
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(node.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "${node.capabilities.size} capabilities · ${node.batteryLevel}% battery",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                node.status.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = statusColor
            )
        }
    }
}

@Composable
private fun TaskCard(task: AgentTask) {
    val statusColor = when (task.status) {
        TaskStatus.COMPLETED -> Color(0xFF4CAF50)
        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        TaskStatus.FAILED -> MaterialTheme.colorScheme.error
        TaskStatus.PENDING -> MaterialTheme.colorScheme.outline
        TaskStatus.DELEGATED -> Color(0xFFFF9800)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Task,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    task.type.name.lowercase().replace('_', ' '),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    task.status.name.take(4),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun EmptyTasksCard(onAddGoal: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text("No tasks yet", style = MaterialTheme.typography.titleSmall)
            Text(
                "Add a goal and let the agents get to work",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onAddGoal) { Text("Add Goal") }
        }
    }
}

@Composable
private fun NetworkStatusChip(status: NetworkStatus) {
    val (color, label) = when (status) {
        NetworkStatus.CONNECTED -> Color(0xFF4CAF50) to "Connected"
        NetworkStatus.SEARCHING -> Color(0xFFFF9800) to "Searching"
        NetworkStatus.OFFLINE -> MaterialTheme.colorScheme.outline to "Offline"
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}
