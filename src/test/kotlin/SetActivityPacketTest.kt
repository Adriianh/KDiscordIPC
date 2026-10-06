package dev.cbyrne.kdiscordipc.test

import dev.cbyrne.kdiscordipc.core.packet.inbound.impl.SetActivityPacket
import dev.cbyrne.kdiscordipc.core.packet.pipeline.ByteToMessageDecoder
import dev.cbyrne.kdiscordipc.core.packet.pipeline.MessageToByteEncoder
import dev.cbyrne.kdiscordipc.core.socket.RawPacket
import dev.cbyrne.kdiscordipc.core.util.json
import dev.cbyrne.kdiscordipc.data.activity.Activity
import dev.cbyrne.kdiscordipc.data.activity.button
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SetActivityPacketTest {

    @Test
    fun testDeserializeSetActivityWithButtonObjects() {
        val jsonPayload = """
            {
                "cmd": "SET_ACTIVITY",
                "data": {
                    "details": "Playing a game",
                    "state": "In lobby",
                    "buttons": [
                        {
                            "label": "Click me",
                            "url": "https://google.com"
                        },
                        {
                            "label": "Join Discord",
                            "url": "https://discord.gg/example"
                        }
                    ],
                    "name": "Game Name",
                    "application_id": "123456789",
                    "type": 0
                },
                "nonce": "test-nonce"
            }
        """.trimIndent()

        val packet = json.decodeFromString<SetActivityPacket>(jsonPayload)
        assertNotNull(packet.data)
        assertEquals("Playing a game", packet.data?.details)
        assertEquals(2, packet.data?.buttons?.size)
        assertEquals("Click me", packet.data?.buttons?.get(0)?.label)
        assertEquals("https://google.com", packet.data?.buttons?.get(0)?.url)
        assertEquals("Join Discord", packet.data?.buttons?.get(1)?.label)
        assertEquals("https://discord.gg/example", packet.data?.buttons?.get(1)?.url)
    }

    @Test
    fun testDeserializeSetActivityWithStringButtons() {
        val jsonPayload = """
            {
                "cmd": "SET_ACTIVITY",
                "data": {
                    "details": "Playing a game",
                    "state": "In lobby",
                    "buttons": [
                        "Click me",
                        "Join Discord"
                    ],
                    "metadata": {
                        "button_urls": [
                            "https://google.com",
                            "https://discord.gg/example"
                        ]
                    },
                    "name": "Game Name",
                    "application_id": "123456789",
                    "type": 0
                },
                "nonce": "test-nonce"
            }
        """.trimIndent()

        val packet = json.decodeFromString<SetActivityPacket>(jsonPayload)
        assertNotNull(packet.data)
        assertEquals(2, packet.data?.buttons?.size)
        assertEquals("Click me", packet.data?.buttons?.get(0)?.label)
        assertEquals("Join Discord", packet.data?.buttons?.get(1)?.label)
        assertEquals(listOf("https://google.com", "https://discord.gg/example"), packet.data?.metadata?.buttonUrls)
    }

    @Test
    fun testDeserializeSetActivityWithEmptyButtonsOrMissingFields() {
        val jsonPayload = """
            {
                "cmd": "SET_ACTIVITY",
                "data": {
                    "details": "No buttons"
                },
                "nonce": "test-nonce"
            }
        """.trimIndent()

        val packet = json.decodeFromString<SetActivityPacket>(jsonPayload)
        assertNotNull(packet.data)
        assertEquals("No buttons", packet.data?.details)
        assertEquals(emptyList(), packet.data?.buttons)
        assertNull(packet.data?.metadata)
    }

    @Test
    fun testDeserializeSetActivityWithNullData() {
        val jsonPayload = """
            {
                "cmd": "SET_ACTIVITY",
                "data": null,
                "nonce": "test-nonce"
            }
        """.trimIndent()

        val packet = json.decodeFromString<SetActivityPacket>(jsonPayload)
        assertNull(packet.data)
    }

    @Test
    fun testByteToMessageDecoderWithButtonObjects() {
        val jsonPayload = """{"cmd":"SET_ACTIVITY","data":{"details":"Test","buttons":[{"label":"Button","url":"https://example.com"}]},"nonce":"123"}"""
        val rawPacket = RawPacket(1, jsonPayload.length, jsonPayload.toByteArray())

        val decoded = ByteToMessageDecoder.decode(rawPacket) as? SetActivityPacket
        assertNotNull(decoded)
        assertEquals(1, decoded.data?.buttons?.size)
        assertEquals("Button", decoded.data?.buttons?.first()?.label)
        assertEquals("https://example.com", decoded.data?.buttons?.first()?.url)
    }

    @Test
    fun testSerializeOutboundActivityWithButtons() {
        val activity = Activity("Playing").apply {
            button("Click me", "https://google.com")
        }

        val jsonString = json.encodeToString(activity)
        val decoded = json.decodeFromString<Activity>(jsonString)
        assertEquals(1, decoded.buttons?.size)
        assertEquals("Click me", decoded.buttons?.first()?.label)
        assertEquals("https://google.com", decoded.buttons?.first()?.url)
    }
}
