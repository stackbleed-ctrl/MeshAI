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

// DataStore extension property
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "meshai_prefs")

// -----------------------------------------------------------------------
// Coroutine scope qualifier
// -----------------------------------------------------------------------

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

// -----------------------------------------------------------------------
// App-level DI module
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
// Database module
// -----------------------------------------------------------------------

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MeshAIDatabase =
        Room.databaseBuilder(
            context,
            MeshAIDatabase::class.java,
            "meshai.db"
        )
            .fallbackToDestructiveMigration() // Replace with proper migrations in production
            .build()

    @Provides
    fun provideTaskDao(db: MeshAIDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideMemoryDao(db: MeshAIDatabase): MemoryDao = db.memoryDao()

    @Provides
    fun provideNodeDao(db: MeshAIDatabase): NodeDao = db.nodeDao()
}
