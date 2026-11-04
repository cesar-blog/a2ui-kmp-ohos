@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package ai.littlepress.a2ui.compat.platform

import ai.littlepress.a2ui.compat.icu.a2ui_icu_free
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value

internal fun requireCString(value: String): String {
    require('\u0000' !in value) { "Embedded NUL is not supported by the OHOS ICU bridge" }
    return value
}

internal inline fun icuString(operation: String, call: (CPointer<IntVar>) -> CPointer<ByteVar>?): String =
    memScoped {
        val error = alloc<IntVar>()
        error.value = 0
        val result = call(error.ptr)
        try {
            check(error.value <= 0 && result != null) { "$operation failed: ICU status ${error.value}" }
            result.toKString()
        } finally {
            a2ui_icu_free(result)
        }
    }

internal inline fun icuInt(operation: String, call: (CPointer<IntVar>) -> Int): Int = memScoped {
    val error = alloc<IntVar>()
    error.value = 0
    val result = call(error.ptr)
    check(error.value <= 0) { "$operation failed: ICU status ${error.value}" }
    result
}

internal inline fun icuDouble(operation: String, call: (CPointer<IntVar>) -> Double): Double = memScoped {
    val error = alloc<IntVar>()
    error.value = 0
    val result = call(error.ptr)
    check(error.value <= 0) { "$operation failed: ICU status ${error.value}" }
    result
}
