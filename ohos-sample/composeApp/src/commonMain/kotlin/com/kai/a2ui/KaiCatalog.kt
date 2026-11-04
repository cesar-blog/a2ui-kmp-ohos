package com.kai.a2ui

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val KAI_CATALOG_ID = "https://kai-lab.example/catalogs/booking/v1"

val KaiCatalog: A2uiCatalog = A2uiCatalog(
    catalogId = KAI_CATALOG_ID,
    components = listOf(Panel, Heading, Label, Input, Party, Action),
)

internal val Ink = Color(0xFF1C2940)
internal val Accent = Color(0xFF1555D7)
internal val Muted = Color(0xFF65738A)

/**
 * A small application-owned catalog. These are ordinary Compose 1.9 components;
 * protocol parsing, data paths, component state and actions remain A2UI's implementation.
 */
private object Panel : A2uiComponent {
    private val children = A2uiProperty.childList("children", required = true)
    override val name = "Panel"
    override val description = "A vertical card of child components."
    override val properties = listOf(children)

    @Composable
    override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
        Column(
            modifier.fillMaxWidth()
                .background(Color.White, RoundedCornerShape(18.dp))
                .border(1.dp, Color(0xFFDDE5F2), RoundedCornerShape(18.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            properties.bindChildReferences(children)?.forEach { child ->
                when (val state = observeA2uiComponentState(child)) {
                    is A2uiComponentState.Success -> A2uiComponent(state.component)
                    is A2uiComponentState.Loading -> BasicText("Loading ${child.id}…")
                    is A2uiComponentState.Error -> BasicText("Component error: ${state.exception.message}")
                }
            }
        }
    }
}

private object Heading : A2uiComponent {
    private val text = A2uiProperty.dynamicString("text", required = true)
    override val name = "Heading"
    override val description = "The card heading."
    override val properties = listOf(text)

    @Composable
    override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
        BasicText(properties.bind(text).orEmpty(), modifier, TextStyle(Ink, 28.sp, FontWeight.Bold))
    }
}

private object Label : A2uiComponent {
    private val text = A2uiProperty.dynamicString("text", required = true)
    override val name = "Label"
    override val description = "A label, optionally bound to the data model."
    override val properties = listOf(text)

    @Composable
    override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
        BasicText(properties.bind(text).orEmpty(), modifier, TextStyle(Muted, 16.sp))
    }
}

private object Input : A2uiComponent {
    private val label = A2uiProperty.dynamicString("label", required = true)
    private val value = A2uiProperty.dynamicString("value", required = true)
    override val name = "Input"
    override val description = "An editable name, bound to an A2UI data-model path."
    override val properties = listOf(label, value)

    @Composable
    override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
        val update = properties.bindUpdater(value)
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicText(properties.bind(label).orEmpty(), style = TextStyle(Muted, 14.sp))
            BasicTextField(
                value = properties.bind(value).orEmpty(),
                onValueChange = { update?.invoke(it) },
                readOnly = update == null,
                modifier = Modifier.fillMaxWidth().testTag("guestName")
                    .border(1.dp, Color(0xFFB6C5DE), RoundedCornerShape(10.dp))
                    .padding(14.dp),
                textStyle = TextStyle(Ink, 20.sp),
                singleLine = true,
            )
        }
    }
}

private object Party : A2uiComponent {
    private val value = A2uiProperty.dynamicString("value", required = true)
    override val name = "Party"
    override val description = "A one-to-four guest selector with two-way data binding."
    override val properties = listOf(value)

    @Composable
    override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
        val selected = properties.bind(value)
        val update = properties.bindUpdater(value)
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BasicText("用餐人数", style = TextStyle(Muted, 14.sp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (1..4).forEach { number ->
                    val current = number.toString()
                    val active = selected == current
                    BasicText(
                        current,
                        Modifier.testTag("party-$number")
                            .background(if (active) Accent else Color(0xFFEDF2FA), RoundedCornerShape(10.dp))
                            .clickable(enabled = update != null, role = Role.Button) {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                update?.invoke(current)
                            }
                            .padding(horizontal = 22.dp, vertical = 12.dp),
                        TextStyle(if (active) Color.White else Ink, 18.sp, FontWeight.Medium),
                    )
                }
            }
        }
    }
}

private object Action : A2uiComponent {
    private val label = A2uiProperty.dynamicString("label", required = true)
    private val action = A2uiProperty.action("action", required = true)
    override val name = "Action"
    override val description = "Dispatch an A2UI event using current bound values."
    override val properties = listOf(label, action)

    @Composable
    override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
        val payload = properties[action]
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        BasicText(
            properties.bind(label).orEmpty(),
            modifier.fillMaxWidth()
                .background(Accent, RoundedCornerShape(12.dp))
                .clickable(enabled = payload != null, role = Role.Button) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    payload?.let(::dispatchAction)
                }
                .padding(horizontal = 20.dp, vertical = 15.dp),
            TextStyle(Color.White, 18.sp, FontWeight.Medium, textAlign = TextAlign.Center),
        )
    }
}
