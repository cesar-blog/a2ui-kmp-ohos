package ai.littlepress.a2ui.compat

import ai.littlepress.a2ui.compat.android.util.JsonReader
import ai.littlepress.a2ui.compat.android.util.JsonToken
import ai.littlepress.a2ui.compat.android.util.MalformedJsonException
import ai.littlepress.a2ui.compat.java.io.StringReader
import ai.littlepress.a2ui.compat.java.lang.System
import ai.littlepress.a2ui.compat.java.text.languageTag
import ai.littlepress.a2ui.compat.java.util.Locales
import ai.littlepress.a2ui.compat.java.util.country
import ai.littlepress.a2ui.compat.java.util.language
import ai.littlepress.a2ui.compat.java.util.concurrent.atomic.AtomicInteger
import ai.littlepress.a2ui.compat.java.util.concurrent.locks.ReentrantLock
import ai.littlepress.a2ui.compat.java.util.concurrent.locks.sleepMillis
import ai.littlepress.a2ui.compat.java.util.concurrent.locks.startThread
import ai.littlepress.a2ui.compat.kotlin.concurrent.withLock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/** These tests do not require access to ICU date/number data files in the shell process. */
class OhosCompatTest {
    @Test
    fun localePreservesScriptRegionAndNumberingExtensionAndRejectsMalformedTags() {
        val chinese = Locales.forLanguageTag("zh-Hans-CN")
        assertEquals("zh", chinese.language)
        assertEquals("CN", chinese.country)
        assertEquals("zh-Hans-CN", chinese.languageTag)

        val explicitDigits = Locales.forLanguageTag("ar-EG-u-nu-latn")
        assertEquals("ar", explicitDigits.language)
        assertEquals("EG", explicitDigits.country)
        assertEquals("ar-EG-u-nu-latn", explicitDigits.languageTag)
        assertEquals(Locales.US, Locales.forLanguageTag("en-US"))
        assertFailsWith<IllegalArgumentException> { Locales.forLanguageTag("en_US") }
    }

    @Test
    fun reentrantLockSerializesContendedNativeThreadsAndFullyReleasesOwnership() {
        val lock = ReentrantLock(true)
        val completed = AtomicInteger(0)
        val failures = AtomicInteger(0)
        val active = AtomicInteger(0)
        val state = Counter()
        val threadCount = 4
        val iterations = 1500

        repeat(threadCount) {
            startThread {
                try {
                    repeat(iterations) {
                        lock.withLock {
                            // A second acquisition must preserve ownership until both releases.
                            lock.withLock {
                                if (active.incrementAndGet() != 1) failures.incrementAndGet()
                                state.value++
                                active.decrementAndGet()
                            }
                        }
                    }
                } catch (_: Throwable) {
                    failures.incrementAndGet()
                } finally {
                    completed.incrementAndGet()
                }
            }
        }
        val deadline = System.currentTimeMillis() + 10_000
        while (completed.get() != threadCount && System.currentTimeMillis() < deadline) sleepMillis(1)
        assertEquals(threadCount, completed.get(), "Native lock contenders did not finish")
        assertEquals(0, failures.get(), "Mutual exclusion or recursive ownership failed")
        assertEquals(threadCount * iterations, lock.withLock { state.value })
        assertEquals(0, active.get())
        assertFailsWith<IllegalStateException> { lock.unlock() }
    }

    @Test
    fun jsonReaderSkipsUnknownNestedPayloadAndPreservesTokenAfterConversionFailure() {
        val reader = JsonReader(StringReader("""
            [{"surfaceId":"booking",
              "unknown":{"nested":[true,null,{"ignore":"\u4e2d"}]},
              "value":{"guest":"A\u0064a","party":["2"]}},"NaN"]
        """.trimIndent()))
        reader.beginArray()
        reader.beginObject()
        assertEquals("surfaceId", reader.nextName())
        assertEquals("booking", reader.nextString())
        assertEquals("unknown", reader.nextName())
        reader.skipValue()
        assertEquals("value", reader.nextName())
        reader.beginObject()
        assertEquals("guest", reader.nextName())
        assertEquals("Ada", reader.nextString())
        assertEquals("party", reader.nextName())
        reader.beginArray()
        assertEquals(2, reader.nextInt())
        assertFalse(reader.hasNext())
        reader.endArray()
        reader.endObject()
        reader.endObject()
        assertFailsWith<NumberFormatException> { reader.nextDouble() }
        assertEquals(JsonToken.STRING, reader.peek())
        assertEquals("NaN", reader.nextString())
        reader.endArray()
        assertEquals(JsonToken.END_DOCUMENT, reader.peek())

        val malformed = JsonReader(StringReader("{\"broken\":[1,]}"))
        assertFailsWith<MalformedJsonException> { malformed.skipValue() }
    }

    private class Counter(var value: Int = 0)
}
