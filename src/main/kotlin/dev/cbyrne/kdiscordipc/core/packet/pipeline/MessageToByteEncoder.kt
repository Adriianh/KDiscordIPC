package dev.cbyrne.kdiscordipc.core.packet.pipeline

import dev.cbyrne.kdiscordipc.KDiscordIPC
import dev.cbyrne.kdiscordipc.core.packet.outbound.OutboundPacket
import dev.cbyrne.kdiscordipc.core.packet.outbound.impl.*
import dev.cbyrne.kdiscordipc.core.util.headerLength
import dev.cbyrne.kdiscordipc.core.util.json
import dev.cbyrne.kdiscordipc.core.util.reverse
import kotlinx.serialization.encodeToString
import java.nio.ByteBuffer

object MessageToByteEncoder {
    internal inline fun <reified T : OutboundPacket> encode(packet: T, nonce: String?): ByteArray {
        nonce?.let { packet.nonce = it }

        val data = when (packet) {
            is HandshakePacket -> json.encodeToString(HandshakePacket.serializer(), packet)
            is AuthenticatePacket -> json.encodeToString(AuthenticatePacket.serializer(), packet)
            is GetUserPacket -> json.encodeToString(GetUserPacket.serializer(), packet)
            is GetRelationshipsPacket -> json.encodeToString(GetRelationshipsPacket.serializer(), packet)
            is SubscribePacket -> json.encodeToString(SubscribePacket.serializer(), packet)
            is AcceptActivityInvitePacket -> json.encodeToString(AcceptActivityInvitePacket.serializer(), packet)
            is SetActivityPacket -> json.encodeToString(SetActivityPacket.serializer(), packet)
            is AuthorizePacket -> json.encodeToString(AuthorizePacket.serializer(), packet)
            is CommandPacket -> json.encodeToString(CommandPacket.serializer(), packet)
            is GetVoiceSettingsPacket -> json.encodeToString(GetVoiceSettingsPacket.serializer(), packet)
            is SetVoiceSettingsPacket -> json.encodeToString(SetVoiceSettingsPacket.serializer(), packet)
            else -> json.encodeToString(packet)
        }
        KDiscordIPC.logger.debug("Encoding: $data")

        val bytes = data.encodeToByteArray()
        val buffer = ByteBuffer.allocate(headerLength + bytes.size)
        buffer.putInt(packet.opcode.reverse())
        buffer.putInt(bytes.size.reverse())
        buffer.put(bytes)

        return buffer.array()
    }
}