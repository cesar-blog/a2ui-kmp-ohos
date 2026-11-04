@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

package ai.littlepress.a2ui.compat.java.lang

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlin.native.identityHashCode
import platform.posix.gettimeofday
import platform.posix.timeval

public actual object System {
    public actual fun currentTimeMillis(): Long = memScoped {
        val time = alloc<timeval>()
        check(gettimeofday(time.ptr, null) == 0) { "gettimeofday failed" }
        time.tv_sec.toLong() * 1000 + time.tv_usec.toLong() / 1000
    }

    public actual fun identityHashCode(x: Any?): Int = x.identityHashCode()
}
