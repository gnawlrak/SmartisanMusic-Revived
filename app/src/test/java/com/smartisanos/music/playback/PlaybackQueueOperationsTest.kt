package com.smartisanos.music.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackQueueOperationsTest {

    @Test
    fun playableQueueFilterRemapsStartIndexAfterDroppingUnresolvedOnlineItems() {
        val result = filterPlayableQueueItemStates(
            itemStates = listOf(
                onlineItem(hasPlaybackUri = false),
                onlineItem(hasPlaybackUri = true),
                onlineItem(hasPlaybackUri = false),
                onlineItem(hasPlaybackUri = true),
            ),
            startIndex = 1,
        )

        assertEquals(
            PlaybackQueueFilterState(
                keptIndices = listOf(1, 3),
                startIndex = 0,
            ),
            result,
        )
    }

    @Test
    fun playableQueueFilterRejectsQueueWhenSelectedOnlineItemHasNoPlaybackUri() {
        val result = filterPlayableQueueItemStates(
            itemStates = listOf(
                onlineItem(hasPlaybackUri = true),
                onlineItem(hasPlaybackUri = false),
                onlineItem(hasPlaybackUri = true),
            ),
            startIndex = 1,
        )

        assertNull(result)
    }

    @Test
    fun playableQueueFilterKeepsLocalItemsWithoutPlaybackUri() {
        val result = filterPlayableQueueItemStates(
            itemStates = listOf(
                localItem(hasPlaybackUri = false),
                onlineItem(hasPlaybackUri = true),
            ),
            startIndex = 0,
        )

        assertEquals(
            PlaybackQueueFilterState(
                keptIndices = listOf(0, 1),
                startIndex = 0,
            ),
            result,
        )
    }

    @Test
    fun playableQueueFilterCoercesStartIndexBeforeMapping() {
        val result = filterPlayableQueueItemStates(
            itemStates = listOf(onlineItem(hasPlaybackUri = true)),
            startIndex = 10,
        )

        assertEquals(
            PlaybackQueueFilterState(
                keptIndices = listOf(0),
                startIndex = 0,
            ),
            result,
        )
    }

    private fun onlineItem(hasPlaybackUri: Boolean): PlaybackQueueItemFilterState {
        return PlaybackQueueItemFilterState(
            isOnline = true,
            hasPlaybackUri = hasPlaybackUri,
        )
    }

    private fun localItem(hasPlaybackUri: Boolean): PlaybackQueueItemFilterState {
        return PlaybackQueueItemFilterState(
            isOnline = false,
            hasPlaybackUri = hasPlaybackUri,
        )
    }
}
