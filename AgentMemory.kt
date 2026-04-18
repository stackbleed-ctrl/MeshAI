package com.meshai.storage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentMemory @Inject constructor() {
    private val memory = mutableMapOf<String, String>()
    fun store(key: String, value: String) { memory[key] = value }
    fun recall(key: String): String? = memory[key]
    fun recallAll(): Map<String, String> = memory.toMap()
}
