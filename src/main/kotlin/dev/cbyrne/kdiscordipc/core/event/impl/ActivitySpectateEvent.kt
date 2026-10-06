package dev.cbyrne.kdiscordipc.core.event.impl

import dev.cbyrne.kdiscordipc.core.event.Event
import dev.cbyrne.kdiscordipc.core.event.data.ActivitySpectateEventData

data class ActivitySpectateEvent(val data: ActivitySpectateEventData) : Event
