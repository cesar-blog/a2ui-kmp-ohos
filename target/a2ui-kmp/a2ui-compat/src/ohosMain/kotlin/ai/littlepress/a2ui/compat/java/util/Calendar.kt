@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package ai.littlepress.a2ui.compat.java.util

import ai.littlepress.a2ui.compat.icu.a2ui_icu_calendar
import ai.littlepress.a2ui.compat.java.lang.System
import ai.littlepress.a2ui.compat.platform.icuDouble
import ai.littlepress.a2ui.compat.platform.requireCString

internal actual class PlatformCalendar actual constructor(zoneId: String) {
    private val zone = requireCString(zoneId)

    actual var timeInMillis: Long = System.currentTimeMillis()

    actual fun get(field: Int): Int =
        icuDouble("Calendar.get") { a2ui_icu_calendar(zone, timeInMillis.toDouble(), field, 0, 0, it) }.toInt()

    actual fun set(field: Int, value: Int) {
        timeInMillis = icuDouble("Calendar.set") {
            a2ui_icu_calendar(zone, timeInMillis.toDouble(), field, value, 1, it)
        }.toLong()
    }

    actual fun clear() {
        timeInMillis = icuDouble("Calendar.clear") {
            a2ui_icu_calendar(zone, 0.0, 0, 0, 2, it)
        }.toLong()
    }
}
