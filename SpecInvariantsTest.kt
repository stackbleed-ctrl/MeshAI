package com.meshai

import com.meshai.agent.AgentNode
import com.meshai.agent.AgentTask
import com.meshai.agent.TaskOrigin
import com.meshai.agent.safety.KillSwitch
import com.meshai.agent.safety.SafetyGate
import com.meshai.agent.safety.ToolRequest
import com.meshai.mesh.MeshMessage
import com.meshai.mesh.MessageType
import com.meshai.tools.AgentTool
import com.meshai.tools.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * SpecInvariantsTest — machine-checkable enforcement of MeshAI Protocol Spec v1.0.0.
 *
 * Each test corresponds to a NON-NEGOTIABLE INVARIANT from the spec.
 * Test name format: `invariant_<ID>_<description>`
 *
 * If any of these tests fail, a spec invariant has been violated in code.
 */
class SpecInvariantsTest {

    private lateinit var killSwitch: KillSwitch
    private lateinit var safetyGate: SafetyGate

    @Before
    fun setUp() {
        killSwitch = KillSwitch()
        safetyGate = SafetyGate(killSwitch)
    }

    // -------------------------------------------------------------------------
    // INV-001 / SAFETY-001 — SafetyGate is mandatory
    // (Structural: ToolExecutionGuard always calls SafetyGate — tested via INV-003)
    // -------------------------------------------------------------------------

    @Test
    fun `invariant_INV001_safetyGate_allows_valid_local_request`() {
        // SPEC_REF: SAFETY-001 — gate must allow well-formed local requests
        val request = ToolRequest(
            toolId = "reason",
            origin = TaskOrigin.LOCAL,
            ownerPresent = true
        )
        val decision = safetyGate.evaluate(request)
        assertTrue("SafetyGate should allow valid local request", decision is SafetyGate.Decision.Allow)
    }

    // -------------------------------------------------------------------------
    // INV-002 / SAFETY-002 — SMS denied for remote origin
    // -------------------------------------------------------------------------

    @Test
    fun `invariant_INV002_sms_denied_for_remote_origin`() {
        // SPEC_REF: SAFETY-002 / INV-002
        // send_sms from a REMOTE task must be rejected with an exception (hard assert in SafetyGate)
        val request = ToolRequest(
            toolId = "send_sms",
            origin = TaskOrigin.REMOTE,
            ownerPresent = true
        )
        val threw = try {
            safetyGate.evaluate(request)
            false
        } catch (e: IllegalStateException) {
            e.message?.contains("SAFETY-002") == true
        }
        assertTrue("INV-002: send_sms must throw for REMOTE origin", threw)
    }

    @Test
    fun `invariant_INV002_sms_allowed_for_local_owner_present`() {
        // SPEC_REF: SAFETY-002 — LOCAL origin with owner present must be allowed
        val request = ToolRequest(
            toolId = "send_sms",
            origin = TaskOrigin.LOCAL,
            ownerPresent = true,
            isIrreversible = true,
            ownerApproved = true
        )
        val decision = safetyGate.evaluate(request)
        assertTrue("INV-002: send_sms must be allowed for LOCAL + owner present", decision is SafetyGate.Decision.Allow)
    }

    // -------------------------------------------------------------------------
    // INV-003 / SAFETY-003 — KillSwitch halts all paths
    // -------------------------------------------------------------------------

    @Test
    fun `invariant_INV003_killswitch_halts_safetyGate`() {
        // SPEC_REF: SAFETY-003 / INV-003
        killSwitch.halt("test halt")
        assertTrue("KillSwitch should be halted", killSwitch.isHalted)

        val request = ToolRequest(
            toolId = "reason",
            origin = TaskOrigin.LOCAL,
            ownerPresent = true
        )
        val decision = safetyGate.evaluate(request)
        assertTrue("INV-003: SafetyGate must DENY all requests when KillSwitch is active",
            decision is SafetyGate.Decision.Deny)
        val reason = (decision as SafetyGate.Decision.Deny).reason
        assertTrue("INV-003: Deny reason must reference SAFETY-003", reason.contains("SAFETY-003"))
    }

    @Test
    fun `invariant_INV003_killswitch_resumes_on_owner_gesture`() {
        // SPEC_REF: SAFETY-003 — resumption requires explicit owner gesture
        killSwitch.halt("test")
        killSwitch.resumeByOwner()
        assertFalse("KillSwitch should not be halted after owner resume", killSwitch.isHalted)
    }

    // -------------------------------------------------------------------------
    // INV-004 / MESH-001 — MeshMessage must be signed
    // -------------------------------------------------------------------------

    @Test
    fun `invariant_INV004_unsigned_message_assertion_fires`() {
        // SPEC_REF: MESH-001 / INV-004
        val msg = buildTestMessage(signature = null)
        val threw = try {
            with(MeshMessage.Companion) { msg.assertSigned() }
            false
        } catch (e: IllegalArgumentException) {
            e.message?.contains("MESH-001") == true
        }
        assertTrue("INV-004: assertSigned() must throw for null signature with MESH-001 message", threw)
    }

    @Test
    fun `invariant_INV004_signed_message_passes_assertion`() {
        // SPEC_REF: MESH-001 — signed message must not throw
        val msg = buildTestMessage(signature = "fakesig_abc123")
        var threw = false
        try {
            with(MeshMessage.Companion) { msg.assertSigned() }
        } catch (e: Exception) {
            threw = true
        }
        assertFalse("INV-004: signed message must pass assertSigned()", threw)
    }

    // -------------------------------------------------------------------------
    // INV-006 / SAFETY-004 — Owner-absent irreversible action prevention
    // -------------------------------------------------------------------------

    @Test
    fun `invariant_INV006_irreversible_action_denied_when_owner_absent_and_not_approved`() {
        // SPEC_REF: SAFETY-004 / INV-006
        val request = ToolRequest(
            toolId = "send_sms",
            origin = TaskOrigin.LOCAL,
            ownerPresent = false,
            ownerApproved = false,
            isIrreversible = true
        )
        val decision = safetyGate.evaluate(request)
        assertTrue("INV-006: irreversible action must be denied when owner absent + not approved",
            decision is SafetyGate.Decision.Deny)
    }

    @Test
    fun `invariant_INV006_irreversible_action_allowed_when_pre_approved`() {
        // SPEC_REF: SAFETY-004 — pre-approved tasks may execute irreversible actions without owner
        val request = ToolRequest(
            toolId = "send_sms",
            origin = TaskOrigin.LOCAL,
            ownerPresent = false,
            ownerApproved = true,
            isIrreversible = true
        )
        val decision = safetyGate.evaluate(request)
        assertTrue("INV-006: pre-approved irreversible task must be allowed",
            decision is SafetyGate.Decision.Allow)
    }

    // -------------------------------------------------------------------------
    // INV-007 / LOOP-001 — MAX_STEPS constant value
    // -------------------------------------------------------------------------

    @Test
    fun `invariant_INV007_max_steps_is_12`() {
        // SPEC_REF: LOOP-001 / INV-007
        // Verify via reflection so changing the constant breaks the test
        val clazz = Class.forName("com.meshai.agent.ReActLoop\$Companion")
        val field = clazz.getDeclaredField("MAX_STEPS")
        field.isAccessible = true
        val value = field.getInt(null)
        assertEquals("INV-007: MAX_STEPS must be exactly 12 per spec", 12, value)
    }

    // -------------------------------------------------------------------------
    // INV-008 / MESH-003 — TTL must be 1..5
    // -------------------------------------------------------------------------

    @Test
    fun `invariant_INV008_ttl_zero_throws`() {
        // SPEC_REF: MESH-003 / INV-008
        val threw = try {
            buildTestMessage(ttl = 0)
            false
        } catch (e: IllegalArgumentException) {
            e.message?.contains("MESH-003") == true
        }
        assertTrue("INV-008: TTL=0 must throw with MESH-003 in message", threw)
    }

    @Test
    fun `invariant_INV008_ttl_six_throws`() {
        // SPEC_REF: MESH-003 / INV-008
        val threw = try {
            buildTestMessage(ttl = 6)
            false
        } catch (e: IllegalArgumentException) {
            e.message?.contains("MESH-003") == true
        }
        assertTrue("INV-008: TTL=6 must throw with MESH-003 in message", threw)
    }

    @Test
    fun `invariant_INV008_ttl_5_is_valid`() {
        // SPEC_REF: MESH-003 — max valid TTL
        val msg = buildTestMessage(ttl = 5)
        assertEquals(5, msg.ttl)
    }

    @Test
    fun `invariant_INV008_ttl_1_is_valid`() {
        // SPEC_REF: MESH-003 — min valid TTL
        val msg = buildTestMessage(ttl = 1)
        assertEquals(1, msg.ttl)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildTestMessage(
        ttl: Int = 5,
        signature: String? = "test_sig"
    ) = MeshMessage(
        messageId = UUID.randomUUID().toString(),
        originNodeId = "node-test",
        targetNodeId = null,
        type = MessageType.HEARTBEAT,
        payload = "{}",
        ttl = ttl,
        signature = signature
    )

    private fun fakeNode() = AgentNode(
        nodeId = "test-node",
        displayName = "TestPhone",
        capabilities = emptyList(),
        batteryLevel = 80,
        isOwnerPresent = true
    )
}
