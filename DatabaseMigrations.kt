package com.meshai.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database migration: version 1 → 2
 *
 * Adds distributed lease columns to agent_tasks required by [AgentTask] v2
 * and [TaskLeaseManager]. Columns are nullable so all existing rows get
 * NULL values (unclaimed state) automatically.
 *
 * Register this in [MeshAIDatabase]:
 *
 * ```kotlin
 * Room.databaseBuilder(context, MeshAIDatabase::class.java, "meshai.db")
 *     .addMigrations(MIGRATION_1_2)
 *     .build()
 * ```
 *
 * Also bump the @Database version annotation to version = 2.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE agent_tasks ADD COLUMN ownerNodeId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE agent_tasks ADD COLUMN executorNodeId TEXT")
        db.execSQL("ALTER TABLE agent_tasks ADD COLUMN executorLeasedAt INTEGER")
        db.execSQL("ALTER TABLE agent_tasks ADD COLUMN executorLeaseExpiry INTEGER")
    }
}
