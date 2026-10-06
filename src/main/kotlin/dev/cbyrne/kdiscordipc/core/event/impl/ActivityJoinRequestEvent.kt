package dev.cbyrne.kdiscordipc.core.event.impl

import dev.cbyrne.kdiscordipc.core.event.Event
import dev.cbyrne.kdiscordipc.core.event.data.ActivityJoinRequestEventData

data class ActivityJoinRequestEvent(val data: ActivityJoinRequestEventData) : Event
