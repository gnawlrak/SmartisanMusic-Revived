package com.smartisanos.music.playback.liveupdate

import android.content.Context
import android.content.SharedPreferences

/**
 * 歌词通知设置存储
 *
 * 仅保留两个开关：超级岛歌词、实况通知歌词。
 * 所有设置保存在 SharedPreferences 中，支持实时读取（每帧读取）。
 */
class LyricsNotificationSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 是否启用超级岛（焦点通知）歌词 */
    var superIslandEnabled: Boolean
        get() = prefs.getBoolean(KEY_SUPER_ISLAND, DEFAULT_SUPER_ISLAND)
        set(value) = prefs.edit().putBoolean(KEY_SUPER_ISLAND, value).apply()

    /** 是否启用实况通知歌词 */
    var liveUpdateEnabled: Boolean
        get() = prefs.getBoolean(KEY_LIVE_UPDATE, DEFAULT_LIVE_UPDATE)
        set(value) = prefs.edit().putBoolean(KEY_LIVE_UPDATE, value).apply()

    /**
     * 是否启用 LSPosed hook 模式（仅 xposed flavor 有意义）。
     * 开启后通知管理器停止发焦点/实况通知，由 SystemUI 内的 hook 渲染超级岛。
     */
    var xposedModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_XPOSED_MODE, DEFAULT_XPOSED_MODE)
        set(value) = prefs.edit().putBoolean(KEY_XPOSED_MODE, value).apply()

    companion object {
        private const val PREFS_NAME = "lyrics_notification_settings"

        private const val KEY_SUPER_ISLAND = "super_island_enabled"
        private const val KEY_LIVE_UPDATE = "live_update_enabled"
        private const val KEY_XPOSED_MODE = "xposed_mode_enabled"

        const val DEFAULT_SUPER_ISLAND = false
        const val DEFAULT_LIVE_UPDATE = true
        const val DEFAULT_XPOSED_MODE = false
    }
}
