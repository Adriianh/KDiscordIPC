package dev.cbyrne.kdiscordipc.data.activity

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ActivityType.ActivityTypeSerializer::class)
enum class ActivityType(val type: Int) {
    Playing(0),
    Streaming(1),
    Listening(2),
    Watching(3),
    Custom(4),
    Competing(5);

    class ActivityTypeSerializer : KSerializer<ActivityType> {
        override val descriptor: SerialDescriptor = Int.serializer().descriptor

        override fun deserialize(decoder: Decoder): ActivityType {
            val type = decoder.decodeInt()
            return values().firstOrNull { it.type == type } ?: Playing
        }

        override fun serialize(encoder: Encoder, value: ActivityType) {
            encoder.encodeInt(value.type)
        }
    }
}
