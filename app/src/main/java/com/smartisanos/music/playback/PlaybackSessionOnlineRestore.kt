package com.smartisanos.music.playback

import androidx.media3.common.MediaItem
import com.smartisanos.music.data.online.OnlineTrack
import com.smartisanos.music.data.online.OnlineTrackIdentity
import com.smartisanos.music.data.online.toMediaItem
import com.smartisanos.music.data.online.withOnlinePlaybackPlaceholderUri

internal fun PlaybackQueueSnapshotItem.hasOnlineDisplayMetadata(): Boolean {
    return title.isNotBlank() &&
        artist.isNotBlank() &&
        (durationMs > 0L || artworkUri.isNotBlank())
}

internal fun PlaybackQueueSnapshotItem.toOnlineSnapshotMediaItem(
    identity: OnlineTrackIdentity,
): MediaItem {
    return toOnlineSnapshotTrack(identity)
        .toMediaItem()
        .withOnlinePlaybackPlaceholderUri()
}

internal fun PlaybackQueueSnapshotItem.toOnlineSnapshotTrack(
    identity: OnlineTrackIdentity,
): OnlineTrack {
    return OnlineTrack(
        source = identity.source,
        trackId = identity.trackId,
        title = title.takeIf(String::isNotBlank) ?: identity.trackId,
        artist = artist,
        album = album.takeIf(String::isNotBlank),
        durationMs = durationMs,
        artworkUrl = artworkUri.takeIf(String::isNotBlank),
    )
}
