package com.kai.a2ui

import ai.littlepress.a2ui.compat.java.text.NumberFormat
import ai.littlepress.a2ui.compat.java.text.SimpleDateFormat
import ai.littlepress.a2ui.compat.java.util.Date
import ai.littlepress.a2ui.compat.java.util.Locales
import ai.littlepress.a2ui.compat.java.util.TimeZone
import ai.littlepress.a2ui.compat.java.util.country
import ai.littlepress.a2ui.compat.java.util.language

/** Runs the compatibility APIs in the actual app process, independently of rendering. */
internal fun runIcuProbe(): String {
    val checks = listOf(
        probe("locale", "地区", "zh-CN") {
            val locale = Locales.forLanguageTag("zh-CN")
            "${locale.language}-${locale.country}"
        },
        probe("number", "数字", "1,234.5") {
            NumberFormat.getNumberInstance(Locales.US).format(1234.5)
        },
        probe("date", "日期", "2024-01-01") {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locales.US)
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            formatter.format(Date(1704067200000L))
        },
    )
    val passed = checks.count { it.second }
    println("A2UI_ICU_PROBE SUMMARY ${if (passed == checks.size) "PASS" else "FAIL"} $passed/${checks.size}")
    return "ICU 平台验证：$passed/${checks.size} 通过 · " +
        checks.joinToString(" · ") { (label, success) -> "$label ${if (success) "✓" else "失败"}" }
}

private inline fun probe(
    name: String,
    label: String,
    expected: String,
    operation: () -> String,
): Pair<String, Boolean> {
    return try {
        val actual = operation()
        val matches = actual == expected
        println("A2UI_ICU_PROBE $name ${if (matches) "PASS" else "FAIL"} value=$actual expected=$expected")
        label to matches
    } catch (failure: Throwable) {
        println("A2UI_ICU_PROBE $name FAIL ${failure.toString().replace('\n', ' ')}")
        label to false
    }
}
