package com.smartisanos.music.util

import android.content.Context
import android.graphics.Bitmap
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.request.crossfade
import coil3.size.Precision

/**
 * 图片请求工具。
 *
 * 提供 [fastScrollableImageRequest] 作为列表 / 卡片场景的统一入口，强制启用三级缓存
 * （内存、磁盘、网络）并使用 [Precision.INEXACT]，让 Coil 在滚动高峰期允许返回
 * 接近目标尺寸的位图，避免为追求精确尺寸而阻塞主线程。
 *
 * 仅依赖 Coil 的稳定 API，不引入实验性特性。
 */

/**
 * 构造一个用于列表 / 卡片滚动的图片请求。
 *
 * @param data 图像数据，通常是网络 URL 字符串或本地 uri 字符串。
 * @param sizePx 期望解码的边长（像素）。会同时作为 Coil 的目标尺寸，触发下采样。
 * @param crossfade 是否启用淡入淡出过渡。
 */
fun fastScrollableImageRequest(
    context: Context,
    data: Any?,
    sizePx: Int = 512,
    crossfade: Boolean = true,
): ImageRequest {
    val builder = ImageRequest.Builder(context)
        .data(data)
        .size(sizePx)
        .precision(Precision.INEXACT)
        .crossfade(crossfade)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED)
    if (isLocalImageSource(data)) {
        // 本地源多为嵌入封面或 MediaStore 缩略图，关闭硬件位图并降级到 RGB_565
        // 可显著降低内存占用，且不影响视觉。
        builder
            .allowHardware(false)
            .bitmapConfig(Bitmap.Config.RGB_565)
    }
    return builder.build()
}

/**
 * 判断数据源是否为本地图像（content / file / resource / 绝对路径）。
 */
fun isLocalImageSource(data: Any?): Boolean {
    val normalized = data?.toString()?.trim()?.lowercase().orEmpty()
    return normalized.startsWith("content://") ||
        normalized.startsWith("file://") ||
        normalized.startsWith("android.resource://") ||
        normalized.startsWith("/")
}
