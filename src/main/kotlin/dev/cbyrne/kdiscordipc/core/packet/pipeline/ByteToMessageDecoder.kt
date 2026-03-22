package dev.cbyrne.kdiscordipc.core.packet.pipeline

import dev.cbyrne.kdiscordipc.KDiscordIPC
import dev.cbyrne.kdiscordipc.core.error.DecodeError
import dev.cbyrne.kdiscordipc.core.packet.inbound.InboundPacket
import dev.cbyrne.kdiscordipc.core.socket.RawPacket
import dev.cbyrne.kdiscordipc.core.util.json
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString

import dev.cbyrne.kdiscordipc.core.packet.inbound.impl.*
import kotlinx.serialization.json.*

object ByteToMessageDecoder {
    fun decode(packet: RawPacket): InboundPacket? {
        try {
            val data = packet.data.decodeToString()
            if (data.isEmpty()) {
                throw DecodeError.InvalidData(null)
            }

            KDiscordIPC.logger.debug("Decoding: $data")

            val jsonElement = json.parseToJsonElement(data)
            val evt = jsonElement.jsonObject["evt"]?.jsonPrimitive?.contentOrNull
            val cmd = jsonElement.jsonObject["cmd"]?.jsonPrimitive?.contentOrNull

            return when (evt) {
                "READY" -> json.decodeFromJsonElement(DispatchEventPacket.Ready.serializer(), jsonElement)
                "CURRENT_USER_UPDATE" -> json.decodeFromJsonElement(DispatchEventPacket.UserUpdate.serializer(), jsonElement)
                "VOICE_SETTINGS_UPDATE" -> json.decodeFromJsonElement(DispatchEventPacket.VoiceSettingsUpdate.serializer(), jsonElement)
                "ACTIVITY_JOIN" -> json.decodeFromJsonElement(DispatchEventPacket.ActivityJoin.serializer(), jsonElement)
                "ACTIVITY_INVITE" -> json.decodeFromJsonElement(DispatchEventPacket.ActivityInvite.serializer(), jsonElement)
                "ERROR" -> json.decodeFromJsonElement(DispatchEventPacket.Error.serializer(), jsonElement)
                else -> when (cmd) {
                    "SET_ACTIVITY" -> json.decodeFromJsonElement(SetActivityPacket.serializer(), jsonElement)
                    "AUTHENTICATE" -> json.decodeFromJsonElement(AuthenticatePacket.serializer(), jsonElement)
                    "AUTHORIZE" -> json.decodeFromJsonElement(AuthorizePacket.serializer(), jsonElement)
                    "GET_VOICE_SETTINGS" -> json.decodeFromJsonElement(GetVoiceSettingsPacket.serializer(), jsonElement)
                    "SET_VOICE_SETTINGS" -> json.decodeFromJsonElement(SetVoiceSettingsPacket.serializer(), jsonElement)
                    "GET_USER" -> json.decodeFromJsonElement(GetUserPacket.serializer(), jsonElement)
                    "GET_RELATIONSHIPS" -> json.decodeFromJsonElement(GetRelationshipsPacket.serializer(), jsonElement)
                    "SUBSCRIBE" -> json.decodeFromJsonElement(SubscribePacket.serializer(), jsonElement)
                    "ACCEPT_ACTIVITY_INVITE" -> json.decodeFromJsonElement(AcceptActivityInvitePacket.serializer(), jsonElement)
                    else -> null
                }
            }
        } catch (e: SerializationException) {
            // We didn't receive the full data, probably because the socket was closed.
            throw DecodeError.InvalidData(e)
        } catch (e: IllegalStateException) {
            if (e.message?.lowercase()?.contains("unknown packet command") == true) {
                return null
            }

            throw e
        } catch (e: Exception) {
            KDiscordIPC.logger.debug(
                "Caught error when decoding packet (op: ${packet.opcode}, length: ${packet.length})",
                e
            )
        }

        return null
    }
}