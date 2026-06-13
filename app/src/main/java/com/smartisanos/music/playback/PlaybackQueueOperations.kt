package com.smartisanos.music.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.smartisanos.music.data.online.isOnlineMediaItem
import com.smartisanos.music.isExternalAudioLaunchItem
import kotlin.random.Random

internal fun Player?.replaceQueueAndPlay(
    mediaItems: List<MediaItem>,
    startIndex: Int = 0,
    shuffleModeEnabled: Boolean = false,
) {
    val player = this ?: return
    val playableQueue = mediaItems.filterPlayableQueueItems(startIndex) ?: return
    player.shuffleModeEnabled = shuffleModeEnabled
    player.setMediaItems(playableQueue.mediaItems, playableQueue.startIndex, 0L)
    player.prepare()
    player.play()
}

internal fun Player?.replaceQueueAndPlayShuffled(
    mediaItems: List<MediaItem>,
    random: Random = Random.Default,
) {
    val player = this ?: return
    if (mediaItems.isEmpty()) {
        return
    }
    player.replaceQueueAndPlay(
        mediaItems = mediaItems,
        startIndex = random.nextInt(mediaItems.size),
        shuffleModeEnabled = true,
    )
}

internal data class PlaybackQueueFilterState(
    val keptIndices: List<Int>,
    val startIndex: Int,
)

internal data class PlaybackQueueItemFilterState(
    val isOnline: Boolean,
    val hasPlaybackUri: Boolean,
)

internal fun filterPlayableQueueItemStates(
    itemStates: List<PlaybackQueueItemFilterState>,
    startIndex: Int,
): PlaybackQueueFilterState? {
    if (itemStates.isEmpty()) {
        return null
    }
    val safeStartIndex = startIndex.coerceIn(0, itemStates.lastIndex)
    val keptIndices = mutableListOf<Int>()
    var mappedStartIndex = -1
    itemStates.forEachIndexed { index, itemState ->
        val keepItem = !itemState.isOnline || itemState.hasPlaybackUri
        if (!keepItem) {
            return@forEachIndexed
        }
        if (index == safeStartIndex) {
            mappedStartIndex = keptIndices.size
        }
        keptIndices += index
    }
    if (mappedStartIndex < 0) {
        return null
    }
    return PlaybackQueueFilterState(
        keptIndices = keptIndices,
        startIndex = mappedStartIndex,
    )
}

private data class PlayableQueueItems(
    val mediaItems: List<MediaItem>,
    val startIndex: Int,
)

private fun List<MediaItem>.filterPlayableQueueItems(startIndex: Int): PlayableQueueItems? {
    val filterState = filterPlayableQueueItemStates(
        itemStates = map { item ->
            PlaybackQueueItemFilterState(
                isOnline = item.isOnlineMediaItem(),
                hasPlaybackUri = item.localConfiguration?.uri != null,
            )
        },
        startIndex = startIndex,
    ) ?: return null
    return PlayableQueueItems(
        mediaItems = filterState.keptIndices.map(::get),
        startIndex = filterState.startIndex,
    )
}

internal val MediaItem.stableKey: String?
    get() = mediaMetadata.extras
        ?.getString(LocalAudioLibrary.StableKeyExtraKey)
        ?.trim()
        ?.takeIf(String::isNotEmpty)

internal fun Player.deduplicateQueueCandidates(candidates: List<MediaItem>): List<MediaItem> {
    if (candidates.isEmpty()) {
        return emptyList()
    }
    val seenStableKeys = linkedSetOf<String>()
    val seenMediaIds = linkedSetOf<String>()
    val seenMediaIdsWithoutStableKey = linkedSetOf<String>()
    for (index in 0 until mediaItemCount) {
        val item = getMediaItemAt(index)
        val mediaId = item.mediaId.trim().takeIf(String::isNotEmpty)
        val stableKey = item.stableKey
        stableKey?.let(seenStableKeys::add)
        mediaId?.let(seenMediaIds::add)
        if (stableKey == null) {
            mediaId?.let(seenMediaIdsWithoutStableKey::add)
        }
    }
    return candidates.filter { item ->
        if (item.isExternalAudioLaunchItem()) {
            return@filter true
        }
        val stableKey = item.stableKey
        val mediaId = item.mediaId.trim()
        val normalizedMediaId = mediaId.takeIf(String::isNotEmpty)
        val alreadyQueued = if (stableKey != null) {
            stableKey in seenStableKeys ||
                normalizedMediaId?.let(seenMediaIdsWithoutStableKey::contains) == true
        } else {
            normalizedMediaId?.let(seenMediaIds::contains) == true
        }
        if (alreadyQueued) {
            false
        } else {
            stableKey?.let(seenStableKeys::add)
            normalizedMediaId?.let(seenMediaIds::add)
            if (stableKey == null) {
                normalizedMediaId?.let(seenMediaIdsWithoutStableKey::add)
            }
            true
        }
    }
}

internal fun Player?.removeMediaItemsByMediaIds(mediaIds: Set<String>) {
    val player = this ?: return
    val normalizedMediaIds = mediaIds.asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .toSet()
    if (normalizedMediaIds.isEmpty()) {
        return
    }

    for (index in player.mediaItemCount - 1 downTo 0) {
        if (player.getMediaItemAt(index).mediaId in normalizedMediaIds) {
            player.removeMediaItem(index)
        }
    }
}

internal fun Player?.removeMediaItemsMatching(predicate: (MediaItem) -> Boolean) {
    val player = this ?: return
    var rangeEnd = player.mediaItemCount
    var index = rangeEnd - 1
    while (index >= 0) {
        if (!predicate(player.getMediaItemAt(index))) {
            rangeEnd = index
            index -= 1
            continue
        }

        var rangeStart = index
        while (rangeStart > 0 && predicate(player.getMediaItemAt(rangeStart - 1))) {
            rangeStart -= 1
        }
        player.removeMediaItems(rangeStart, rangeEnd)
        rangeEnd = rangeStart
        index = rangeStart - 1
    }
}

internal fun MediaItem.withPlaybackRating(score: Int): MediaItem {
    val normalizedScore = score.coerceIn(0, 5)
    val extras = Bundle(mediaMetadata.extras ?: Bundle()).apply {
        RatingExtraKeys.forEach(::remove)
        if (normalizedScore > 0) {
            putLong(LocalAudioLibrary.RatingExtraKey, normalizedScore.toLong())
        }
    }
    val metadata = mediaMetadata.buildUpon()
        .setExtras(extras)
        .build()
    return buildUpon()
        .setMediaMetadata(metadata)
        .build()
}

private val RatingExtraKeys = listOf(
    LocalAudioLibrary.RatingExtraKey,
    "star",
    "score",
    "rating",
    "play_score",
)
