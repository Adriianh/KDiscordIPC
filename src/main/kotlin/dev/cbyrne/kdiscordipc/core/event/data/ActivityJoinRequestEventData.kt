package dev.cbyrne.kdiscordipc.core.event.data

import dev.cbyrne.kdiscordipc.data.user.User
import kotlinx.serialization.Serializable

@Serializable
data class ActivityJoinRequestEventData(
    val user: User
) : EventData()
