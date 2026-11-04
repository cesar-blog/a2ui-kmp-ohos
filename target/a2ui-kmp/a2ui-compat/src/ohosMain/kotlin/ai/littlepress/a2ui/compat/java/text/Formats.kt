@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package ai.littlepress.a2ui.compat.java.text

import ai.littlepress.a2ui.compat.icu.a2ui_icu_calendar
import ai.littlepress.a2ui.compat.icu.a2ui_icu_date_format
import ai.littlepress.a2ui.compat.icu.a2ui_icu_date_parse
import ai.littlepress.a2ui.compat.icu.a2ui_icu_default_zone
import ai.littlepress.a2ui.compat.icu.a2ui_icu_number_currency
import ai.littlepress.a2ui.compat.icu.a2ui_icu_number_default
import ai.littlepress.a2ui.compat.icu.a2ui_icu_number_format
import ai.littlepress.a2ui.compat.java.util.Currency
import ai.littlepress.a2ui.compat.java.util.Date
import ai.littlepress.a2ui.compat.java.util.Locale
import ai.littlepress.a2ui.compat.java.util.TimeZone
import ai.littlepress.a2ui.compat.java.util.androidId
import ai.littlepress.a2ui.compat.platform.icuDouble
import ai.littlepress.a2ui.compat.platform.icuInt
import ai.littlepress.a2ui.compat.platform.icuString
import ai.littlepress.a2ui.compat.platform.requireCString
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value

public actual class SimpleDateFormat actual constructor(pattern: String, private val locale: Locale) {
    private val androidPattern = AndroidDatePattern(requireCString(pattern))

    public actual var timeZone: TimeZone = TimeZone.getTimeZone(
        icuString("DateFormat.defaultTimeZone") { a2ui_icu_default_zone(it) },
    )
    public actual var isLenient: Boolean = true

    private fun patternFor(millis: Long): String = androidPattern.toIcu(timeZone.id) {
        val day = icuDouble("DateFormat.isoWeekday") {
            a2ui_icu_calendar(timeZone.id, millis.toDouble(), 7, 0, 0, it)
        }.toInt()
        (day + 5) % 7 + 1
    }

    public actual fun format(date: Date): String = icuString("SimpleDateFormat.format") {
        a2ui_icu_date_format(patternFor(date.time), locale.androidId, requireCString(timeZone.id),
            if (isLenient) 1 else 0, date.time.toDouble(), it)
    }

    public actual fun parse(text: String): Date? {
        requireCString(text)
        check(!androidPattern.dependsOnDate) {
            "OHOS SimpleDateFormat parsing of the Java 'u' weekday pattern is not supported"
        }
        return memScoped {
            val error = alloc<IntVar>()
            val position = alloc<IntVar>()
            error.value = 0
            position.value = 0
            val value = a2ui_icu_date_parse(patternFor(0), locale.androidId, requireCString(timeZone.id),
                if (isLenient) 1 else 0, text, position.ptr, error.ptr)
            if (error.value > 0 || position.value == 0) {
                throw ParseException("Unparseable date: \"$text\" (ICU ${error.value})", position.value)
            }
            Date(value.toLong())
        }
    }
}

public actual class NumberFormat private constructor(private val locale: Locale, private val style: Int) {
    private var minimum: Int = nativeDefault(0)
    private var maximum: Int = nativeDefault(1)

    public actual var isGroupingUsed: Boolean = nativeDefault(2) != 0

    public actual var minimumFractionDigits: Int
        get() = minimum
        set(value) {
            minimum = value.coerceAtLeast(0)
            if (minimum > maximum) maximum = minimum
        }

    public actual var maximumFractionDigits: Int
        get() = maximum
        set(value) {
            maximum = value.coerceAtLeast(0)
            if (maximum < minimum) minimum = maximum
        }

    public actual var currency: Currency? = icuString("NumberFormat.currency") {
        a2ui_icu_number_currency(locale.androidId, style, it)
    }.let { if (it.isEmpty()) null else Currency.getInstance(it) }

    public actual fun format(number: Double): String = icuString("NumberFormat.format") {
        a2ui_icu_number_format(locale.androidId, style, if (isGroupingUsed) 1 else 0, minimum, maximum,
            currency?.currencyCode, if (number.isFinite()) number.toString() else null, number, it)
    }

    private fun nativeDefault(attribute: Int): Int = icuInt("NumberFormat.defaults") {
        a2ui_icu_number_default(locale.androidId, style, attribute, it)
    }

    public actual companion object {
        public actual fun getNumberInstance(locale: Locale): NumberFormat = NumberFormat(locale, 0)

        public actual fun getCurrencyInstance(locale: Locale): NumberFormat = NumberFormat(locale, 1)

        public actual fun getPercentInstance(locale: Locale): NumberFormat = NumberFormat(locale, 2)
    }
}
