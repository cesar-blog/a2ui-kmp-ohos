@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package ai.littlepress.a2ui.compat.java.text

import ai.littlepress.a2ui.compat.icu.a2ui_icu_date_pattern
import ai.littlepress.a2ui.compat.icu.a2ui_icu_locale_part
import ai.littlepress.a2ui.compat.java.util.Locale
import ai.littlepress.a2ui.compat.platform.icuString

internal actual fun datePattern(style: Int, locale: Locale): String =
    icuString("DateFormat.datePattern") { a2ui_icu_date_pattern(locale.id, style, 0, it) }

internal actual fun timePattern(style: Int, locale: Locale): String =
    icuString("DateFormat.timePattern") { a2ui_icu_date_pattern(locale.id, style, 1, it) }

internal actual val Locale.languageTag: String
    get() = icuString("Locale.languageTag") { a2ui_icu_locale_part(id, 2, it) }
