package com.meshai.agent

import com.meshai.llm.LlmEngine
import com.meshai.llm.LlmMessage
import com.meshai.tools.ToolRegistry
import com.meshai.tools.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ReActLoopTest {

    private lateinit var llmEngine: LlmEngine
    private lateinit var toolRegistry: ToolRegistry
    private lateinit var agentMemory: AgentMemory
    private lateinit var reActLoop: ReActLoop

    private val testNode = AgentNode(
        nodeId = "test-node-001",
        displayName = "TestNode",
        status = NodeStatus.IDLE,
        batteryLevel = 80,
        isOwnerPresent = true
    )

    @Before
    fun setUp() {
        llmEngine = mock()
        toolRegistry = mock()
        agentMemory = mock()
        reActLoop = ReActLoop(llmEngine, toolRegistry, agentMemory)
    }

    @Test
    fun `execute returns final answer when LLM signals completion`() = runTest {
        val task = AgentTask(
            title = "Test task",
            description = "Say hello",
            type = TaskType.LLM_REASONING
        )

        whenever(llmEngine.complete(any(), any())).thenReturn(
            "Thought: I have the answer.\nFINAL ANSWER: Hello, world!"
        )

        val result = reActLoop.execute(task, testNode)

        assertEquals("Hello, world!", result)
    }

    @Test
    fun `execute calls tool when LLM produces action`() = runTest {
        val task = AgentTask(
            title = "Send SMS",
            description = "Send a test SMS",
            type = TaskType.SEND_SMS
        )

        // First call: LLM produces tool action
        // Second call: LLM produces final answer
        var callCount = 0
        whenever(llmEngine.complete(any(), any())).thenAnswer {
            callCount++
            if (callCount == 1) {
                "Thought: I should send the SMS.\nAction: send_sms\nAction Input: {\"to\": \"+1234567890\", \"message\": \"test\"}"
            } else {
                "FINAL ANSWER: SMS was sent successfully."
            }
        }

        whenever(toolRegistry.executeTool(any(), any())).thenReturn(
            ToolResult.success("SMS sent to +1234567890")
        )

        val result = reActLoop.execute(task, testNode)

        assertTrue(result.contains("SMS"))
    }

    @Test
    fun `execute handles tool failure gracefully`() = runTest {
        val task = AgentTask(
            title = "Failing task",
            description = "This tool will fail",
            type = TaskType.CUSTOM
        )

        var callCount = 0
        whenever(llmEngine.complete(any(), any())).thenAnswer {
            callCount++
            if (callCount == 1) {
                "Action: nonexistent_tool\nAction Input: {}"
            } else {
                "FINAL ANSWER: I was unable to complete the task due to a tool error."
            }
        }

        whenever(toolRegistry.executeTool(any(), any())).thenReturn(
            ToolResult.failure("Unknown tool")
        )

        // Should not throw
        val result = reActLoop.execute(task, testNode)
        assertTrue(result.isNotBlank())
    }

    @Test
    fun `loop state transitions correctly`() = runTest {
        val task = AgentTask(
            title = "State test",
            description = "Check state",
            type = TaskType.LLM_REASONING
        )

        whenever(llmEngine.complete(any(), any())).thenReturn("FINAL ANSWER: Done.")

        assertEquals(ReActLoop.LoopState.Idle, reActLoop.loopState.value)

        reActLoop.execute(task, testNode)

        assertTrue(reActLoop.loopState.value is ReActLoop.LoopState.Completed)
    }
}
