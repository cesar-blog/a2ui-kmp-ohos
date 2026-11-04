package com.kai.a2ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class A2uiDemoTest {
    @Test
    fun editedInputAndPartyAreIncludedInTheRealA2uiEvent() = runComposeUiTest {
        setContent { App() }
        waitUntil(10_000) { onAllNodesWithText("确认预订").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("guestName").performTextReplacement("Lin")
        onNodeWithTag("party-3").performClick()
        onNodeWithText("确认预订", useUnmergedTree = true).performClick()
        waitUntil(10_000) {
            onAllNodesWithText("book_table", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTag("eventLog").assertTextContains("guest=Lin", substring = true)
        onNodeWithTag("eventLog").assertTextContains("party=3", substring = true)
    }

    @Test
    fun incomingDataAndComponentDeltasUpdateTheSurface() = runComposeUiTest {
        setContent { App() }
        onNodeWithText("动态更新").performClick()
        waitUntil(10_000) {
            onAllNodesWithText("可以取餐了").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText("确认取餐").performClick()
        waitUntil(10_000) {
            onAllNodesWithText("confirm_pickup", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTag("eventLog").assertTextContains("status=可以取餐了", substring = true)
    }
}
