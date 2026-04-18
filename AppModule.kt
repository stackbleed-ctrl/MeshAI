package com.meshai.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.meshai.data.db.MeshAIDatabase
import com.meshai.data.db.MemoryDao
import com.meshai.data.db.NodeDao
import com.meshai.data.db.TaskDao
import com.meshai.mesh.JsonMeshCodec
import com.meshai.mesh.MeshCodec
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "meshai_prefs")

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

// -----------------------------------------------------------------------
// App-level DI
// -----------------------------------------------------------------------

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore
}

// -----------------------------------------------------------------------
// Database
// -----------------------------------------------------------------------

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MeshAIDatabase =
        Room.databaseBuilder(context, MeshAIDatabase::class.java, "meshai.db")
            .addMigrations(com.meshai.data.db.MIGRATION_1_2)
            .build()

    @Provides fun provideTaskDao(db: MeshAIDatabase): TaskDao     = db.taskDao()
    @Provides fun provideMemoryDao(db: MeshAIDatabase): MemoryDao = db.memoryDao()
    @Provides fun provideNodeDao(db: MeshAIDatabase): NodeDao     = db.nodeDao()
}

// -----------------------------------------------------------------------
// Protocol / Codec
// -----------------------------------------------------------------------

@Module
@InstallIn(SingletonComponent::class)
abstract class CodecModule {
    /**
     * Bind [JsonMeshCodec] as the [MeshCodec] implementation.
     * Swap this binding to a protobuf codec without touching any call site.
     */
    @Binds
    @Singleton
    abstract fun bindMeshCodec(impl: JsonMeshCodec): MeshCodec
}

// -----------------------------------------------------------------------
// Runtime
// -----------------------------------------------------------------------

/**
 * RuntimeModule wires the new kernel-based runtime.
 *
 * The kernel loop is launched by [RuntimeController.start], which is called
 * from [AgentForegroundService.onStartCommand]. All execution flows through
 * [MeshKernel] — the service has no direct task logic.
 *
 * Binding notes:
 * - [ApplicationScope] is the coroutine scope for [RuntimeController].
 *   It uses SupervisorJob + Dispatchers.Default so the kernel coroutine
 *   survives individual task failures without killing the parent scope.
 * - [TaskScheduler], [DecisionEngine], [CapabilityRegistry], [EnvelopeDispatcher],
 *   and [MeshKernel] are all @Singleton — injected by Hilt automatically.
 *   No @Provides needed unless you want to override the implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
object RuntimeModule {

    /**
     * [RuntimeController] needs the application-scoped coroutine scope
     * so the kernel loop survives configuration changes and is not bound
     * to an Activity or ViewModel lifetime.
     */
    @Provides
    @Singleton
    fun provideRuntimeController(
        kernel: com.meshai.agent.MeshKernel,
        @ApplicationScope scope: CoroutineScope
    ): com.meshai.service.RuntimeController =
        com.meshai.service.RuntimeController(kernel, scope)
}
