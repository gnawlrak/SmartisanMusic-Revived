package com.smartisanos.music.playback

import androidx.media3.common.MediaItem
import com.smartisanos.music.ExternalAudioMediaIdPrefix
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSessionMediaItemsTest {

    @Test
    fun onlineItemsCanResolveDirectlyWithoutLibraryLookup() {
        val item = MediaItem.Builder()
            .setMediaId("online:netease:12345")
            .build()

        assertTrue(item.canResolveDirectSessionPlaybackItem())
    }

    @Test
    fun externalAudioItemsCanResolveDirectlyWithoutLibraryLookup() {
        val item = MediaItem.Builder()
            .setMediaId("${ExternalAudioMediaIdPrefix}1")
            .build()

        assertTrue(item.canResolveDirectSessionPlaybackItem())
        assertNotNull(listOf(item).resolveDirectSessionPlaybackItemsOrNull())
    }

    @Test
    fun localLibraryItemsStillRequireLibraryLookup() {
        val item = MediaItem.Builder()
            .setMediaId("local-song-id")
            .build()

        assertFalse(item.canResolveDirectSessionPlaybackItem())
        assertNull(listOf(item).resolveDirectSessionPlaybackItemsOrNull())
    }

    @Test
    fun mixedQueuesStillUseLibraryLookupForLocalItems() {
        val onlineItem = MediaItem.Builder()
            .setMediaId("online:netease:12345")
            .build()
        val localItem = MediaItem.Builder()
            .setMediaId("local-song-id")
            .build()

        assertNull(listOf(localItem, onlineItem).resolveDirectSessionPlaybackItemsOrNull())
    }
}
