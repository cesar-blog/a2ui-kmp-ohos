package com.kai.a2ui

import ai.littlepress.a2ui.compat.OhosCompatTest
import androidx.a2ui.compose.runtime.CompatKeyedSideEffectTest
import androidx.a2ui.compose.runtime.OhosProtocolBoundaryTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Enabled only with -PwithNativeSelfTests=true, using compiled upstream test KLIBs. */
internal fun nativeSelfTestRunner(): (suspend () -> String)? = ::runNativeSelfTests

private suspend fun runNativeSelfTests(): String = withContext(Dispatchers.Default) {
    val cases: List<Pair<String, () -> Unit>> = listOf(
        "compat.locale" to {
            OhosCompatTest().localePreservesScriptRegionAndNumberingExtensionAndRejectsMalformedTags()
        },
        "compat.nativeLock" to {
            OhosCompatTest().reentrantLockSerializesContendedNativeThreadsAndFullyReleasesOwnership()
        },
        "compat.jsonReader" to {
            OhosCompatTest().jsonReaderSkipsUnknownNestedPayloadAndPreservesTokenAfterConversionFailure()
        },
        "runtime.keyedSideEffect" to {
            CompatKeyedSideEffectTest().unchangedKeysDoNotRepeatEffectsAfterUnrelatedRecompositions()
        },
        "runtime.protocolBoundary" to {
            OhosProtocolBoundaryTest().submitReadsEditedValuesAndKeepsOtherSurfacesIsolated()
        },
    )
    var passed = 0
    val failed = mutableListOf<String>()
    cases.forEach { (name, runTest) ->
        println("A2UI_SELF_TEST START $name")
        try {
            runTest()
            passed++
            println("A2UI_SELF_TEST PASS $name")
        } catch (failure: Throwable) {
            failed += name
            println("A2UI_SELF_TEST FAIL $name ${failure.toString().replace('\n', ' ')}")
        }
    }
    println("A2UI_SELF_TEST SUMMARY ${if (failed.isEmpty()) "PASS" else "FAIL"} $passed/${cases.size}")
    "Native 单测：$passed/${cases.size} 通过" +
        if (failed.isEmpty()) "" else " · 失败：${failed.joinToString()}"
}
