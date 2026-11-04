package com.kai.a2ui

import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun App(platformProbe: String? = null, nativeSelfTestRunner: (suspend () -> String)? = null) {
    var tab by remember { mutableStateOf("预订表单") }
    var replay by remember { mutableStateOf(0) }
    var nativeTestReport by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(nativeSelfTestRunner) {
        nativeSelfTestRunner?.let { runTests ->
            nativeTestReport = "Native 单测：运行中"
            nativeTestReport = try {
                runTests()
            } catch (failure: Throwable) {
                println("A2UI_SELF_TEST RUNNER_FAIL ${failure.toString().replace('\n', ' ')}")
                "Native 单测：运行器失败（见日志）"
            }
        }
    }
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF3F6FC))
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        BasicText("KAI 开造 Lab · A2UI", style = TextStyle(Ink, 30.sp, FontWeight.Bold))
        BasicText("KMP 2.2.21-1.2.0 / CMP 1.9.2-1.2.0", style = TextStyle(Muted, 13.sp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("预订表单", "动态更新").forEach { title ->
                BasicText(
                    title,
                    Modifier.background(if (tab == title) Accent else Color.White, RoundedCornerShape(9.dp))
                        .clickable(role = Role.Tab) { tab = title }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    TextStyle(if (tab == title) Color.White else Ink, 16.sp, FontWeight.Medium),
                )
            }
            if (tab == "动态更新") {
                BasicText("重新播放", Modifier.clickable(role = Role.Button) { replay++ }.padding(10.dp), TextStyle(Accent, 15.sp))
            }
        }
        key(tab, replay) { A2uiDemo(updates = tab == "动态更新") }
        BasicText("预置消息 · 真实 A2UI 解析与渲染 · 尚未连接模型", style = TextStyle(Muted, 12.sp))
        platformProbe?.let { result ->
            BasicText(result, Modifier.testTag("platformProbe"), TextStyle(Muted, 12.sp))
        }
        nativeTestReport?.let { result ->
            BasicText(result, Modifier.testTag("nativeSelfTests"), TextStyle(Muted, 12.sp))
        }
    }
}

@Composable
private fun A2uiDemo(updates: Boolean) {
    val processor = remember { A2uiMessageProcessor(catalogs = listOf(KaiCatalog)) }
    var events by remember { mutableStateOf(listOf<String>()) }
    var received by remember { mutableStateOf(0) }

    LaunchedEffect(processor) {
        // Subscribe synchronously before processing even the first wire message.
        // outboundEvents has no replay; otherwise an early schema error could be missed.
        launch(start = CoroutineStart.UNDISPATCHED) {
            processor.outboundEvents.collect { message ->
                val description = describe(message)
                events = (events + description).takeLast(8)
                println("A2UI_OUTBOUND $description")
            }
        }
        launch { withContext(Dispatchers.Default) { processor.collectMessages() } }
        val parser = A2uiMessageParser()
        val messages = if (updates) AgentMessages.updates else AgentMessages.booking.map { 0L to it }
        messages.forEach { (delayMillis, message) ->
            delay(delayMillis)
            processor.processInput(parser, message)
            received++
        }
    }

    val surfaces by processor.activeSurfaces.collectAsState()
    surfaces.forEach { surface ->
        key(surface.id) {
            when (val state = observeA2uiComponentState(surface)) {
                is A2uiComponentState.Success -> A2uiComponent(state.component)
                is A2uiComponentState.Loading -> BasicText("Loading surface…")
                is A2uiComponentState.Error -> BasicText("Surface error: ${state.exception.message}")
            }
        }
    }
    Column(
        Modifier.fillMaxWidth().background(Color(0xFFE6EDF9), RoundedCornerShape(14.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BasicText("出站事件（本地记录）", style = TextStyle(Ink, 16.sp, FontWeight.Bold))
        BasicText("$received 条协议消息已接收", style = TextStyle(Muted, 12.sp))
        BasicText(
            if (events.isEmpty()) "等待点击按钮" else events.joinToString("\n\n"),
            Modifier.testTag("eventLog"),
            TextStyle(Ink, 13.sp, fontFamily = FontFamily.Monospace),
        )
    }
}

private fun describe(message: A2uiClientToServerMessage): String = when (message) {
    is A2uiClientEventMessage -> "${message.type} from ${message.componentId} on ${message.surfaceId}: ${message.context}"
    is A2uiClientErrorMessage -> "error ${message.code} on ${message.surfaceId}: ${message.message}"
}
