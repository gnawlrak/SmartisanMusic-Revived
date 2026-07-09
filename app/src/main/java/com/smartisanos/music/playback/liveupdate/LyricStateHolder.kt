package com.smartisanos.music.playback.liveupdate

import android.graphics.Bitmap

/**
 * 跨进程歌词状态快照（播放器进程写、LSPosed hook 进程读）。
 *
 * 播放器侧由 [LyricsLiveUpdateManager] 在轮询循环里持续更新；
 * xposed flavor 的 LyricProviderService（AIDL）把这些值返回给运行在 SystemUI 内的 hook。
 *
 * 字段全部 @Volatile，保证可见性即可（非原子组合读，hook 侧容错）。
 */
object LyricStateHolder {
    @Volatile @JvmField var title: String = ""
    @Volatile @JvmField var artist: String = ""
    @Volatile @JvmField var album: String = ""
    @Volatile @JvmField var currentLine: String = ""
    @Volatile @JvmField var positionMs: Long = 0L
    @Volatile @JvmField var durationMs: Long = 0L
    @Volatile @JvmField var lineStartMs: Long = 0L
    @Volatile @JvmField var lineEndMs: Long = 0L
    @Volatile @JvmField var isPlaying: Boolean = false
    @Volatile @JvmField var hasLyrics: Boolean = false
    @Volatile @JvmField var packageName: String = ""
    /** 当前行的逐字 token JSON（[{t,s,e},...]），空串=无逐字时间戳 */
    @Volatile @JvmField var currentLineTokens: String = ""
    /** 封面取色：已唱高光色（vibrant），0=未提取 */
    @Volatile @JvmField var primaryColor: Int = 0
    /** 封面取色：未唱暗色（muted），0=未提取 */
    @Volatile @JvmField var secondaryColor: Int = 0
    /** 专辑封面 URI（content:// 格式），供 LSPosed 模块取色 */
    @Volatile @JvmField var coverUri: String = ""

    fun snapshot(): Snapshot = Snapshot(
        title = title,
        artist = artist,
        album = album,
        currentLine = currentLine,
        positionMs = positionMs,
        durationMs = durationMs,
        lineStartMs = lineStartMs,
        lineEndMs = lineEndMs,
        isPlaying = isPlaying,
        hasLyrics = hasLyrics,
        packageName = packageName,
        currentLineTokens = currentLineTokens,
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        coverUri = coverUri,
    )

    data class Snapshot(
        @JvmField val title: String,
        @JvmField val artist: String,
        @JvmField val album: String,
        @JvmField val currentLine: String,
        @JvmField val positionMs: Long,
        @JvmField val durationMs: Long,
        @JvmField val lineStartMs: Long,
        @JvmField val lineEndMs: Long,
        @JvmField val isPlaying: Boolean,
        @JvmField val hasLyrics: Boolean,
        @JvmField val packageName: String,
        @JvmField val currentLineTokens: String,
        @JvmField val primaryColor: Int,
        @JvmField val secondaryColor: Int,
        @JvmField val coverUri: String,
    )
}
