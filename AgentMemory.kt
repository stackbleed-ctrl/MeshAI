package com.meshai.agent

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.meshai.data.db.MemoryDao
import com.meshai.data.db.MemoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agent memory system with two tiers:
 *
 * 1. **Short-term (DataStore)**: Fast key-value store for in-session context.
 *    Survives process death but is intentionally compact (< 100 entries).
 *
 * 2. **Long-term (Room)**: Persistent encrypted database for cross-session
 *    recall, shared knowledge base entries, and mesh-broadcast facts.
 *
 * The shared mesh knowledge base is stored in the same Room table but
 * marked with [MemoryEntry.isShared] = true. These entries are gossiped
 * to peer nodes via the mesh message protocol.
 */
@Singleton
class AgentMemory @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val memoryDao: MemoryDao
) {

    // -----------------------------------------------------------------------
    // Short-term (DataStore) — in-session
    // -----------------------------------------------------------------------

    /** Store a short-term key-value pair */
    suspend fun store(key: String, value: String) {
        Timber.d("[Memory] Storing short-term: $key")
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("mem_$key")] = value
        }
    }

    /** Retrieve a short-term value */
    fun observe(key: String): Flow<String?> =
        dataStore.data.map { prefs ->
            prefs[stringPreferencesKey("mem_$key")]
        }

    /** Clear all short-term memory (e.g., end of session) */
    suspend fun clearShortTerm() {
        dataStore.edit { prefs ->
            val keysToRemove = prefs.asMap().keys.filter { it.name.startsWith("mem_") }
            keysToRemove.forEach { prefs.remove(it) }
        }
    }

    // -----------------------------------------------------------------------
    // Long-term (Room) — persistent + shared knowledge base
    // -----------------------------------------------------------------------

    /** Persist a memory entry to the long-term store */
    suspend fun remember(entry: MemoryEntry) {
        Timber.d("[Memory] Persisting: ${entry.key}")
        memoryDao.insert(entry)
    }

    /** Recall a specific memory by key */
    suspend fun recall(key: String): MemoryEntry? = memoryDao.getByKey(key)

    /** Observe all memories for the mesh knowledge base UI */
    fun observeAll(): Flow<List<MemoryEntry>> = memoryDao.observeAll()

    /** All entries marked as shared (for mesh gossip) */
    suspend fun getSharedEntries(): List<MemoryEntry> = memoryDao.getShared()

    /** Merge incoming shared entries from a peer node */
    suspend fun mergeFromPeer(entries: List<MemoryEntry>) {
        Timber.d("[Memory] Merging ${entries.size} entries from peer")
        entries.forEach { entry ->
            val existing = memoryDao.getByKey(entry.key)
            // Last-write-wins based on timestamp
            if (existing == null || entry.timestamp > existing.timestamp) {
                memoryDao.insert(entry.copy(isShared = true))
            }
        }
    }

    /** Prune entries older than [maxAgeMs] */
    suspend fun pruneOld(maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000L) {
        val cutoff = Instant.now().toEpochMilli() - maxAgeMs
        memoryDao.deleteBefore(cutoff)
        Timber.d("[Memory] Pruned entries older than ${maxAgeMs / 3600000}h")
    }
}
