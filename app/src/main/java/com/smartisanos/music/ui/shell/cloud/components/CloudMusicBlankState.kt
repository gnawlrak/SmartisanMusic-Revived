package com.smartisanos.music.ui.shell.cloud.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartisanos.music.R
import com.smartisanos.music.ui.shell.cloud.CloudAccentColor
import com.smartisanos.music.ui.shell.cloud.CloudSecondaryTextColor
import com.smartisanos.music.ui.shell.cloud.components.cloudMusicPressable

@Composable
internal fun CloudMusicBlankState(
    title: String,
    subtitle: String?,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ComposeColor(0xFFF8F8F8)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.blank_search),
                contentDescription = null,
                colorFilter = ColorFilter.tint(ComposeColor(0xFFD0D0D0)),
                modifier = Modifier
                    .width(80.dp)
                    .height(80.dp),
            )
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 16.sp,
                    color = ComposeColor(0x99000000),
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.padding(top = 16.dp),
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = CloudSecondaryTextColor,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (!actionText.isNullOrBlank() && onActionClick != null) {
                Box(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .width(160.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(CloudAccentColor)
                        .cloudMusicPressable(onClick = onActionClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = actionText,
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = ComposeColor.White,
                        ),
                    )
                }
            }
        }
    }
}
