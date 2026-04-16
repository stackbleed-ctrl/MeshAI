package com.meshai.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.meshai.agent.TaskPriority
import com.meshai.agent.TaskStatus
import com.meshai.agent.TaskType
import kotlinx.coroutines.flow.Flow

// -----------------------------------------------------------------------
// Database
// -----------------------------------------------------------------------

@Database(
    entities = [TaskEntity::class, MemoryEntry::class, NodeEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class MeshAIDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun memoryDao(): MemoryDao
    abstract fun nodeDao(): NodeDao
}

// -----------------------------------------------------------------------
// Type converters
// -----------------------------------------------------------------------

class RoomConverters {
    @TypeConverter fun fromTaskType(value: TaskType): String = value.name
    @TypeConverter fun toTaskType(value: String): TaskType = TaskType.valueOf(value)

    @TypeConverter fun fromTaskStatus(value: TaskStatus): String = value.name
    @TypeConverter fun toTaskStatus(value: String): TaskStatus = TaskStatus.valueOf(value)

    @TypeConverter fun fromTaskPriority(value: TaskPriority): String = value.name
    @TypeConverter fun toTaskPriority(value: String): TaskPriority = TaskPriority.valueOf(value)
}

// -----------------------------------------------------------------------
// Task entity + DAO
// -----------------------------------------------------------------------

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val taskId: String,
    val title: String,
    val description: String,
    val type: TaskType,
    val priority: TaskPriority,
    val status: TaskStatus,
    val originNodeId: String?,
    val assignedNodeId: String?,
    val createdEpoch: Long,
    val completedEpoch: Long?,
    val result: String?
)

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE status = 'PENDING' ORDER BY priority DESC, createdEpoch ASC")
    suspend fun getPending(): List<TaskEntity>

    @Query("SELECT * FROM tasks ORDER BY createdEpoch DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET status = :status WHERE taskId = :taskId")
    suspend fun updateStatus(taskId: String, status: TaskStatus)

    @Query("UPDATE tasks SET status = 'COMPLETED', completedEpoch = :epoch, result = :result WHERE taskId = :taskId")
    suspend fun complete(taskId: String, epoch: Long, result: String)

    @Query("DELETE FROM tasks WHERE completedEpoch < :before")
    suspend fun deleteCompleted(before: Long)
}

// -----------------------------------------------------------------------
// Memory entity + DAO
// -----------------------------------------------------------------------

@Entity(tableName = "memory")
data class MemoryEntry(
    @PrimaryKey val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_shared") val isShared: Boolean = false,
    val sourceNodeId: String? = null
)

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MemoryEntry)

    @Query("SELECT * FROM memory WHERE `key` = :key")
    suspend fun getByKey(key: String): MemoryEntry?

    @Query("SELECT * FROM memory ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<MemoryEntry>>

    @Query("SELECT * FROM memory WHERE is_shared = 1")
    suspend fun getShared(): List<MemoryEntry>

    @Query("DELETE FROM memory WHERE timestamp < :before")
    suspend fun deleteBefore(before: Long)
}

// -----------------------------------------------------------------------
// Node entity + DAO
// -----------------------------------------------------------------------

@Entity(tableName = "nodes")
data class NodeEntity(
    @PrimaryKey val nodeId: String,
    val displayName: String,
    val capabilities: String,  // JSON array
    val status: String,
    val batteryLevel: Int,
    val isOwnerPresent: Boolean,
    val meshIp: String?,
    val lastSeenEpoch: Long
)

@Dao
interface NodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(node: NodeEntity)

    @Query("SELECT * FROM nodes")
    fun observeAll(): Flow<List<NodeEntity>>

    @Query("SELECT * FROM nodes WHERE nodeId = :nodeId")
    suspend fun getById(nodeId: String): NodeEntity?

    @Query("UPDATE nodes SET status = :status, lastSeenEpoch = :epoch WHERE nodeId = :nodeId")
    suspend fun updateStatus(nodeId: String, status: String, epoch: Long)

    @Query("DELETE FROM nodes WHERE lastSeenEpoch < :before")
    suspend fun pruneStale(before: Long)
}
