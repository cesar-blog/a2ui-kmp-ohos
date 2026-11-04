package androidx.a2ui.compose.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember

/**
 * Compose 1.9 has only the unkeyed SideEffect. Keep a non-observable commit marker per key set,
 * so an unchanged error is reported once instead of on every unrelated recomposition. The marker
 * changes only after a successful composition commit; abandoned compositions run no effect.
 */
@Composable
internal fun CompatKeyedSideEffect(vararg keys: Any?, effect: () -> Unit) {
    val committed = remember(*keys) { booleanArrayOf(false) }
    SideEffect {
        if (!committed[0]) {
            committed[0] = true
            effect()
        }
    }
}
