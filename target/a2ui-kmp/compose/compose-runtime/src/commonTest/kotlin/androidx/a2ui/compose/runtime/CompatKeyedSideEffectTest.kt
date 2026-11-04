package androidx.a2ui.compose.runtime

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals

/** Protects the error-reporting semantics of the Compose 1.9 keyed-effect backport. */
class CompatKeyedSideEffectTest {
    @Test
    fun unchangedKeysDoNotRepeatEffectsAfterUnrelatedRecompositions() {
        val recomposer = Recomposer(EmptyCoroutineContext)
        val composition = Composition(NoOpApplier(), recomposer)
        val key = mutableStateOf("invalid-first")
        val unrelated = mutableStateOf(0)
        val effects = mutableListOf<String>()
        val content: @Composable () -> Unit = {
            unrelated.value
            val currentKey = key.value
            CompatKeyedSideEffect(currentKey) { effects.add(currentKey) }
        }
        try {
            composition.setContent(content)
            assertEquals(listOf("invalid-first"), effects)

            unrelated.value++
            composition.setContent(content)
            assertEquals(listOf("invalid-first"), effects)

            key.value = "invalid-second"
            composition.setContent(content)
            assertEquals(listOf("invalid-first", "invalid-second"), effects)

            key.value = "invalid-first"
            composition.setContent(content)
            assertEquals(listOf("invalid-first", "invalid-second", "invalid-first"), effects)
        } finally {
            composition.dispose()
            recomposer.cancel()
        }
    }

    private class NoOpApplier : AbstractApplier<Unit>(Unit) {
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun onClear() = Unit
    }
}
