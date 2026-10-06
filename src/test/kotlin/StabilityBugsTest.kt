package dev.cbyrne.kdiscordipc.test

import dev.cbyrne.kdiscordipc.KDiscordIPC
import dev.cbyrne.kdiscordipc.core.error.IPCError
import dev.cbyrne.kdiscordipc.core.event.data.ActivityJoinRequestEventData
import dev.cbyrne.kdiscordipc.core.event.data.ActivitySpectateEventData
import dev.cbyrne.kdiscordipc.core.event.data.VoiceChannelSelectEventData
import dev.cbyrne.kdiscordipc.core.packet.inbound.impl.DispatchEventPacket
import dev.cbyrne.kdiscordipc.core.packet.outbound.impl.HandshakePacket
import dev.cbyrne.kdiscordipc.core.packet.pipeline.ByteToMessageDecoder
import dev.cbyrne.kdiscordipc.core.socket.RawPacket
import dev.cbyrne.kdiscordipc.core.socket.Socket
import dev.cbyrne.kdiscordipc.core.util.json
import dev.cbyrne.kdiscordipc.core.util.temporaryDirectory
import dev.cbyrne.kdiscordipc.data.activity.InboundActivity
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StabilityBugsTest {

    @Test
    fun testTemporaryDirectoryIsNotNull() {
        assertTrue(temporaryDirectory.isNotEmpty(), "temporaryDirectory should not be empty")
    }

    @Test
    fun testVoiceChannelSelectWithNullFields() {
        val jsonPayload = """
            {
                "channel_id": null,
                "guild_id": null
            }
        """.trimIndent()

        val data = json.decodeFromString<VoiceChannelSelectEventData>(jsonPayload)
        assertNull(data.channelId)
        assertNull(data.guildId)
    }

    @Test
    fun testVoiceChannelSelectWithValues() {
        val jsonPayload = """
            {
                "channel_id": "123456789",
                "guild_id": "987654321"
            }
        """.trimIndent()

        val data = json.decodeFromString<VoiceChannelSelectEventData>(jsonPayload)
        assertEquals("123456789", data.channelId)
        assertEquals("987654321", data.guildId)
    }

    @Test
    fun testInboundActivityWithMinimalFields() {
        val jsonPayload = """
            {
                "application_id": "123",
                "id": "act-1",
                "name": "Minimal Game"
            }
        """.trimIndent()

        val activity = json.decodeFromString<InboundActivity>(jsonPayload)
        assertEquals("123", activity.applicationId)
        assertEquals("act-1", activity.id)
        assertEquals("Minimal Game", activity.name)
        assertNull(activity.assets)
        assertNull(activity.party)
        assertNull(activity.state)
        assertNull(activity.sessionId)
    }

    @Test
    fun testByteToMessageDecoderDecodesNewEvents() {
        // VOICE_CHANNEL_SELECT
        val voiceSelectJson = """{"cmd":"DISPATCH","evt":"VOICE_CHANNEL_SELECT","data":{"channel_id":"123","guild_id":"456"}}"""
        val voicePacket = ByteToMessageDecoder.decode(RawPacket(1, voiceSelectJson.length, voiceSelectJson.toByteArray()))
        assertTrue(voicePacket is DispatchEventPacket.VoiceChannelSelect)
        assertEquals("123", voicePacket.data.channelId)

        // ACTIVITY_SPECTATE
        val spectateJson = """{"cmd":"DISPATCH","evt":"ACTIVITY_SPECTATE","data":{"secret":"spectate-secret-123"}}"""
        val spectatePacket = ByteToMessageDecoder.decode(RawPacket(1, spectateJson.length, spectateJson.toByteArray()))
        assertTrue(spectatePacket is DispatchEventPacket.ActivitySpectate)
        assertEquals("spectate-secret-123", spectatePacket.data.secret)

        // ACTIVITY_JOIN_REQUEST
        val joinReqJson = """{"cmd":"DISPATCH","evt":"ACTIVITY_JOIN_REQUEST","data":{"user":{"id":"999","username":"Gamer","discriminator":"0001"}}}"""
        val joinReqPacket = ByteToMessageDecoder.decode(RawPacket(1, joinReqJson.length, joinReqJson.toByteArray()))
        assertTrue(joinReqPacket is DispatchEventPacket.ActivityJoinRequest)
        assertEquals("999", joinReqPacket.data.user.id)
        assertEquals("Gamer", joinReqPacket.data.user.username)
    }

    @Test
    fun testSendPacketTimeout() = runBlocking {
        val dummySocket = object : Socket {
            override val connected: Boolean = true
            override fun connect(file: File) {}
            override fun read(): RawPacket {
                Thread.sleep(10000)
                throw java.io.EOFException()
            }
            override fun write(bytes: ByteArray) {}
            override fun close() {}
        }

        val ipc = KDiscordIPC("12345", socketSupplier = { dummySocket }, scope = this)

        // sendPacket should timeout after 200ms
        assertFailsWith<TimeoutCancellationException> {
            ipc.sendPacket<DispatchEventPacket.Ready>(HandshakePacket(1, "12345"), timeoutMs = 200L)
        }
    }
}
