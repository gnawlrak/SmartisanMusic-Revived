package com.smartisanos.music.playback

import androidx.media3.common.MediaItem
import com.smartisanos.music.data.online.OnlineMusicProvider
import com.smartisanos.music.data.online.OnlineTrackIdentity
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

    @Test
    fun queueItemsStoreRoundTripsOnlineMetadata() {
        val items = listOf(
            PlaybackQueueSnapshotItem(
                mediaId = "online:netease:3395198557",
                stableKey = "online:netease:3395198557",
                title = "After You",
                artist = "Meghan Trainor/AJ Mitchell",
                album = "After You",
                durationMs = 302_000L,
                artworkUri = "https://p1.music.126.net/cover.jpg",
            ),
        )

        val decodedItems = items.encodeQueueItemsForStore().decodeQueueItemsFromStore()

        assertEquals(items, decodedItems)
    }

    @Test
    fun queueItemsStoreStillDecodesLegacyFormat() {
        val decodedItems = "online:netease:3395198557\tonline:netease:3395198557"
            .decodeQueueItemsFromStore()

        assertEquals(
            listOf(
                PlaybackQueueSnapshotItem(
                    mediaId = "online:netease:3395198557",
                    stableKey = "online:netease:3395198557",
                ),
            ),
            decodedItems,
        )
    }

    @Test
    fun onlineSnapshotTrackRestoresMetadata() {
        val track = PlaybackQueueSnapshotItem(
            mediaId = "online:netease:3395198557",
            stableKey = "online:netease:3395198557",
            title = "After You",
            artist = "Meghan Trainor/AJ Mitchell",
            album = "After You",
            durationMs = 302_000L,
            artworkUri = "https://p1.music.126.net/cover.jpg",
        ).toOnlineSnapshotTrack(
            OnlineTrackIdentity(
                source = OnlineMusicProvider.Netease.sourceId,
                trackId = "3395198557",
            ),
        )

        assertEquals("netease", track.source)
        assertEquals("3395198557", track.trackId)
        assertEquals("After You", track.title)
        assertEquals("Meghan Trainor/AJ Mitchell", track.artist)
        assertEquals("After You", track.album)
        assertEquals(302_000L, track.durationMs)
        assertEquals("https://p1.music.126.net/cover.jpg", track.artworkUrl)
    }

    @Test
    fun onlineSnapshotMetadataIsCompleteWhenTitleArtistAndDisplayAssetExist() {
        val item = PlaybackQueueSnapshotItem(
            mediaId = "online:netease:3395198557",
            title = "After You",
            artist = "Meghan Trainor/AJ Mitchell",
            durationMs = 302_000L,
        )

        assertTrue(item.hasOnlineDisplayMetadata())
    }

    @Test
    fun onlineSnapshotMetadataIsIncompleteForLegacyTrackIdOnlyItems() {
        val item = PlaybackQueueSnapshotItem(
            mediaId = "online:netease:3395198557",
            stableKey = "online:netease:3395198557",
        )

        assertFalse(item.hasOnlineDisplayMetadata())
    }
}
