package com.smartisanos.music.ui.shell.cloud

import android.content.Context
import android.net.Uri
import android.util.Size
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.smartisanos.music.R
import com.smartisanos.music.data.favorite.FavoriteSongsRepository
import com.smartisanos.music.data.online.NeteaseAccountActionResult
import com.smartisanos.music.data.online.NeteaseAccountActionStatus
import com.smartisanos.music.data.online.NeteaseAuthStore
import com.smartisanos.music.data.online.OnlineAccountPlaylist
import com.smartisanos.music.data.online.OnlineAlbum
import com.smartisanos.music.data.online.OnlineArtist
import com.smartisanos.music.data.online.OnlineArtistIntroduction
import com.smartisanos.music.data.online.OnlineBanner
import com.smartisanos.music.data.online.OnlineMusicHome
import com.smartisanos.music.data.online.OnlineMusicProvider
import com.smartisanos.music.data.online.OnlineMusicProviderRepository
import com.smartisanos.music.data.online.OnlineRadio
import com.smartisanos.music.data.online.OnlineRadioHome
import com.smartisanos.music.data.online.OnlineMusicRepositoryRouter
import com.smartisanos.music.data.online.OnlinePlaylist
import com.smartisanos.music.data.online.OnlineSearchResults
import com.smartisanos.music.data.online.OnlineSearchHotKeyword
import com.smartisanos.music.data.online.OnlineTrack
import com.smartisanos.music.data.online.buildOnlineMediaId
import com.smartisanos.music.data.online.onlineIdentityOrNull
import com.smartisanos.music.data.online.toMediaItem
import com.smartisanos.music.playback.LocalPlaybackBrowser
import com.smartisanos.music.playback.loadArtworkBitmap
import com.smartisanos.music.playback.replaceQueueAndPlay
import com.smartisanos.music.ui.shell.LegacyPlaylistDeleteDialog
import com.smartisanos.music.ui.shell.LegacyPlaylistDeleteRequest
import com.smartisanos.music.ui.components.SmartisanDrawableBackground
import com.smartisanos.music.ui.shell.LegacyPlaylistBlankView
import com.smartisanos.music.ui.shell.LegacySlideSelectionStartArea
import com.smartisanos.music.ui.shell.addLegacyPortListFooter
import com.smartisanos.music.ui.shell.bindLegacyPortListFooter
import com.smartisanos.music.ui.shell.legacyWrappedAdapter
import com.smartisanos.music.ui.shell.legacyPlaylistDetailActionButton
import com.smartisanos.music.ui.shell.legacySlideSelectionController
import com.smartisanos.music.ui.shell.songs.LegacySongsAdapter
import com.smartisanos.music.ui.shell.songs.LegacySongsSectionMode
import com.smartisanos.music.ui.shell.songs.LegacySongsSortDisplayMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

@Composable
internal fun LegacyPortCloudMusicPage(
    active: Boolean,
    playbackBarOverlayHeight: Dp,
    searchOpenRequest: Int = 0,
    onSearchOpenRequestHandled: () -> Unit = {},
    accountRefreshRequest: Int = 0,
    onTrackMoreClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val authStore = remember(appContext) {
        NeteaseAuthStore(appContext)
    }
    val favoriteRepository = remember(appContext) {
        FavoriteSongsRepository.getInstance(appContext)
    }
    val repositoryRouter = remember(appContext) {
        OnlineMusicRepositoryRouter(appContext)
    }
    val activeRepository: OnlineMusicProviderRepository = remember(repositoryRouter) {
        repositoryRouter.repositoryFor(OnlineMusicProvider.Netease)
    }
    var query by rememberSaveable { mutableStateOf("") }
    var authState by remember { mutableStateOf(authStore.load()) }
    var authRevision by remember { mutableStateOf(0) }
    var homeState by remember { mutableStateOf<CloudMusicState>(CloudMusicState.LoadingFeatured) }
    var searchResultsState by remember { mutableStateOf<CloudSearchResultsState>(CloudSearchResultsState.Idle) }
    var hotSearchState by remember { mutableStateOf<CloudHotSearchState>(CloudHotSearchState.Idle) }
    var searchCategory by rememberSaveable { mutableStateOf(CloudSearchCategory.All) }
    var bannerItems by remember { mutableStateOf<List<OnlineBanner>>(emptyList()) }
    var featuredHomeState by remember { mutableStateOf<CloudFeaturedHomeState>(CloudFeaturedHomeState.Loading) }
    var radioHomeState by remember { mutableStateOf<CloudRadioHomeState>(CloudRadioHomeState.Loading) }
    var selectedRadio by remember { mutableStateOf<OnlineRadio?>(null) }
    var radioDetailReturnMode by rememberSaveable { mutableStateOf(CloudHomeMode.Radio) }
    var radioTrackState by remember { mutableStateOf<CloudMusicState>(CloudMusicState.Idle) }
    var artistState by remember { mutableStateOf<CloudArtistState>(CloudArtistState.Idle) }
    var selectedArtist by remember { mutableStateOf<OnlineArtist?>(null) }
    var artistDetailReturnMode by rememberSaveable { mutableStateOf(CloudHomeMode.Artists) }
    var artistTracksState by remember { mutableStateOf<CloudMusicState>(CloudMusicState.Idle) }
    var artistAlbums by remember { mutableStateOf<List<OnlineAlbum>>(emptyList()) }
    var artistIntroductions by remember { mutableStateOf<List<OnlineArtistIntroduction>>(emptyList()) }
    var selectedBanner by remember { mutableStateOf<OnlineBanner?>(null) }
    var bannerTrackState by remember { mutableStateOf<CloudMusicState>(CloudMusicState.Idle) }
    var selectedOnlinePlaylist by remember { mutableStateOf<OnlinePlaylist?>(null) }
    var selectedOnlineAlbum by remember { mutableStateOf<OnlineAlbum?>(null) }
    var onlineDetailReturnMode by rememberSaveable { mutableStateOf<CloudHomeMode?>(null) }
    var featuredTrackDetailState by remember { mutableStateOf<CloudMusicState>(CloudMusicState.Idle) }
    var onlinePlaylistTracksState by remember { mutableStateOf<CloudMusicState>(CloudMusicState.Idle) }
    var onlineAlbumTracksState by remember { mutableStateOf<CloudMusicState>(CloudMusicState.Idle) }
    var accountPlaylists by remember { mutableStateOf<List<OnlineAccountPlaylist>>(emptyList()) }
    var accountAlbums by remember { mutableStateOf<List<OnlineAlbum>>(emptyList()) }
    var accountRadios by remember { mutableStateOf<List<OnlineRadio>>(emptyList()) }
    var selectedAccountPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var handledAccountRefreshRequest by remember { mutableStateOf(0) }
    var homeMode by rememberSaveable { mutableStateOf(CloudHomeMode.Featured) }
    var searchActive by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(active) {
        if (active) {
            val latestAuthState = authStore.load()
            if (latestAuthState != authState) {
                authState = latestAuthState
                authRevision += 1
            }
        }
    }

    LaunchedEffect(active, authRevision, authState.isLoggedIn) {
        if (!active || !authState.isLoggedIn) {
            return@LaunchedEffect
        }
        val likedTrackIds = runSuspendCatching {
            activeRepository.accountLikedTrackIds()
        }.getOrNull().orEmpty()
        if (likedTrackIds.isNotEmpty()) {
            favoriteRepository.addMissing(
                likedTrackIds.mapTo(linkedSetOf()) { trackId ->
                    buildOnlineMediaId(OnlineMusicProvider.Netease.sourceId, trackId)
                },
            )
        }
    }

    LaunchedEffect(active, searchOpenRequest) {
        if (active && searchOpenRequest > 0) {
            searchActive = true
            onSearchOpenRequestHandled()
        }
    }

    LaunchedEffect(active, accountRefreshRequest) {
        if (!active || accountRefreshRequest <= handledAccountRefreshRequest) {
            return@LaunchedEffect
        }
        handledAccountRefreshRequest = accountRefreshRequest
        authState = authStore.load()
        authRevision += 1
    }

    BackHandler(
        enabled = active && (
            searchActive ||
                homeMode == CloudHomeMode.ArtistTracks ||
                homeMode == CloudHomeMode.ArtistAlbums ||
                homeMode == CloudHomeMode.FeaturedTracks ||
                homeMode == CloudHomeMode.FeaturedPlaylists ||
                homeMode == CloudHomeMode.FeaturedCharts ||
                homeMode == CloudHomeMode.FeaturedAlbums ||
                homeMode == CloudHomeMode.FeaturedArtists ||
                homeMode == CloudHomeMode.BannerTrack ||
                homeMode == CloudHomeMode.RadioList ||
                homeMode == CloudHomeMode.RadioTracks ||
                homeMode == CloudHomeMode.RadioPrograms ||
                homeMode == CloudHomeMode.OnlinePlaylistTracks ||
                homeMode == CloudHomeMode.OnlineAlbumTracks
            ),
    ) {
        when {
            searchActive -> {
                query = ""
                searchActive = false
            }
            homeMode == CloudHomeMode.ArtistTracks -> {
                val returnMode = artistDetailReturnMode
                selectedArtist = null
                artistDetailReturnMode = CloudHomeMode.Artists
                artistTracksState = CloudMusicState.Idle
                artistAlbums = emptyList()
                artistIntroductions = emptyList()
                homeMode = returnMode
            }
            homeMode == CloudHomeMode.ArtistAlbums -> {
                homeMode = CloudHomeMode.ArtistTracks
            }
            homeMode == CloudHomeMode.FeaturedPlaylists ||
                homeMode == CloudHomeMode.FeaturedCharts ||
                homeMode == CloudHomeMode.FeaturedAlbums ||
                homeMode == CloudHomeMode.FeaturedArtists -> {
                homeMode = CloudHomeMode.Featured
            }
            homeMode == CloudHomeMode.BannerTrack -> {
                selectedBanner = null
                bannerTrackState = CloudMusicState.Idle
                homeMode = CloudHomeMode.Featured
            }
            homeMode == CloudHomeMode.RadioTracks -> {
                homeMode = CloudHomeMode.Radio
            }
            homeMode == CloudHomeMode.RadioList -> {
                homeMode = CloudHomeMode.Radio
            }
            homeMode == CloudHomeMode.RadioPrograms -> {
                val returnMode = radioDetailReturnMode
                selectedRadio = null
                radioDetailReturnMode = CloudHomeMode.Radio
                radioTrackState = CloudMusicState.Idle
                homeMode = returnMode
            }
            homeMode == CloudHomeMode.FeaturedTracks ||
                homeMode == CloudHomeMode.OnlinePlaylistTracks ||
                homeMode == CloudHomeMode.OnlineAlbumTracks -> {
                val returnMode = onlineDetailReturnMode ?: CloudHomeMode.Featured
                selectedOnlinePlaylist = null
                selectedOnlineAlbum = null
                onlineDetailReturnMode = null
                featuredTrackDetailState = CloudMusicState.Idle
                onlinePlaylistTracksState = CloudMusicState.Idle
                onlineAlbumTracksState = CloudMusicState.Idle
                homeMode = returnMode
            }
        }
    }

    LaunchedEffect(authRevision) {
        bannerItems = runSuspendCatching {
            activeRepository.featuredBanners()
        }.getOrDefault(emptyList())
    }

    LaunchedEffect(authRevision) {
        featuredHomeState = CloudFeaturedHomeState.Loading
        val result = runSuspendCatching {
            activeRepository.featuredHome()
        }
        featuredHomeState = result.fold(
            onSuccess = { home ->
                if (home.isEmpty()) {
                    CloudFeaturedHomeState.Empty
                } else {
                    CloudFeaturedHomeState.Success(home)
                }
            },
            onFailure = {
                CloudFeaturedHomeState.Error
            },
        )
    }

    LaunchedEffect(homeMode, authRevision) {
        if (homeMode != CloudHomeMode.Radio) {
            return@LaunchedEffect
        }
        radioHomeState = CloudRadioHomeState.Loading
        val result = runSuspendCatching {
            activeRepository.featuredRadioHome()
        }
        radioHomeState = result.fold(
            onSuccess = { home ->
                if (home.isEmpty) {
                    CloudRadioHomeState.Empty
                } else {
                    CloudRadioHomeState.Success(home)
                }
            },
            onFailure = {
                CloudRadioHomeState.Error
            },
        )
    }

    LaunchedEffect(
        homeMode,
        authRevision,
        authState.isLoggedIn,
        selectedAccountPlaylistId,
    ) {
        if (
            homeMode == CloudHomeMode.Artists ||
            homeMode == CloudHomeMode.ArtistTracks ||
            homeMode == CloudHomeMode.ArtistAlbums ||
            homeMode == CloudHomeMode.Collections ||
            homeMode == CloudHomeMode.FeaturedTracks ||
            homeMode == CloudHomeMode.FeaturedPlaylists ||
            homeMode == CloudHomeMode.FeaturedCharts ||
            homeMode == CloudHomeMode.FeaturedAlbums ||
            homeMode == CloudHomeMode.FeaturedArtists ||
            homeMode == CloudHomeMode.BannerTrack ||
            homeMode == CloudHomeMode.Radio ||
            homeMode == CloudHomeMode.RadioList ||
            homeMode == CloudHomeMode.RadioTracks ||
            homeMode == CloudHomeMode.RadioPrograms ||
            homeMode == CloudHomeMode.OnlinePlaylistTracks ||
            homeMode == CloudHomeMode.OnlineAlbumTracks
        ) {
            return@LaunchedEffect
        }
        if (!authState.isLoggedIn || homeMode == CloudHomeMode.Featured) {
            accountPlaylists = emptyList()
            accountAlbums = emptyList()
            accountRadios = emptyList()
            selectedAccountPlaylistId = null
            homeState = CloudMusicState.Idle
            return@LaunchedEffect
        }

        homeState = CloudMusicState.LoadingAccount
        val playlistResult = runSuspendCatching {
            activeRepository.accountPlaylists()
        }
        val albumResult = runSuspendCatching {
            activeRepository.accountAlbums()
        }
        val radioResult = runSuspendCatching {
            activeRepository.accountRadios()
        }
        val cloudPlaylists = playlistResult.getOrNull()
        val cloudAlbums = albumResult.getOrNull().orEmpty()
        val cloudRadios = radioResult.getOrNull().orEmpty()
        if (
            playlistResult.isFailure && albumResult.isFailure && radioResult.isFailure ||
            cloudPlaylists == null && cloudAlbums.isEmpty() && cloudRadios.isEmpty()
        ) {
            accountPlaylists = emptyList()
            accountAlbums = emptyList()
            accountRadios = emptyList()
            homeState = CloudMusicState.AccountError
            return@LaunchedEffect
        }

        accountPlaylists = cloudPlaylists.orEmpty()
        accountAlbums = cloudAlbums
        accountRadios = cloudRadios
        authState = authStore.load()

        val selectedPlaylist = accountPlaylists.firstOrNull { playlist ->
            playlist.playlistId == selectedAccountPlaylistId
        } ?: accountPlaylists.firstOrNull(OnlineAccountPlaylist::isLikedSongs)
            ?: accountPlaylists.firstOrNull()
        if (selectedPlaylist != null && selectedAccountPlaylistId != selectedPlaylist.playlistId) {
            selectedAccountPlaylistId = selectedPlaylist.playlistId
        }

        if (homeMode == CloudHomeMode.Playlists) {
            homeState = if (accountPlaylists.isEmpty() && accountAlbums.isEmpty() && accountRadios.isEmpty()) {
                CloudMusicState.AccountEmpty
            } else {
                CloudMusicState.Success(emptyList())
            }
            return@LaunchedEffect
        }

        if (selectedPlaylist == null) {
            homeState = CloudMusicState.AccountEmpty
            return@LaunchedEffect
        }

        val tracksResult = runSuspendCatching {
            activeRepository.accountPlaylistTracks(selectedPlaylist)
        }
        homeState = tracksResult.fold(
            onSuccess = { tracks ->
                if (tracks.isEmpty()) {
                    CloudMusicState.AccountEmpty
                } else {
                    CloudMusicState.Success(tracks)
                }
            },
            onFailure = {
                CloudMusicState.AccountError
            },
        )
    }

    LaunchedEffect(homeMode, authRevision, searchActive, query) {
        if (homeMode != CloudHomeMode.Artists || searchActive && query.trim().isNotEmpty()) {
            return@LaunchedEffect
        }
        artistState = CloudArtistState.Loading
        val result = runSuspendCatching {
            activeRepository.featuredArtists()
        }
        artistState = result.fold(
            onSuccess = { artists ->
                if (artists.isEmpty()) {
                    CloudArtistState.Empty
                } else {
                    CloudArtistState.Success(artists)
                }
            },
            onFailure = {
                CloudArtistState.Error
            },
        )
    }

    LaunchedEffect(homeMode, selectedArtist?.artistId, authRevision) {
        val artist = selectedArtist
        if (homeMode != CloudHomeMode.ArtistTracks || artist == null) {
            return@LaunchedEffect
        }
        artistTracksState = CloudMusicState.LoadingFeatured
        artistAlbums = emptyList()
        artistIntroductions = emptyList()
        val result = runSuspendCatching {
            activeRepository.artistTopTracks(artist)
        }
        artistTracksState = result.fold(
            onSuccess = { tracks ->
                if (tracks.isEmpty()) {
                    CloudMusicState.FeaturedEmpty
                } else {
                    CloudMusicState.Success(tracks)
                }
            },
            onFailure = {
                CloudMusicState.FeaturedError
            },
        )
        artistAlbums = runSuspendCatching {
            activeRepository.artistAlbums(artist)
        }.getOrDefault(emptyList())
        artistIntroductions = runSuspendCatching {
            activeRepository.artistIntroduction(artist)
        }.getOrDefault(emptyList())
    }

    LaunchedEffect(homeMode, featuredHomeState) {
        if (homeMode != CloudHomeMode.FeaturedTracks) {
            return@LaunchedEffect
        }
        featuredTrackDetailState = when (val state = featuredHomeState) {
            CloudFeaturedHomeState.Loading -> CloudMusicState.LoadingFeatured
            CloudFeaturedHomeState.Empty -> CloudMusicState.FeaturedEmpty
            CloudFeaturedHomeState.Error -> CloudMusicState.FeaturedError
            is CloudFeaturedHomeState.Success -> {
                if (state.home.tracks.isEmpty()) {
                    CloudMusicState.FeaturedEmpty
                } else {
                    CloudMusicState.Success(state.home.tracks)
                }
            }
        }
    }

    LaunchedEffect(homeMode, selectedRadio?.radioId, authRevision, authState.isLoggedIn) {
        val radio = selectedRadio
        if (homeMode != CloudHomeMode.RadioPrograms || radio == null) {
            return@LaunchedEffect
        }
        if (!authState.isLoggedIn) {
            radioTrackState = CloudMusicState.RadioLoginRequired
            return@LaunchedEffect
        }
        radioTrackState = CloudMusicState.LoadingRadio
        val result = runSuspendCatching {
            activeRepository.radioTracks(radio)
        }
        radioTrackState = result.fold(
            onSuccess = { tracks ->
                if (tracks.isEmpty()) {
                    CloudMusicState.RadioEmpty
                } else {
                    CloudMusicState.Success(tracks)
                }
            },
            onFailure = {
                CloudMusicState.RadioError
            },
        )
    }

    LaunchedEffect(homeMode, selectedBanner?.bannerId, selectedBanner?.targetTrackId, authRevision) {
        if (homeMode != CloudHomeMode.BannerTrack) {
            return@LaunchedEffect
        }
        val trackId = selectedBanner
            ?.targetTrackId
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        if (trackId == null) {
            bannerTrackState = CloudMusicState.FeaturedEmpty
            return@LaunchedEffect
        }
        bannerTrackState = CloudMusicState.LoadingFeatured
        val result = runSuspendCatching {
            activeRepository.track(trackId)
        }
        bannerTrackState = result.fold(
            onSuccess = { track ->
                if (track == null) {
                    CloudMusicState.FeaturedEmpty
                } else {
                    CloudMusicState.Success(listOf(track))
                }
            },
            onFailure = {
                CloudMusicState.FeaturedError
            },
        )
    }

    LaunchedEffect(homeMode, selectedOnlinePlaylist?.playlistId, selectedOnlineAlbum?.albumId, authRevision) {
        when (homeMode) {
            CloudHomeMode.OnlinePlaylistTracks -> {
                val playlist = selectedOnlinePlaylist ?: return@LaunchedEffect
                onlinePlaylistTracksState = CloudMusicState.LoadingFeatured
                val result = runSuspendCatching {
                    activeRepository.playlistTracks(playlist)
                }
                onlinePlaylistTracksState = result.fold(
                    onSuccess = { tracks ->
                        if (tracks.isEmpty()) {
                            CloudMusicState.FeaturedEmpty
                        } else {
                            CloudMusicState.Success(tracks)
                        }
                    },
                    onFailure = {
                        CloudMusicState.FeaturedError
                    },
                )
            }
            CloudHomeMode.OnlineAlbumTracks -> {
                val album = selectedOnlineAlbum ?: return@LaunchedEffect
                onlineAlbumTracksState = CloudMusicState.LoadingFeatured
                val result = runSuspendCatching {
                    activeRepository.albumTracks(album)
                }
                onlineAlbumTracksState = result.fold(
                    onSuccess = { tracks ->
                        if (tracks.isEmpty()) {
                            CloudMusicState.FeaturedEmpty
                        } else {
                            CloudMusicState.Success(tracks)
                        }
                    },
                    onFailure = {
                        CloudMusicState.FeaturedError
                    },
                )
            }
            else -> Unit
        }
    }

    LaunchedEffect(searchActive, query, authRevision) {
        if (!searchActive || query.trim().isNotEmpty()) {
            return@LaunchedEffect
        }
        hotSearchState = CloudHotSearchState.Loading
        val result = runSuspendCatching {
            activeRepository.searchHotKeywords()
        }
        hotSearchState = result.fold(
            onSuccess = { keywords ->
                if (keywords.isEmpty()) {
                    CloudHotSearchState.Empty
                } else {
                    CloudHotSearchState.Success(keywords)
                }
            },
            onFailure = {
                CloudHotSearchState.Error
            },
        )
    }

    LaunchedEffect(query, authRevision, searchActive, homeMode) {
        val normalizedQuery = query.trim()
        if (!searchActive || normalizedQuery.isEmpty()) {
            searchResultsState = CloudSearchResultsState.Idle
            return@LaunchedEffect
        }
        searchResultsState = CloudSearchResultsState.Loading
        delay(CloudSearchDebounceMs)
        val result = runSuspendCatching {
            activeRepository.searchAll(normalizedQuery)
        }
        searchResultsState = result.fold(
            onSuccess = { results ->
                if (!results.hasResults) {
                    CloudSearchResultsState.Empty(normalizedQuery)
                } else {
                    CloudSearchResultsState.Success(results)
                }
            },
            onFailure = {
                CloudSearchResultsState.Error(normalizedQuery)
            },
        )
    }

    val selectedProviderLoggedIn = authState.isLoggedIn
    val normalizedQuery = query.trim()
    val searchResultVisible = searchActive && normalizedQuery.isNotEmpty()
    val searchPromptVisible = searchActive && normalizedQuery.isEmpty()
    val accountLibraryVisible = !searchResultVisible && selectedProviderLoggedIn
    val selectedAccountPlaylist = accountPlaylists.firstOrNull { playlist ->
        playlist.playlistId == selectedAccountPlaylistId
    }
    fun selectMineOrLogin() {
        if (!selectedProviderLoggedIn) {
            Toast.makeText(context, R.string.cloud_music_login_in_settings, Toast.LENGTH_SHORT).show()
            return
        }
        selectedAccountPlaylistId = null
        homeMode = CloudHomeMode.Playlists
    }
    fun selectCollections() {
        query = ""
        selectedOnlinePlaylist = null
        onlineDetailReturnMode = null
        onlinePlaylistTracksState = CloudMusicState.Idle
        homeMode = CloudHomeMode.Collections
    }
    fun selectArtists() {
        query = ""
        selectedArtist = null
        artistDetailReturnMode = CloudHomeMode.Artists
        artistTracksState = CloudMusicState.Idle
        artistAlbums = emptyList()
        artistIntroductions = emptyList()
        homeMode = CloudHomeMode.Artists
    }
    fun selectRadio() {
        query = ""
        selectedRadio = null
        radioDetailReturnMode = CloudHomeMode.Radio
        radioTrackState = CloudMusicState.Idle
        homeMode = CloudHomeMode.Radio
    }
    fun openRadioTracks() {
        homeMode = CloudHomeMode.RadioTracks
    }
    fun openRadioList() {
        homeMode = CloudHomeMode.RadioList
    }
    fun openRadioPrograms(
        radio: OnlineRadio,
        returnMode: CloudHomeMode = CloudHomeMode.Radio,
    ) {
        selectedRadio = radio
        radioDetailReturnMode = returnMode
        radioTrackState = if (authState.isLoggedIn) {
            CloudMusicState.LoadingRadio
        } else {
            CloudMusicState.RadioLoginRequired
        }
        homeMode = CloudHomeMode.RadioPrograms
    }
    fun openArtist(
        artist: OnlineArtist,
        returnMode: CloudHomeMode = CloudHomeMode.Artists,
    ) {
        selectedArtist = artist
        artistDetailReturnMode = returnMode
        query = ""
        searchActive = false
        homeMode = CloudHomeMode.ArtistTracks
    }
    fun openArtistAlbums() {
        if (artistAlbums.isNotEmpty()) {
            homeMode = CloudHomeMode.ArtistAlbums
        }
    }
    fun selectRecommendHome() {
        query = ""
        selectedArtist = null
        artistDetailReturnMode = CloudHomeMode.Artists
        artistTracksState = CloudMusicState.Idle
        artistAlbums = emptyList()
        artistIntroductions = emptyList()
        selectedBanner = null
        bannerTrackState = CloudMusicState.Idle
        selectedRadio = null
        radioTrackState = CloudMusicState.Idle
        selectedAccountPlaylistId = null
        selectedOnlinePlaylist = null
        selectedOnlineAlbum = null
        onlineDetailReturnMode = null
        featuredTrackDetailState = CloudMusicState.Idle
        onlinePlaylistTracksState = CloudMusicState.Idle
        onlineAlbumTracksState = CloudMusicState.Idle
        homeMode = CloudHomeMode.Featured
    }
    fun openFeaturedTracks() {
        homeMode = CloudHomeMode.FeaturedTracks
    }
    fun openFeaturedPlaylists() {
        homeMode = CloudHomeMode.FeaturedPlaylists
    }
    fun openFeaturedCharts() {
        homeMode = CloudHomeMode.FeaturedCharts
    }
    fun openFeaturedAlbums() {
        homeMode = CloudHomeMode.FeaturedAlbums
    }
    fun openFeaturedArtists() {
        homeMode = CloudHomeMode.FeaturedArtists
    }
    fun openOnlinePlaylist(
        playlist: OnlinePlaylist,
        returnMode: CloudHomeMode = CloudHomeMode.Featured,
    ) {
        selectedOnlinePlaylist = playlist
        selectedOnlineAlbum = null
        onlineDetailReturnMode = returnMode
        onlinePlaylistTracksState = CloudMusicState.LoadingFeatured
        homeMode = CloudHomeMode.OnlinePlaylistTracks
    }
    fun openOnlineAlbum(
        album: OnlineAlbum,
        returnMode: CloudHomeMode = CloudHomeMode.Featured,
    ) {
        selectedOnlineAlbum = album
        selectedOnlinePlaylist = null
        onlineDetailReturnMode = returnMode
        onlineAlbumTracksState = CloudMusicState.LoadingFeatured
        homeMode = CloudHomeMode.OnlineAlbumTracks
    }
    fun openBannerTarget(banner: OnlineBanner) {
        when {
            !banner.targetTrackId.isNullOrBlank() -> {
                selectedBanner = banner
                bannerTrackState = CloudMusicState.LoadingFeatured
                homeMode = CloudHomeMode.BannerTrack
            }
            !banner.targetAlbumId.isNullOrBlank() -> {
                openOnlineAlbum(
                    album = OnlineAlbum(
                        provider = banner.provider,
                        albumId = banner.targetAlbumId,
                        title = banner.subtitle?.takeIf(String::isNotBlank) ?: banner.title,
                        artworkUrl = banner.imageUrl,
                    ),
                    returnMode = CloudHomeMode.Featured,
                )
            }
            !banner.targetPlaylistId.isNullOrBlank() -> {
                openOnlinePlaylist(
                    playlist = OnlinePlaylist(
                        provider = banner.provider,
                        playlistId = banner.targetPlaylistId,
                        title = banner.subtitle?.takeIf(String::isNotBlank) ?: banner.title,
                        artworkUrl = banner.imageUrl,
                    ),
                    returnMode = CloudHomeMode.Featured,
                )
            }
            else -> Unit
        }
    }
    val selectedQuickEntry = when {
        searchActive -> null
        homeMode == CloudHomeMode.Collections ||
            (homeMode == CloudHomeMode.OnlinePlaylistTracks && onlineDetailReturnMode == CloudHomeMode.Collections) ->
            CloudMusicHomeEntry.Collection
        (homeMode == CloudHomeMode.OnlinePlaylistTracks || homeMode == CloudHomeMode.OnlineAlbumTracks) &&
            onlineDetailReturnMode == CloudHomeMode.Playlists -> CloudMusicHomeEntry.Mine
        homeMode == CloudHomeMode.FeaturedTracks ||
            homeMode == CloudHomeMode.FeaturedPlaylists ||
            homeMode == CloudHomeMode.FeaturedCharts ||
            homeMode == CloudHomeMode.FeaturedAlbums ||
            homeMode == CloudHomeMode.FeaturedArtists ||
            homeMode == CloudHomeMode.BannerTrack ||
            homeMode == CloudHomeMode.OnlinePlaylistTracks ||
            homeMode == CloudHomeMode.OnlineAlbumTracks -> CloudMusicHomeEntry.Recommend
        homeMode == CloudHomeMode.ArtistTracks && artistDetailReturnMode != CloudHomeMode.Artists ->
            CloudMusicHomeEntry.Recommend
        homeMode == CloudHomeMode.ArtistAlbums && artistDetailReturnMode != CloudHomeMode.Artists ->
            CloudMusicHomeEntry.Recommend
        homeMode == CloudHomeMode.Artists || homeMode == CloudHomeMode.ArtistTracks ->
            CloudMusicHomeEntry.Artist
        homeMode == CloudHomeMode.ArtistAlbums -> CloudMusicHomeEntry.Artist
        homeMode == CloudHomeMode.Radio ||
            homeMode == CloudHomeMode.RadioList ||
            homeMode == CloudHomeMode.RadioTracks -> CloudMusicHomeEntry.Radio
        homeMode == CloudHomeMode.RadioPrograms ->
            if (radioDetailReturnMode == CloudHomeMode.Playlists) {
                CloudMusicHomeEntry.Mine
            } else {
                CloudMusicHomeEntry.Radio
            }
        homeMode == CloudHomeMode.Playlists || homeMode == CloudHomeMode.Tracks -> CloudMusicHomeEntry.Mine
        !accountLibraryVisible || homeMode == CloudHomeMode.Featured -> CloudMusicHomeEntry.Recommend
        else -> CloudMusicHomeEntry.Collection
    }
    val sectionTitle = when {
        searchPromptVisible -> stringResource(R.string.cloud_music_section_search)
        searchResultVisible -> stringResource(R.string.cloud_music_section_search_results)
        homeMode == CloudHomeMode.Playlists -> stringResource(R.string.cloud_music_section_account_library)
        homeMode == CloudHomeMode.Artists -> stringResource(R.string.cloud_music_section_hot_artists)
        homeMode == CloudHomeMode.ArtistTracks ->
            selectedArtist?.name ?: stringResource(R.string.cloud_music_section_artist_tracks)
        homeMode == CloudHomeMode.ArtistAlbums -> stringResource(R.string.cloud_music_section_artist_albums)
        homeMode == CloudHomeMode.FeaturedTracks -> stringResource(R.string.cloud_music_section_daily_recommend)
        homeMode == CloudHomeMode.Collections -> stringResource(R.string.cloud_music_section_featured_playlists)
        homeMode == CloudHomeMode.FeaturedPlaylists ->
            stringResource(R.string.cloud_music_section_featured_playlists)
        homeMode == CloudHomeMode.FeaturedCharts -> stringResource(R.string.cloud_music_section_hot_charts)
        homeMode == CloudHomeMode.FeaturedAlbums -> stringResource(R.string.cloud_music_section_new_albums)
        homeMode == CloudHomeMode.FeaturedArtists -> stringResource(R.string.cloud_music_section_hot_artists)
        homeMode == CloudHomeMode.BannerTrack ->
            selectedBanner?.title ?: stringResource(R.string.cloud_music_banner_fallback_title)
        homeMode == CloudHomeMode.Radio -> stringResource(R.string.cloud_music_section_radio)
        homeMode == CloudHomeMode.RadioList -> stringResource(R.string.cloud_music_section_hot_radios)
        homeMode == CloudHomeMode.RadioTracks -> stringResource(R.string.cloud_music_section_radio_programs)
        homeMode == CloudHomeMode.RadioPrograms ->
            selectedRadio?.title ?: stringResource(R.string.cloud_music_section_hot_radios)
        homeMode == CloudHomeMode.OnlinePlaylistTracks ->
            selectedOnlinePlaylist?.title ?: stringResource(R.string.cloud_music_section_featured_playlists)
        homeMode == CloudHomeMode.OnlineAlbumTracks ->
            selectedOnlineAlbum?.title ?: stringResource(R.string.cloud_music_section_new_albums)
        !accountLibraryVisible || homeMode == CloudHomeMode.Featured ->
            stringResource(R.string.cloud_music_section_netease_picks)
        selectedAccountPlaylist != null -> selectedAccountPlaylist.displayTitle(context)
        else -> stringResource(R.string.cloud_music_liked_songs_entry)
    }
    val featuredHomeVisible = !searchActive &&
        normalizedQuery.isEmpty() &&
        (
            homeMode == CloudHomeMode.Featured ||
                (!selectedProviderLoggedIn && homeMode == CloudHomeMode.Tracks)
            )
    val contentKey = when {
        searchPromptVisible -> CloudMusicContentKey.SearchPrompt
        searchResultVisible -> CloudMusicContentKey.SearchResults
        homeMode == CloudHomeMode.Playlists -> CloudMusicContentKey.Playlists
        homeMode == CloudHomeMode.Artists -> CloudMusicContentKey.Artists
        homeMode == CloudHomeMode.ArtistTracks -> CloudMusicContentKey.ArtistTracks
        homeMode == CloudHomeMode.ArtistAlbums -> CloudMusicContentKey.ArtistAlbums
        homeMode == CloudHomeMode.FeaturedTracks -> CloudMusicContentKey.FeaturedTracksDetail
        homeMode == CloudHomeMode.Collections -> CloudMusicContentKey.Collections
        homeMode == CloudHomeMode.FeaturedPlaylists -> CloudMusicContentKey.FeaturedPlaylists
        homeMode == CloudHomeMode.FeaturedCharts -> CloudMusicContentKey.FeaturedCharts
        homeMode == CloudHomeMode.FeaturedAlbums -> CloudMusicContentKey.FeaturedAlbums
        homeMode == CloudHomeMode.FeaturedArtists -> CloudMusicContentKey.FeaturedArtists
        homeMode == CloudHomeMode.BannerTrack -> CloudMusicContentKey.BannerTrack
        homeMode == CloudHomeMode.Radio -> CloudMusicContentKey.Radio
        homeMode == CloudHomeMode.RadioList -> CloudMusicContentKey.RadioList
        homeMode == CloudHomeMode.RadioTracks -> CloudMusicContentKey.RadioTracks
        homeMode == CloudHomeMode.RadioPrograms -> CloudMusicContentKey.RadioPrograms
        homeMode == CloudHomeMode.OnlinePlaylistTracks -> CloudMusicContentKey.OnlinePlaylistTracks
        homeMode == CloudHomeMode.OnlineAlbumTracks -> CloudMusicContentKey.OnlineAlbumTracks
        !accountLibraryVisible || homeMode == CloudHomeMode.Featured -> CloudMusicContentKey.FeaturedTracks
        else -> CloudMusicContentKey.AccountTracks
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ComposeColor.Transparent),
    ) {
        AnimatedVisibility(
            visible = searchActive,
            enter = expandVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                fadeIn(animationSpec = tween(140)),
            exit = shrinkVertically(animationSpec = tween(160, easing = FastOutSlowInEasing)) +
                fadeOut(animationSpec = tween(120)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            CloudMusicSearchField(
                query = query,
                hint = stringResource(R.string.cloud_music_search_hint_netease),
                active = active && searchActive,
                onQueryChange = { value -> query = value },
                onCancel = {
                    query = ""
                    searchActive = false
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AnimatedVisibility(
            visible = featuredHomeVisible,
            enter = expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                fadeIn(animationSpec = tween(180)),
            exit = shrinkVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                fadeOut(animationSpec = tween(120)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            CloudMusicBannerStrip(
                banners = bannerItems,
                active = active,
                onBannerClick = ::openBannerTarget,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!searchActive) {
            CloudMusicHomeEntryRow(
                selectedEntry = selectedQuickEntry,
                onMineClick = ::selectMineOrLogin,
                onRecommendClick = ::selectRecommendHome,
                onRadioClick = ::selectRadio,
                onCollectionClick = ::selectCollections,
                onArtistClick = ::selectArtists,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            SmartisanDrawableBackground(
                drawableRes = R.drawable.account_background,
                modifier = Modifier.matchParentSize(),
            )
            Column(modifier = Modifier.matchParentSize()) {
                CloudMusicSectionTitle(
                    title = sectionTitle,
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    AnimatedContent(
                        targetState = contentKey,
                        transitionSpec = {
                            (
                                fadeIn(animationSpec = tween(180)) +
                                    slideInVertically(
                                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                                        initialOffsetY = { height -> height / 12 },
                                    )
                                ) togetherWith (
                                fadeOut(animationSpec = tween(120)) +
                                    slideOutVertically(
                                        animationSpec = tween(160, easing = FastOutSlowInEasing),
                                        targetOffsetY = { height -> -height / 16 },
                                    )
                                )
                        },
                        label = "cloud music content transition",
                        modifier = Modifier.matchParentSize(),
                    ) { key ->
                        when (key) {
                            CloudMusicContentKey.SearchPrompt -> {
                                CloudHotSearchContent(
                                    state = hotSearchState,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    onRetryClick = { authRevision += 1 },
                                    onKeywordClick = { keyword ->
                                        query = keyword
                                        searchCategory = CloudSearchCategory.All
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            CloudMusicContentKey.SearchResults -> {
                                CloudMusicSearchResultsContent(
                                    state = searchResultsState,
                                    selectedCategory = searchCategory,
                                    onCategoryChange = { category -> searchCategory = category },
                                    repository = activeRepository,
                                    authLoggedIn = authState.isLoggedIn,
                                    active = active,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    onRetryClick = { authRevision += 1 },
                                    onTrackMoreClick = onTrackMoreClick,
                                    onPlaylistClick = { playlist ->
                                        query = ""
                                        searchActive = false
                                        openOnlinePlaylist(playlist)
                                    },
                                    onAlbumClick = { album ->
                                        query = ""
                                        searchActive = false
                                        openOnlineAlbum(album)
                                    },
                                    onArtistClick = { artist ->
                                        openArtist(artist, CloudHomeMode.Featured)
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            CloudMusicContentKey.Playlists -> {
                                when {
                                    homeState == CloudMusicState.LoadingAccount -> CloudMusicBlankState(
                                        title = stringResource(R.string.cloud_music_account_playlists_loading),
                                        subtitle = null,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    homeState == CloudMusicState.AccountError -> CloudMusicBlankState(
                                        title = stringResource(R.string.cloud_music_account_playlists_error),
                                        subtitle = stringResource(R.string.cloud_music_empty_subtitle),
                                        actionText = stringResource(R.string.cloud_music_retry),
                                        onActionClick = { authRevision += 1 },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    accountPlaylists.isEmpty() && accountAlbums.isEmpty() && accountRadios.isEmpty() -> CloudMusicBlankState(
                                        title = stringResource(R.string.cloud_music_playlists_empty),
                                        subtitle = stringResource(R.string.cloud_music_empty_subtitle),
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    else -> CloudMusicAccountLibraryList(
                                        playlists = accountPlaylists,
                                        albums = accountAlbums,
                                        radios = accountRadios,
                                        selectedPlaylistId = selectedAccountPlaylistId,
                                        active = active,
                                        playbackBarOverlayHeight = playbackBarOverlayHeight,
                                        onPlaylistClick = { playlist ->
                                            selectedAccountPlaylistId = playlist.playlistId
                                            homeMode = CloudHomeMode.Tracks
                                        },
                                        onAlbumClick = { album ->
                                            openOnlineAlbum(album, CloudHomeMode.Playlists)
                                        },
                                        onRadioClick = { radio ->
                                            openRadioPrograms(radio, CloudHomeMode.Playlists)
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            CloudMusicContentKey.Artists -> {
                                CloudMusicArtistStateContent(
                                    state = artistState,
                                    active = active,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    onRetryClick = { authRevision += 1 },
                                    onArtistClick = { artist -> openArtist(artist, CloudHomeMode.Artists) },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            CloudMusicContentKey.Collections -> {
                                CloudFeaturedHomeCoverListContent(
                                    state = featuredHomeState,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    onRetryClick = { authRevision += 1 },
                                    items = { home -> home.playlists },
                                    title = OnlinePlaylist::title,
                                    subtitle = { playlist -> playlist.homeSubtitle(LocalContext.current) },
                                    imageUrl = OnlinePlaylist::artworkUrl,
                                    onItemClick = { playlist ->
                                        openOnlinePlaylist(playlist, CloudHomeMode.Collections)
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            CloudMusicContentKey.FeaturedPlaylists -> {
                                CloudFeaturedHomeCoverListContent(
                                    state = featuredHomeState,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    onRetryClick = { authRevision += 1 },
                                    items = { home -> home.playlists },
                                    title = OnlinePlaylist::title,
                                    subtitle = { playlist -> playlist.homeSubtitle(LocalContext.current) },
                                    imageUrl = OnlinePlaylist::artworkUrl,
                                    onItemClick = { playlist ->
                                        openOnlinePlaylist(playlist, CloudHomeMode.FeaturedPlaylists)
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            CloudMusicContentKey.FeaturedCharts -> {
                                CloudFeaturedHomeCoverListContent(
                                    state = featuredHomeState,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    onRetryClick = { authRevision += 1 },
                                    items = { home -> home.charts },
                                    title = OnlinePlaylist::title,
                                    subtitle = { playlist -> playlist.homeSubtitle(LocalContext.current) },
                                    imageUrl = OnlinePlaylist::artworkUrl,
                                    onItemClick = { playlist ->
                                        openOnlinePlaylist(playlist, CloudHomeMode.FeaturedCharts)
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            CloudMusicContentKey.FeaturedAlbums -> {
                                CloudFeaturedHomeCoverListContent(
                                    state = featuredHomeState,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    onRetryClick = { authRevision += 1 },
                                    items = { home -> home.albums },
                                    title = OnlineAlbum::title,
                                    subtitle = { album -> album.albumSubtitle(LocalContext.current) },
                                    imageUrl = OnlineAlbum::artworkUrl,
                                    onItemClick = { album ->
                                        openOnlineAlbum(album, CloudHomeMode.FeaturedAlbums)
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            CloudMusicContentKey.FeaturedArtists -> {
                                CloudFeaturedHomeArtistListContent(
                                    state = featuredHomeState,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    active = active,
                                    onRetryClick = { authRevision += 1 },
                                    onArtistClick = { artist ->
                                        openArtist(artist, CloudHomeMode.FeaturedArtists)
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            CloudMusicContentKey.ArtistAlbums -> {
                                CloudSearchCoverResultList(
                                    items = artistAlbums,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    title = OnlineAlbum::title,
                                    subtitle = { album -> album.albumSubtitle(context) },
                                    imageUrl = OnlineAlbum::artworkUrl,
                                    onItemClick = { album ->
                                        openOnlineAlbum(album, CloudHomeMode.ArtistAlbums)
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            CloudMusicContentKey.ArtistTracks -> {
                                val artist = selectedArtist
                                CloudMusicTrackDetailContent(
                                    state = artistTracksState,
                                    title = artist?.name,
                                    subtitle = artist?.subtitleText(context),
                                    artworkUrl = artist?.artworkUrl,
                                    repository = activeRepository,
                                    authLoggedIn = authState.isLoggedIn,
                                    active = active,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    onRetryClick = { authRevision += 1 },
                                    onTrackMoreClick = onTrackMoreClick,
                                    extraContent = if (artistAlbums.isNotEmpty() || artistIntroductions.isNotEmpty()) {
                                        {
                                            CloudArtistIntroductionSection(
                                                sections = artistIntroductions,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                            CloudHomeAlbumSection(
                                                title = stringResource(R.string.cloud_music_section_artist_albums),
                                                albums = artistAlbums,
                                                actionText = stringResource(R.string.cloud_music_section_view_all),
                                                onActionClick = ::openArtistAlbums,
                                                onAlbumClick = { album ->
                                                    openOnlineAlbum(album, CloudHomeMode.ArtistTracks)
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            CloudMusicContentKey.FeaturedTracks -> {
                                CloudFeaturedHomeContent(
                                    state = featuredHomeState,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    onRetryClick = { authRevision += 1 },
                                    onTracksClick = ::openFeaturedTracks,
                                    onPlaylistsClick = ::openFeaturedPlaylists,
                                    onChartsClick = ::openFeaturedCharts,
                                    onAlbumsClick = ::openFeaturedAlbums,
                                    onArtistsClick = ::openFeaturedArtists,
                                    onPlaylistClick = { playlist ->
                                        openOnlinePlaylist(playlist, CloudHomeMode.Featured)
                                    },
                                    onAlbumClick = { album ->
                                        openOnlineAlbum(album, CloudHomeMode.Featured)
                                    },
                                    onArtistClick = { artist ->
                                        openArtist(artist, CloudHomeMode.Featured)
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            CloudMusicContentKey.BannerTrack -> {
                                val banner = selectedBanner
                                CloudMusicTrackDetailContent(
                                    state = bannerTrackState,
                                    title = banner?.title,
                                    subtitle = banner?.subtitle,
                                    artworkUrl = banner?.imageUrl,
                                    repository = activeRepository,
                                    authLoggedIn = authState.isLoggedIn,
                                    active = active,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    onRetryClick = { authRevision += 1 },
                                    onTrackMoreClick = onTrackMoreClick,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            CloudMusicContentKey.Radio -> {
                                CloudRadioHomeContent(
                                    state = radioHomeState,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    onRetryClick = { authRevision += 1 },
                                    onTracksClick = ::openRadioTracks,
                                    onRadioListClick = ::openRadioList,
                                    onRadioClick = { radio -> openRadioPrograms(radio) },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            CloudMusicContentKey.RadioList -> {
                                CloudRadioListContent(
                                    state = radioHomeState,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    onRetryClick = { authRevision += 1 },
                                    onRadioClick = { radio -> openRadioPrograms(radio) },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            CloudMusicContentKey.RadioTracks -> {
                                CloudRadioTrackStateContent(
                                    state = radioHomeState,
                                    repository = activeRepository,
                                    authLoggedIn = authState.isLoggedIn,
                                    active = active,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    onRetryClick = { authRevision += 1 },
                                    onTrackMoreClick = onTrackMoreClick,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            CloudMusicContentKey.RadioPrograms -> {
                                val radio = selectedRadio
                                CloudMusicTrackDetailContent(
                                    state = radioTrackState,
                                    title = radio?.title,
                                    subtitle = radio?.subtitleText(context),
                                    artworkUrl = radio?.artworkUrl,
                                    repository = activeRepository,
                                    authLoggedIn = authState.isLoggedIn,
                                    active = active,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    onRetryClick = { authRevision += 1 },
                                    onTrackMoreClick = onTrackMoreClick,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            CloudMusicContentKey.FeaturedTracksDetail -> {
                                CloudMusicStateContent(
                                    state = featuredTrackDetailState,
                                    repository = activeRepository,
                                    authLoggedIn = authState.isLoggedIn,
                                    active = active,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    onRetryClick = { authRevision += 1 },
                                    onTrackMoreClick = onTrackMoreClick,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            CloudMusicContentKey.OnlinePlaylistTracks -> {
                                val playlist = selectedOnlinePlaylist
                                CloudMusicTrackDetailContent(
                                    state = onlinePlaylistTracksState,
                                    title = playlist?.title,
                                    subtitle = playlist?.homeSubtitle(context),
                                    artworkUrl = playlist?.artworkUrl,
                                    repository = activeRepository,
                                    authLoggedIn = authState.isLoggedIn,
                                    active = active,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    onRetryClick = { authRevision += 1 },
                                    onTrackMoreClick = onTrackMoreClick,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            CloudMusicContentKey.OnlineAlbumTracks -> {
                                val album = selectedOnlineAlbum
                                CloudMusicTrackDetailContent(
                                    state = onlineAlbumTracksState,
                                    title = album?.title,
                                    subtitle = album?.albumSubtitle(context),
                                    artworkUrl = album?.artworkUrl,
                                    repository = activeRepository,
                                    authLoggedIn = authState.isLoggedIn,
                                    active = active,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    onRetryClick = { authRevision += 1 },
                                    onTrackMoreClick = onTrackMoreClick,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            CloudMusicContentKey.AccountTracks -> {
                                CloudMusicStateContent(
                                    state = homeState,
                                    repository = activeRepository,
                                    authLoggedIn = authState.isLoggedIn,
                                    active = active,
                                    playbackBarOverlayHeight = playbackBarOverlayHeight,
                                    onRetryClick = { authRevision += 1 },
                                    onTrackMoreClick = onTrackMoreClick,
                                    accountPlaylist = selectedAccountPlaylist,
                                    onAccountPlaylistTracksChanged = { authRevision += 1 },
                                    onAccountPlaylistDeleted = { deletedPlaylist ->
                                        accountPlaylists = accountPlaylists.filterNot { playlist ->
                                            playlist.playlistId == deletedPlaylist.playlistId
                                        }
                                        selectedAccountPlaylistId = null
                                        homeMode = CloudHomeMode.Playlists
                                        authRevision += 1
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudMusicSearchField(
    query: String,
    hint: String,
    active: Boolean,
    onQueryChange: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(active) {
        if (active) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        }
    }

    Row(
        modifier = modifier
            .height(CloudSearchBarHeight)
            .background(ComposeColor.White)
            .padding(horizontal = 15.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = ComposeColor(0xCC000000),
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
            ),
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                val clearInteractionSource = remember { MutableInteractionSource() }
                val clearPressed by clearInteractionSource.collectIsPressedAsState()
                Box(modifier = Modifier.fillMaxSize()) {
                    SmartisanDrawableBackground(
                        drawableRes = R.drawable.search_field,
                        modifier = Modifier.matchParentSize(),
                    )
                    Image(
                        painter = painterResource(R.drawable.search_bar_left_icon),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 6.dp)
                            .width(24.dp)
                            .height(30.dp),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(
                                start = 36.dp,
                                end = if (query.isNotEmpty()) 30.dp else 12.dp,
                            ),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = hint,
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    color = ComposeColor(0x66000000),
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .align(Alignment.CenterEnd)
                                .clickable(
                                    interactionSource = clearInteractionSource,
                                    indication = null,
                                    onClick = { onQueryChange("") },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(
                                    if (clearPressed) {
                                        R.drawable.text_clear_btn_pressed
                                    } else {
                                        R.drawable.text_clear_btn
                                    },
                                ),
                                contentDescription = stringResource(R.string.clear_search_text),
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }
                }
            },
        )
        Text(
            text = stringResource(R.string.cloud_music_search_cancel),
            style = TextStyle(
                fontSize = 14.sp,
                color = CloudAccentColor,
            ),
            maxLines = 1,
            modifier = Modifier
                .padding(start = 12.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onCancel,
                ),
        )
    }
}

@Composable
private fun CloudMusicHomeEntryRow(
    selectedEntry: CloudMusicHomeEntry?,
    onMineClick: () -> Unit,
    onRecommendClick: () -> Unit,
    onRadioClick: () -> Unit,
    onCollectionClick: () -> Unit,
    onArtistClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = listOf(
        CloudMusicHomeEntry.Mine to onMineClick,
        CloudMusicHomeEntry.Recommend to onRecommendClick,
        CloudMusicHomeEntry.Radio to onRadioClick,
        CloudMusicHomeEntry.Collection to onCollectionClick,
        CloudMusicHomeEntry.Artist to onArtistClick,
    )
    Column(modifier = modifier.background(ComposeColor.White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(CloudHomeEntryRowHeight)
                .padding(start = 12.dp, end = 12.dp, top = 11.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            entries.forEach { (entry, onClick) ->
                CloudMusicHomeEntryButton(
                    entry = entry,
                    selected = selectedEntry == entry,
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        CloudMusicDivider()
    }
}

@Composable
private fun CloudMusicHomeEntryButton(
    entry: CloudMusicHomeEntry,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(entry.iconRes),
            contentDescription = stringResource(entry.labelRes),
            modifier = Modifier.size(36.dp),
        )
        Text(
            text = stringResource(entry.labelRes),
            style = TextStyle(
                fontSize = 10.sp,
                color = if (selected) CloudAccentColor else ComposeColor(0x99000000),
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}

@Composable
private fun CloudMusicSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.height(CloudSectionTitleHeight),
        contentAlignment = Alignment.CenterStart,
    ) {
        SmartisanDrawableBackground(
            drawableRes = R.drawable.home_recommend_title_bg,
            modifier = Modifier.matchParentSize(),
        )
        Text(
            text = title,
            style = TextStyle(
                fontSize = 15.sp,
                color = ComposeColor(0xCC000000),
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 11.dp, end = 32.dp),
        )
    }
}

@Composable
private fun CloudMusicDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.67.dp)
            .background(ComposeColor(0xFFEBEBEB)),
    )
}

@Composable
private fun CloudMusicBannerStrip(
    banners: List<OnlineBanner>,
    active: Boolean,
    onBannerClick: (OnlineBanner) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fallbackTitle = stringResource(R.string.cloud_music_banner_fallback_title)
    val fallbackSubtitle = stringResource(R.string.cloud_music_banner_fallback_subtitle)
    val fallbackBanner = remember(fallbackTitle, fallbackSubtitle) {
        OnlineBanner(
            provider = OnlineMusicProvider.Netease,
            bannerId = "netease-fallback",
            title = fallbackTitle,
            subtitle = fallbackSubtitle,
        )
    }
    val visibleBanners = remember(banners, fallbackBanner) {
        banners
            .filter { banner -> banner.title.isNotBlank() || !banner.imageUrl.isNullOrBlank() }
            .take(CloudBannerMaxCount)
            .ifEmpty { listOf(fallbackBanner) }
    }
    var currentIndex by remember(visibleBanners) { mutableStateOf(0) }
    val safeIndex = currentIndex.coerceIn(0, visibleBanners.lastIndex)

    LaunchedEffect(active, visibleBanners) {
        if (!active || visibleBanners.size <= 1) {
            return@LaunchedEffect
        }
        while (true) {
            delay(CloudBannerAutoScrollMs)
            currentIndex = (currentIndex + 1) % visibleBanners.size
        }
    }

    Box(
        modifier = modifier
            .height(CloudBannerHeight)
            .background(ComposeColor.White)
            .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(5.dp))
                .border(
                    width = 0.67.dp,
                    color = ComposeColor(0x1F000000),
                    shape = RoundedCornerShape(5.dp),
                ),
        ) {
            AnimatedContent(
                targetState = visibleBanners[safeIndex],
                transitionSpec = {
                    (
                        fadeIn(animationSpec = tween(220)) +
                            slideInVertically(
                                animationSpec = tween(260, easing = FastOutSlowInEasing),
                                initialOffsetY = { height -> height / 8 },
                            )
                        ) togetherWith (
                        fadeOut(animationSpec = tween(160)) +
                            slideOutVertically(
                                animationSpec = tween(220, easing = FastOutSlowInEasing),
                                targetOffsetY = { height -> -height / 10 },
                            )
                        )
                },
                label = "cloud music banner transition",
                modifier = Modifier.matchParentSize(),
            ) { banner ->
                val bannerClickable = !banner.targetTrackId.isNullOrBlank() ||
                    !banner.targetAlbumId.isNullOrBlank() ||
                    !banner.targetPlaylistId.isNullOrBlank()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            enabled = bannerClickable,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { onBannerClick(banner) },
                        ),
                ) {
                    CloudMusicBannerImage(
                        imageUrl = banner.imageUrl,
                        modifier = Modifier.matchParentSize(),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(ComposeColor(0x99000000))
                            .padding(horizontal = 13.dp, vertical = 8.dp),
                    ) {
                        Column {
                            Text(
                                text = banner.title,
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color = ComposeColor.White,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            banner.subtitle?.takeIf(String::isNotBlank)?.let { subtitle ->
                                Text(
                                    text = subtitle,
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        color = ComposeColor(0xCCFFFFFF),
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                        }
                    }
                }
            }
            if (visibleBanners.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    visibleBanners.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .width(if (index == safeIndex) 12.dp else 5.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(
                                    if (index == safeIndex) {
                                        ComposeColor.White
                                    } else {
                                        ComposeColor(0x80FFFFFF)
                                    },
                                ),
                        )
                    }
                }
            }
        }
    }
    CloudMusicDivider()
}

@Composable
private fun CloudMusicBannerImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, imageUrl) {
        value = null
        val safeUrl = imageUrl?.takeIf(String::isNotBlank) ?: return@produceState
        value = runCatching {
            val mediaItem = MediaItem.Builder()
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setArtworkUri(Uri.parse(safeUrl))
                        .build(),
                )
                .build()
            loadArtworkBitmap(
                context = context.applicationContext,
                mediaItem = mediaItem,
                size = Size(CloudBannerArtworkWidthPx, CloudBannerArtworkHeightPx),
            )
        }.getOrNull()
    }
    val loadedBitmap = bitmap
    if (loadedBitmap != null) {
        Image(
            bitmap = loadedBitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(ComposeColor(0xFFB9312D)),
        )
    }
}

@Composable
private fun CloudMusicTrackDetailContent(
    state: CloudMusicState,
    title: String?,
    subtitle: String?,
    artworkUrl: String?,
    repository: OnlineMusicProviderRepository,
    authLoggedIn: Boolean,
    active: Boolean,
    playbackBarOverlayHeight: Dp,
    onRetryClick: () -> Unit,
    onTrackMoreClick: (MediaItem) -> Unit,
    extraContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (!title.isNullOrBlank() || !subtitle.isNullOrBlank() || !artworkUrl.isNullOrBlank()) {
            CloudMusicDetailHeader(
                title = title.orEmpty(),
                subtitle = subtitle,
                artworkUrl = artworkUrl,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        extraContent?.invoke()
        CloudMusicStateContent(
            state = state,
            repository = repository,
            authLoggedIn = authLoggedIn,
            active = active,
            playbackBarOverlayHeight = playbackBarOverlayHeight,
            onRetryClick = onRetryClick,
            onTrackMoreClick = onTrackMoreClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun CloudMusicDetailHeader(
    title: String,
    subtitle: String?,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.background(ComposeColor.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(CloudDetailHeaderHeight)
                .padding(start = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CloudMusicCoverImage(
                imageUrl = artworkUrl,
                modifier = Modifier
                    .size(CloudDetailHeaderArtworkSize)
                    .clip(RoundedCornerShape(4.dp))
                    .border(
                        width = 0.67.dp,
                        color = ComposeColor(0x1F000000),
                        shape = RoundedCornerShape(4.dp),
                    ),
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = ComposeColor(0xCC000000),
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.takeIf(String::isNotBlank)?.let { subtitleText ->
                    Text(
                        text = subtitleText,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = CloudSecondaryTextColor,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
        CloudMusicDivider()
    }
}

@Composable
private fun CloudMusicStateContent(
    state: CloudMusicState,
    repository: OnlineMusicProviderRepository,
    authLoggedIn: Boolean,
    active: Boolean,
    playbackBarOverlayHeight: Dp,
    onRetryClick: () -> Unit,
    onTrackMoreClick: (MediaItem) -> Unit,
    accountPlaylist: OnlineAccountPlaylist? = null,
    onAccountPlaylistTracksChanged: () -> Unit = {},
    onAccountPlaylistDeleted: (OnlineAccountPlaylist) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when (state) {
        CloudMusicState.Idle -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_empty_title),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            modifier = modifier,
        )
        CloudMusicState.LoadingAccount -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_account_tracks_loading),
            subtitle = null,
            modifier = modifier,
        )
        CloudMusicState.LoadingFeatured -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_featured_loading),
            subtitle = null,
            modifier = modifier,
        )
        CloudMusicState.LoadingRadio -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_radio_loading),
            subtitle = null,
            modifier = modifier,
        )
        CloudMusicState.AccountEmpty -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_liked_empty),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            modifier = modifier,
        )
        CloudMusicState.AccountError -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_liked_error),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            actionText = stringResource(R.string.cloud_music_retry),
            onActionClick = onRetryClick,
            modifier = modifier,
        )
        CloudMusicState.FeaturedEmpty -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_empty_title),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            modifier = modifier,
        )
        CloudMusicState.FeaturedError -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_featured_error),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            actionText = stringResource(R.string.cloud_music_retry),
            onActionClick = onRetryClick,
            modifier = modifier,
        )
        CloudMusicState.RadioEmpty -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_radio_empty),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            modifier = modifier,
        )
        CloudMusicState.RadioError -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_radio_error),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            actionText = stringResource(R.string.cloud_music_retry),
            onActionClick = onRetryClick,
            modifier = modifier,
        )
        CloudMusicState.RadioLoginRequired -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_radio_login_required),
            subtitle = stringResource(R.string.cloud_music_radio_login_required_subtitle),
            modifier = modifier,
        )
        is CloudMusicState.Success -> CloudMusicResultList(
            tracks = state.tracks,
            repository = repository,
            playFailedMessageRes = when {
                !authLoggedIn -> R.string.cloud_music_login_in_settings
                else -> R.string.netease_online_music_play_failed
            },
            active = active,
            playbackBarOverlayHeight = playbackBarOverlayHeight,
            onTrackMoreClick = onTrackMoreClick,
            editableAccountPlaylist = accountPlaylist?.takeIf(OnlineAccountPlaylist::isEditable),
            onAccountPlaylistTracksChanged = onAccountPlaylistTracksChanged,
            onAccountPlaylistDeleted = onAccountPlaylistDeleted,
            modifier = modifier,
        )
    }
}

@Composable
private fun CloudMusicSearchResultsContent(
    state: CloudSearchResultsState,
    selectedCategory: CloudSearchCategory,
    onCategoryChange: (CloudSearchCategory) -> Unit,
    repository: OnlineMusicProviderRepository,
    authLoggedIn: Boolean,
    active: Boolean,
    playbackBarOverlayHeight: Dp,
    onRetryClick: () -> Unit,
    onTrackMoreClick: (MediaItem) -> Unit,
    onPlaylistClick: (OnlinePlaylist) -> Unit,
    onAlbumClick: (OnlineAlbum) -> Unit,
    onArtistClick: (OnlineArtist) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        CloudSearchResultsState.Idle -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_empty_title),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            modifier = modifier,
        )
        CloudSearchResultsState.Loading -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_loading),
            subtitle = null,
            modifier = modifier,
        )
        is CloudSearchResultsState.Empty -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_no_result),
            subtitle = state.query,
            modifier = modifier,
        )
        is CloudSearchResultsState.Error -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_error),
            subtitle = state.query,
            actionText = stringResource(R.string.cloud_music_retry),
            onActionClick = onRetryClick,
            modifier = modifier,
        )
        is CloudSearchResultsState.Success -> {
            Column(modifier = modifier) {
                CloudSearchCategoryBar(
                    selectedCategory = selectedCategory,
                    onCategoryChange = onCategoryChange,
                    modifier = Modifier.fillMaxWidth(),
                )
                when (selectedCategory) {
                    CloudSearchCategory.All -> CloudSearchAllResults(
                        results = state.results,
                        playbackBarOverlayHeight = playbackBarOverlayHeight,
                        onTracksClick = { onCategoryChange(CloudSearchCategory.Tracks) },
                        onArtistsClick = { onCategoryChange(CloudSearchCategory.Artists) },
                        onAlbumsClick = { onCategoryChange(CloudSearchCategory.Albums) },
                        onPlaylistsClick = { onCategoryChange(CloudSearchCategory.Playlists) },
                        onPlaylistClick = onPlaylistClick,
                        onAlbumClick = onAlbumClick,
                        onArtistClick = onArtistClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    CloudSearchCategory.Tracks -> {
                        if (state.results.tracks.isEmpty()) {
                            CloudMusicBlankState(
                                title = stringResource(R.string.cloud_music_no_result),
                                subtitle = state.results.query,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            )
                        } else {
                            CloudMusicResultList(
                                tracks = state.results.tracks,
                                repository = repository,
                                playFailedMessageRes = when {
                                    !authLoggedIn -> R.string.cloud_music_login_in_settings
                                    else -> R.string.netease_online_music_play_failed
                                },
                                active = active,
                                playbackBarOverlayHeight = playbackBarOverlayHeight,
                                onTrackMoreClick = onTrackMoreClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            )
                        }
                    }
                    CloudSearchCategory.Artists -> {
                        if (state.results.artists.isEmpty()) {
                            CloudMusicBlankState(
                                title = stringResource(R.string.cloud_music_no_result),
                                subtitle = state.results.query,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            )
                        } else {
                            CloudMusicArtistList(
                                artists = state.results.artists,
                                selectedArtistId = null,
                                active = active,
                                playbackBarOverlayHeight = playbackBarOverlayHeight,
                                onArtistClick = onArtistClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            )
                        }
                    }
                    CloudSearchCategory.Albums -> CloudSearchCoverResultList(
                        items = state.results.albums,
                        playbackBarOverlayHeight = playbackBarOverlayHeight,
                        title = OnlineAlbum::title,
                        subtitle = { album -> album.albumSubtitle(LocalContext.current) },
                        imageUrl = OnlineAlbum::artworkUrl,
                        onItemClick = onAlbumClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    CloudSearchCategory.Playlists -> CloudSearchCoverResultList(
                        items = state.results.playlists,
                        playbackBarOverlayHeight = playbackBarOverlayHeight,
                        title = OnlinePlaylist::title,
                        subtitle = { playlist -> playlist.homeSubtitle(LocalContext.current) },
                        imageUrl = OnlinePlaylist::artworkUrl,
                        onItemClick = onPlaylistClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudHotSearchContent(
    state: CloudHotSearchState,
    playbackBarOverlayHeight: Dp,
    onRetryClick: () -> Unit,
    onKeywordClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        CloudHotSearchState.Idle,
        CloudHotSearchState.Loading -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_hot_search_loading),
            subtitle = null,
            modifier = modifier,
        )
        CloudHotSearchState.Empty -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_hot_search_empty),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            modifier = modifier,
        )
        CloudHotSearchState.Error -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_hot_search_error),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            actionText = stringResource(R.string.cloud_music_retry),
            onActionClick = onRetryClick,
            modifier = modifier,
        )
        is CloudHotSearchState.Success -> {
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .background(ComposeColor.White)
                    .padding(bottom = playbackBarOverlayHeight + 10.dp),
            ) {
                CloudHomeSectionHeader(
                    title = stringResource(R.string.cloud_music_section_hot_search),
                    actionText = null,
                    onClick = null,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.keywords.forEachIndexed { index, keyword ->
                    CloudHotSearchRow(
                        index = index + 1,
                        keyword = keyword,
                        onClick = { onKeywordClick(keyword.keyword) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CloudMusicDivider()
                }
            }
        }
    }
}

@Composable
private fun CloudHotSearchRow(
    index: Int,
    keyword: OnlineSearchHotKeyword,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val heatText = keyword.score.toHotSearchHeatText(context)
    Row(
        modifier = modifier
            .height(CloudHotSearchRowHeight)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(start = 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = index.toString(),
            style = TextStyle(
                fontSize = 14.sp,
                color = if (index <= 3) CloudAccentColor else ComposeColor(0x66000000),
            ),
            modifier = Modifier.width(28.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = keyword.keyword,
                style = TextStyle(
                    fontSize = 15.sp,
                    color = ComposeColor(0xCC000000),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            keyword.subtitle?.takeIf(String::isNotBlank)?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = CloudSecondaryTextColor,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
        if (heatText != null) {
            Text(
                text = heatText,
                style = TextStyle(
                    fontSize = 10.sp,
                    color = ComposeColor(0x4D000000),
                ),
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun CloudSearchCategoryBar(
    selectedCategory: CloudSearchCategory,
    onCategoryChange: (CloudSearchCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(CloudSearchCategoryBarHeight)
            .background(ComposeColor.White),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CloudSearchCategory.entries.forEach { category ->
            val selected = category == selectedCategory
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { onCategoryChange(category) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(category.labelRes),
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = if (selected) CloudAccentColor else ComposeColor(0x99000000),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(22.dp)
                            .height(2.dp)
                            .background(CloudAccentColor),
                    )
                }
            }
        }
    }
    CloudMusicDivider()
}

@Composable
private fun CloudSearchAllResults(
    results: OnlineSearchResults,
    playbackBarOverlayHeight: Dp,
    onTracksClick: () -> Unit,
    onArtistsClick: () -> Unit,
    onAlbumsClick: () -> Unit,
    onPlaylistsClick: () -> Unit,
    onPlaylistClick: (OnlinePlaylist) -> Unit,
    onAlbumClick: (OnlineAlbum) -> Unit,
    onArtistClick: (OnlineArtist) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = playbackBarOverlayHeight + 10.dp),
    ) {
        CloudHomeTrackPreviewSection(
            title = stringResource(R.string.search_tab_songs),
            tracks = results.tracks,
            onClick = onTracksClick,
            modifier = Modifier.fillMaxWidth(),
        )
        CloudHomeArtistSection(
            title = stringResource(R.string.search_tab_artists),
            artists = results.artists,
            actionText = stringResource(R.string.cloud_music_section_view_all),
            onActionClick = onArtistsClick,
            onArtistClick = onArtistClick,
            modifier = Modifier.fillMaxWidth(),
        )
        CloudHomeAlbumSection(
            title = stringResource(R.string.search_tab_albums),
            albums = results.albums,
            actionText = stringResource(R.string.cloud_music_section_view_all),
            onActionClick = onAlbumsClick,
            onAlbumClick = onAlbumClick,
            modifier = Modifier.fillMaxWidth(),
        )
        CloudHomePlaylistSection(
            title = stringResource(R.string.cloud_music_entry_collection),
            playlists = results.playlists,
            actionText = stringResource(R.string.cloud_music_section_view_all),
            onActionClick = onPlaylistsClick,
            onPlaylistClick = onPlaylistClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun <T> CloudSearchCoverResultList(
    items: List<T>,
    playbackBarOverlayHeight: Dp,
    title: (T) -> String,
    subtitle: @Composable (T) -> String?,
    imageUrl: (T) -> String?,
    onItemClick: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_no_result),
            subtitle = null,
            modifier = modifier,
        )
        return
    }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .background(ComposeColor.White)
            .padding(bottom = playbackBarOverlayHeight + 10.dp),
    ) {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CloudSearchCoverRowHeight)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { onItemClick(item) },
                    )
                    .padding(start = 12.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CloudMusicCoverImage(
                    imageUrl = imageUrl(item),
                    modifier = Modifier
                        .size(CloudSearchCoverArtworkSize)
                        .clip(RoundedCornerShape(4.dp))
                        .border(
                            width = 0.67.dp,
                            color = ComposeColor(0x1F000000),
                            shape = RoundedCornerShape(4.dp),
                        ),
                )
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = title(item),
                        style = TextStyle(
                            fontSize = 15.sp,
                            color = ComposeColor(0xCC000000),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    subtitle(item)?.takeIf(String::isNotBlank)?.let { subtitleText ->
                        Text(
                            text = subtitleText,
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = CloudSecondaryTextColor,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }
                }
            }
            CloudMusicDivider()
        }
    }
}

@Composable
private fun CloudFeaturedHomeContent(
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
        CloudFeaturedHomeState.Loading -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_featured_loading),
            subtitle = null,
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
                CloudHomeTrackPreviewSection(
                    title = stringResource(R.string.cloud_music_section_daily_recommend),
                    tracks = state.home.tracks,
                    onClick = onTracksClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                CloudHomePlaylistSection(
                    title = stringResource(R.string.cloud_music_section_featured_playlists),
                    playlists = state.home.playlists,
                    actionText = stringResource(R.string.cloud_music_section_view_all),
                    onActionClick = onPlaylistsClick,
                    onPlaylistClick = onPlaylistClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                CloudHomePlaylistSection(
                    title = stringResource(R.string.cloud_music_section_hot_charts),
                    playlists = state.home.charts,
                    actionText = stringResource(R.string.cloud_music_section_view_all),
                    onActionClick = onChartsClick,
                    onPlaylistClick = onPlaylistClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                CloudHomeAlbumSection(
                    title = stringResource(R.string.cloud_music_section_new_albums),
                    albums = state.home.albums,
                    actionText = stringResource(R.string.cloud_music_section_view_all),
                    onActionClick = onAlbumsClick,
                    onAlbumClick = onAlbumClick,
                    modifier = Modifier.fillMaxWidth(),
                )
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

@Composable
private fun CloudRadioHomeContent(
    state: CloudRadioHomeState,
    playbackBarOverlayHeight: Dp,
    onRetryClick: () -> Unit,
    onTracksClick: () -> Unit,
    onRadioListClick: () -> Unit,
    onRadioClick: (OnlineRadio) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        CloudRadioHomeState.Loading -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_radio_loading),
            subtitle = null,
            modifier = modifier,
        )
        CloudRadioHomeState.Empty -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_radio_empty),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            modifier = modifier,
        )
        CloudRadioHomeState.Error -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_radio_error),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            actionText = stringResource(R.string.cloud_music_retry),
            onActionClick = onRetryClick,
            modifier = modifier,
        )
        is CloudRadioHomeState.Success -> {
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = playbackBarOverlayHeight + 10.dp),
            ) {
                CloudHomeTrackPreviewSection(
                    title = stringResource(R.string.cloud_music_section_radio_programs),
                    tracks = state.home.tracks,
                    onClick = onTracksClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                CloudHomeRadioSection(
                    title = stringResource(R.string.cloud_music_section_hot_radios),
                    radios = state.home.radios,
                    actionText = stringResource(R.string.cloud_music_section_view_all),
                    onActionClick = onRadioListClick,
                    onRadioClick = onRadioClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CloudRadioListContent(
    state: CloudRadioHomeState,
    playbackBarOverlayHeight: Dp,
    onRetryClick: () -> Unit,
    onRadioClick: (OnlineRadio) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        CloudRadioHomeState.Loading -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_radio_loading),
            subtitle = null,
            modifier = modifier,
        )
        CloudRadioHomeState.Empty -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_radio_empty),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            modifier = modifier,
        )
        CloudRadioHomeState.Error -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_radio_error),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            actionText = stringResource(R.string.cloud_music_retry),
            onActionClick = onRetryClick,
            modifier = modifier,
        )
        is CloudRadioHomeState.Success -> CloudSearchCoverResultList(
            items = state.home.radios,
            playbackBarOverlayHeight = playbackBarOverlayHeight,
            title = OnlineRadio::title,
            subtitle = { radio -> radio.subtitleText(LocalContext.current) },
            imageUrl = OnlineRadio::artworkUrl,
            onItemClick = onRadioClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun CloudRadioTrackStateContent(
    state: CloudRadioHomeState,
    repository: OnlineMusicProviderRepository,
    authLoggedIn: Boolean,
    active: Boolean,
    playbackBarOverlayHeight: Dp,
    onRetryClick: () -> Unit,
    onTrackMoreClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        CloudRadioHomeState.Loading -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_radio_loading),
            subtitle = null,
            modifier = modifier,
        )
        CloudRadioHomeState.Empty -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_radio_empty),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            modifier = modifier,
        )
        CloudRadioHomeState.Error -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_radio_error),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            actionText = stringResource(R.string.cloud_music_retry),
            onActionClick = onRetryClick,
            modifier = modifier,
        )
        is CloudRadioHomeState.Success -> CloudMusicStateContent(
            state = if (state.home.tracks.isEmpty()) {
                CloudMusicState.RadioEmpty
            } else {
                CloudMusicState.Success(state.home.tracks)
            },
            repository = repository,
            authLoggedIn = authLoggedIn,
            active = active,
            playbackBarOverlayHeight = playbackBarOverlayHeight,
            onRetryClick = onRetryClick,
            onTrackMoreClick = onTrackMoreClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun <T> CloudFeaturedHomeCoverListContent(
    state: CloudFeaturedHomeState,
    playbackBarOverlayHeight: Dp,
    onRetryClick: () -> Unit,
    items: (OnlineMusicHome) -> List<T>,
    title: (T) -> String,
    subtitle: @Composable (T) -> String?,
    imageUrl: (T) -> String?,
    onItemClick: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        CloudFeaturedHomeState.Loading -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_featured_loading),
            subtitle = null,
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
        )
    }
}

@Composable
private fun CloudFeaturedHomeArtistListContent(
    state: CloudFeaturedHomeState,
    active: Boolean,
    playbackBarOverlayHeight: Dp,
    onRetryClick: () -> Unit,
    onArtistClick: (OnlineArtist) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        CloudFeaturedHomeState.Loading -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_artists_loading),
            subtitle = null,
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
private fun CloudHomeTrackPreviewSection(
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
            actionText = stringResource(R.string.cloud_music_section_view_all),
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        )
        tracks.take(CloudHomeTrackPreviewCount).forEachIndexed { index, track ->
            CloudHomeTrackPreviewRow(
                index = index + 1,
                track = track,
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
            )
            CloudMusicDivider()
        }
    }
}

@Composable
private fun CloudArtistIntroductionSection(
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
                maxLines = CloudArtistIntroMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
        CloudMusicDivider()
    }
}

@Composable
private fun CloudHomePlaylistSection(
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
        visiblePlaylists.forEachIndexed { index, playlist ->
            CloudHomeCoverCard(
                title = playlist.title,
                subtitle = playlist.homeSubtitle(context),
                imageUrl = playlist.artworkUrl,
                onClick = { onPlaylistClick(playlist) },
            )
            if (index != visiblePlaylists.lastIndex) {
                Spacer(modifier = Modifier.width(10.dp))
            }
        }
    }
}

@Composable
private fun CloudHomeAlbumSection(
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
    CloudHomeCoverSection(
        title = title,
        actionText = actionText,
        onActionClick = onActionClick,
        modifier = modifier,
    ) {
        visibleAlbums.forEachIndexed { index, album ->
            CloudHomeCoverCard(
                title = album.title,
                subtitle = album.artist ?: stringResource(R.string.cloud_music_album_provider_netease),
                imageUrl = album.artworkUrl,
                onClick = { onAlbumClick(album) },
            )
            if (index != visibleAlbums.lastIndex) {
                Spacer(modifier = Modifier.width(10.dp))
            }
        }
    }
}

@Composable
private fun CloudHomeArtistSection(
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
    val visibleArtists = remember(artists) {
        artists.take(CloudHomeCoverPreviewCount)
    }
    CloudHomeCoverSection(
        title = title,
        actionText = actionText,
        onActionClick = onActionClick,
        modifier = modifier,
    ) {
        visibleArtists.forEachIndexed { index, artist ->
            CloudHomeCoverCard(
                title = artist.name,
                subtitle = artist.subtitleText(LocalContext.current),
                imageUrl = artist.artworkUrl,
                onClick = { onArtistClick(artist) },
            )
            if (index != visibleArtists.lastIndex) {
                Spacer(modifier = Modifier.width(10.dp))
            }
        }
    }
}

@Composable
private fun CloudHomeRadioSection(
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
    val visibleRadios = remember(radios) {
        radios.take(CloudHomeCoverPreviewCount)
    }
    CloudHomeCoverSection(
        title = title,
        actionText = actionText,
        onActionClick = onActionClick,
        modifier = modifier,
    ) {
        visibleRadios.forEachIndexed { index, radio ->
            CloudHomeCoverCard(
                title = radio.title,
                subtitle = radio.cardSubtitle(LocalContext.current),
                imageUrl = radio.artworkUrl,
                onClick = { onRadioClick(radio) },
            )
            if (index != visibleRadios.lastIndex) {
                Spacer(modifier = Modifier.width(10.dp))
            }
        }
    }
}

@Composable
private fun CloudHomeCoverSection(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier = modifier.background(ComposeColor.White),
    ) {
        CloudHomeSectionHeader(
            title = title,
            actionText = actionText,
            onClick = onActionClick,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 12.dp),
        ) {
            content()
        }
        CloudMusicDivider()
    }
}

@Composable
private fun CloudHomeSectionHeader(
    title: String,
    actionText: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(CloudSectionTitleHeight)
            .clickable(
                enabled = onClick != null,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { onClick?.invoke() },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        SmartisanDrawableBackground(
            drawableRes = R.drawable.home_recommend_title_bg,
            modifier = Modifier.matchParentSize(),
        )
        Text(
            text = title,
            style = TextStyle(
                fontSize = 15.sp,
                color = ComposeColor(0xCC000000),
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 11.dp, end = if (actionText == null) 12.dp else 72.dp),
        )
        if (actionText != null) {
            Text(
                text = actionText,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = CloudAccentColor,
                ),
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
            )
        }
    }
}

@Composable
private fun CloudHomeTrackPreviewRow(
    index: Int,
    track: OnlineTrack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(CloudHomeTrackPreviewRowHeight)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(start = 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = index.toString(),
            style = TextStyle(
                fontSize = 13.sp,
                color = ComposeColor(0x66000000),
            ),
            modifier = Modifier.width(26.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = track.title,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = ComposeColor(0xCC000000),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist.takeIf(String::isNotBlank)
                    ?: track.album
                    ?: stringResource(R.string.cloud_music_provider_netease),
                style = TextStyle(
                    fontSize = 11.sp,
                    color = CloudSecondaryTextColor,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun CloudHomeCoverCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(CloudHomeCoverCardWidth)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
    ) {
        CloudMusicCoverImage(
            imageUrl = imageUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(4.dp))
                .border(
                    width = 0.67.dp,
                    color = ComposeColor(0x1F000000),
                    shape = RoundedCornerShape(4.dp),
                ),
        )
        Text(
            text = title,
            style = TextStyle(
                fontSize = 12.sp,
                color = ComposeColor(0xCC000000),
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp),
        )
        subtitle?.takeIf(String::isNotBlank)?.let { subtitleText ->
            Text(
                text = subtitleText,
                style = TextStyle(
                    fontSize = 10.sp,
                    color = CloudSecondaryTextColor,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun CloudMusicCoverImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, imageUrl) {
        value = null
        val safeUrl = imageUrl?.takeIf(String::isNotBlank) ?: return@produceState
        value = runCatching {
            val mediaItem = MediaItem.Builder()
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setArtworkUri(Uri.parse(safeUrl))
                        .build(),
                )
                .build()
            loadArtworkBitmap(
                context = context.applicationContext,
                mediaItem = mediaItem,
                size = Size(CloudCoverArtworkSizePx, CloudCoverArtworkSizePx),
            )
        }.getOrNull()
    }
    val loadedBitmap = bitmap
    if (loadedBitmap != null) {
        Image(
            bitmap = loadedBitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(ComposeColor(0xFFEDEDED)),
        )
    }
}

@Composable
private fun CloudMusicArtistStateContent(
    state: CloudArtistState,
    active: Boolean,
    playbackBarOverlayHeight: Dp,
    onRetryClick: () -> Unit,
    onArtistClick: (OnlineArtist) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        CloudArtistState.Idle,
        CloudArtistState.Loading -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_artists_loading),
            subtitle = null,
            modifier = modifier,
        )
        CloudArtistState.Empty -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_artists_empty),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            modifier = modifier,
        )
        CloudArtistState.Error -> CloudMusicBlankState(
            title = stringResource(R.string.cloud_music_artists_error),
            subtitle = stringResource(R.string.cloud_music_empty_subtitle),
            actionText = stringResource(R.string.cloud_music_retry),
            onActionClick = onRetryClick,
            modifier = modifier,
        )
        is CloudArtistState.Success -> CloudMusicArtistList(
            artists = state.artists,
            selectedArtistId = null,
            active = active,
            playbackBarOverlayHeight = playbackBarOverlayHeight,
            onArtistClick = onArtistClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun CloudMusicArtistList(
    artists: List<OnlineArtist>,
    selectedArtistId: String?,
    active: Boolean,
    playbackBarOverlayHeight: Dp,
    onArtistClick: (OnlineArtist) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playbackBarOverlayHeightPx = with(LocalDensity.current) {
        playbackBarOverlayHeight.roundToPx()
    }
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            FrameLayout(viewContext).apply {
                setBackgroundColor(Color.TRANSPARENT)
                LayoutInflater.from(viewContext).inflate(R.layout.smart_pinnedlist, this, true)
                findViewById<ListView>(R.id.list)?.apply {
                    divider = ColorDrawable(viewContext.getColor(R.color.listview_divider_color))
                    dividerHeight = viewContext.resources.getDimensionPixelSize(R.dimen.listview_dividerHeight)
                    selector = viewContext.getDrawable(R.drawable.listview_selector)
                    cacheColorHint = Color.TRANSPARENT
                    setBackgroundColor(Color.TRANSPARENT)
                    layoutAnimation = AnimationUtils.loadLayoutAnimation(viewContext, R.anim.list_anim_layout)
                    addLegacyPortListFooter()
                }
            }
        },
        update = { root ->
            root.visibility = if (active) View.VISIBLE else View.INVISIBLE
            val listView = root.findViewById<ListView>(R.id.list) ?: return@AndroidView
            listView.apply {
                val nextPaddingBottom = playbackBarOverlayHeightPx
                if (paddingBottom != nextPaddingBottom || clipToPadding) {
                    setPadding(paddingLeft, paddingTop, paddingRight, nextPaddingBottom)
                    clipToPadding = false
                }
            }
            listView.bindLegacyPortListFooter(
                pluralsRes = R.plurals.legacy_artist_count,
                count = artists.size,
            )
            val adapter = listView.legacyWrappedAdapter<CloudMusicArtistAdapter>()
                ?: CloudMusicArtistAdapter().also { nextAdapter ->
                    listView.adapter = nextAdapter
                }
            val changed = adapter.updateItems(
                nextItems = artists,
                nextSelectedArtistId = selectedArtistId,
            )
            if (changed) {
                listView.scheduleLayoutAnimation()
            } else {
                adapter.updateVisibleRows(listView)
            }
            listView.setOnItemClickListener { _, _, position, _ ->
                adapter.itemAt(position)?.let(onArtistClick)
            }
        },
    )
}

private class CloudMusicArtistAdapter : BaseAdapter() {
    private var artists: List<OnlineArtist> = emptyList()
    private var selectedArtistId: String? = null

    fun updateItems(
        nextItems: List<OnlineArtist>,
        nextSelectedArtistId: String?,
    ): Boolean {
        val changed = artists != nextItems || selectedArtistId != nextSelectedArtistId
        if (changed) {
            artists = nextItems
            selectedArtistId = nextSelectedArtistId
            notifyDataSetChanged()
        }
        return changed
    }

    fun updateVisibleRows(listView: ListView) {
        for (index in 0 until listView.childCount) {
            val position = listView.firstVisiblePosition + index
            val artist = itemAt(position) ?: continue
            bindArtistRow(listView.getChildAt(index), artist)
        }
    }

    fun itemAt(position: Int): OnlineArtist? = artists.getOrNull(position)

    override fun getCount(): Int = artists.size

    override fun getItem(position: Int): Any? = itemAt(position)

    override fun getItemId(position: Int): Long = artists.getOrNull(position)?.artistId?.toLongOrNull() ?: position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: createArtistRow(parent)
        itemAt(position)?.let { artist -> bindArtistRow(view, artist) }
        return view
    }

    private fun createArtistRow(parent: ViewGroup): View {
        val context = parent.context
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.listview_selector)
            isClickable = true
            isFocusable = true
            setPadding(context.dpPx(15), 0, context.dpPx(15), 0)
            layoutParams = AbsListView.LayoutParams(
                AbsListView.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.listview_item_height),
            )
            addView(
                TextView(context).apply {
                    id = R.id.listview_item_line_one
                    gravity = Gravity.CENTER_VERTICAL
                    includeFontPadding = false
                    isSingleLine = true
                    ellipsize = TextUtils.TruncateAt.END
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_size_large))
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
            addView(
                TextView(context).apply {
                    id = R.id.listview_item_line_two
                    gravity = Gravity.CENTER_VERTICAL
                    includeFontPadding = false
                    isSingleLine = true
                    ellipsize = TextUtils.TruncateAt.END
                    setTextColor(context.getColor(R.color.list_item_second_line))
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_size_micro))
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = context.dpPx(5)
                },
            )
        }
    }

    private fun bindArtistRow(view: View, artist: OnlineArtist) {
        val context = view.context
        view.findViewById<TextView>(R.id.listview_item_line_one)?.apply {
            text = artist.name
            setTextColor(
                if (artist.artistId == selectedArtistId) {
                    Color.rgb(177, 36, 32)
                } else {
                    context.getColor(R.color.list_item_first_line)
                },
            )
        }
        view.findViewById<TextView>(R.id.listview_item_line_two)?.text =
            artist.subtitleText(context)
    }
}

@Composable
private fun CloudMusicAccountLibraryList(
    playlists: List<OnlineAccountPlaylist>,
    albums: List<OnlineAlbum>,
    radios: List<OnlineRadio>,
    selectedPlaylistId: String?,
    active: Boolean,
    playbackBarOverlayHeight: Dp,
    onPlaylistClick: (OnlineAccountPlaylist) -> Unit,
    onAlbumClick: (OnlineAlbum) -> Unit,
    onRadioClick: (OnlineRadio) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playbackBarOverlayHeightPx = with(LocalDensity.current) {
        playbackBarOverlayHeight.roundToPx()
    }
    val items = remember(playlists, albums, radios) {
        playlists.map { playlist -> CloudAccountLibraryItem.Playlist(playlist) } +
            albums.map { album -> CloudAccountLibraryItem.Album(album) } +
            radios.map { radio -> CloudAccountLibraryItem.Radio(radio) }
    }
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            FrameLayout(viewContext).apply {
                setBackgroundColor(Color.TRANSPARENT)
                LayoutInflater.from(viewContext).inflate(R.layout.smart_pinnedlist, this, true)
                findViewById<ListView>(R.id.list)?.apply {
                    divider = ColorDrawable(viewContext.getColor(R.color.listview_divider_color))
                    dividerHeight = viewContext.resources.getDimensionPixelSize(R.dimen.listview_dividerHeight)
                    selector = viewContext.getDrawable(R.drawable.listview_selector)
                    cacheColorHint = Color.TRANSPARENT
                    setBackgroundColor(Color.TRANSPARENT)
                    layoutAnimation = AnimationUtils.loadLayoutAnimation(viewContext, R.anim.list_anim_layout)
                    addLegacyPortListFooter()
                }
            }
        },
        update = { root ->
            root.visibility = if (active) View.VISIBLE else View.INVISIBLE
            val listView = root.findViewById<ListView>(R.id.list) ?: return@AndroidView
            listView.apply {
                val nextPaddingBottom = playbackBarOverlayHeightPx
                if (paddingBottom != nextPaddingBottom || clipToPadding) {
                    setPadding(paddingLeft, paddingTop, paddingRight, nextPaddingBottom)
                    clipToPadding = false
                }
            }
            listView.bindLegacyPortListFooter(
                pluralsRes = R.plurals.cloud_music_account_library_count,
                count = items.size,
            )
            val adapter = listView.legacyWrappedAdapter<CloudMusicAccountLibraryAdapter>()
                ?: CloudMusicAccountLibraryAdapter().also { nextAdapter ->
                    listView.adapter = nextAdapter
                }
            val changed = adapter.updateItems(
                nextItems = items,
                nextSelectedPlaylistId = selectedPlaylistId,
            )
            if (changed) {
                listView.scheduleLayoutAnimation()
            } else {
                adapter.updateVisibleRows(listView)
            }
            listView.setOnItemClickListener { _, _, position, _ ->
                when (val item = adapter.itemAt(position)) {
                    is CloudAccountLibraryItem.Playlist -> onPlaylistClick(item.playlist)
                    is CloudAccountLibraryItem.Album -> onAlbumClick(item.album)
                    is CloudAccountLibraryItem.Radio -> onRadioClick(item.radio)
                    null -> Unit
                }
            }
        },
    )
}

private sealed interface CloudAccountLibraryItem {
    data class Playlist(val playlist: OnlineAccountPlaylist) : CloudAccountLibraryItem
    data class Album(val album: OnlineAlbum) : CloudAccountLibraryItem
    data class Radio(val radio: OnlineRadio) : CloudAccountLibraryItem
}

private class CloudMusicAccountLibraryAdapter : BaseAdapter() {
    private var items: List<CloudAccountLibraryItem> = emptyList()
    private var selectedPlaylistId: String? = null

    fun updateItems(
        nextItems: List<CloudAccountLibraryItem>,
        nextSelectedPlaylistId: String?,
    ): Boolean {
        val changed = items != nextItems || selectedPlaylistId != nextSelectedPlaylistId
        if (changed) {
            items = nextItems
            selectedPlaylistId = nextSelectedPlaylistId
            notifyDataSetChanged()
        }
        return changed
    }

    fun updateVisibleRows(listView: ListView) {
        for (index in 0 until listView.childCount) {
            val position = listView.firstVisiblePosition + index
            val item = itemAt(position) ?: continue
            bindAccountLibraryRow(listView.getChildAt(index), item)
        }
    }

    fun itemAt(position: Int): CloudAccountLibraryItem? = items.getOrNull(position)

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any? = itemAt(position)

    override fun getItemId(position: Int): Long {
        return when (val item = itemAt(position)) {
            is CloudAccountLibraryItem.Playlist ->
                item.playlist.playlistId.toLongOrNull() ?: position.toLong()
            is CloudAccountLibraryItem.Album ->
                item.album.albumId.toLongOrNull()?.let { albumId -> -albumId } ?: -(position.toLong() + 1L)
            is CloudAccountLibraryItem.Radio ->
                item.radio.radioId.toLongOrNull()?.let { radioId -> Long.MIN_VALUE + radioId } ?: Long.MIN_VALUE + position.toLong()
            null -> position.toLong()
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: createAccountLibraryRow(parent)
        itemAt(position)?.let { item -> bindAccountLibraryRow(view, item) }
        return view
    }

    private fun createAccountLibraryRow(parent: ViewGroup): View {
        val context = parent.context
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.listview_selector)
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            setPadding(context.dpPx(15), 0, context.dpPx(15), 0)
            layoutParams = AbsListView.LayoutParams(
                AbsListView.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.listview_item_height),
            )
            addView(
                TextView(context).apply {
                    id = R.id.listview_item_line_one
                    gravity = Gravity.CENTER_VERTICAL
                    includeFontPadding = false
                    isSingleLine = true
                    ellipsize = TextUtils.TruncateAt.END
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_size_large))
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
            addView(
                TextView(context).apply {
                    id = R.id.listview_item_line_two
                    gravity = Gravity.CENTER_VERTICAL
                    includeFontPadding = false
                    isSingleLine = true
                    ellipsize = TextUtils.TruncateAt.END
                    setTextColor(context.getColor(R.color.list_item_second_line))
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_size_micro))
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = context.dpPx(5)
                },
            )
        }
    }

    private fun bindAccountLibraryRow(view: View, item: CloudAccountLibraryItem) {
        val context = view.context
        view.findViewById<TextView>(R.id.listview_item_line_one)?.apply {
            text = item.displayTitle(context)
            setTextColor(
                if (item is CloudAccountLibraryItem.Playlist && item.playlist.playlistId == selectedPlaylistId) {
                    Color.rgb(177, 36, 32)
                } else {
                    context.getColor(R.color.list_item_first_line)
                },
            )
        }
        view.findViewById<TextView>(R.id.listview_item_line_two)?.text =
            item.displaySubtitle(context)
    }
}

private fun CloudAccountLibraryItem.displayTitle(context: Context): String {
    return when (this) {
        is CloudAccountLibraryItem.Playlist -> playlist.displayTitle(context)
        is CloudAccountLibraryItem.Album -> album.title
        is CloudAccountLibraryItem.Radio -> radio.title
    }
}

private fun CloudAccountLibraryItem.displaySubtitle(context: Context): String {
    return when (this) {
        is CloudAccountLibraryItem.Playlist -> listOf(
            context.getString(R.string.cloud_music_account_playlist_label),
            context.getString(R.string.cloud_music_playlist_track_count, playlist.trackCount),
        )
        is CloudAccountLibraryItem.Album -> listOfNotNull(
            context.getString(R.string.cloud_music_account_album_label),
            album.albumSubtitle(context),
        )
        is CloudAccountLibraryItem.Radio -> listOfNotNull(
            context.getString(R.string.cloud_music_account_radio_label),
            radio.subtitleText(context),
        )
    }.joinToString(" · ")
}

private fun OnlineAccountPlaylist.displayTitle(context: Context): String {
    return if (isLikedSongs) {
        context.getString(R.string.cloud_music_liked_songs_entry)
    } else {
        title
    }
}

@Composable
private fun CloudMusicResultList(
    tracks: List<OnlineTrack>,
    repository: OnlineMusicProviderRepository,
    playFailedMessageRes: Int,
    active: Boolean,
    playbackBarOverlayHeight: Dp,
    onTrackMoreClick: (MediaItem) -> Unit,
    editableAccountPlaylist: OnlineAccountPlaylist? = null,
    onAccountPlaylistTracksChanged: () -> Unit = {},
    onAccountPlaylistDeleted: (OnlineAccountPlaylist) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val browser = LocalPlaybackBrowser.current
    val scope = rememberCoroutineScope()
    var queuePopulateJob by remember { mutableStateOf<Job?>(null) }
    var queuePopulateToken by remember { mutableStateOf(0L) }
    var editMode by remember { mutableStateOf(false) }
    var selectedMediaIds by remember { mutableStateOf(emptySet<String>()) }
    var deleteRequest by remember { mutableStateOf<LegacyPlaylistDeleteRequest?>(null) }
    var removeInFlight by remember { mutableStateOf(false) }
    var deletePlaylistInFlight by remember { mutableStateOf(false) }
    val mediaItems = remember(tracks) {
        tracks.map { track -> track.toMediaItem() }
    }
    val mediaIds = remember(mediaItems) {
        mediaItems.mapTo(linkedSetOf(), MediaItem::mediaId)
    }
    val playbackBarOverlayHeightPx = with(LocalDensity.current) {
        playbackBarOverlayHeight.roundToPx()
    }

    LaunchedEffect(editableAccountPlaylist?.playlistId, mediaIds) {
        if (editableAccountPlaylist == null) {
            editMode = false
            selectedMediaIds = emptySet()
        } else {
            selectedMediaIds = selectedMediaIds.intersect(mediaIds)
        }
    }

    BackHandler(enabled = active && editMode) {
        editMode = false
        selectedMediaIds = emptySet()
    }

    fun showPlayFailed() {
        Toast.makeText(context, playFailedMessageRes, Toast.LENGTH_SHORT).show()
    }

    fun updateSelection(mediaId: String, selected: Boolean) {
        selectedMediaIds = if (selected) {
            selectedMediaIds + mediaId
        } else {
            selectedMediaIds - mediaId
        }
    }

    fun playCloudQueueFromIndex(
        startIndex: Int,
        shuffle: Boolean = false,
        onResolveFailed: (() -> Unit)? = null,
    ) {
        if (mediaItems.isEmpty()) {
            return
        }
        val safeStartIndex = startIndex.coerceIn(0, mediaItems.lastIndex)
        val requestToken = queuePopulateToken + 1L
        queuePopulateToken = requestToken
        queuePopulateJob?.cancel()
        queuePopulateJob = scope.launch {
            val startItem = withContext(Dispatchers.IO) {
                runCatching {
                    repository.resolvePlayableMediaItems(
                        mediaItems = listOf(mediaItems[safeStartIndex]),
                        includeLyrics = true,
                    ).firstOrNull()
                }.getOrNull()
            }
            if (queuePopulateToken != requestToken) {
                return@launch
            }
            if (startItem == null) {
                onResolveFailed?.invoke()
                showPlayFailed()
                return@launch
            }
            val queueItems = mediaItems.toMutableList().apply {
                this[safeStartIndex] = startItem
            }
            browser.replaceQueueAndPlay(
                mediaItems = queueItems,
                startIndex = safeStartIndex,
                shuffleModeEnabled = shuffle,
            )
        }
    }

    fun removeSelectedFromAccountPlaylist() {
        val playlist = editableAccountPlaylist ?: return
        if (removeInFlight || selectedMediaIds.isEmpty()) {
            return
        }
        val trackIds = mediaItems
            .asSequence()
            .filter { item -> item.mediaId in selectedMediaIds }
            .mapNotNull { item ->
                item.onlineIdentityOrNull()
                    ?.takeIf { identity -> identity.source == playlist.provider.sourceId }
                    ?.trackId
            }
            .distinct()
            .toList()
        if (trackIds.isEmpty()) {
            selectedMediaIds = emptySet()
            return
        }
        removeInFlight = true
        scope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    repository.removeTracksFromAccountPlaylist(
                        playlist = playlist,
                        trackIds = trackIds,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                NeteaseAccountActionResult(NeteaseAccountActionStatus.Failed)
            } finally {
                removeInFlight = false
            }
            when (result.status) {
                NeteaseAccountActionStatus.Success -> {
                    editMode = false
                    selectedMediaIds = emptySet()
                    onAccountPlaylistTracksChanged()
                    Toast.makeText(
                        context,
                        R.string.netease_online_music_playlist_remove_success,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                NeteaseAccountActionStatus.RequiresLogin -> {
                    Toast.makeText(context, R.string.online_music_login_required, Toast.LENGTH_SHORT).show()
                }
                NeteaseAccountActionStatus.Failed -> {
                    Toast.makeText(
                        context,
                        R.string.netease_online_music_playlist_remove_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    fun deleteAccountPlaylist() {
        val playlist = editableAccountPlaylist ?: return
        if (deletePlaylistInFlight) {
            return
        }
        deletePlaylistInFlight = true
        scope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    repository.deleteAccountPlaylist(playlist)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                NeteaseAccountActionResult(NeteaseAccountActionStatus.Failed)
            } finally {
                deletePlaylistInFlight = false
            }
            when (result.status) {
                NeteaseAccountActionStatus.Success -> {
                    editMode = false
                    selectedMediaIds = emptySet()
                    onAccountPlaylistDeleted(playlist)
                    Toast.makeText(
                        context,
                        R.string.netease_online_music_playlist_delete_success,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                NeteaseAccountActionStatus.RequiresLogin -> {
                    Toast.makeText(context, R.string.online_music_login_required, Toast.LENGTH_SHORT).show()
                }
                NeteaseAccountActionStatus.Failed -> {
                    Toast.makeText(
                        context,
                        R.string.netease_online_music_playlist_delete_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    LegacyPlaylistDeleteDialog(
        request = deleteRequest,
        onDismiss = {
            deleteRequest = null
        },
        onConfirm = { request ->
            deleteRequest = null
            when (request) {
                LegacyPlaylistDeleteRequest.NeteaseDetailPlaylist -> deleteAccountPlaylist()
                LegacyPlaylistDeleteRequest.DetailTracks -> removeSelectedFromAccountPlaylist()
                LegacyPlaylistDeleteRequest.DetailPlaylist,
                LegacyPlaylistDeleteRequest.RootSelected -> Unit
            }
        },
    )

    Column(modifier = modifier) {
        val accountPlaylistEditable = editableAccountPlaylist != null
        if (accountPlaylistEditable) {
            CloudMusicAccountPlaylistActionBar(
                actionInFlight = removeInFlight || deletePlaylistInFlight,
                editMode = editMode,
                selectedCount = selectedMediaIds.size,
                totalCount = mediaItems.size,
                onShuffleClick = {
                    playCloudQueueFromIndex(
                        startIndex = Random.nextInt(mediaItems.size),
                        shuffle = true,
                    )
                },
                onDeletePlaylistClick = {
                    deleteRequest = LegacyPlaylistDeleteRequest.NeteaseDetailPlaylist
                },
                onEditClick = {
                    editMode = true
                    selectedMediaIds = emptySet()
                },
                onSelectAllClick = {
                    selectedMediaIds = mediaIds
                },
                onRemoveClick = {
                    if (selectedMediaIds.isNotEmpty()) {
                        deleteRequest = LegacyPlaylistDeleteRequest.DetailTracks
                    }
                },
                onCancelEditClick = {
                    editMode = false
                    selectedMediaIds = emptySet()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            CloudMusicPlayActionBar(
                enabled = mediaItems.isNotEmpty(),
                onPlayAllClick = {
                    playCloudQueueFromIndex(startIndex = 0)
                },
                onShuffleClick = {
                    playCloudQueueFromIndex(
                        startIndex = Random.nextInt(mediaItems.size),
                        shuffle = true,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        CloudMusicDivider()
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = { viewContext ->
                FrameLayout(viewContext).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    LayoutInflater.from(viewContext).inflate(R.layout.smart_pinnedlist, this, true)
                    findViewById<ListView>(R.id.list)?.apply {
                        divider = ColorDrawable(viewContext.getColor(R.color.listview_divider_color))
                        dividerHeight = viewContext.resources.getDimensionPixelSize(R.dimen.listview_dividerHeight)
                        selector = viewContext.getDrawable(R.drawable.listview_selector)
                        cacheColorHint = Color.TRANSPARENT
                        setBackgroundColor(Color.TRANSPARENT)
                        layoutAnimation = AnimationUtils.loadLayoutAnimation(viewContext, R.anim.list_anim_layout)
                        addLegacyPortListFooter()
                    }
                }
            },
            update = { root ->
                root.visibility = if (active) View.VISIBLE else View.INVISIBLE
                val listView = root.findViewById<ListView>(R.id.list) ?: return@AndroidView
                listView.apply {
                    val nextPaddingBottom = playbackBarOverlayHeightPx
                    if (paddingBottom != nextPaddingBottom || clipToPadding) {
                        setPadding(paddingLeft, paddingTop, paddingRight, nextPaddingBottom)
                        clipToPadding = false
                    }
                }
                listView.bindLegacyPortListFooter(
                    pluralsRes = R.plurals.track_count,
                    count = mediaItems.size,
                )
                val adapter = listView.legacyWrappedAdapter<LegacySongsAdapter>()
                    ?: LegacySongsAdapter().also { adapter ->
                        listView.adapter = adapter
                    }
                adapter.onMoreClick = { item ->
                    if (!editMode) {
                        onTrackMoreClick(item)
                    }
                }
                val previousEditMode = listView.getTag(R.id.elvitem) as? Boolean
                val animateEditMode = previousEditMode != null && previousEditMode != editMode
                listView.setTag(R.id.elvitem, editMode)
                val listContentChanged = adapter.updateItems(
                    nextItems = mediaItems,
                    nextCurrentMediaId = browser?.currentMediaItem?.mediaId,
                    nextCurrentIsPlaying = browser?.isPlaying == true,
                    nextDisplayMode = LegacySongsSortDisplayMode.Name,
                    nextSectionMode = LegacySongsSectionMode.None,
                    nextQuickBarCollapsedVisibleWidth = 0,
                    nextEditMode = editMode,
                    nextSelectedMediaIds = selectedMediaIds,
                )
                if (listContentChanged) {
                    listView.setSelection(0)
                    listView.scheduleLayoutAnimation()
                } else {
                    adapter.updateVisibleSongRows(
                        listView = listView,
                        animateEditMode = animateEditMode,
                    )
                }
                if (listView.getTag(R.id.list) !== browser) {
                    (listView.getTag(R.id.text) as? Player.Listener)?.let { oldListener ->
                        (listView.getTag(R.id.list) as? Player)?.removeListener(oldListener)
                    }
                    if (browser != null) {
                        val playbackListener = object : Player.Listener {
                            override fun onEvents(player: Player, events: Player.Events) {
                                adapter.setPlaybackState(
                                    nextCurrentMediaId = player.currentMediaItem?.mediaId,
                                    nextCurrentIsPlaying = player.isPlaying,
                                )
                                adapter.updateVisiblePlaybackState(listView)
                            }
                        }
                        browser.addListener(playbackListener)
                        listView.setTag(R.id.text, playbackListener)
                    } else {
                        listView.setTag(R.id.text, null)
                    }
                    listView.setTag(R.id.list, browser)
                }
                val slideSelectionController = listView.legacySlideSelectionController(
                    startArea = LegacySlideSelectionStartArea.Checkbox,
                )
                slideSelectionController.update(
                    enabled = editMode,
                    selectedKeys = selectedMediaIds,
                    keyAtPosition = { position ->
                        adapter.itemAt(position)?.mediaId
                    },
                    onSelectionChange = { mediaId, selected ->
                        updateSelection(mediaId, selected)
                    },
                )
                listView.setOnTouchListener { _, event ->
                    slideSelectionController.handleTouch(event)
                }
                listView.setOnItemClickListener { _, _, position, _ ->
                    val item = adapter.itemAt(position) ?: return@setOnItemClickListener
                    if (editMode) {
                        updateSelection(item.mediaId, item.mediaId !in selectedMediaIds)
                        return@setOnItemClickListener
                    }
                    adapter.setPlaybackState(item.mediaId, true)
                    adapter.updateVisiblePlaybackState(listView)
                    val startIndex = mediaItems.indexOfFirst { candidate ->
                        candidate.mediaId == item.mediaId
                    }.takeIf { index -> index >= 0 } ?: 0
                    playCloudQueueFromIndex(
                        startIndex = startIndex,
                        onResolveFailed = {
                            adapter.setPlaybackState(
                                nextCurrentMediaId = browser?.currentMediaItem?.mediaId,
                                nextCurrentIsPlaying = browser?.isPlaying == true,
                            )
                            adapter.updateVisiblePlaybackState(listView)
                        },
                    )
                }
            },
            onRelease = { root ->
                val listView = root.findViewById<ListView>(R.id.list)
                if (listView != null) {
                    (listView.getTag(R.id.text) as? Player.Listener)?.let { oldListener ->
                        (listView.getTag(R.id.list) as? Player)?.removeListener(oldListener)
                    }
                }
            },
        )
    }
}

@Composable
private fun CloudMusicPlayActionBar(
    enabled: Boolean,
    onPlayAllClick: () -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier
            .background(ComposeColor.White)
            .padding(horizontal = 15.dp),
        factory = { viewContext ->
            FrameLayout(viewContext).apply {
                LayoutInflater.from(viewContext).inflate(R.layout.layout_play_container, this, true)
            }
        },
        update = { root ->
            root.findViewById<View>(R.id.bt_play)?.apply {
                isEnabled = enabled
                alpha = if (enabled) 1f else 0.22f
                setOnClickListener {
                    if (enabled) {
                        onPlayAllClick()
                    }
                }
            }
            root.findViewById<View>(R.id.bt_shuffle)?.apply {
                isEnabled = enabled
                alpha = if (enabled) 1f else 0.22f
                setOnClickListener {
                    if (enabled) {
                        onShuffleClick()
                    }
                }
            }
        },
    )
}

@Composable
private fun CloudMusicAccountPlaylistActionBar(
    actionInFlight: Boolean,
    editMode: Boolean,
    selectedCount: Int,
    totalCount: Int,
    onShuffleClick: () -> Unit,
    onDeletePlaylistClick: () -> Unit,
    onEditClick: () -> Unit,
    onSelectAllClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onCancelEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier
            .height(CloudAccountPlaylistActionBarHeight)
            .background(ComposeColor.White)
            .padding(horizontal = 15.dp),
        factory = { viewContext ->
            FrameLayout(viewContext).apply {
                setBackgroundColor(Color.WHITE)
            }
        },
        update = { root ->
            root.removeAllViews()
            if (editMode) {
                root.addView(
                    cloudMusicAccountPlaylistEditActions(
                        context = root.context,
                        enabled = !actionInFlight,
                        selectedCount = selectedCount,
                        totalCount = totalCount,
                        onSelectAllClick = onSelectAllClick,
                        onRemoveClick = onRemoveClick,
                        onCancelEditClick = onCancelEditClick,
                    ),
                    FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
                )
            } else {
                root.addView(
                    cloudMusicAccountPlaylistNormalActions(
                        context = root.context,
                        trackActionsEnabled = totalCount > 0 && !actionInFlight,
                        deleteEnabled = !actionInFlight,
                        onShuffleClick = onShuffleClick,
                        onDeletePlaylistClick = onDeletePlaylistClick,
                        onEditClick = onEditClick,
                    ),
                    FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
                )
            }
        },
    )
}

private fun cloudMusicAccountPlaylistNormalActions(
    context: Context,
    trackActionsEnabled: Boolean,
    deleteEnabled: Boolean,
    onShuffleClick: () -> Unit,
    onDeletePlaylistClick: () -> Unit,
    onEditClick: () -> Unit,
): LinearLayout {
    return LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        addView(
            legacyPlaylistDetailActionButton(context, R.drawable.btn_shuffle2_selector, R.string.s_random_play).apply {
                bindCloudMusicAccountActionEnabled(trackActionsEnabled)
                setOnClickListener {
                    if (isEnabled) {
                        onShuffleClick()
                    }
                }
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f),
        )
        addView(
            legacyPlaylistDetailActionButton(context, R.drawable.btn_deletelist2_selector, R.string.netease_playlist_delete_action).apply {
                bindCloudMusicAccountActionEnabled(deleteEnabled)
                setOnClickListener {
                    if (isEnabled) {
                        onDeletePlaylistClick()
                    }
                }
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                leftMargin = -context.dpPx(6)
            },
        )
        addView(
            legacyPlaylistDetailActionButton(context, R.drawable.btn_editlist2_selector, R.string.netease_playlist_edit_action).apply {
                bindCloudMusicAccountActionEnabled(trackActionsEnabled)
                setOnClickListener {
                    if (isEnabled) {
                        onEditClick()
                    }
                }
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                leftMargin = -context.dpPx(6)
            },
        )
    }
}

private fun View.bindCloudMusicAccountActionEnabled(enabled: Boolean) {
    isEnabled = enabled
    alpha = if (enabled) 1f else 0.28f
    isClickable = enabled
    isFocusable = enabled
}

private fun cloudMusicAccountPlaylistEditActions(
    context: Context,
    enabled: Boolean,
    selectedCount: Int,
    totalCount: Int,
    onSelectAllClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onCancelEditClick: () -> Unit,
): LinearLayout {
    return LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(
            cloudMusicAccountActionButton(
                context = context,
                iconRes = null,
                text = context.getString(R.string.cloud_music_select_all),
                enabled = enabled && totalCount > 0,
                onClick = onSelectAllClick,
            ),
            LinearLayout.LayoutParams(context.dpPx(76), LinearLayout.LayoutParams.MATCH_PARENT),
        )
        addView(
            TextView(context).apply {
                text = context.getString(R.string.selected_item_format, selectedCount, totalCount)
                setSingleLine(true)
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(context.getColor(R.color.list_item_second_line))
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_size_better))
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f),
        )
        addView(
            cloudMusicAccountActionButton(
                context = context,
                iconRes = R.drawable.btn_delete_song2_selector,
                text = context.getString(R.string.delete_track),
                enabled = enabled && selectedCount > 0,
                destructive = true,
                onClick = onRemoveClick,
            ),
            LinearLayout.LayoutParams(context.dpPx(104), LinearLayout.LayoutParams.MATCH_PARENT),
        )
        addView(
            cloudMusicAccountActionButton(
                context = context,
                iconRes = null,
                text = context.getString(R.string.cancel),
                enabled = enabled,
                onClick = onCancelEditClick,
            ),
            LinearLayout.LayoutParams(context.dpPx(68), LinearLayout.LayoutParams.MATCH_PARENT),
        )
    }
}

private fun cloudMusicAccountActionButton(
    context: Context,
    iconRes: Int?,
    text: String,
    enabled: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit,
): LinearLayout {
    return LinearLayout(context).apply {
        gravity = Gravity.CENTER
        orientation = LinearLayout.HORIZONTAL
        setBackgroundResource(if (destructive) R.drawable.btn_red_bg_selector else R.drawable.title_button_bg_selector)
        isEnabled = enabled
        alpha = if (enabled) 1f else 0.28f
        isClickable = enabled
        isFocusable = enabled
        iconRes?.let { res ->
            addView(
                android.widget.ImageView(context).apply {
                    setImageResource(res)
                    isDuplicateParentStateEnabled = true
                },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    rightMargin = context.dpPx(8)
                },
            )
        }
        addView(
            TextView(context).apply {
                setSingleLine(true)
                ellipsize = TextUtils.TruncateAt.END
                this.text = text
                gravity = Gravity.CENTER
                setTextColor(
                    context.getColor(
                        when {
                            destructive -> R.color.btn_text_color_red
                            else -> R.color.btn_text_color_blue
                        },
                    ),
                )
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.button_text_size))
                typeface = Typeface.DEFAULT_BOLD
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT),
        )
        setOnClickListener {
            if (isEnabled) {
                onClick()
            }
        }
    }
}

@Composable
private fun CloudMusicBlankState(
    title: String,
    subtitle: String?,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            FrameLayout(viewContext).apply {
                setBackgroundResource(R.drawable.account_background)
            }
        },
        update = { root ->
            val content = CloudMusicBlankContent(
                title = title,
                subtitle = subtitle.orEmpty(),
                actionText = actionText.orEmpty(),
            )
            if (root.tag == content) {
                root.findViewById<Button>(R.id.btn_ok)?.setOnClickListener {
                    onActionClick?.invoke()
                }
                return@AndroidView
            }
            root.tag = content
            root.removeAllViews()
            val contentColumn = LinearLayout(root.context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            contentColumn.addView(
                LegacyPlaylistBlankView(
                    context = root.context,
                    iconRes = R.drawable.blank_search,
                    primaryText = content.title,
                    secondaryText = content.subtitle,
                ),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
            if (content.actionText.isNotBlank() && onActionClick != null) {
                contentColumn.addView(
                    Button(root.context).apply {
                        id = R.id.btn_ok
                        text = content.actionText
                        isAllCaps = false
                        gravity = Gravity.CENTER
                        includeFontPadding = true
                        setTypeface(typeface, Typeface.BOLD)
                        setTextColor(Color.WHITE)
                        setTextSize(
                            TypedValue.COMPLEX_UNIT_PX,
                            resources.getDimension(R.dimen.semi_large_text_size),
                        )
                        setBackgroundResource(R.drawable.shrink_long_btn_red_selector)
                        minWidth = 0
                        minimumWidth = 0
                        minHeight = 0
                        minimumHeight = 0
                        setOnClickListener { onActionClick.invoke() }
                    },
                    LinearLayout.LayoutParams(
                        root.context.dpPx(160),
                        root.context.dpPx(48),
                    ).apply {
                        topMargin = root.context.dpPx(24)
                    },
                )
            }
            root.addView(
                contentColumn,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        },
    )
}

private data class CloudMusicBlankContent(
    val title: String,
    val subtitle: String,
    val actionText: String,
)

private sealed interface CloudMusicState {
    object Idle : CloudMusicState
    object LoadingAccount : CloudMusicState
    object LoadingFeatured : CloudMusicState
    object LoadingRadio : CloudMusicState
    object AccountEmpty : CloudMusicState
    object AccountError : CloudMusicState
    object FeaturedEmpty : CloudMusicState
    object FeaturedError : CloudMusicState
    object RadioEmpty : CloudMusicState
    object RadioError : CloudMusicState
    object RadioLoginRequired : CloudMusicState
    data class Success(val tracks: List<OnlineTrack>) : CloudMusicState
}

private sealed interface CloudFeaturedHomeState {
    object Loading : CloudFeaturedHomeState
    object Empty : CloudFeaturedHomeState
    object Error : CloudFeaturedHomeState
    data class Success(val home: OnlineMusicHome) : CloudFeaturedHomeState
}

private sealed interface CloudRadioHomeState {
    object Loading : CloudRadioHomeState
    object Empty : CloudRadioHomeState
    object Error : CloudRadioHomeState
    data class Success(val home: OnlineRadioHome) : CloudRadioHomeState
}

private sealed interface CloudSearchResultsState {
    object Idle : CloudSearchResultsState
    object Loading : CloudSearchResultsState
    data class Empty(val query: String) : CloudSearchResultsState
    data class Error(val query: String) : CloudSearchResultsState
    data class Success(val results: OnlineSearchResults) : CloudSearchResultsState
}

private sealed interface CloudHotSearchState {
    object Idle : CloudHotSearchState
    object Loading : CloudHotSearchState
    object Empty : CloudHotSearchState
    object Error : CloudHotSearchState
    data class Success(val keywords: List<OnlineSearchHotKeyword>) : CloudHotSearchState
}

private sealed interface CloudArtistState {
    object Idle : CloudArtistState
    object Loading : CloudArtistState
    object Empty : CloudArtistState
    object Error : CloudArtistState
    data class Success(val artists: List<OnlineArtist>) : CloudArtistState
}

private enum class CloudHomeMode {
    Tracks,
    Playlists,
    Collections,
    Featured,
    FeaturedTracks,
    FeaturedPlaylists,
    FeaturedCharts,
    FeaturedAlbums,
    FeaturedArtists,
    BannerTrack,
    Radio,
    RadioList,
    RadioTracks,
    RadioPrograms,
    OnlinePlaylistTracks,
    OnlineAlbumTracks,
    Artists,
    ArtistAlbums,
    ArtistTracks,
}

private enum class CloudMusicContentKey {
    SearchPrompt,
    SearchResults,
    AccountTracks,
    Collections,
    FeaturedTracks,
    FeaturedTracksDetail,
    FeaturedPlaylists,
    FeaturedCharts,
    FeaturedAlbums,
    FeaturedArtists,
    BannerTrack,
    Radio,
    RadioList,
    RadioTracks,
    RadioPrograms,
    OnlinePlaylistTracks,
    OnlineAlbumTracks,
    Playlists,
    Artists,
    ArtistAlbums,
    ArtistTracks,
}

private enum class CloudSearchCategory(
    val labelRes: Int,
) {
    All(R.string.cloud_music_search_tab_all),
    Tracks(R.string.search_tab_songs),
    Artists(R.string.search_tab_artists),
    Albums(R.string.search_tab_albums),
    Playlists(R.string.cloud_music_entry_collection),
}

private enum class CloudMusicHomeEntry(
    val labelRes: Int,
    val iconRes: Int,
) {
    Mine(
        labelRes = R.string.cloud_music_entry_mine,
        iconRes = R.drawable.net_icon_my_music,
    ),
    Recommend(
        labelRes = R.string.cloud_music_entry_recommend,
        iconRes = R.drawable.net_icon_recommend,
    ),
    Radio(
        labelRes = R.string.cloud_music_entry_radio,
        iconRes = R.drawable.net_icon_radio,
    ),
    Collection(
        labelRes = R.string.cloud_music_entry_collection,
        iconRes = R.drawable.net_icon_collection,
    ),
    Artist(
        labelRes = R.string.cloud_music_entry_artist,
        iconRes = R.drawable.net_icon_artist,
    ),
}

private val CloudSearchBarHeight = 50.dp
private val CloudSearchCategoryBarHeight = 42.dp
private val CloudBannerHeight = 142.dp
private val CloudHomeCoverCardWidth = 96.dp
private val CloudHomeTrackPreviewRowHeight = 48.dp
private val CloudHotSearchRowHeight = 58.dp
private val CloudSearchCoverRowHeight = 72.dp
private val CloudSearchCoverArtworkSize = 48.dp
private val CloudDetailHeaderHeight = 88.dp
private val CloudDetailHeaderArtworkSize = 64.dp
private val CloudAccountPlaylistActionBarHeight = 48.dp
private val CloudHomeEntryRowHeight = 78.dp
private val CloudSectionTitleHeight = 39.dp
private val CloudAccentColor = ComposeColor(0xFFE65C53)
private val CloudSecondaryTextColor = ComposeColor(0x80000000)
private const val CloudSearchDebounceMs = 350L
private const val CloudBannerAutoScrollMs = 5_000L
private const val CloudBannerMaxCount = 6
private const val CloudBannerArtworkWidthPx = 900
private const val CloudBannerArtworkHeightPx = 320
private const val CloudCoverArtworkSizePx = 260
private const val CloudHomeTrackPreviewCount = 5
private const val CloudHomeCoverPreviewCount = 6
private const val CloudArtistIntroMaxLines = 5

private suspend inline fun <T> runSuspendCatching(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }
}

private fun OnlineMusicHome.isEmpty(): Boolean {
    return tracks.isEmpty() &&
        playlists.isEmpty() &&
        charts.isEmpty() &&
        albums.isEmpty() &&
        artists.isEmpty()
}

private fun OnlinePlaylist.homeSubtitle(context: Context): String? {
    return subtitle?.takeIf(String::isNotBlank)
        ?: when {
            trackCount > 0 -> context.getString(R.string.cloud_music_playlist_track_count, trackCount)
            playCount >= 10_000L ->
                context.getString(R.string.cloud_music_play_count_wan, playCount / 10_000L)
            playCount > 0L -> context.getString(R.string.cloud_music_play_count, playCount)
            else -> null
        }
}

private fun OnlineAlbum.albumSubtitle(context: Context): String? {
    val artistText = artist?.takeIf(String::isNotBlank)
    val countText = trackCount
        .takeIf { count -> count > 0 }
        ?.let { count -> context.getString(R.string.cloud_music_playlist_track_count, count) }
    return listOfNotNull(artistText, countText)
        .joinToString(" · ")
        .takeIf(String::isNotBlank)
        ?: context.getString(R.string.cloud_music_album_provider_netease)
}

private fun OnlineArtist.subtitleText(context: Context): String {
    val aliasText = subtitle?.takeIf(String::isNotBlank)
    val countText = when {
        trackCount > 0 && albumCount > 0 ->
            context.getString(R.string.cloud_music_artist_track_album_count, trackCount, albumCount)
        trackCount > 0 -> context.getString(R.string.cloud_music_artist_track_count, trackCount)
        albumCount > 0 -> context.getString(R.string.cloud_music_artist_album_count, albumCount)
        else -> null
    }
    return listOfNotNull(aliasText, countText)
        .joinToString(" · ")
        .takeIf(String::isNotBlank)
        ?: context.getString(R.string.cloud_music_artist_provider_netease)
}

private fun OnlineRadio.cardSubtitle(context: Context): String? {
    return subtitle?.takeIf(String::isNotBlank)
        ?: category?.takeIf(String::isNotBlank)
        ?: creator?.takeIf(String::isNotBlank)
        ?: programCount.takeIf { count -> count > 0 }?.let { count ->
            context.getString(R.string.cloud_music_radio_program_count, count)
        }
}

private fun OnlineRadio.subtitleText(context: Context): String {
    val categoryText = category?.takeIf(String::isNotBlank)
    val programText = programCount.takeIf { count -> count > 0 }?.let { count ->
        context.getString(R.string.cloud_music_radio_program_count, count)
    }
    val playText = playCount.takeIf { count -> count > 0L }?.let { count ->
        context.getString(R.string.cloud_music_play_count, count)
    }
    return listOfNotNull(categoryText, programText, playText)
        .joinToString(" · ")
        .takeIf(String::isNotBlank)
        ?: subtitle
        ?: context.getString(R.string.cloud_music_radio_provider_netease)
}

private fun Long.toHotSearchHeatText(context: Context): String? {
    return takeIf { score -> score >= 10_000L }
        ?.let { score -> context.getString(R.string.cloud_music_hot_search_heat_wan, score / 10_000L) }
}

private fun Context.dpPx(value: Int): Int {
    return (value * resources.displayMetrics.density).toInt()
}
