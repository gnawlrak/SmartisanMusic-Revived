package com.smartisanos.music.ui.shell.cloud.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.smartisanos.music.R
import com.smartisanos.music.data.online.OnlineBanner
import com.smartisanos.music.data.online.OnlineMusicProvider
import com.smartisanos.music.ui.shell.cloud.CloudBannerArtworkWidthPx
import com.smartisanos.music.ui.shell.cloud.CloudBannerAutoScrollMs
import com.smartisanos.music.ui.shell.cloud.CloudBannerHeight
import com.smartisanos.music.ui.shell.cloud.CloudBannerMaxCount
import com.smartisanos.music.util.fastScrollableImageRequest
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CloudMusicBannerStrip(
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
    val pagerState = rememberPagerState(pageCount = { visibleBanners.size })
    val currentIndex = pagerState.currentPage

    LaunchedEffect(active, visibleBanners) {
        if (!active || visibleBanners.size <= 1) {
            return@LaunchedEffect
        }
        while (true) {
            delay(CloudBannerAutoScrollMs)
            val nextPage = (pagerState.currentPage + 1) % visibleBanners.size
            pagerState.animateScrollToPage(nextPage)
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
                .clip(RoundedCornerShape(8.dp))
                .background(ComposeColor(0xFFF5F5F5)),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val banner = visibleBanners[page]
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
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        ComposeColor.Transparent,
                                        ComposeColor(0x99000000),
                                    ),
                                ),
                            )
                            .padding(horizontal = 13.dp, vertical = 10.dp),
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
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    visibleBanners.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentIndex) 6.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentIndex) {
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
internal fun CloudMusicBannerImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val safeUrl = imageUrl?.takeIf(String::isNotBlank)
    if (safeUrl == null) {
        Box(modifier = modifier.background(ComposeColor(0xFFB9312D)))
        return
    }
    val request = fastScrollableImageRequest(
        context = context,
        data = safeUrl,
        sizePx = CloudBannerArtworkWidthPx,
    )
    // 红色占位作为背景，加载未完成时透出，加载完成后被 Coil 绘制的位图覆盖。
    Box(modifier = modifier.background(ComposeColor(0xFFB9312D))) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
    }
}
