package dev.cbyrne.kdiscordipc.core.event.data

import kotlinx.serialization.Serializable

@Serializable
data class ActivitySpectateEventData(
    val secret: String
) : EventData()
