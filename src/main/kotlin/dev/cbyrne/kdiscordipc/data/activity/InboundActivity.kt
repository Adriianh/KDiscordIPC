package dev.cbyrne.kdiscordipc.data.activity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InboundActivity(
    @SerialName("application_id")
    val applicationId: String = "",
    val assets: Activity.Assets? = null,
    val details: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    val flags: Int? = null,
    val id: String? = null,
    val name: String = "",
    val party: Party? = null,
    @SerialName("session_id")
    val sessionId: String? = null,
    val state: String? = null,
    val timestamps: Timestamps? = null,
    val type: ActivityType = ActivityType.Playing
) {
    @Serializable
    data class Party(
        val id: String? = null
    )

    @Serializable
    data class Timestamps(
        val start: Long? = 0,
        val end: Long? = 0
    )
}