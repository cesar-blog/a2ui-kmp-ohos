@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package ai.littlepress.a2ui.compat.java.util

import ai.littlepress.a2ui.compat.icu.a2ui_icu_default_locale
import ai.littlepress.a2ui.compat.icu.a2ui_icu_locale_from_tag
import ai.littlepress.a2ui.compat.icu.a2ui_icu_locale_part
import ai.littlepress.a2ui.compat.platform.OhosA2uiPlatform
import ai.littlepress.a2ui.compat.platform.icuString
import ai.littlepress.a2ui.compat.platform.requireCString
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value

public actual class Locale internal constructor(internal val id: String) {
    override fun equals(other: Any?): Boolean = other is Locale && id == other.id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = id
}

public actual val Locale.language: String
    get() = icuString("Locale.language") { a2ui_icu_locale_part(id, 0, it) }

public actual val Locale.country: String
    get() = icuString("Locale.country") { a2ui_icu_locale_part(id, 1, it) }

public actual object Locales {
    public actual val US: Locale = Locale("en_US")

    public actual fun getDefault(): Locale {
        val configured = OhosA2uiPlatform.localeTag()
        if (configured != null) return forLanguageTag(configured)
        return Locale(checkNotNull(a2ui_icu_default_locale()) { "ICU process locale is unavailable" }.toKString())
    }

    public actual fun forLanguageTag(tag: String): Locale {
        requireCString(tag)
        if (tag.isEmpty()) return Locale("")
        return memScoped {
            val parsed = alloc<IntVar>()
            parsed.value = 0
            val id = icuString("Locale.forLanguageTag") { a2ui_icu_locale_from_tag(tag, parsed.ptr, it) }
            require(parsed.value == tag.length) { "Invalid BCP 47 language tag: $tag" }
            Locale(id)
        }
    }
}

/** Preserve Android's recorded default digit system while honoring explicit Unicode nu extensions. */
internal val Locale.androidId: String
    get() {
        if ("numbers=" in id) return id
        val parts = id.substringBefore('@').split('_')
        val script = parts.drop(1).firstOrNull { it.length == 4 }
        val region = parts.drop(1).firstOrNull {
            it.length == 2 || it.length == 3 && it.all(Char::isDigit)
        }
        val numbering = androidNumbering(parts.firstOrNull().orEmpty(), script, region)
        return id + (if ('@' in id) ";" else "@") + "numbers=$numbering"
    }
