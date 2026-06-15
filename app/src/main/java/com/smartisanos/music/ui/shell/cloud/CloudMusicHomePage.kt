package com.smartisanos.music.ui.shell.cloud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartisanos.music.R
import com.smartisanos.music.data.online.OnlineAlbum
import com.smartisanos.music.data.online.OnlineArtist
import com.smartisanos.music.data.online.OnlineArtistIntroduction
import com.smartisanos.music.data.online.OnlineMusicHome
import com.smartisanos.music.data.online.OnlinePlaylist
import com.smartisanos.music.data.online.OnlineRadio
import com.smartisanos.music.data.online.OnlineTrack
import com.smartisanos.music.ui.shell.cloud.components.CloudHomeCoverCard
import com.smartisanos.music.ui.shell.cloud.components.CloudHomeCoverSection
import com.smartisanos.music.ui.shell.cloud.components.CloudHomeSectionHeader
import com.smartisanos.music.ui.shell.cloud.components.CloudHomeAnimatedSection
import com.smartisanos.music.ui.shell.cloud.components.CloudMusicBlankState
import com.smartisanos.music.ui.shell.cloud.components.CloudMusicCoverImage
import com.smartisanos.music.ui.shell.cloud.components.CloudMusicDelayedLoadingState
import com.smartisanos.music.ui.shell.cloud.components.CloudMusicDivider
import com.smartisanos.music.ui.shell.cloud.components.cloudMusicPressable

internal sealed interface CloudFeaturedHomeState {
    object Loading : CloudFeaturedHomeState
    object Empty : CloudFeaturedHomeState
    object Error : CloudFeaturedHomeState
    data class Success(val home: OnlineMusicHome) : CloudFeaturedHomeState
}

@Composable
internal fun CloudFeaturedHomeContent(
    state: CloudFeaturedHomeState,
    playbackBarOverlayHeight: Dp,
    onRetryClick: () -> Unit,
    onTracksClick: () -> Unit,
    onPlaylistsClick: () -> Unit,
    onChartsClick: () -> Unit,
    onAlbumsClick: () -> Unit,
    onArtistsClick: () -> Unit,
    onPlaylistClick: (OnlinePlaylist) -> Unit,
    onAlbumClick: (OnlineAlbum) -> Unit,
    onArtistClick: (OnlineArtist) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        CloudFeaturedHomeState.Loading -> CloudMusicDelayedLoadingState(
            title = stringResource(R.string.cloud_music_featured_loading),
            modifier = modifier,
        )
        CloudFeaturedHomeState.Empty -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_empty_title),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            modifier = modifier,
        )
        CloudFeaturedHomeState.Error -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_featured_error),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            actionText = stringResource(R.string.cloud_music_retry),
            onActionClick = onRetryClick,
            modifier = modifier,
        )
        is CloudFeaturedHomeState.Success -> {
            val bottomPadding = playbackBarOverlayHeight + 10.dp
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = bottomPadding),
            ) {
                CloudHomeAnimatedSection(index = 0) {
                    CloudHomeTrackPreviewSection(
                        title = stringResource(R.string.cloud_music_section_daily_recommend),
                        tracks = state.home.tracks,
                        onClick = onTracksClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                CloudHomeAnimatedSection(index = 1) {
                    CloudHomePlaylistSection(
                        title = stringResource(R.string.cloud_music_section_featured_playlists),
                        playlists = state.home.playlists,
                        actionText = stringResource(R.string.cloud_music_section_view_all),
                        onActionClick = onPlaylistsClick,
                        onPlaylistClick = onPlaylistClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                CloudHomeAnimatedSection(index = 2) {
                    CloudHomePlaylistSection(
                        title = stringResource(R.string.cloud_music_section_hot_charts),
                        playlists = state.home.charts,
                        actionText = stringResource(R.string.cloud_music_section_view_all),
                        onActionClick = onChartsClick,
                        onPlaylistClick = onPlaylistClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                CloudHomeAnimatedSection(index = 3) {
                    CloudHomeAlbumSection(
                        title = stringResource(R.string.cloud_music_section_new_albums),
                        albums = state.home.albums,
                        actionText = stringResource(R.string.cloud_music_section_view_all),
                        onActionClick = onAlbumsClick,
                        onAlbumClick = onAlbumClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                CloudHomeAnimatedSection(index = 4) {
                    CloudHomeArtistSection(
                        title = stringResource(R.string.cloud_music_section_hot_artists),
                        artists = state.home.artists,
                        actionText = stringResource(R.string.cloud_music_section_view_all),
                        onActionClick = onArtistsClick,
                        onArtistClick = onArtistClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
internal fun <T> CloudFeaturedHomeCoverListContent(
    state: CloudFeaturedHomeState,
    playbackBarOverlayHeight: Dp,
    onRetryClick: () -> Unit,
    items: (OnlineMusicHome) -> List<T>,
    title: (T) -> String,
    subtitle: @Composable (T) -> String?,
    imageUrl: (T) -> String?,
    onItemClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    key: ((T) -> Any)? = null,
) {
    when (state) {
        CloudFeaturedHomeState.Loading -> CloudMusicDelayedLoadingState(
            title = stringResource(R.string.cloud_music_featured_loading),
            modifier = modifier,
        )
        CloudFeaturedHomeState.Empty -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_empty_title),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            modifier = modifier,
        )
        CloudFeaturedHomeState.Error -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_featured_error),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            actionText = stringResource(R.string.cloud_music_retry),
            onActionClick = onRetryClick,
            modifier = modifier,
        )
        is CloudFeaturedHomeState.Success -> CloudSearchCoverResultList(
            items = items(state.home),
            playbackBarOverlayHeight = playbackBarOverlayHeight,
            title = title,
            subtitle = subtitle,
            imageUrl = imageUrl,
            onItemClick = onItemClick,
            modifier = modifier,
            key = key,
        )
    }
}

@Composable
internal fun CloudFeaturedHomeArtistListContent(
    state: CloudFeaturedHomeState,
    active: Boolean,
    playbackBarOverlayHeight: Dp,
    onRetryClick: () -> Unit,
    onArtistClick: (OnlineArtist) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        CloudFeaturedHomeState.Loading -> CloudMusicDelayedLoadingState(
            title = stringResource(R.string.cloud_music_artists_loading),
            modifier = modifier,
        )
        CloudFeaturedHomeState.Empty -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_artists_empty),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            modifier = modifier,
        )
        CloudFeaturedHomeState.Error -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_artists_error),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            actionText = stringResource(R.string.cloud_music_retry),
            onActionClick = onRetryClick,
            modifier = modifier,
        )
        is CloudFeaturedHomeState.Success -> {
            if (state.home.artists.isEmpty()) {
                CloudMusicBlankState(
                    title = stringResource(R.string.cloud_music_artists_empty),
                    subtitle = stringResource(R.string.cloud_music_empty_subtitle),
                    modifier = modifier,
                )
            } else {
                CloudMusicArtistList(
                    artists = state.home.artists,
                    selectedArtistId = null,
                    active = active,
                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                    onArtistClick = onArtistClick,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
internal fun CloudHomeTrackPreviewSection(
    title: String,
    tracks: List<OnlineTrack>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tracks.isEmpty()) {
        return
    }
    Column(
        modifier = modifier.background(ComposeColor.White),
    ) {
        CloudHomeSectionHeader(
            title = title,
            actionText = null,
            onClick = null,
            modifier = Modifier.fillMaxWidth(),
        )
        CloudHomeDailyRecommendCard(
            tracks = tracks,
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        )
        CloudMusicDivider()
    }
}

@Composable
private fun CloudHomeDailyRecommendCard(
    tracks: List<OnlineTrack>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val weekDay = remember {
        val calendar = java.util.Calendar.getInstance()
        when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> "星期一"
            java.util.Calendar.TUESDAY -> "星期二"
            java.util.Calendar.WEDNESDAY -> "星期三"
            java.util.Calendar.THURSDAY -> "星期四"
            java.util.Calendar.FRIDAY -> "星期五"
            java.util.Calendar.SATURDAY -> "星期六"
            else -> "星期日"
        }
    }

    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        ComposeColor(0xFFFFF5F5),
                        ComposeColor(0xFFFFE8E6),
                    ),
                ),
            )
            .cloudMusicPressable(onClick = onClick)
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(end = 148.dp),
        ) {
            Text(
                text = weekDay,
                style = TextStyle(
                    fontSize = 13.sp,
                    color = CloudAccentColor,
                ),
            )
            Text(
                text = stringResource(R.string.cloud_music_section_daily_recommend),
                style = TextStyle(
                    fontSize = 18.sp,
                    color = ComposeColor(0xE6000000),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = context.getString(R.string.cloud_music_playlist_track_count, tracks.size),
                style = TextStyle(
                    fontSize = 12.sp,
                    color = CloudSecondaryTextColor,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        val previewTracks = remember(tracks) { tracks.take(3) }
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy((-8).dp),
        ) {
            previewTracks.forEach { track ->
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ComposeColor(0xFFEDEDED))
                        .border(
                            width = 1.dp,
                            color = ComposeColor.White,
                            shape = RoundedCornerShape(4.dp),
                        ),
                ) {
                    val artworkUrl = track.artworkUrl
                    if (!artworkUrl.isNullOrBlank()) {
                        CloudMusicCoverImage(
                            imageUrl = artworkUrl,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CloudArtistIntroductionSection(
    sections: List<OnlineArtistIntroduction>,
    modifier: Modifier = Modifier,
) {
    val intro = remember(sections) {
        sections.firstOrNull { section -> section.title.contains("简介") }
            ?: sections.firstOrNull()
    } ?: return
    Column(
        modifier = modifier.background(ComposeColor.White),
    ) {
        CloudHomeSectionHeader(
            title = stringResource(R.string.cloud_music_section_artist_intro),
            actionText = null,
            onClick = null,
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 9.dp, end = 12.dp, bottom = 12.dp),
        ) {
            if (intro.title.isNotBlank() && !intro.title.contains("简介")) {
                Text(
                    text = intro.title,
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = ComposeColor(0xCC000000),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
            }
            Text(
                text = intro.text,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = CloudSecondaryTextColor,
                    lineHeight = 18.sp,
                ),
            )
        }
        CloudMusicDivider()
    }
}

@Composable
internal fun CloudHomePlaylistSection(
    title: String,
    playlists: List<OnlinePlaylist>,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    onPlaylistClick: (OnlinePlaylist) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (playlists.isEmpty()) {
        return
    }
    val context = LocalContext.current
    val visiblePlaylists = remember(playlists) {
        playlists.take(CloudHomeCoverPreviewCount)
    }
    CloudHomeCoverSection(
        title = title,
        actionText = actionText,
        onActionClick = onActionClick,
        modifier = modifier,
    ) {
        items(
            items = visiblePlaylists,
            key = { playlist -> playlist.playlistId },
        ) { playlist ->
            CloudHomeCoverCard(
                title = playlist.title,
                subtitle = playlist.homeSubtitle(context),
                imageUrl = playlist.artworkUrl,
                onClick = { onPlaylistClick(playlist) },
            )
        }
    }
}

@Composable
internal fun CloudHomeAlbumSection(
    title: String,
    albums: List<OnlineAlbum>,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    onAlbumClick: (OnlineAlbum) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (albums.isEmpty()) {
        return
    }
    val visibleAlbums = remember(albums) {
        albums.take(CloudHomeCoverPreviewCount)
    }
    val fallbackArtist = stringResource(R.string.cloud_music_album_provider_netease)
    CloudHomeCoverSection(
        title = title,
        actionText = actionText,
        onActionClick = onActionClick,
        modifier = modifier,
    ) {
        items(
            items = visibleAlbums,
            key = { album -> album.albumId },
        ) { album ->
            CloudHomeCoverCard(
                title = album.title,
                subtitle = album.artist ?: fallbackArtist,
                imageUrl = album.artworkUrl,
                onClick = { onAlbumClick(album) },
            )
        }
    }
}

@Composable
internal fun CloudHomeArtistSection(
    title: String,
    artists: List<OnlineArtist>,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    onArtistClick: (OnlineArtist) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (artists.isEmpty()) {
        return
    }
    val context = LocalContext.current
    val visibleArtists = remember(artists) {
        artists.take(CloudHomeCoverPreviewCount)
    }
    CloudHomeCoverSection(
        title = title,
        actionText = actionText,
        onActionClick = onActionClick,
        modifier = modifier,
    ) {
        items(
            items = visibleArtists,
            key = { artist -> artist.artistId },
        ) { artist ->
            CloudHomeCoverCard(
                title = artist.name,
                subtitle = artist.subtitleText(context),
                imageUrl = artist.artworkUrl,
                onClick = { onArtistClick(artist) },
            )
        }
    }
}

@Composable
internal fun CloudHomeRadioSection(
    title: String,
    radios: List<OnlineRadio>,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    onRadioClick: (OnlineRadio) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (radios.isEmpty()) {
        return
    }
    val context = LocalContext.current
    val visibleRadios = remember(radios) {
        radios.take(CloudHomeCoverPreviewCount)
    }
    CloudHomeCoverSection(
        title = title,
        actionText = actionText,
        onActionClick = onActionClick,
        modifier = modifier,
    ) {
        items(
            items = visibleRadios,
            key = { radio -> radio.radioId },
        ) { radio ->
            CloudHomeCoverCard(
                title = radio.title,
                subtitle = radio.cardSubtitle(context),
                imageUrl = radio.artworkUrl,
                onClick = { onRadioClick(radio) },
            )
        }
    }
}
