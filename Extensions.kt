package com.meshai.core.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach

/** Logs errors from a Flow without terminating it. */
fun <T> Flow<T>.catchAndLog(tag: String, log: (String, Throwable) -> Unit): Flow<T> =
    catch { e -> log(tag, e) }

/** Millisecond timestamp helper. */
fun nowMs(): Long = System.currentTimeMillis()
