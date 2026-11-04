package ai.littlepress.a2ui.compat.android.text.format

import ai.littlepress.a2ui.compat.java.text.DateFormat
import ai.littlepress.a2ui.compat.java.text.timePattern
import ai.littlepress.a2ui.compat.java.util.Locales
import ai.littlepress.a2ui.compat.platform.OhosA2uiPlatform

internal actual fun platformIs24HourFormat(): Boolean =
    OhosA2uiPlatform.is24Hour() ?: hasDesignator(timePattern(DateFormat.LONG, Locales.getDefault()), 'H')
