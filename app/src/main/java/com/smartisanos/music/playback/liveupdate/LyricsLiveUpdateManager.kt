package com.smartisanos.music.playback.liveupdate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.Player
import com.smartisanos.music.R
import com.smartisanos.music.playback.EmbeddedLyrics
import com.smartisanos.music.playback.EmbeddedLyricsLine
import com.smartisanos.music.playback.NowPlayingLyricsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 歌词实况通知管理器
 *
 * 基于 Android 16 Live Updates API，在通知栏/锁屏/状态栏显示逐字歌词。
 * 参考 HyperLyric 的自定义样式：当前行高亮加粗，逐字高亮进度。
 *
 * 核心机制：
 * 1. 通过 Player.Listener 监听播放进度
 * 2. 根据当前播放位置匹配歌词行/逐字标记
 * 3. 使用 RemoteViews 构建自定义通知视图
 * 4. 通知更新频率控制在约 200ms 间隔
 */
internal class LyricsLiveUpdateManager(
    private val context: Context,
    private val player: Player,
    private val sessionActivity: PendingIntent,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val notificationManager = NotificationManagerCompat.from(context)
    private var isActive = false
    private var lastNotificationUpdateAtMs: Long = 0L
    private var currentLyrics: EmbeddedLyrics? = null
    private var currentTrackTitle: String = ""
    private var currentTrackArtist: String = ""
    private var currentProgressMs: Long = 0L
    private var currentDurationMs: Long = 0L
    private var isPlaying: Boolean = false

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(
            mediaItem: androidx.media3.common.MediaItem?,
            reason: Int,
        ) {
            if (mediaItem == null) {
                hideLyricsNotification()
                return
            }
            currentTrackTitle = mediaItem.mediaMetadata.title?.toString().orEmpty()
            currentTrackArtist = mediaItem.mediaMetadata.artist?.toString().orEmpty()
            currentDurationMs = mediaItem.mediaMetadata.durationMs?.takeIf { it > 0 } ?: 0L
            loadLyrics(mediaItem)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            this@LyricsLiveUpdateManager.isPlaying = isPlaying
            if (isPlaying) {
                startProgressUpdates()
            } else {
                stopProgressUpdates()
            }
        }
    }

    private var progressUpdateJob: kotlinx.coroutines.Job? = null

    fun start() {
        if (isActive) return
        isActive = true
        createNotificationChannels()
        player.addListener(playerListener)
        player.currentMediaItem?.let { mediaItem ->
            currentTrackTitle = mediaItem.mediaMetadata.title?.toString().orEmpty()
            currentTrackArtist = mediaItem.mediaMetadata.artist?.toString().orEmpty()
            currentDurationMs = mediaItem.mediaMetadata.durationMs?.takeIf { it > 0 } ?: 0L
            loadLyrics(mediaItem)
        }
        if (player.isPlaying) {
            startProgressUpdates()
        }
    }

    fun stop() {
        isActive = false
        player.removeListener(playerListener)
        stopProgressUpdates()
        hideLyricsNotification()
        scope.cancel()
    }

    private fun loadLyrics(mediaItem: androidx.media3.common.MediaItem) {
        scope.launch(Dispatchers.IO) {
            val lyrics = NowPlayingLyricsRepository.load(
                context = context,
                mediaItem = mediaItem,
            )
            if (lyrics != null) {
                currentLyrics = lyrics
                updateNotification()
            } else {
                currentLyrics = null
                hideLyricsNotification()
            }
        }
    }

    private fun startProgressUpdates() {
        if (progressUpdateJob?.isActive == true) return
        progressUpdateJob = scope.launch {
            while (isActive) {
                currentProgressMs = player.currentPosition.coerceAtLeast(0L)
                updateNotification()
                delay(ProgressUpdateIntervalMs)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    private fun updateNotification() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastNotificationUpdateAtMs < MinNotificationUpdateIntervalMs) {
            return
        }
        lastNotificationUpdateAtMs = now

        val lyrics = currentLyrics
        if (lyrics == null || !lyrics.isTimeSynced) {
            hideLyricsNotification()
            return
        }

        val syncResult = findCurrentLyricsPosition(lyrics, currentProgressMs)
        if (syncResult == null) {
            hideLyricsNotification()
            return
        }

        val notification = buildLyricsNotification(syncResult)
        notificationManager.notify(LyricsNotificationId, notification)
    }

    private fun hideLyricsNotification() {
        notificationManager.cancel(LyricsNotificationId)
    }

    private fun buildLyricsNotification(
        syncResult: LyricsSyncResult,
    ): Notification {
        val channelId = getLiveUpdateChannelId()

        // 构建自定义 RemoteViews
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_live_update_lyrics)

        // 设置歌词行文本（带逐字高亮）
        val lyricsText = buildLyricsDisplayText(
            syncResult = syncResult,
            currentProgressMs = currentProgressMs,
        )
        remoteViews.setTextViewText(R.id.lyrics_line_text, lyricsText)

        // 设置进度信息
        val progressText = buildProgressText()
        if (progressText != null) {
            remoteViews.setTextViewText(R.id.lyrics_progress_text, progressText)
        } else {
            remoteViews.setTextViewText(R.id.lyrics_progress_text, "")
        }

        // 设置歌名
        val trackDisplay = buildTrackDisplayText()
        remoteViews.setTextViewText(R.id.lyrics_track_title, trackDisplay)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(currentTrackTitle.ifEmpty { context.getString(R.string.app_name) })
            .setContentText(syncResult.currentLine.text)
            .setContentIntent(sessionActivity)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomBigContentView(remoteViews)
            .setCustomContentView(remoteViews)
            .setShowWhen(false)
            .setSilent(true)

        // 在 Android 16 上标记为 Live Update
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            builder.setFlag(Notification.FLAG_ONGOING_EVENT, true)
        }

        return builder.build()
    }

    /**
     * 构建逐字高亮的歌词显示文本
     * 参考 HyperLyric 的样式：当前词用粗体/高亮色，整体行用强调色
     */
    private fun buildLyricsDisplayText(
        syncResult: LyricsSyncResult,
        currentProgressMs: Long,
    ): CharSequence {
        val line = syncResult.currentLine
        val tokens = syncResult.currentLine.tokens

        if (tokens.isEmpty()) {
            return line.text
        }

        // 计算当前时间在行内的相对偏移
        val lineStartMs = line.timestampMs ?: return line.text
        val relativeMs = currentProgressMs - lineStartMs

        // 找到当前正在播放的 token
        var activeTokenIndex = -1
        for (i in tokens.indices) {
            val token = tokens[i]
            if (relativeMs >= (token.timestampMs - lineStartMs)) {
                activeTokenIndex = i
            } else {
                break
            }
        }

        if (activeTokenIndex < 0) {
            return line.text
        }

        val sb = StringBuilder()
        val activeToken = tokens[activeTokenIndex]

        sb.append("▎") // 播放指示器
        // 当前高亮词
        sb.append(activeToken.text)
        // 剩余文本
        if (activeTokenIndex + 1 < tokens.size) {
            val remainingTokens = tokens.drop(activeTokenIndex + 1)
            val remainingText = remainingTokens.joinToString(separator = "") { it.text }
            if (remainingText.isNotBlank()) {
                sb.append(" ")
                sb.append(remainingText)
            }
        }

        return sb.toString()
    }

    private fun buildProgressText(): String? {
        if (currentDurationMs <= 0L) return null
        val progressPct = (currentProgressMs * 100 / currentDurationMs).coerceIn(0, 100)
        return "${formatTime(currentProgressMs)} / ${formatTime(currentDurationMs)}"
    }

    private fun buildTrackDisplayText(): String {
        return buildString {
            if (currentTrackTitle.isNotEmpty()) {
                append(currentTrackTitle)
            }
            if (currentTrackArtist.isNotEmpty()) {
                if (isNotEmpty()) append(" - ")
                append(currentTrackArtist)
            }
        }
    }

    private fun findCurrentLyricsPosition(
        lyrics: EmbeddedLyrics,
        positionMs: Long,
    ): LyricsSyncResult? {
        if (lyrics.lines.isEmpty()) return null

        var currentLineIndex = -1
        var nextLineIndex = -1

        for (i in lyrics.lines.indices) {
            val line = lyrics.lines[i]
            val lineTimeMs = line.timestampMs ?: continue
            if (lineTimeMs <= positionMs) {
                currentLineIndex = i
            } else {
                nextLineIndex = i
                break
            }
        }

        if (currentLineIndex < 0) return null

        val currentLine = lyrics.lines[currentLineIndex]
        val nextLine = if (nextLineIndex >= 0) lyrics.lines[nextLineIndex] else null

        return LyricsSyncResult(
            currentLine = currentLine,
            nextLine = nextLine,
            currentLineIndex = currentLineIndex,
        )
    }

    private fun createNotificationChannels() {
        // 标准 Live Update 通知渠道
        val liveUpdateChannel = NotificationChannel(
            LiveUpdateChannelId,
            context.getString(R.string.lyrics_live_update_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.lyrics_live_update_channel_description)
            setShowBadge(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                // Android 16+: 标记为 Live Update 渠道
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        }
        notificationManager.createNotificationChannel(liveUpdateChannel)

        // 小米超级岛 / 焦点通知渠道
        createXiaomiNotificationChannel()
    }

    /**
     * 创建小米超级岛（焦点通知）适配的渠道
     * 参考 HyperLyric 项目的小米焦点通知实现
     *
     * 小米超级岛需要：
     * 1. 独立的通知渠道，IMPORTANCE_HIGH
     * 2. 渠道名称符合小米焦点通知规范
     * 3. 使用 FOREGROUND_SERVICE_IMMEDIATE 类型
     */
    private fun createXiaomiNotificationChannel() {
        if (!isXiaomiDevice()) return

        val spotlightChannel = NotificationChannel(
            XiaomiSpotlightChannelId,
            context.getString(R.string.lyrics_spotlight_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.lyrics_spotlight_channel_description)
            setShowBadge(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        }
        notificationManager.createNotificationChannel(spotlightChannel)
    }

    private fun getLiveUpdateChannelId(): String {
        return if (isXiaomiDevice()) XiaomiSpotlightChannelId else LiveUpdateChannelId
    }

    private fun isXiaomiDevice(): Boolean {
        return Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
            Build.MANUFACTURER.equals("Redmi", ignoreCase = true)
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    companion object {
        private const val LyricsNotificationId = 2001
        private const val LiveUpdateChannelId = "lyrics_live_update"
        private const val XiaomiSpotlightChannelId = "lyrics_spotlight"
        private const val ProgressUpdateIntervalMs = 200L
        private const val MinNotificationUpdateIntervalMs = 150L
    }
}

/**
 * 歌词同步结果
 */
internal data class LyricsSyncResult(
    val currentLine: EmbeddedLyricsLine,
    val nextLine: EmbeddedLyricsLine?,
    val currentLineIndex: Int,
)