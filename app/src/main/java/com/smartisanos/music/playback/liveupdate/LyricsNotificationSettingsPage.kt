package com.smartisanos.music.playback.liveupdate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartisanos.music.R

/**
 * 歌词通知设置页面
 *
 * 仅提供两个开关：超级岛歌词、实况通知歌词。
 */
@Composable
fun LyricsNotificationSettingsPage(
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val settings = remember { LyricsNotificationSettings(context) }

    var superIslandEnabled by remember { mutableStateOf(settings.superIslandEnabled) }
    var liveUpdateEnabled by remember { mutableStateOf(settings.liveUpdateEnabled) }
    var xposedModeEnabled by remember { mutableStateOf(settings.xposedModeEnabled) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.lyrics_notification_settings_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        SettingsSwitchRow(
            title = stringResource(R.string.lyrics_super_island_enabled),
            checked = superIslandEnabled,
            onCheckedChange = {
                superIslandEnabled = it
                settings.superIslandEnabled = it
            },
        )

        HorizontalDivider()

        SettingsSwitchRow(
            title = stringResource(R.string.lyrics_live_update_enabled),
            checked = liveUpdateEnabled,
            onCheckedChange = {
                liveUpdateEnabled = it
                settings.liveUpdateEnabled = it
            },
        )

        HorizontalDivider()

        SettingsSwitchRow(
            title = stringResource(R.string.lyrics_xposed_mode_enabled),
            checked = xposedModeEnabled,
            onCheckedChange = {
                xposedModeEnabled = it
                settings.xposedModeEnabled = it
            },
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
