package com.smartisanos.music.ui.shell.cloud.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartisanos.music.ui.shell.cloud.CloudHomeCoverCardWidth
import com.smartisanos.music.ui.shell.cloud.CloudSecondaryTextColor

@Composable
internal fun CloudHomeCoverCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(CloudHomeCoverCardWidth)
            .cloudMusicPressable(onClick = onClick),
    ) {
        CloudMusicCoverImage(
            imageUrl = imageUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(ComposeColor(0xFFF0F0F0)),
        )
        Text(
            text = title,
            style = TextStyle(
                fontSize = 13.sp,
                color = ComposeColor(0xCC000000),
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        subtitle?.takeIf(String::isNotBlank)?.let { subtitleText ->
            Text(
                text = subtitleText,
                style = TextStyle(
                    fontSize = 11.sp,
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
internal fun CloudHomeCoverSection(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
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
        // 用 LazyRow 替代 Row+horizontalScroll：卡片在滚动时按需组合并回收复用，
        // 避免一次性组合全部卡片及其封面图。
        // 原实现的 padding(start=12,top=12,end=12,bottom=14) 由 contentPadding 还原；
        // 项间 10dp 由调用方承担的 Spacer 改为统一的 spacedBy。
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
        CloudMusicDivider()
    }
}
