package ai.littlepress.a2ui.compat.java.text

// Android's java.text.SimpleDateFormat (what the official A2UI artifacts use) mostly agrees with
// ICU, which the OHOS Native SDK uses, but not everywhere. This rewrites a pattern for OHOS ICU where
// they differ, as measured on an Android 15 device (tests/integration, PatternLetterProbe):
// unsupported letters throw, 'u' is the ISO day of the week, 'ddd' is zero-padded, 'c' is like 'E',
// 'a' and 'G' always use the short form, 'XXXX' throws, 'ZZZZZ' never prints "Z", 'b' and 'B'
// print nothing, 'z' prints "UTC" for UTC, and an unterminated quote throws.
internal class AndroidDatePattern(pattern: String) {
    private sealed interface Part
    private class Literal(val icu: String) : Part
    private class IsoWeekday(val width: Int) : Part
    private class ShortZoneName(val width: Int) : Part

    private val parts: List<Part> = parse(pattern)

    val dependsOnDate: Boolean = parts.any { it is IsoWeekday }

    /** The OHOS ICU pattern; [isoWeekday] (1 = Monday) is only called for 'u'. */
    fun toIcu(timeZoneId: String, isoWeekday: () -> Int): String = buildString {
        for (part in parts) {
            when (part) {
                is Literal -> append(part.icu)
                is IsoWeekday -> append(isoWeekday().toString().padStart(part.width, '0'))
                is ShortZoneName ->
                    if (timeZoneId == "UTC") append("'UTC'") else append("z".repeat(part.width))
            }
        }
    }

    private companion object {
        const val SUPPORTED = "GyYMLwWDdFEcuaHkKhmsSzZXbB"

        fun parse(pattern: String): List<Part> {
            val parts = mutableListOf<Part>()
            var i = 0
            while (i < pattern.length) {
                val c = pattern[i]
                when {
                    c == '\'' -> {
                        // Quoted text and '' are the same in both pattern languages; copy them as is.
                        // Inside quoted text '' is a quote, so this finds the closing quote too.
                        val close = pattern.indexOf('\'', i + 1)
                        if (close < 0) throw IllegalArgumentException("Unterminated quote")
                        val end = close + 1
                        parts += Literal(pattern.substring(i, end))
                        i = end
                    }
                    c in 'a'..'z' || c in 'A'..'Z' -> {
                        var end = i
                        while (end < pattern.length && pattern[end] == c) end++
                        parts += letter(c, end - i)
                        i = end
                    }
                    else -> {
                        parts += Literal(c.toString())
                        i++
                    }
                }
            }
            return parts
        }

        fun letter(c: Char, width: Int): Part {
            if (c !in SUPPORTED) throw IllegalArgumentException("Illegal pattern character '$c'")
            return when (c) {
                'u' -> IsoWeekday(width)
                'd' -> Literal(if (width >= 3) "0".repeat(width - 2) + "dd" else "d".repeat(width))
                'c' -> Literal("E".repeat(width))
                'a', 'G' -> Literal(c.toString())
                'X' -> if (width >= 4) throw IllegalArgumentException("invalid ISO 8601 format: length=$width")
                    else Literal("X".repeat(width))
                'Z' -> Literal(if (width == 5) "xxx" else "Z".repeat(width))
                'b', 'B' -> Literal("")
                'z' -> if (width <= 3) ShortZoneName(width) else Literal("z".repeat(width))
                else -> Literal(c.toString().repeat(width))
            }
        }
    }
}
