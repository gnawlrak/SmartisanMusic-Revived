package com.smartisanos.music.playback

import androidx.media3.common.MediaItem
import com.smartisanos.music.data.online.isOnlineMediaItem
import com.smartisanos.music.data.online.withOnlinePlaybackPlaceholderUri
import com.smartisanos.music.isExternalAudioLaunchItem

internal fun MediaItem.canResolveDirectSessionPlaybackItem(): Boolean {
    return isOnlineMediaItem() || isExternalAudioLaunchItem()
}

internal fun MediaItem.toDirectSessionPlaybackItemOrNull(): MediaItem? {
    return when {
        isOnlineMediaItem() -> withOnlinePlaybackPlaceholderUri()
        isExternalAudioLaunchItem() -> this
        else -> null
    }
}

internal fun List<MediaItem>.resolveDirectSessionPlaybackItemsOrNull(): MutableList<MediaItem>? {
    val resolvedItems = ArrayList<MediaItem>(size)
    for (item in this) {
        resolvedItems += item.toDirectSessionPlaybackItemOrNull() ?: return null
    }
    return resolvedItems
}
