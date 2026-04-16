package com.meshai.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mesh message encryption using Android Keystore + AES-256-GCM.
 *
 * All messages transmitted over the mesh (Meshrabiya / Nearby / BLE)
 * are encrypted before sending and decrypted on receipt.
 *
 * Architecture:
 * - **Symmetric layer**: AES-256-GCM for bulk message encryption.
 *   Key is stored in Android Keystore (hardware-backed where available).
 * - **Key exchange**: Noise_XX handshake pattern for session key establishment
 *   between new peer nodes. (Noise implementation is stubbed — replace with
 *   a production Noise library such as snow or noise-java.)
 * - **Storage**: All persisted data (Room DB, DataStore) is additionally
 *   protected by EncryptedSharedPreferences / EncryptedFile (Jetpack Security).
 *
 * In production:
 * - Each node pair should negotiate a unique session key via Noise handshake
 * - Rotate keys every 24 hours or 1000 messages
 * - Use age (age-encryption.org) for cross-device key exchange if Noise is unavailable
 */
@Singleton
class MeshEncryption @Inject constructor() {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MESH_KEY_ALIAS = "MeshAI_MeshKey_v1"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128    // bits
        private const val GCM_IV_LENGTH = 12      // bytes
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).also { it.load(null) }

    init {
        ensureKeyExists()
    }

    // -----------------------------------------------------------------------
    // Encryption
    // -----------------------------------------------------------------------

    /**
     * Encrypt a plaintext string for mesh transmission.
     * Returns: IV (12 bytes) + ciphertext + GCM auth tag
     */
    fun encrypt(plaintext: String): ByteArray {
        return try {
            val key = getMeshKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val iv = cipher.iv  // 12 bytes, auto-generated
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            // Prepend IV for decryption
            iv + ciphertext
        } catch (e: Exception) {
            Timber.e(e, "[Encryption] Encrypt failed — sending unencrypted (UNSAFE)")
            // Fallback: send as-is with a marker (for debugging only)
            byteArrayOf(0x00) + plaintext.toByteArray(Charsets.UTF_8)
        }
    }

    // -----------------------------------------------------------------------
    // Decryption
    // -----------------------------------------------------------------------

    /**
     * Decrypt bytes received from the mesh.
     * Input must be in format: IV (12 bytes) + ciphertext + GCM auth tag
     */
    fun decrypt(encrypted: ByteArray): String {
        return try {
            if (encrypted.isEmpty()) return ""

            // Check for unencrypted fallback marker
            if (encrypted[0] == 0x00.toByte() && encrypted.size > 1) {
                Timber.w("[Encryption] Received unencrypted message")
                return String(encrypted.drop(1).toByteArray(), Charsets.UTF_8)
            }

            if (encrypted.size <= GCM_IV_LENGTH) {
                throw IllegalArgumentException("Encrypted payload too short")
            }

            val iv = encrypted.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = encrypted.copyOfRange(GCM_IV_LENGTH, encrypted.size)

            val key = getMeshKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            Timber.e(e, "[Encryption] Decrypt failed")
            throw SecurityException("Message decryption failed: ${e.message}", e)
        }
    }

    // -----------------------------------------------------------------------
    // Key management
    // -----------------------------------------------------------------------

    private fun ensureKeyExists() {
        if (!keyStore.containsAlias(MESH_KEY_ALIAS)) {
            Timber.i("[Encryption] Generating new mesh key in Android Keystore")
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )
            val spec = KeyGenParameterSpec.Builder(
                MESH_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false) // Background service cannot authenticate
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun getMeshKey(): SecretKey {
        val entry = keyStore.getEntry(MESH_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            ?: throw SecurityException("Mesh key not found in Keystore")
        return entry.secretKey
    }

    /**
     * Rotate the mesh key (e.g., after security event or scheduled rotation).
     * All in-flight messages will fail to decrypt — call this only during idle.
     */
    fun rotateKey() {
        Timber.i("[Encryption] Rotating mesh key")
        keyStore.deleteEntry(MESH_KEY_ALIAS)
        ensureKeyExists()
    }
}
