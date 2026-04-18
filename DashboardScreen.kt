package com.meshai.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meshai.core.model.AgentNode
import com.meshai.core.model.AgentTask
import com.meshai.core.model.NodeStatus

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    MeshAITheme {
        Scaffold(
            topBar = { MeshTopBar(state) },
            containerColor = Color(0xFF06101E)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                AgentModeToggle(
                    enabled  = state.agentModeOverride,
                    onToggle = viewModel::toggleAgentMode
                )
                Spacer(Modifier.height(16.dp))
                GoalInputCard(onSubmit = viewModel::submitGoal, isLoading = state.isLoading)
                Spacer(Modifier.height(16.dp))
                NodeGrid(nodes = state.nodes)
                Spacer(Modifier.height(16.dp))
                TaskList(tasks = state.tasks)
                state.error?.let { ErrorBanner(it, onDismiss = viewModel::clearError) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeshTopBar(state: DashboardUiState) = TopAppBar(
    title = {
        Text(
            "MESH AI",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            letterSpacing = 4.sp
        )
    },
    actions = {
        StatusChip(
            label = if (state.isOwnerPresent) "OWNER PRESENT" else "AGENT MODE",
            color = if (state.isOwnerPresent) Color(0xFF3DDC84) else Color(0xFFFFAA00)
        )
        Spacer(Modifier.width(8.dp))
        StatusChip(label = "${state.connectedPeers} PEERS", color = Color(0xFF00D4FF))
        Spacer(Modifier.width(8.dp))
    },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B1D35))
)

@Composable
private fun StatusChip(label: String, color: Color) = Surface(
    shape = MaterialTheme.shapes.small,
    color = color.copy(alpha = 0.12f),
    border = ButtonDefaults.outlinedButtonBorder
) {
    Text(
        label,
        modifier  = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        color     = color,
        fontFamily= FontFamily.Monospace,
        fontSize  = 10.sp,
        fontWeight= FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun AgentModeToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) = Row(
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        "AGENT MODE",
        fontFamily = FontFamily.Monospace,
        color = Color(0xFF3DDC84),
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier.weight(1f)
    )
    Switch(
        checked  = enabled,
        onCheckedChange = onToggle,
        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF3DDC84))
    )
}

@Composable
private fun GoalInputCard(onSubmit: (String) -> Unit, isLoading: Boolean) {
    var text by remember { mutableStateOf("") }
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1D35))) {
        Column(Modifier.padding(16.dp)) {
            Text("DEFINE GOAL", fontFamily = FontFamily.Monospace, color = Color(0xFF00D4FF),
                fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Monitor front door, alert me if motion…",
                    fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Color(0xFF3DDC84),
                    unfocusedBorderColor = Color(0xFF3DDC84).copy(alpha = 0.3f)
                )
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = { if (text.isNotBlank()) { onSubmit(text); text = "" } },
                enabled  = !isLoading && text.isNotBlank(),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC84))
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), color = Color.Black)
                else Text("DISPATCH", fontFamily = FontFamily.Monospace, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NodeGrid(nodes: List<AgentNode>) {
    Text("MESH NODES (${nodes.size})", fontFamily = FontFamily.Monospace,
        color = Color(0xFF3DDC84), fontSize = 11.sp, letterSpacing = 2.sp)
    Spacer(Modifier.height(8.dp))
    if (nodes.isEmpty()) {
        Text("Scanning for peers…", fontFamily = FontFamily.Monospace,
            color = Color(0xFF3DDC84).copy(alpha = 0.5f), fontSize = 12.sp)
    } else {
        nodes.forEach { NodeChip(it) }
    }
}

@Composable
private fun NodeChip(node: AgentNode) {
    val statusColor = when (node.status) {
        NodeStatus.ACTIVE      -> Color(0xFF3DDC84)
        NodeStatus.AGENT_MODE  -> Color(0xFFFFAA00)
        NodeStatus.SLEEPING    -> Color(0xFF888888)
        NodeStatus.OFFLINE     -> Color(0xFFFF4444)
        else                   -> Color(0xFF00D4FF)
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = MaterialTheme.shapes.small, color = statusColor.copy(alpha = 0.15f)) {
            Text(node.displayName,
                modifier  = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                fontFamily= FontFamily.Monospace, fontSize = 12.sp, color = statusColor)
        }
        Spacer(Modifier.width(8.dp))
        Text("${node.batteryLevel}%  ·  ${node.status}",
            fontFamily = FontFamily.Monospace, fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.45f))
    }
}

@Composable
private fun TaskList(tasks: List<AgentTask>) {
    Text("TASK QUEUE (${tasks.size})", fontFamily = FontFamily.Monospace,
        color = Color(0xFF00D4FF), fontSize = 11.sp, letterSpacing = 2.sp)
    Spacer(Modifier.height(8.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(tasks) { task ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1D35))) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(task.title, fontFamily = FontFamily.Monospace, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(task.description, fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, maxLines = 2)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("${task.priority}", fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp, color = Color(0xFF3DDC84))
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(error: String, onDismiss: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFF4444).copy(alpha = 0.15f))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(error, fontFamily = FontFamily.Monospace, color = Color(0xFFFF4444),
                modifier = Modifier.weight(1f), fontSize = 12.sp)
            TextButton(onClick = onDismiss) { Text("DISMISS", fontFamily = FontFamily.Monospace, color = Color(0xFFFF4444)) }
        }
    }
}
