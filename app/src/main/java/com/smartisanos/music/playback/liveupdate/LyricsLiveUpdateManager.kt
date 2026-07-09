package com.smartisanos.music.playback.liveupdate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.util.TypedValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import com.smartisanos.music.R
import com.smartisanos.music.playback.EmbeddedLyrics
import com.smartisanos.music.playback.EmbeddedLyricsLine
import com.smartisanos.music.playback.EmbeddedLyricsToken
import com.smartisanos.music.playback.NowPlayingArtworkRepository
import com.smartisanos.music.playback.NowPlayingLyricsRepository
import com.smartisanos.music.playback.extractEmbeddedLyrics
import com.smartisanos.music.hook.LyricContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * 歌词通知管理器：超级岛（焦点通知）+ 实况通知。
 *
 * 对齐 HyperLyric 通知模式的做法（避免逐帧切片跑马灯带来的卡顿/字号抖动/空档）：
 * 1. 位置轮询 ~15Hz（只读 player.currentPosition，不发通知）。
 * 2. notify() 只在“显示内容”（歌词行/歌名/艺人/封面）变化时触发 → 远低于系统节流阈值，丝滑。
 * 3. 右侧歌词发整行原文；可选按 13sp Paint 像素截断（TruncateToFit）保证字号恒定。
 * 4. 填充策略：前奏静态首行、间奏/尾奏保持末行、永不空白。
 */
internal class LyricsLiveUpdateManager(
    private val context: Context,
    private val player: Player,
    private val sessionActivity: PendingIntent,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val notificationManager = NotificationManagerCompat.from(context)
    private val settings = LyricsNotificationSettings(context)

    private var isActive = false
    private var currentLyrics: EmbeddedLyrics? = null
    private var currentTrackTitle: String = ""
    private var currentTrackArtist: String = ""
    private var currentTrackAlbum: String = ""
    private var currentCoverUri: String = ""
    private var currentProgressMs: Long = 0L
    private var currentMediaItem: MediaItem? = null
    private var currentArtworkBitmap: Bitmap? = null
    private var lastSentKey: String? = null
    private var lastTrackInfoKey: String? = null

    /** 用于按像素截断歌词的 Paint（HyperLyric 以 13sp 为基准测量）。 */
    private val fitPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            IslandFontSizeSp,
            context.resources.displayMetrics,
        )
        typeface = Typeface.DEFAULT
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int,
        ) {
            currentMediaItem = mediaItem
            if (mediaItem == null) {
                hideLyricsNotification()
                return
            }
            lastSentKey = null
            lastTrackInfoKey = null
            currentLyrics = null
            currentTrackTitle = mediaItem.mediaMetadata.title?.toString().orEmpty()
            currentTrackArtist = mediaItem.mediaMetadata.artist?.toString().orEmpty()
            currentTrackAlbum = mediaItem.mediaMetadata.albumTitle?.toString().orEmpty()
            currentCoverUri = mediaItem.mediaMetadata.artworkUri?.toString().orEmpty()
            loadArtwork(mediaItem)
            loadLyrics(mediaItem)
        }

        override fun onTracksChanged(tracks: Tracks) {
            val trackLyrics = extractEmbeddedLyrics(tracks)
            if (trackLyrics != null) {
                currentLyrics = trackLyrics
                lastSentKey = null
                updateNotification()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // 始终保持轮询运行：暂停时也要持续发布状态（isPlaying=false + 冻结进度），
            // 否则 LSPosed hook 会读到陈旧的 playing=true，暂停后仍按播放外推进度→抖动。
            startProgressUpdates()
        }
    }

    private var progressUpdateJob: kotlinx.coroutines.Job? = null

    fun start() {
        if (isActive) return
        isActive = true
        createNotificationChannels()
        player.addListener(playerListener)
        scope.launch {
            delay(StartupDelayMs)
            if (!isActive) return@launch
            lastSentKey = null
            player.currentMediaItem?.let { mediaItem ->
                currentMediaItem = mediaItem
                currentTrackTitle = mediaItem.mediaMetadata.title?.toString().orEmpty()
                currentTrackArtist = mediaItem.mediaMetadata.artist?.toString().orEmpty()
                currentTrackAlbum = mediaItem.mediaMetadata.albumTitle?.toString().orEmpty()
                currentCoverUri = mediaItem.mediaMetadata.artworkUri?.toString().orEmpty()
                extractEmbeddedLyrics(player.currentTracks)?.let { currentLyrics = it }
                loadArtwork(mediaItem)
                loadLyrics(mediaItem)
            }
            if (player.isPlaying) {
                startProgressUpdates()
            }
        }
    }

    fun stop() {
        isActive = false
        player.removeListener(playerListener)
        stopProgressUpdates()
        hideLyricsNotification()
        scope.cancel()
    }

    private fun loadLyrics(mediaItem: MediaItem) {
        NowPlayingLyricsRepository.peek(mediaItem)?.let { cached ->
            currentLyrics = cached
            lastSentKey = null
            updateNotification()
        }

        scope.launch(Dispatchers.IO) {
            try {
                val lyrics = NowPlayingLyricsRepository.load(
                    context = context,
                    mediaItem = mediaItem,
                )
                if (lyrics != null) {
                    currentLyrics = lyrics
                    lastSentKey = null
                    updateNotification()
                } else if (currentLyrics == null) {
                    updateNotification()
                }
            } catch (_: Exception) {
                if (currentLyrics == null) {
                    updateNotification()
                }
            }
        }
    }

    private fun loadArtwork(item: MediaItem) {
        scope.launch(Dispatchers.IO) {
            try {
                val bitmap = NowPlayingArtworkRepository.load(
                    context = context,
                    mediaItem = item,
                    size = Size(256, 256),
                )
                currentArtworkBitmap = bitmap
                extractPaletteColors(bitmap)
                if (isActive) {
                    lastSentKey = null
                    updateNotification()
                }
            } catch (_: Exception) {
                currentArtworkBitmap = null
            }
        }
    }

    /** 从封面提取主色/副色写入 LyricStateHolder，供 hook 侧取色。 */
    private fun extractPaletteColors(bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled) {
            LyricStateHolder.primaryColor = 0
            LyricStateHolder.secondaryColor = 0
            return
        }
        runCatching {
            val palette = androidx.palette.graphics.Palette.from(bitmap).generate()
            LyricStateHolder.primaryColor = palette.getVibrantColor(0xFFFFFFFF.toInt())
            LyricStateHolder.secondaryColor = palette.getMutedColor(0xFF999999.toInt())
        }.onFailure {
            LyricStateHolder.primaryColor = 0
            LyricStateHolder.secondaryColor = 0
        }
    }

    /**
     * 位置轮询循环：~15Hz 读 player.currentPosition，触发 updateNotification。
     * notify() 本身只在显示内容变化时才发（见 updateNotification 去重）。
     */
    private fun startProgressUpdates() {
        if (progressUpdateJob?.isActive == true) return
        progressUpdateJob = scope.launch {
            while (isActive) {
                currentProgressMs = player.currentPosition.coerceAtLeast(0L)
                updateNotification()
                delay(PollIntervalMs)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    /**
     * 把当前歌词/进度/元数据快照写入 [LyricStateHolder]，供 LSPosed hook（AIDL）读取。
     */
    private fun publishState() {
        val lyrics = currentLyrics
        val tsLyrics = if (lyrics != null && lyrics.isTimeSynced && lyrics.lines.isNotEmpty()) lyrics else null
        val line = tsLyrics?.let { currentDisplayLine(it, currentProgressMs) }
        val lineTs = line?.timestampMs ?: 0L
        val nextTs = tsLyrics?.let { findNextTimestamp(it, line) } ?: 0L
        LyricStateHolder.apply {
            title = currentTrackTitle
            artist = currentTrackArtist
            album = currentTrackAlbum
            coverUri = currentCoverUri
            currentLine = line?.text.orEmpty()
            currentLineTokens = serializeTokens(line?.tokens)
            positionMs = currentProgressMs
            durationMs = player.duration.coerceAtLeast(0L)
            lineStartMs = lineTs
            lineEndMs = nextTs
            isPlaying = player.isPlaying
            hasLyrics = tsLyrics != null
            packageName = context.packageName
        }
        context.contentResolver.notifyChange(LyricContract.SNAPSHOT_URI, null)
    }

    /** 把逐字 token 列表序列化为 JSON 数组字符串，供 hook 侧解析。 */
    private fun serializeTokens(tokens: List<EmbeddedLyricsToken>?): String {
        if (tokens.isNullOrEmpty()) return ""
        return runCatching {
            val arr = JSONArray()
            for (tok in tokens) {
                arr.put(JSONObject().apply {
                    put("t", tok.text)
                    put("s", tok.timestampMs)
                    put("e", tok.endTimestampMs ?: tok.timestampMs)
                })
            }
            arr.toString()
        }.getOrDefault("")
    }

    private fun findNextTimestamp(
        lyrics: EmbeddedLyrics,
        current: EmbeddedLyricsLine?,
    ): Long {
        if (current == null) return 0L
        val curTs = current.timestampMs ?: return 0L
        // 使用 timestampMs 相等匹配而非引用相等（===），
        // 防止歌词重解析后对象重建导致 indexOfFirst 返回 -1
        val idx = lyrics.lines.indexOfFirst { it.timestampMs == current.timestampMs }
        if (idx < 0) return 0L
        for (i in (idx + 1) until lyrics.lines.size) {
            val ts = lyrics.lines[i].timestampMs
            if (ts != null && ts > curTs) return ts
        }
        val dur = player.duration
        return if (dur > curTs) dur else 0L
    }

    private enum class DisplayMode { OFF, SUPER_ISLAND, LIVE_UPDATE }

    private fun effectiveMode(): DisplayMode {
        // LSPosed hook 模式：由 SystemUI 内 hook 接管超级岛，停止发通知。
        if (settings.xposedModeEnabled) return DisplayMode.OFF
        val wantSuper = settings.superIslandEnabled
        val wantLive = settings.liveUpdateEnabled
        return when {
            !wantSuper && !wantLive -> DisplayMode.OFF
            wantSuper && isXiaomiDevice() -> DisplayMode.SUPER_ISLAND
            wantLive -> DisplayMode.LIVE_UPDATE
            else -> DisplayMode.OFF
        }
    }

    private fun updateNotification() {
        publishState()
        val mode = effectiveMode()
        if (mode == DisplayMode.OFF) {
            hideLyricsNotification()
            return
        }

        val lyrics = currentLyrics
        if (lyrics == null || !lyrics.isTimeSynced || lyrics.lines.isEmpty()) {
            showTrackInfoNotification(mode)
            return
        }

        val line = currentDisplayLine(lyrics, currentProgressMs)
        if (line == null) {
            showTrackInfoNotification(mode)
            return
        }

        val lyricText = fitLyric(line.text)
        val progress = songProgressPercent()
        // 去重：歌词行/元数据变化 或 进度推进 2% 才更新（推进 Live Update 进度条，又不刷太频）。
        val key = "${mode.name}\u0000$lyricText\u0000$currentTrackTitle\u0000$currentTrackArtist\u0000${if (currentArtworkBitmap != null) 1 else 0}\u0000${progress / 2}"
        if (key == lastSentKey) {
            return
        }
        lastSentKey = key
        lastTrackInfoKey = null

        try {
            val notification = when (mode) {
                DisplayMode.SUPER_ISLAND -> buildFocusNotification(lyricText)
                DisplayMode.LIVE_UPDATE -> buildLiveUpdateNotification(lyricText, progress)
                DisplayMode.OFF -> return
            }
            notificationManager.notify(LyricsNotificationId, notification)
        } catch (_: Exception) {
        }
    }

    private fun showTrackInfoNotification(mode: DisplayMode) {
        val key = "track\u0000${mode.name}\u0000$currentTrackTitle\u0000$currentTrackArtist\u0000${if (currentArtworkBitmap != null) 1 else 0}"
        if (key == lastTrackInfoKey && lastSentKey == null) {
            return
        }
        lastTrackInfoKey = key
        lastSentKey = null
        try {
            val notification = when (mode) {
                DisplayMode.SUPER_ISLAND -> buildFocusNotification(currentTrackTitle.ifEmpty { currentTrackArtist })
                DisplayMode.LIVE_UPDATE -> buildLiveUpdateNotification(currentTrackTitle.ifEmpty { currentTrackArtist }, songProgressPercent())
                DisplayMode.OFF -> return
            }
            notificationManager.notify(LyricsNotificationId, notification)
        } catch (_: Exception) {
        }
    }

    private fun hideLyricsNotification() {
        if (lastSentKey != null || lastTrackInfoKey != null) {
            lastSentKey = null
            lastTrackInfoKey = null
        }
        notificationManager.cancel(LyricsNotificationId)
    }

    /**
     * 选择当前应显示的歌词行（永不返回 null 当歌词非空）：
     * - 前奏（未到首行时间戳）：返回首行（静态显示）。
     * - 正常/间奏/尾奏：返回时间戳不超过当前进度的最后一行；间奏时自然保持该行（末尾状态）。
     */
    private fun currentDisplayLine(
        lyrics: EmbeddedLyrics,
        positionMs: Long,
    ): EmbeddedLyricsLine? {
        val lines = lyrics.lines
        if (lines.isEmpty()) return null

        val firstTsIdx = lines.indexOfFirst { it.timestampMs != null }
        if (firstTsIdx < 0) return lines[0]
        val firstTs = lines[firstTsIdx].timestampMs ?: return lines[firstTsIdx]

        if (positionMs < firstTs) return lines[firstTsIdx]

        var idx = firstTsIdx
        for (i in firstTsIdx until lines.size) {
            val ts = lines[i].timestampMs ?: continue
            if (ts <= positionMs) idx = i else break
        }
        return lines[idx]
    }

    /**
     * 右侧歌词文本：默认发整行原文（靠 HyperOS 原生 marquee 匀速滚动）。
     * TruncateToFit=true 时按 13sp Paint 像素截断到右侧槽位宽度 → 字号必稳（HyperLyric 做法）。
     */
    private fun fitLyric(text: String): String {
        if (text.isEmpty()) return text
        if (!TruncateToFit) return text
        val budgetPx = RightSlotSp * context.resources.displayMetrics.density
        if (fitPaint.measureText(text) <= budgetPx) return text
        val count = fitPaint.breakText(text, true, budgetPx, null)
        return text.substring(0, count.coerceIn(1, text.length)).trimEnd()
    }

    /**
     * 实况通知。
     * - Android 16+ (API 36)：用 [Notification.ProgressStyle] 注册成系统「实况通知 / Live Update」，
     *   以歌曲进度为进度条；歌词放在 contentText。
     * - 低版本：回退 BigTextStyle 持续通知。
     */
    private fun buildLiveUpdateNotification(lyricText: CharSequence, progress: Int): Notification {
        val contentTitle = currentTrackTitle.ifEmpty { currentTrackArtist }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            val builder = Notification.Builder(context, LiveUpdateChannelId)
                .setSmallIcon(R.drawable.ic_notification_lyrics)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(sessionActivity)
                .setContentTitle(contentTitle)
                .setSubText(currentTrackArtist)
                .setContentText(lyricText)
                .setShowWhen(false)
                .setWhen(System.currentTimeMillis())
            currentArtworkBitmap?.let { builder.setLargeIcon(it) }

            if (Build.VERSION.SDK_INT >= 36) {
                builder.setStyle(buildProgressStyle(progress))
                builder.setCategory(Notification.CATEGORY_PROGRESS)
            } else {
                builder.setStyle(
                    Notification.BigTextStyle()
                        .bigText(lyricText)
                        .setBigContentTitle(contentTitle)
                        .setSummaryText(currentTrackArtist),
                )
                builder.setCategory(Notification.CATEGORY_STATUS)
            }
            return builder.build()
        }

        val builder = NotificationCompat.Builder(context, LiveUpdateChannelId)
            .setSmallIcon(R.drawable.ic_notification_lyrics)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(sessionActivity)
            .setContentTitle(contentTitle)
            .setSubText(currentTrackArtist)
            .setContentText(lyricText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(lyricText)
                    .setBigContentTitle(contentTitle)
                    .setSummaryText(currentTrackArtist),
            )
            .setShowWhen(false)
            .setWhen(System.currentTimeMillis())
            .setSilent(true)
            .setColorized(false)
        currentArtworkBitmap?.let { builder.setLargeIcon(it) }
        return builder.build()
    }

    /** Android 16 Live Update 进度样式：已播放段着色 + 未播放段，playhead 跟随歌曲进度。 */
    private fun buildProgressStyle(progress: Int): Notification.ProgressStyle {
        val p = progress.coerceIn(0, 100)
        val segments = ArrayList<Notification.ProgressStyle.Segment>(2)
        if (p > 0) {
            segments.add(Notification.ProgressStyle.Segment(p).setColor(LiveUpdateColor))
        }
        if (p < 100) {
            segments.add(Notification.ProgressStyle.Segment(100 - p))
        }
        return Notification.ProgressStyle()
            .setProgressSegments(segments)
            .setProgress(p)
            .setStyledByProgress(false)
    }

    private fun songProgressPercent(): Int {
        val duration = player.duration.coerceAtLeast(0L)
        if (duration <= 0L) return 0
        return ((currentProgressMs.coerceAtLeast(0L) * 100) / duration).toInt().coerceIn(0, 100)
    }

    /**
     * 焦点通知（超级岛）：构建 miui.focus.param JSON。
     * 左侧 imageTextInfoLeft = 专辑封面 + 歌名(上)/艺人(下)；右侧 textInfo = 歌词。
     */
    private fun buildFocusNotification(lyricText: String): Notification {
        val focusJson = buildFocusParamV2Json(lyricText)

        val builder = NotificationCompat.Builder(context, XiaomiSpotlightChannelId)
            .setSmallIcon(R.drawable.ic_notification_lyrics)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCustomContentView(null)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(sessionActivity)
            .setContentTitle(currentTrackTitle.ifEmpty { currentTrackArtist })
            .setSubText(currentTrackArtist)
            .setContentText(lyricText)
            .setShowWhen(false)
            .setWhen(System.currentTimeMillis())

        val extras = Bundle()
        extras.putBoolean("mFocusNotification", true)
        extras.putString("miui.focus.param", focusJson)

        val picsBundle = Bundle()
        val artworkBitmap = currentArtworkBitmap
        if (artworkBitmap != null && !artworkBitmap.isRecycled) {
            picsBundle.putParcelable("miui.focus.pic_album", Icon.createWithBitmap(artworkBitmap))
        }
        extras.putBundle("miui.focus.pics", picsBundle)

        builder.addExtras(extras)
        return builder.build()
    }

    private fun buildFocusParamV2Json(lyricText: String): String {
        val root = JSONObject()
        val paramV2 = JSONObject()

        paramV2.put("islandFirstFloat", false)
        paramV2.put("updatable", true)
        paramV2.put("reopen", "reopen")
        paramV2.put("param_island", buildParamIsland(lyricText))
        paramV2.put("baseInfo", buildBaseInfo(lyricText))
        if (currentArtworkBitmap != null) {
            paramV2.put("picInfo", buildPicInfo(2))
        }
        paramV2.put("aodTitle", currentTrackTitle.ifEmpty { currentTrackArtist })
        paramV2.put("aodPic", "miui.focus.pic_album")

        root.put("param_v2", paramV2)
        return root.toString()
    }

    private fun buildParamIsland(lyricText: String): JSONObject {
        val json = JSONObject()
        json.put("bigIslandArea", buildBigIslandArea(lyricText))
        json.put("smallIslandArea", buildSmallIslandArea(lyricText))
        return json
    }

    private fun buildBigIslandArea(lyricText: String): JSONObject {
        val json = JSONObject()

        val imageTextLeft = JSONObject()
        imageTextLeft.put("type", 1)
        if (currentArtworkBitmap != null) {
            imageTextLeft.put("picInfo", buildPicInfo(1))
        }
        imageTextLeft.put("textInfo", buildLeftTextInfo())
        json.put("imageTextInfoLeft", imageTextLeft)

        val islandTitleText = JSONObject()
        islandTitleText.put("title", lyricText)
        islandTitleText.put("showHighlightColor", true)
        json.put("textInfo", islandTitleText)

        return json
    }

    /** 左侧文本：上方歌名，下方艺人。 */
    private fun buildLeftTextInfo(): JSONObject {
        val textInfo = JSONObject()
        textInfo.put("title", currentTrackTitle)
        if (currentTrackArtist.isNotEmpty()) {
            textInfo.put("content", currentTrackArtist)
        }
        textInfo.put("showHighlightColor", false)
        return textInfo
    }

    private fun buildSmallIslandArea(lyricText: String): JSONObject {
        val json = JSONObject()
        val combinePicInfo = JSONObject()
        combinePicInfo.put("picInfo", buildPicInfo(1))
        json.put("combinePicInfo", combinePicInfo)

        if (lyricText.isNotEmpty()) {
            val textInfo = JSONObject()
            textInfo.put("title", lyricText)
            textInfo.put("showHighlightColor", true)
            json.put("textInfo", textInfo)
        }
        return json
    }

    private fun buildBaseInfo(lyricText: String): JSONObject {
        val json = JSONObject()
        json.put("type", 2)
        json.put("title", currentTrackTitle)
        json.put("content", lyricText)
        return json
    }

    private fun buildPicInfo(type: Int, picKey: String = "miui.focus.pic_album"): JSONObject {
        val json = JSONObject()
        json.put("type", type)
        json.put("pic", picKey)
        if (type == 2) {
            json.put("picDark", picKey)
        }
        return json
    }

    private fun createNotificationChannels() {
        val liveUpdateChannel = NotificationChannel(
            LiveUpdateChannelId,
            context.getString(R.string.lyrics_live_update_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.lyrics_live_update_channel_description)
            setSound(null, null)
            setShowBadge(false)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(liveUpdateChannel)

        createXiaomiNotificationChannel()
    }

    private fun createXiaomiNotificationChannel() {
        if (!isXiaomiDevice()) return

        val spotlightChannel = NotificationChannel(
            XiaomiSpotlightChannelId,
            context.getString(R.string.lyrics_spotlight_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.lyrics_spotlight_channel_description)
            setSound(null, null)
            setShowBadge(false)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(spotlightChannel)
    }

    private fun isXiaomiDevice(): Boolean {
        return Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
            Build.MANUFACTURER.equals("Redmi", ignoreCase = true)
    }

    companion object {
        private const val LyricsNotificationId = 2001
        private const val LiveUpdateChannelId = "lyrics_live_update"
        private const val XiaomiSpotlightChannelId = "lyrics_spotlight"
        private const val StartupDelayMs = 500L

        /** 位置轮询间隔（~15Hz）。notify 只在显示内容变化时发，远低于此频率。 */
        private const val PollIntervalMs = 66L

        /** 岛上歌词字号基准（HyperLyric 以 13sp 测量）。 */
        private const val IslandFontSizeSp = 13f

        /**
         * 超长行处理：
         * - false（默认）：发整句原文，靠 HyperOS 原生 marquee 匀速滚动。
         * - true：按 13sp Paint 像素截断到右侧槽位 → 字号必稳（HyperLyric 做法）。
         * 实测整句版若字号抖动/溢出，改为 true。
         */
        private const val TruncateToFit = false

        /** 右侧槽位宽度（sp），仅 TruncateToFit=true 时使用。 */
        private const val RightSlotSp = 120

        /** 实况通知进度条已播放段颜色。 */
        private const val LiveUpdateColor = 0xFF3482FF.toInt()
    }
}