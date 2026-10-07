@file:Suppress("unused")

package dev.cbyrne.kdiscordipc.data.activity

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

@Serializable
data class Activity(
    var details: String? = null,
    var state: String? = null,
    var timestamps: Timestamps? = null,
    var assets: Assets? = null,
    var party: Party? = null,
    var secrets: Secrets? = null,
    var buttons: MutableList<Button>? = null,
    var instance: Boolean? = false,
    var type: ActivityType? = ActivityType.Playing,
    @SerialName("status_display_type")
    var statusDisplayType: StatusDisplayType? = null
) {
    @Serializable
    data class Timestamps(
        var start: Long,
        var end: Long?
    )

    @Serializable
    data class Assets(
        @SerialName("large_image")
        var largeImage: String? = null,
        @SerialName("large_text")
        var largeText: String? = null,
        @SerialName("small_image")
        var smallImage: String? = null,
        @SerialName("small_text")
        var smallText: String? = null
    )

    @Serializable
    data class Party(
        var id: String,
        var size: PartySize
    ) {
        @Serializable(with = PartySize.PartySizeSerializer::class)
        data class PartySize(
            var currentSize: Int,
            var maxSize: Int
        ) {
            class PartySizeSerializer : KSerializer<PartySize> {
                override val descriptor: SerialDescriptor =
                    IntArraySerializer().descriptor

                override fun deserialize(decoder: Decoder) =
                    decoder.decodeStructure(IntArraySerializer().descriptor) {
                        val currentSize = decodeIntElement(Int.serializer().descriptor, 0)
                        val maxSize = decodeIntElement(Int.serializer().descriptor, 1)
                        PartySize(currentSize, maxSize)
                    }

                override fun serialize(encoder: Encoder, value: PartySize) {
                    encoder.encodeCollection(IntArraySerializer().descriptor, 2) {
                        encodeIntElement(Int.serializer().descriptor, 0, value.currentSize)
                        encodeIntElement(Int.serializer().descriptor, 1, value.maxSize)
                    }
                }
            }
        }
    }

    @Serializable
    data class Secrets(
        var join: String? = null,
        var match: String? = null,
        var spectate: String? = null
    )

    @Serializable(with = Button.ButtonSerializer::class)
    data class Button(
        var label: String,
        var url: String = ""
    ) {
        class ButtonSerializer : KSerializer<Button> {
            override val descriptor: SerialDescriptor =
                buildClassSerialDescriptor("dev.cbyrne.kdiscordipc.data.activity.Activity.Button") {
                    element<String>("label")
                    element<String>("url", isOptional = true)
                }

            override fun deserialize(decoder: Decoder): Button {
                return if (decoder is JsonDecoder) {
                    when (val element = decoder.decodeJsonElement()) {
                        is JsonPrimitive -> Button(element.content, "")
                        is JsonObject -> {
                            val label = element["label"]?.jsonPrimitive?.content ?: ""
                            val url = element["url"]?.jsonPrimitive?.content ?: ""
                            Button(label, url)
                        }
                        else -> error("Unexpected JSON element for Button: $element")
                    }
                } else {
                    decoder.decodeStructure(descriptor) {
                        var label = ""
                        var url = ""
                        while (true) {
                            when (val index = decodeElementIndex(descriptor)) {
                                0 -> label = decodeStringElement(descriptor, 0)
                                1 -> url = decodeStringElement(descriptor, 1)
                                CompositeDecoder.DECODE_DONE -> break
                                else -> error("Unexpected index: $index")
                            }
                        }
                        Button(label, url)
                    }
                }
            }

            override fun serialize(encoder: Encoder, value: Button) {
                encoder.encodeStructure(descriptor) {
                    encodeStringElement(descriptor, 0, value.label)
                    encodeStringElement(descriptor, 1, value.url)
                }
            }
        }
    }
}

fun activity(
    details: String? = null,
    state: String? = null,
    init: Activity.() -> Unit
) = Activity(details, state).apply(init)

fun Activity.button(label: String, url: String) {
    if (this.buttons == null)
        this.buttons = mutableListOf()

    this.buttons?.add(Activity.Button(label, url))
}

fun Activity.timestamps(start: Long, end: Long? = null) {
    this.timestamps = Activity.Timestamps(start, end)
}

fun Activity.smallImage(key: String, text: String? = null) {
    if (this.assets == null)
        this.assets = Activity.Assets()

    this.assets?.smallImage = key
    this.assets?.smallText = text
}

fun Activity.largeImage(key: String, text: String? = null) {
    if (this.assets == null)
        this.assets = Activity.Assets()

    this.assets?.largeImage = key
    this.assets?.largeText = text
}

fun Activity.party(id: String, currentSize: Int, maxSize: Int) {
    this.party = Activity.Party(id, Activity.Party.PartySize(currentSize, maxSize))
}

fun Activity.secrets(join: String? = null, match: String? = null, spectate: String? = null) {
    this.secrets = Activity.Secrets(join, match, spectate)
}

fun Activity.type(type: ActivityType) {
    this.type = type
}

fun Activity.statusDisplayType(type: StatusDisplayType) {
    this.statusDisplayType = type
}

fun Activity.statusDisplayType(value: Int) {
    this.statusDisplayType = StatusDisplayType.values().firstOrNull { it.value == value }
}