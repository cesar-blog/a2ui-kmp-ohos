package ai.littlepress.a2ui.compat.platform

import ai.littlepress.a2ui.compat.java.util.concurrent.locks.ReentrantLock

/**
 * The application supplies Ability/ArkTS services that have no portable C Native SDK equivalent.
 * Register callbacks on application startup. URL callbacks must dispatch to the Ability thread
 * themselves; exceptions are propagated to the caller. No URL is silently discarded.
 *
 * Without preference callbacks the library uses ICU's process locale and that locale's clock
 * convention. Those are defaults, not a claim to have read the user's HarmonyOS preferences.
 */
public object OhosA2uiPlatform {
    private val lock = ReentrantLock(false)
    private var urlOpener: ((String) -> Unit)? = null
    private var localeProvider: (() -> String)? = null
    private var clockPreference: (() -> Boolean)? = null

    public fun configure(
        openUrl: (String) -> Unit,
        localeTag: () -> String,
        is24Hour: () -> Boolean,
    ) {
        locked {
            urlOpener = openUrl
            localeProvider = localeTag
            clockPreference = is24Hour
        }
    }

    internal fun openUrl(url: String) {
        val opener = locked { urlOpener }
        checkNotNull(opener) {
            "Register OhosA2uiPlatform.configure with an Ability URL opener before using openUrl"
        }(url)
    }

    internal fun localeTag(): String? = locked { localeProvider }?.invoke()

    internal fun is24Hour(): Boolean? = locked { clockPreference }?.invoke()

    private inline fun <T> locked(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }
}
