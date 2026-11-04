package androidx.a2ui.compose.runtime

import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinitionCollection
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.catalog.A2uiFunctionCollection
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.schema.A2uiSchema
import androidx.compose.runtime.snapshotFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout

/** Exercises the official parser/engine on each native target with the pinned CPF runtime. */
class OhosProtocolBoundaryTest {
    @Test
    fun submitReadsEditedValuesAndKeepsOtherSurfacesIsolated() = runTest {
        val processor = a2uiRuntimeMessageProcessor(listOf(TestCatalog))
        val parser = A2uiMessageParser()
        val processing = launch { processor.collectMessages() }
        try {
            processor.processInput(parser, createSurface("booking"))
            processor.processInput(parser, createSurface("other"))
            processor.processInput(parser, update("booking", "/guest", "\"Ada\""))
            processor.processInput(parser, update("booking", "/party", "[\"2\"]"))
            processor.processInput(parser, update("other", "/guest", "\"private-other-surface\""))
            val surfaces = withTimeout(5_000) {
                processor.activeSurfaces.first { it.size == 2 }
            }
            val booking = assertIs<A2uiCoreSurfaceModel>(surfaces.single { it.id == "booking" })
            withTimeout(5_000) {
                snapshotFlow { booking.dataModel[A2uiDataPath("/party")] }
                    .first { it == listOf("2") }
            }
            val eventAwaiter = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(5_000) { processor.outboundEvents.first() }
            }
            // Same write made by a TextField's bindUpdater, before the submit action is dispatched.
            booking.dataModel.update(A2uiDataPath("/guest"), "Grace")
            booking.dispatchAction(
                "submit",
                mapOf(
                    "event" to mapOf(
                        "name" to "book_table",
                        "context" to mapOf(
                            "guest" to mapOf("path" to "/guest"),
                            "party" to mapOf("path" to "/party"),
                        ),
                    )
                ),
            )
            val event = assertIs<A2uiClientEventMessage>(eventAwaiter.await())
            assertEquals("book_table", event.type)
            assertEquals("booking", event.surfaceId)
            assertEquals("submit", event.componentId)
            assertEquals(mapOf("guest" to "Grace", "party" to listOf("2")), event.context)
            assertNull(event.clientDataModel)

            processor.processInput(parser, """{"version":"v0.9","deleteSurface":{"surfaceId":"booking"}}""")
            val remaining = withTimeout(5_000) {
                processor.activeSurfaces.first { surfaces -> surfaces.none { it.id == "booking" } }
            }
            assertEquals(listOf("other"), remaining.map { it.id })

            processor.processInput(parser, createSurface("booking"))
            val recreated = withTimeout(5_000) {
                processor.activeSurfaces.first { surfaces -> surfaces.any { it.id == "booking" } }
            }
            val reopened = assertIs<A2uiCoreSurfaceModel>(recreated.single { it.id == "booking" })
            assertNull(reopened.dataModel[A2uiDataPath("/guest")])
        } finally {
            processing.cancelAndJoin()
        }
    }

    private object TestCatalog : A2uiRuntimeCatalog, A2uiCoreCatalog {
        override val id = "https://kai.example/catalogs/test/v1"
        override val componentDefinitions = A2uiCoreComponentDefinitionCollection()
        override val functions = A2uiFunctionCollection()
        override val themeSchema: A2uiSchema? = null
    }

    private fun createSurface(id: String) =
        """{"version":"v0.9","createSurface":{"surfaceId":"$id","catalogId":"${TestCatalog.id}"}}"""

    private fun update(surface: String, path: String, jsonValue: String) =
        """{"version":"v0.9","updateDataModel":{"surfaceId":"$surface","path":"$path","value":$jsonValue}}"""
}
