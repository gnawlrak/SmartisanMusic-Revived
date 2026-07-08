package com.smartisanos.music.ui.playback

import android.content.Context
import android.os.Vibrator
import android.util.Log

/**
 * Smartisan OS 专用振动效果。
 *
 * 使用 runCatching 包装整个调用，因为在非 Smartisan 设备上
 * smartisanos.api.VibratorSmt 类不存在，会抛出 NoClassDefFoundError。
 * 吞掉异常后静默降级——没有振动效果比崩溃好。
 */
internal object LegacyPlaybackHaptics {
    fun vibrateEffect(
        context: Context,
        effect: Int = DefaultEffect,
    ) {
        runCatching {
            val vibrator = context.getSystemService(Vibrator::class.java)
            val cls = Class.forName("smartisanos.api.VibratorSmt")
            cls.getMethod("vibrateEffect", Vibrator::class.java, Int::class.java)
                .invoke(null, vibrator, effect)
        }.onFailure { e ->
            Log.w(TAG, "vibrateEffect 失败（非 Smartisan 设备？）: ${e.message}")
        }
    }

    private const val DefaultEffect = 2
    private const val TAG = "LegacyPlaybackHaptics"
}
