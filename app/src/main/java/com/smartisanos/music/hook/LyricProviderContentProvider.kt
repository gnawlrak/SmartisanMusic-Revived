package com.smartisanos.music.hook

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.smartisanos.music.playback.liveupdate.LyricStateHolder

/**
 * 把当前歌词快照通过 ContentProvider 暴露给独立的 LSPosed hook 模块
 * （运行在 SystemUI 进程，按 [LyricContract] 契约查询）。
 *
 * 列名/authority 全部取自本地 [LyricContract]，与 LSPosed 模块的副本保持一致。
 */
class LyricProviderContentProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val s = LyricStateHolder
        val cursor = MatrixCursor(LyricContract.COLUMNS)
        cursor.addRow(
            arrayOf<Any?>(
                s.title,
                s.artist,
                s.album,
                s.currentLine,
                s.positionMs,
                s.durationMs,
                s.lineStartMs,
                s.lineEndMs,
                if (s.isPlaying) 1 else 0,
                if (s.hasLyrics) 1 else 0,
                s.packageName,
                s.currentLineTokens,
                s.primaryColor,
                s.secondaryColor,
                s.coverUri,
            ),
        )
        return cursor
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
