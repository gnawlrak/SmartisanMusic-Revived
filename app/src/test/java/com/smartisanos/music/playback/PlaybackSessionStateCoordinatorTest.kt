package com.smartisanos.music.playback

import androidx.media3.common.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSessionStateCoordinatorTest {

    @Test
    fun restoredQueueDropsUnresolvedOnlineItemsWithoutPlaybackUri() {
        val unresolved = MediaItem.Builder()
            .setMediaId("online:netease:123")
            .build()

        val restoredItems = listOf(unresolved).filterRestorablePlaybackItems()

        assertEquals(emptyList<MediaItem>(), restoredItems)
    }

    @Test
    fun restorablePlaybackItemRequiresPlaybackUri() {
        assertTrue(isRestorablePlaybackItemState(hasPlaybackUri = true))
        assertFalse(isRestorablePlaybackItemState(hasPlaybackUri = false))
    }
}
