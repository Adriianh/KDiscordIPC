package dev.cbyrne.kdiscordipc.data.user

import dev.cbyrne.kdiscordipc.core.event.data.EventData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val username: String,
    val discriminator: String = "0",
    @SerialName("global_name") val globalName: String? = null,
    val avatar: String? = null,
    @SerialName("avatar_decoration_data") val avatarDecorationData: AvatarDecorationData? = null,
    val bot: Boolean? = null,
    val flags: Int? = null,
    @SerialName("premium_type") val premiumType: PremiumType? = null
) : EventData() {
    @Serializable
    data class AvatarDecorationData(
        val asset: String,
        @SerialName("sku_id") val skuId: String? = null
    )

    /**
     * The user's effective display name: [globalName] if present, otherwise [username].
     */
    val effectiveName: String
        get() = globalName ?: username

    /**
     * Returns the CDN URL of the user's avatar, or null if the user has no custom avatar.
     * Supports animated avatars (.gif) when the avatar hash starts with "a_".
     */
    fun avatarUrl(size: Int = 128, format: String? = null): String? {
        val hash = avatar ?: return null
        val ext = format ?: if (hash.startsWith("a_")) "gif" else "png"
        return "https://cdn.discordapp.com/avatars/$id/$hash.$ext?size=$size"
    }

    /**
     * Returns the default Discord avatar URL based on the user ID (for migrated users)
     * or the discriminator (for legacy accounts).
     */
    fun defaultAvatarUrl(): String {
        val index = if (discriminator == "0" || discriminator.isEmpty()) {
            val snowflake = id.toLongOrNull() ?: 0L
            ((snowflake shr 22) % 6).toInt()
        } else {
            (discriminator.toIntOrNull() ?: 0) % 5
        }
        return "https://cdn.discordapp.com/embed/avatars/$index.png"
    }

    /**
     * Returns the custom avatar URL if available, otherwise falls back to the default avatar.
     */
    fun effectiveAvatarUrl(size: Int = 128, format: String? = null): String =
        avatarUrl(size, format) ?: defaultAvatarUrl()

    /**
     * Returns the default-sized (128px) effective avatar URL.
     */
    val effectiveAvatarUrl: String
        get() = effectiveAvatarUrl()
}