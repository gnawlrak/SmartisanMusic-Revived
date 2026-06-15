package com.smartisanos.music.ui.shell.cloud.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.smartisanos.music.playback.loadArtworkBitmap
import com.smartisanos.music.ui.shell.cloud.CloudCoverArtworkSizePx

@Composable
internal fun CloudMusicCoverImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    preserveAspectRatio: Boolean = false,
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, imageUrl, preserveAspectRatio) {
        value = null
        val safeUrl = imageUrl
            ?.takeIf(String::isNotBlank)
            ?.toArtworkRequestUrl(preserveAspectRatio)
            ?: return@produceState
        value = runCatching {
            val mediaItem = MediaItem.Builder()
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setArtworkUri(Uri.parse(safeUrl))
                        .build(),
                )
                .build()
            loadArtworkBitmap(
                context = context.applicationContext,
                mediaItem = mediaItem,
                size = android.util.Size(CloudCoverArtworkSizePx, CloudCoverArtworkSizePx),
            )
        }.getOrNull()
    }
    val loadedBitmap = bitmap
    if (loadedBitmap != null) {
        val imageModifier = if (preserveAspectRatio) {
            modifier.aspectRatio(loadedBitmap.safeAspectRatio())
        } else {
            modifier
        }
        Image(
            bitmap = loadedBitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = contentScale,
            modifier = imageModifier,
        )
    } else {
        val placeholderModifier = if (preserveAspectRatio) {
            modifier.aspectRatio(1f)
        } else {
            modifier
        }
        Box(
            modifier = placeholderModifier.background(ComposeColor(0xFFEDEDED)),
        )
    }
}

private fun Bitmap.safeAspectRatio(): Float {
    return width.toFloat() / height.coerceAtLeast(1).toFloat()
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
