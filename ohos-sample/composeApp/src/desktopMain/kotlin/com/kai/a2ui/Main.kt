package com.kai.a2ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        title = "KAI 开造 Lab · A2UI · KMP 2.2.21 / CMP 1.9.2",
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(width = 680.dp, height = 900.dp),
    ) { App() }
}
