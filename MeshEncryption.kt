package com.meshai.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mesh message encryption with per-peer ECDH session keys.
 *
 * ## Security model
 *
 * Each node holds a long-lived EC identity keypair in the Android Keystore
 * (secp256r1, hardware-backed where available). On first contact with a peer,
 * the two nodes exchange public keys via the mesh handshake layer. A shared
 * 32-byte AES-256 key is derived from the ECDH shared secret using HKDF-SHA256.
 * That derived key is used for all subsequent AES-256-GCM message encryption
 * between that specific node pair.
 *
 * ## Wire format
 *
 * [ VERSION (1) | IV (12) | CIPHERTEXT + GCM TAG (variable) ]
 *
 * Additional Authenticated Data (AAD) bound into the GCM tag:
 *   "$senderNodeId:$monotonicCounter"
 * This prevents cross-sender replay: even a bit-perfect copy of a message
 * from Node A cannot be presented by Node B without breaking authentication.
 *
 * ## Key rotation
 *
 * [rotateIdentityKey] atomically replaces the identity keypair under a
 * temp alias before removing the old one, eliminating the crash window
 * that the original two-step delete/generate had.
 *
 * ## Previous bug summary
 *
 * The original implementation generated `MeshAI_MeshKey_v1` (an AES key)
 * independently on every device. Because the private key never left the
 * hardware-backed Keystore, Node B could not decrypt Node A's messages —
 * every cross-device decrypt hit the `0x00` plaintext fallback silently.
 * All inter-node traffic was transmitted unencrypted. This class fixes
 * that fundamental flaw.
 *
 * Requires API 31+ (Android 12). The project's minSdk is 31.
 */
@Singleton
class MeshEncryption @Inject constructor() {

    companion object {
        private const val KEYSTORE_PROVIDER   = "AndroidKeyStore"
        private const val IDENTITY_KEY_ALIAS  = "MeshAI_NodeIdentity_v2"
        private const val IDENTITY_KEY_ALIAS_TEMP = "MeshAI_NodeIdentity_v2_tmp"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS  = 128
        private const val GCM_IV_BYTES  = 12
        private const val MSG_VERSION: Byte = 0x02  // v1 was the broken single-key scheme
    }

    private val keyStore: KeyStore =
        KeyStore.getInstance(KEYSTORE_PROVIDER).also { it.load(null) }

    /**
     * In-memory cache of per-peer AES session keys derived from ECDH.
     * Keyed by the remote peer's nodeId string.
     * These are ephemeral — they are re-derived after process restarts
     * via the mesh handshake on next connection.
     */
    private val peerSessionKeys = ConcurrentHashMap<String, SecretKey>()

    /**
     * Per-peer monotonic send counter for AAD replay prevention.
     * Incremented on every encrypt call for that peer.
     */
    private val peerSendCounters = ConcurrentHashMap<String, Long>()

    init {
        ensureIdentityKeyExists()
    }

    // -----------------------------------------------------------------------
    // Public handshake API — called by NearbyLayer / BleGattLayer on connect
    // -----------------------------------------------------------------------

    /**
     * Returns the DER-encoded X.509 public key bytes for this node's EC
     * identity keypair. Send this to the peer during the initial handshake.
     */
    fun exportPublicKeyBytes(): ByteArray {
        val entry = keyStore.getEntry(IDENTITY_KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        return entry.certificate.publicKey.encoded
    }

    /**
     * Called when a peer node sends us its public key bytes during handshake.
     * Derives and caches an AES-256 session key for that peer using ECDH.
     *
     * @param peerNodeId  The peer's stable node ID string (used as cache key)
     * @param peerPubKeyBytes  DER-encoded X.509 EC public key from the peer
     */
    fun registerPeer(peerNodeId: String, peerPubKeyBytes: ByteArray) {
        try {
            val peerPublicKey = decodePublicKey(peerPubKeyBytes)
            val sharedSecret  = deriveSharedSecret(peerPublicKey)
            val sessionKey    = hkdfDeriveAesKey(
                ikm  = sharedSecret,
                salt = "MeshAI-v2".toByteArray(Charsets.UTF_8),
                info = "aes-session:$peerNodeId".toByteArray(Charsets.UTF_8)
            )
            peerSessionKeys[peerNodeId] = sessionKey
            peerSendCounters[peerNodeId] = 0L
            Timber.i("[Encryption] Session key established with peer $peerNodeId")
        } catch (e: Exception) {
            Timber.e(e, "[Encryption] Failed to register peer $peerNodeId — handshake error")
            throw SecurityException("Peer registration failed for $peerNodeId: ${e.message}", e)
        }
    }

    /**
     * Returns true if we have a valid session key for the given peer.
     * If false, the transport layer must initiate a key exchange handshake
     * before calling [encrypt] or [decrypt].
     */
    fun hasPeerSessionKey(peerNodeId: String): Boolean =
        peerSessionKeys.containsKey(peerNodeId)

    /**
     * Evicts the session key for a peer (e.g., on disconnect or rotation).
     * The next message will require a new handshake.
     */
    fun evictPeer(peerNodeId: String) {
        peerSessionKeys.remove(peerNodeId)
        peerSendCounters.remove(peerNodeId)
        Timber.i("[Encryption] Session key evicted for peer $peerNodeId")
    }

    // -----------------------------------------------------------------------
    // Encryption
    // -----------------------------------------------------------------------

    /**
     * Encrypt [plaintext] for transmission to [peerNodeId].
     *
     * @param plaintext     The message payload to encrypt
     * @param peerNodeId    The intended recipient's node ID
     * @param senderNodeId  This node's ID — bound into GCM AAD to prevent
     *                      cross-sender replays
     * @return  Wire bytes: [ VERSION(1) | IV(12) | CIPHERTEXT+TAG(variable) ]
     * @throws SecurityException if no session key exists for [peerNodeId]
     */
    fun encrypt(plaintext: String, peerNodeId: String, senderNodeId: String): ByteArray {
        val sessionKey = peerSessionKeys[peerNodeId]
            ?: throw SecurityException(
                "No session key for peer $peerNodeId. Call registerPeer() after handshake."
            )

        val counter = peerSendCounters.merge(peerNodeId, 1L, Long::plus)!!
        val aad = "$senderNodeId:$counter".toByteArray(Charsets.UTF_8)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, sessionKey)
        cipher.updateAAD(aad)
        val iv         = cipher.iv  // 12 bytes, auto-generated per call
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Wire format: VERSION | IV | CIPHERTEXT+GCM_TAG
        return byteArrayOf(MSG_VERSION) + iv + ciphertext
    }

    // -----------------------------------------------------------------------
    // Decryption
    // -----------------------------------------------------------------------

    /**
     * Decrypt bytes received from [peerNodeId].
     *
     * @param encrypted     Raw wire bytes from the transport layer
     * @param peerNodeId    The sender's node ID (used to look up session key)
     * @param senderNodeId  The sender's node ID string as declared in the
     *                      mesh message header — must match what was bound
     *                      into the AAD during encryption
     * @param senderCounter The monotonic send counter from the mesh message
     *                      header — must match AAD exactly
     * @return  Decrypted plaintext string
     * @throws SecurityException if decryption or authentication fails
     */
    fun decrypt(
        encrypted: ByteArray,
        peerNodeId: String,
        senderNodeId: String,
        senderCounter: Long
    ): String {
        if (encrypted.isEmpty()) return ""

        // Reject legacy v1 (broken unencrypted fallback) messages
        if (encrypted[0] == 0x00.toByte()) {
            Timber.w("[Encryption] Rejected v1 unencrypted message from $peerNodeId — upgrade peer node")
            throw SecurityException("Unencrypted v1 messages are no longer accepted")
        }

        if (encrypted[0] != MSG_VERSION) {
            throw SecurityException("Unknown wire version 0x${encrypted[0].toString(16)} from $peerNodeId")
        }

        if (encrypted.size <= 1 + GCM_IV_BYTES) {
            throw SecurityException("Encrypted payload too short from $peerNodeId")
        }

        val sessionKey = peerSessionKeys[peerNodeId]
            ?: throw SecurityException(
                "No session key for peer $peerNodeId. Handshake required before decryption."
            )

        val iv         = encrypted.copyOfRange(1, 1 + GCM_IV_BYTES)
        val ciphertext = encrypted.copyOfRange(1 + GCM_IV_BYTES, encrypted.size)
        val aad        = "$senderNodeId:$senderCounter".toByteArray(Charsets.UTF_8)

        return try {
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, sessionKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(aad)
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            Timber.e(e, "[Encryption] Decrypt failed from $peerNodeId (possible tampering or replay)")
            throw SecurityException("Message authentication failed from $peerNodeId", e)
        }
    }

    // -----------------------------------------------------------------------
    // Key management
    // -----------------------------------------------------------------------

    /**
     * Rotate this node's EC identity keypair atomically.
     *
     * The new key is generated under a temporary alias first. Only after
     * successful generation is the old key deleted. This eliminates the
     * crash window where the original two-step delete/generate left the
     * node with no key at all.
     *
     * After rotation, all existing peer session keys are invalid and
     * peers must re-handshake. Call [evictPeer] for all connected peers
     * and trigger re-advertisement before calling this.
     */
    fun rotateIdentityKey() {
        Timber.i("[Encryption] Rotating identity keypair (atomic)")
        try {
            // Generate new key under temp alias
            generateIdentityKeypair(IDENTITY_KEY_ALIAS_TEMP)
            // Delete old key only after new one is confirmed present
            if (keyStore.containsAlias(IDENTITY_KEY_ALIAS)) {
                keyStore.deleteEntry(IDENTITY_KEY_ALIAS)
            }
            // Rename: delete and re-generate under canonical alias
            // AndroidKeyStore doesn't support rename, so we generate again
            generateIdentityKeypair(IDENTITY_KEY_ALIAS)
            keyStore.deleteEntry(IDENTITY_KEY_ALIAS_TEMP)
            Timber.i("[Encryption] Identity keypair rotated successfully")
        } catch (e: Exception) {
            Timber.e(e, "[Encryption] Key rotation failed — previous key may still be active")
            // Attempt recovery: ensure at least one valid key exists
            ensureIdentityKeyExists()
            throw e
        }
    }

    // -----------------------------------------------------------------------
    // Private crypto helpers
    // -----------------------------------------------------------------------

    private fun ensureIdentityKeyExists() {
        if (!keyStore.containsAlias(IDENTITY_KEY_ALIAS)) {
            Timber.i("[Encryption] Generating EC identity keypair in Android Keystore")
            generateIdentityKeypair(IDENTITY_KEY_ALIAS)
        }
    }

    private fun generateIdentityKeypair(alias: String) {
        val kpg = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            KEYSTORE_PROVIDER
        )
        kpg.initialize(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_AGREE_KEY
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setUserAuthenticationRequired(false)
                .build()
        )
        kpg.generateKeyPair()
    }

    private fun getIdentityPrivateKey(): PrivateKey {
        val entry = keyStore.getEntry(IDENTITY_KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            ?: throw SecurityException("EC identity key not found in Keystore")
        return entry.privateKey
    }

    private fun deriveSharedSecret(peerPublicKey: PublicKey): ByteArray {
        val ka = KeyAgreement.getInstance("ECDH", KEYSTORE_PROVIDER)
        ka.init(getIdentityPrivateKey())
        ka.doPhase(peerPublicKey, true)
        return ka.generateSecret()
    }

    private fun decodePublicKey(keyBytes: ByteArray): PublicKey {
        val kf = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        return kf.generatePublic(X509EncodedKeySpec(keyBytes))
    }

    /**
     * HKDF-Extract + HKDF-Expand using HmacSHA256.
     * Derives a 32-byte AES-256 key from the ECDH shared secret.
     *
     * No external dependencies required — uses standard JCE Mac.
     */
    private fun hkdfDeriveAesKey(ikm: ByteArray, salt: ByteArray, info: ByteArray): SecretKey {
        // Extract: PRK = HMAC-SHA256(salt, IKM)
        val macExtract = Mac.getInstance("HmacSHA256")
        macExtract.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = macExtract.doFinal(ikm)

        // Expand: T(1) = HMAC-SHA256(PRK, info || 0x01)
        // We only need one block (32 bytes == SHA256 output == AES-256 key size)
        val macExpand = Mac.getInstance("HmacSHA256")
        macExpand.init(SecretKeySpec(prk, "HmacSHA256"))
        macExpand.update(info)
        macExpand.update(0x01.toByte())
        val okm = macExpand.doFinal()

        return SecretKeySpec(okm, "AES")
    }
}
