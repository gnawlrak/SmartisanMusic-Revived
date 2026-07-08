package com.smartisanos.music.hook

import android.net.Uri

/**
 * 与独立 LSPosed 模块（LyricsIslandLSPosed）之间的歌词数据公共契约（内联副本）。
 *
 * 本文件在音乐 App 与 LSPosed 模块各有一份相同常量副本（双方无代码依赖），
 * 仅靠此文档化约定保持一致。修改 authority / 列名属破坏性变更，双方必须同步。
 *
 * 音乐 App 通过 [LyricProviderContentProvider]（此 authority）暴露快照；
 * LSPosed 模块（运行在 SystemUI）按本契约查询。
 */
internal object LyricContract {

    const val AUTHORITY = "com.smartisanos.music.lyric"
    val SNAPSHOT_URI: Uri = Uri.parse("content://$AUTHORITY/snapshot")

    const val COL_TITLE = "title"
    const val COL_ARTIST = "artist"
    const val COL_ALBUM = "album"
    const val COL_LINE = "line"
    const val COL_POS = "pos"
    const val COL_DURATION = "duration"
    const val COL_LINE_START = "start"
    const val COL_LINE_END = "end"
    const val COL_IS_PLAYING = "playing"
    const val COL_HAS_LYRICS = "has_lyrics"
    const val COL_PACKAGE = "pkg"
    const val COL_TOKENS = "tokens"
    const val COL_PRIMARY_COLOR = "primary_color"
    const val COL_SECONDARY_COLOR = "secondary_color"
    const val COL_COVER_URI = "cover_uri"

    val COLUMNS = arrayOf(
        COL_TITLE, COL_ARTIST, COL_ALBUM, COL_LINE, COL_POS, COL_DURATION,
        COL_LINE_START, COL_LINE_END, COL_IS_PLAYING, COL_HAS_LYRICS, COL_PACKAGE, COL_TOKENS,
        COL_PRIMARY_COLOR, COL_SECONDARY_COLOR, COL_COVER_URI,
    )
}
