package com.smartisanos.music.playback

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import android.util.Size
import androidx.media3.common.MediaItem
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.request.crossfade
import coil3.size.Precision
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object NowPlayingArtworkRepository {
    private val cache = object : LruCache<ArtworkCacheKey, Bitmap>(artworkCacheSizeKb()) {
        override fun sizeOf(key: ArtworkCacheKey, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    fun peek(
        mediaItem: MediaItem,
        size: Size,
        allowAnySize: Boolean = true,
    ): Bitmap? {
        val identity = mediaItem.artworkRequestKey()
        val exactKey = ArtworkCacheKey(identity, size.width, size.height)
        cache.get(exactKey)?.let { return it }
        if (!allowAnySize) {
            return null
        }
        return cache.snapshot()
            .asSequence()
            .filter { (key, _) -> key.identity == identity }
            .map { (_, bitmap) -> bitmap }
            .maxByOrNull { bitmap -> bitmap.width * bitmap.height }
    }

    suspend fun load(
        context: Context,
        mediaItem: MediaItem,
        size: Size,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val identity = mediaItem.artworkRequestKey()
        val cacheKey = ArtworkCacheKey(identity, size.width, size.height)
        cache.get(cacheKey)?.let { return@withContext it }
        val reusableBitmap = peek(mediaItem, size)

        val bitmap = loadBitmap(appContext, mediaItem, size)
            ?.also { loaded ->
                loaded.prepareToDraw()
                cache.put(cacheKey, loaded)
            }
        bitmap ?: reusableBitmap
    }

    private suspend fun loadBitmap(
        context: Context,
        mediaItem: MediaItem,
        size: Size,
    ): Bitmap? {
        val metadata = mediaItem.mediaMetadata
        return decodeArtworkData(metadata.artworkData, size)
            ?: loadNetworkArtworkBitmap(context, metadata.artworkUri, size)
            ?: loadArtworkBitmapSync(context, mediaItem, size)
    }

    private suspend fun loadNetworkArtworkBitmap(
        context: Context,
        uri: Uri?,
        size: Size,
    ): Bitmap? {
        uri ?: return null
        if (!uri.isNetworkUri()) {
            return null
        }
        return runCatching {
            val imageLoader = SingletonImageLoader.get(context)
            val result = imageLoader.execute(
                ImageRequest.Builder(context)
                    .data(uri.toString())
                    .size(size.width, size.height)
                    .precision(Precision.INEXACT)
                    .crossfade(false)
                    .allowHardware(false)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build(),
            )
            val image = (result as? SuccessResult)?.image ?: return@runCatching null
            image.toBitmap(
                width = image.width.coerceAtLeast(1),
                height = image.height.coerceAtLeast(1),
            ).scaledToFit(size)
        }.getOrNull()
    }
}

private data class ArtworkCacheKey(
    val identity: ArtworkRequestKey,
    val width: Int,
    val height: Int,
)

private fun Uri.isNetworkUri(): Boolean {
    return scheme == "http" || scheme == "https"
}

private fun Bitmap.scaledToFit(size: Size): Bitmap {
    val maxWidth = size.width.coerceAtLeast(1)
    val maxHeight = size.height.coerceAtLeast(1)
    if (width <= maxWidth && height <= maxHeight) {
        return this
    }
    val scale = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
    val scaledWidth = (width * scale).toInt().coerceAtLeast(1)
    val scaledHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
}

private fun artworkCacheSizeKb(): Int {
    val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    return (maxMemoryKb / 16).coerceAtLeast(4 * 1024)
}
