@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package ai.littlepress.a2ui.compat.java.util.concurrent.locks

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
import platform.posix.pthread_create
import platform.posix.pthread_detach
import platform.posix.pthread_tVar
import platform.posix.usleep

actual fun startThread(block: () -> Unit) {
    val reference = StableRef.create(block)
    memScoped {
        val thread = alloc<pthread_tVar>()
        val result = pthread_create(
            thread.ptr,
            null,
            staticCFunction { argument: COpaquePointer? ->
                val job = checkNotNull(argument).asStableRef<() -> Unit>()
                try {
                    job.get().invoke()
                } finally {
                    job.dispose()
                }
                null
            },
            reference.asCPointer(),
        )
        if (result != 0) {
            reference.dispose()
            error("pthread_create failed: $result")
        }
        check(pthread_detach(thread.value) == 0) { "pthread_detach failed" }
    }
}

actual fun sleepMillis(millis: Long) {
    require(millis >= 0)
    var remaining = millis
    while (remaining > 0) {
        val chunk = remaining.coerceAtMost(999L)
        check(usleep((chunk * 1000).toUInt()) == 0) { "usleep failed" }
        remaining -= chunk
    }
}
