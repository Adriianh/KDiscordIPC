package dev.cbyrne.kdiscordipc.data.activity

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Controls which field is prioritized and displayed in the user's status in the Discord member list.
 */
@Serializable(with = StatusDisplayType.StatusDisplayTypeSerializer::class)
enum class StatusDisplayType(val value: Int) {
    Name(0),
    State(1),
    Details(2);

    class StatusDisplayTypeSerializer : KSerializer<StatusDisplayType> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("dev.cbyrne.kdiscordipc.data.activity.StatusDisplayType", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): StatusDisplayType {
            val value = decoder.decodeInt()
            return values().firstOrNull { it.value == value } ?: Name
        }

        override fun serialize(encoder: Encoder, value: StatusDisplayType) {
            encoder.encodeInt(value.value)
        }
    }
}
