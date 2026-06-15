package com.smartisanos.music.ui.shell.cloud.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.smartisanos.music.ui.shell.cloud.CloudCoverArtworkSizePx
import com.smartisanos.music.util.fastScrollableImageRequest

/**
 * 云音乐通用封面图。
 *
 * 基于 Coil 的 [AsyncImage]，享受全局两级缓存（见 [com.smartisanos.music.SmartisanMusicApplication]），
 * 列表滚动 / 转场来回时同一张图不再重复下载或解码。
 *
 * @param preserveAspectRatio 为真时按图片真实宽高比约束尺寸（用于详情页头部），否则填满给定的 modifier。
 */
@Composable
internal fun CloudMusicCoverImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    preserveAspectRatio: Boolean = false,
) {
    val context = LocalContext.current
    val safeUrl = imageUrl
        ?.takeIf(String::isNotBlank)
        ?.toArtworkRequestUrl(preserveAspectRatio)

    if (safeUrl == null) {
        // 无 URL：直接展示占位底色，保持原有 aspectRatio 约束（避免布局跳动）。
        val placeholderModifier = if (preserveAspectRatio) {
            modifier.aspectRatio(1f)
        } else {
            modifier
        }
        Box(modifier = placeholderModifier.background(ComposeColor(0xFFEDEDED)))
        return
    }

    val request = fastScrollableImageRequest(
        context = context,
        data = safeUrl,
        sizePx = CloudCoverArtworkSizePx,
    )

    if (!preserveAspectRatio) {
        // 裁剪填充：背景占位 + Coil 上层覆盖，加载未完成时透出占位色。
        Box(modifier = modifier.background(ComposeColor(0xFFEDEDED))) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.matchParentSize(),
            )
        }
        return
    }

    // 保持宽高比：需要拿到解码后的 intrinsic 尺寸再应用 aspectRatio，
    // 加载完成前用 1:1 占位以避免布局抖动。
    PreserveAspectRatioImage(
        request = request,
        contentScale = contentScale,
        modifier = modifier,
    )
}

@Composable
private fun PreserveAspectRatioImage(
    request: ImageRequest,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
) {
    // 用 painter 而非 AsyncImage，以便在加载完成后读取 intrinsicSize，
    // 按封面真实宽高比约束尺寸（对应原实现的 safeAspectRatio()）。
    // 加载未完成时 intrinsicSize 为 Unspecified/Zero，回退到 1:1 占位避免布局抖动。
    val painter = rememberAsyncImagePainter(model = request)
    val intrinsicSize = painter.intrinsicSize
    val intrinsicWidth = intrinsicSize.width
    val intrinsicHeight = intrinsicSize.height
    // 1f * 提升为浮点上下文，避免整数除法截断宽高比。
    val aspect = if (intrinsicWidth > 0 && intrinsicHeight > 0) {
        1f * intrinsicWidth / intrinsicHeight
    } else {
        1f
    }
    Box(modifier = modifier.aspectRatio(aspect).background(ComposeColor(0xFFEDEDED))) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier.matchParentSize(),
        )
    }
}

private fun String.toArtworkRequestUrl(preserveAspectRatio: Boolean): String {
    val normalizedUrl = replaceFirst("http://", "https://")
    return if (preserveAspectRatio) {
        normalizedUrl.withoutArtworkResizeParam()
    } else {
        normalizedUrl.withArtworkResizeParam(CloudCoverArtworkSizePx)
    }
}

private fun String.withoutArtworkResizeParam(): String {
    val uri = runCatching { Uri.parse(this) }.getOrNull() ?: return this
    if (!uri.queryParameterNames.contains("param")) {
        return this
    }
    return uri.copyWithoutArtworkResizeParam().toString()
}

private fun String.withArtworkResizeParam(sizePx: Int): String {
    val uri = runCatching { Uri.parse(this) }.getOrNull() ?: return this
    if (!uri.isNetworkUri()) {
        return this
    }
    val builder = uri.copyWithoutArtworkResizeParam().buildUpon()
    builder.appendQueryParameter("param", "${sizePx}y${sizePx}")
    return builder.build().toString()
}

private fun Uri.copyWithoutArtworkResizeParam(): Uri {
    val builder = buildUpon().clearQuery()
    queryParameterNames
        .filterNot { name -> name == "param" }
        .forEach { name ->
            getQueryParameters(name).forEach { value ->
                builder.appendQueryParameter(name, value)
            }
        }
    return builder.build()
}

private fun Uri.isNetworkUri(): Boolean {
    return scheme == "http" || scheme == "https"
}
