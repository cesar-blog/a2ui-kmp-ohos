package com.kai.a2ui

/** Ordinary app builds neither link test KLIBs nor expose a test action. */
internal fun nativeSelfTestRunner(): (suspend () -> String)? = null
