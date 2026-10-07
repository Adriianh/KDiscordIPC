package dev.cbyrne.kdiscordipc.test

import dev.cbyrne.kdiscordipc.core.util.json
import dev.cbyrne.kdiscordipc.data.activity.Activity
import dev.cbyrne.kdiscordipc.data.activity.StatusDisplayType
import dev.cbyrne.kdiscordipc.data.activity.statusDisplayType
import dev.cbyrne.kdiscordipc.data.user.User
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NewFeaturesTest {

    @Test
    fun testUserWithGlobalNameAndDiscriminatorZero() {
        val jsonPayload = """
            {
                "id": "843135686173392946",
                "username": "adriianh",
                "discriminator": "0",
                "global_name": "Adrián",
                "avatar": "a_1234567890abcdef"
            }
        """.trimIndent()

        val user = json.decodeFromString<User>(jsonPayload)
        assertEquals("843135686173392946", user.id)
        assertEquals("adriianh", user.username)
        assertEquals("0", user.discriminator)
        assertEquals("Adrián", user.globalName)
        assertEquals("Adrián", user.effectiveName)

        // Animated avatar URL (.gif)
        val avatarUrl = user.avatarUrl(size = 256)
        assertEquals("https://cdn.discordapp.com/avatars/843135686173392946/a_1234567890abcdef.gif?size=256", avatarUrl)
        assertEquals(avatarUrl, user.effectiveAvatarUrl(size = 256))
        assertEquals(user.avatarUrl(), user.effectiveAvatarUrl)
    }

    @Test
    fun testUserWithoutGlobalNameFallsBackToUsername() {
        val jsonPayload = """
            {
                "id": "123456789",
                "username": "classic_user",
                "discriminator": "1234",
                "avatar": null
            }
        """.trimIndent()

        val user = json.decodeFromString<User>(jsonPayload)
        assertEquals("classic_user", user.effectiveName)
        assertNull(user.avatarUrl())
        // Default avatar for discriminator 1234 -> index 1234 % 5 = 4
        assertEquals("https://cdn.discordapp.com/embed/avatars/4.png", user.defaultAvatarUrl())
        assertEquals("https://cdn.discordapp.com/embed/avatars/4.png", user.effectiveAvatarUrl)
    }

    @Test
    fun testUserWithAvatarDecorationData() {
        val jsonPayload = """
            {
                "id": "123456789",
                "username": "gamer",
                "avatar_decoration_data": {
                    "asset": "a_deco_hash_123",
                    "sku_id": "987654321"
                }
            }
        """.trimIndent()

        val user = json.decodeFromString<User>(jsonPayload)
        assertNotNull(user.avatarDecorationData)
        assertEquals("a_deco_hash_123", user.avatarDecorationData?.asset)
        assertEquals("987654321", user.avatarDecorationData?.skuId)
        assertEquals("0", user.discriminator)
    }

    @Test
    fun testStatusDisplayTypeSerializationAndDSL() {
        val activity = Activity("Playing Game").apply {
            statusDisplayType(StatusDisplayType.State)
        }

        assertEquals(StatusDisplayType.State, activity.statusDisplayType)

        val encoded = json.encodeToString(activity)
        assertTrue(encoded.contains("\"status_display_type\":1"), "Serialized JSON should contain integer 1 for State")

        val decoded = json.decodeFromString<Activity>(encoded)
        assertEquals(StatusDisplayType.State, decoded.statusDisplayType)
    }

    @Test
    fun testStatusDisplayTypeDetails() {
        val activity = Activity("Playing Game").apply {
            statusDisplayType(StatusDisplayType.Details)
        }

        assertEquals(StatusDisplayType.Details, activity.statusDisplayType)

        val encoded = json.encodeToString(activity)
        assertTrue(encoded.contains("\"status_display_type\":2"), "Serialized JSON should contain integer 2 for Details")

        val decoded = json.decodeFromString<Activity>(encoded)
        assertEquals(StatusDisplayType.Details, decoded.statusDisplayType)
    }

    @Test
    fun testStatusDisplayTypeIntOverload() {
        val activity = Activity().apply {
            statusDisplayType(2)
        }
        assertEquals(StatusDisplayType.Details, activity.statusDisplayType)
    }
}
