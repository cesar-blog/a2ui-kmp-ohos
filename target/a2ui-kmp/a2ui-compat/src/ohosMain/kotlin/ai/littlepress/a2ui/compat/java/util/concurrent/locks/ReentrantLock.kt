@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

package ai.littlepress.a2ui.compat.java.util.concurrent.locks

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlin.native.ref.createCleaner
import platform.posix.pthread_cond_broadcast
import platform.posix.pthread_cond_destroy
import platform.posix.pthread_cond_init
import platform.posix.pthread_cond_t
import platform.posix.pthread_cond_wait
import platform.posix.pthread_equal
import platform.posix.pthread_mutex_destroy
import platform.posix.pthread_mutex_init
import platform.posix.pthread_mutex_lock
import platform.posix.pthread_mutex_t
import platform.posix.pthread_mutex_unlock
import platform.posix.pthread_self
import platform.posix.pthread_t

private class LockResources {
    val mutex: CPointer<pthread_mutex_t> = nativeHeap.alloc<pthread_mutex_t>().ptr
    val condition: CPointer<pthread_cond_t> = nativeHeap.alloc<pthread_cond_t>().ptr

    init {
        val mutexResult = pthread_mutex_init(mutex, null)
        if (mutexResult != 0) {
            nativeHeap.free(condition)
            nativeHeap.free(mutex)
            error("pthread_mutex_init failed: $mutexResult")
        }
        val conditionResult = pthread_cond_init(condition, null)
        if (conditionResult != 0) {
            pthread_mutex_destroy(mutex)
            nativeHeap.free(condition)
            nativeHeap.free(mutex)
            error("pthread_cond_init failed: $conditionResult")
        }
    }
}

private fun releaseLock(resources: LockResources) {
    pthread_cond_destroy(resources.condition)
    pthread_mutex_destroy(resources.mutex)
    nativeHeap.free(resources.condition)
    nativeHeap.free(resources.mutex)
}

/** Ticket ordering for fair locks; recursive ownership mirrors the upstream JDK contract. */
public actual class ReentrantLock actual constructor(private val fair: Boolean) {
    private val resources = LockResources()
    @Suppress("unused")
    private val cleaner = createCleaner(resources, ::releaseLock)
    private var owner: pthread_t? = null
    private var holdCount: Int = 0
    private var nextTicket: Long = 0
    private var nowServing: Long = 0

    public actual fun lock() {
        checkPthread(pthread_mutex_lock(resources.mutex), "pthread_mutex_lock")
        try {
            val thread = pthread_self()
            if (owner?.let { pthread_equal(it, thread) != 0 } == true) {
                holdCount++
                return
            }
            val ticket = if (fair) nextTicket++ else 0
            while (owner != null || fair && nowServing != ticket) {
                checkPthread(pthread_cond_wait(resources.condition, resources.mutex), "pthread_cond_wait")
            }
            if (fair) nowServing++
            owner = thread
            holdCount = 1
        } finally {
            checkPthread(pthread_mutex_unlock(resources.mutex), "pthread_mutex_unlock")
        }
    }

    public actual fun unlock() {
        checkPthread(pthread_mutex_lock(resources.mutex), "pthread_mutex_lock")
        try {
            check(owner?.let { pthread_equal(it, pthread_self()) != 0 } == true) { "Not the lock owner" }
            holdCount--
            if (holdCount == 0) {
                owner = null
                checkPthread(pthread_cond_broadcast(resources.condition), "pthread_cond_broadcast")
            }
        } finally {
            checkPthread(pthread_mutex_unlock(resources.mutex), "pthread_mutex_unlock")
        }
    }

    private fun checkPthread(result: Int, operation: String) {
        check(result == 0) { "$operation failed: $result" }
    }
}
