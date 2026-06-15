package com.smartisanos.music.ui.shell.cloud

import android.content.Context
import android.graphics.Color
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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.smartisanos.music.R
import com.smartisanos.music.data.online.OnlineAccountPlaylist
import com.smartisanos.music.data.online.OnlineAlbum
import com.smartisanos.music.data.online.OnlineRadio
import com.smartisanos.music.ui.shell.addLegacyPortListFooter
import com.smartisanos.music.ui.shell.bindLegacyPortListFooter
import com.smartisanos.music.ui.shell.legacyWrappedAdapter
import com.smartisanos.music.ui.shell.cloud.components.CloudMusicBlankState
import com.smartisanos.music.ui.shell.cloud.components.CloudMusicDivider
import com.smartisanos.music.ui.shell.cloud.components.cloudMusicPressable

internal sealed interface CloudAccountLibraryItem {
    data class Playlist(val playlist: OnlineAccountPlaylist) : CloudAccountLibraryItem
    data class Album(val album: OnlineAlbum) : CloudAccountLibraryItem
    data class Radio(val radio: OnlineRadio) : CloudAccountLibraryItem
}

internal enum class CloudAccountLibraryFilter(
    val labelRes: Int,
) {
    All(R.string.cloud_music_filter_all),
    Playlist(R.string.cloud_music_filter_playlist),
    Album(R.string.cloud_music_filter_album),
    Radio(R.string.cloud_music_filter_radio),
}

@Composable
internal fun CloudMusicMinePage(
    playlists: List<OnlineAccountPlaylist>,
    albums: List<OnlineAlbum>,
    radios: List<OnlineRadio>,
    selectedPlaylistId: String?,
    active: Boolean,
    playbackBarOverlayHeight: Dp,
    scrollState: CloudLegacyListScrollState? = null,
    onPlaylistClick: (OnlineAccountPlaylist) -> Unit,
    onAlbumClick: (OnlineAlbum) -> Unit,
    onRadioClick: (OnlineRadio) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedFilter by remember { mutableStateOf(CloudAccountLibraryFilter.All) }
    val items = remember(playlists, albums, radios, selectedFilter) {
        when (selectedFilter) {
            CloudAccountLibraryFilter.All ->
                playlists.map { CloudAccountLibraryItem.Playlist(it) } +
                    albums.map { CloudAccountLibraryItem.Album(it) } +
                    radios.map { CloudAccountLibraryItem.Radio(it) }
            CloudAccountLibraryFilter.Playlist ->
                playlists.map { CloudAccountLibraryItem.Playlist(it) }
            CloudAccountLibraryFilter.Album ->
                albums.map { CloudAccountLibraryItem.Album(it) }
            CloudAccountLibraryFilter.Radio ->
                radios.map { CloudAccountLibraryItem.Radio(it) }
        }
    }

    Column(modifier = modifier.background(ComposeColor.White)) {
        CloudMusicFilterBar(
            selectedFilter = selectedFilter,
            onFilterChange = { selectedFilter = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        )
        CloudMusicDivider()
        if (items.isEmpty()) {
            CloudMusicBlankState(
                title = stringResource(R.string.cloud_music_blank_no_content),
                subtitle = null,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        } else {
            CloudMusicAccountLibraryList(
                items = items,
                selectedPlaylistId = selectedPlaylistId,
                active = active,
                playbackBarOverlayHeight = playbackBarOverlayHeight,
                scrollState = scrollState,
                onPlaylistClick = onPlaylistClick,
                onAlbumClick = onAlbumClick,
                onRadioClick = onRadioClick,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
    }
}

@Composable
private fun CloudMusicFilterBar(
    selectedFilter: CloudAccountLibraryFilter,
    onFilterChange: (CloudAccountLibraryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CloudAccountLibraryFilter.entries.forEach { filter ->
            val selected = filter == selectedFilter
            val backgroundColor = if (selected) CloudAccentColor.copy(alpha = 0.12f) else ComposeColor(0xFFF5F5F5)
            val contentColor = if (selected) CloudAccentColor else ComposeColor(0x99000000)

            Box(
                modifier = Modifier
                    .height(28.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(backgroundColor)
                    .cloudMusicPressable(pressedScale = 0.95f, onClick = { onFilterChange(filter) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(filter.labelRes),
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = contentColor,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CloudMusicAccountLibraryList(
    items: List<CloudAccountLibraryItem>,
    selectedPlaylistId: String?,
    active: Boolean,
    playbackBarOverlayHeight: Dp,
    scrollState: CloudLegacyListScrollState? = null,
    onPlaylistClick: (OnlineAccountPlaylist) -> Unit,
    onAlbumClick: (OnlineAlbum) -> Unit,
    onRadioClick: (OnlineRadio) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playbackBarOverlayHeightPx = with(LocalDensity.current) {
        playbackBarOverlayHeight.roundToPx()
    }
    val scrollListener = remember(scrollState) {
        object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) = Unit

            override fun onScroll(
                view: AbsListView?,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int,
            ) {
                val listView = view as? ListView ?: return
                scrollState?.capture(listView)
            }
        }
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
            listView.setOnScrollListener(null)
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
                if (scrollState?.hasPosition == true) {
                    listView.restoreCloudLegacyListScroll(scrollState, force = true)
                } else {
                    listView.scheduleLayoutAnimation()
                }
            } else {
                adapter.updateVisibleRows(listView)
                listView.restoreCloudLegacyListScroll(scrollState)
            }
            listView.setOnScrollListener(scrollListener)
            listView.setOnItemClickListener { _, _, position, _ ->
                when (val item = adapter.itemAt(position)) {
                    is CloudAccountLibraryItem.Playlist -> onPlaylistClick(item.playlist)
                    is CloudAccountLibraryItem.Album -> onAlbumClick(item.album)
                    is CloudAccountLibraryItem.Radio -> onRadioClick(item.radio)
                    null -> Unit
                }
            }
        },
        onRelease = { root ->
            root.findViewById<ListView>(R.id.list)?.let { listView ->
                scrollState?.capture(listView)
                listView.setOnScrollListener(null)
            }
        },
    )
}

internal class CloudMusicAccountLibraryAdapter : BaseAdapter() {
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
                    Color.BLACK
                },
            )
        }
        view.findViewById<TextView>(R.id.listview_item_line_two)?.text =
            item.displaySubtitle(context)
    }
}

internal fun CloudAccountLibraryItem.displayTitle(context: Context): String {
    return when (this) {
        is CloudAccountLibraryItem.Playlist -> playlist.displayTitle(context)
        is CloudAccountLibraryItem.Album -> album.title
        is CloudAccountLibraryItem.Radio -> radio.title
    }
}

internal fun CloudAccountLibraryItem.displaySubtitle(context: Context): String {
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
