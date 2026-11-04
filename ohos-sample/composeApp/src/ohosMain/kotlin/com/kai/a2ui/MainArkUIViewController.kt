package com.kai.a2ui

import androidx.compose.ui.window.ComposeArkUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.initMainHandler
import kotlin.experimental.ExperimentalNativeApi
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_env
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_value

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("MainArkUIViewController")
fun MainArkUIViewController(env: napi_env): napi_value {
    initMainHandler(env)
    // Verify ICU inside the HAP process; shell-process permissions can differ.
    // Individual probe failures are reported without preventing the form from opening.
    val probe = runIcuProbe()
    val selfTestRunner = nativeSelfTestRunner()
    return ComposeArkUIViewController(env) {
        App(platformProbe = probe, nativeSelfTestRunner = selfTestRunner)
    }
}
