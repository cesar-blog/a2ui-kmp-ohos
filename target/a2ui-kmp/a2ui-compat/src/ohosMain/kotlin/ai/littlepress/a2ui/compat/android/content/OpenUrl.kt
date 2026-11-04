package ai.littlepress.a2ui.compat.android.content

import ai.littlepress.a2ui.compat.platform.OhosA2uiPlatform

internal actual fun openUrl(url: String) {
    OhosA2uiPlatform.openUrl(url)
}
