package com.smartisanos.music.ui.shell.cloud

import android.widget.ListView
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.smartisanos.music.R

internal class CloudLegacyListScrollState(
    var firstVisiblePosition: Int = 0,
    var firstVisibleTopOffset: Int = 0,
) {
    private var restoreInProgress = false

    val hasPosition: Boolean
        get() = firstVisiblePosition > 0 || firstVisibleTopOffset != 0

    fun capture(listView: ListView) {
        if (restoreInProgress) {
            return
        }
        firstVisiblePosition = listView.firstVisiblePosition.coerceAtLeast(0)
        firstVisibleTopOffset = listView.getChildAt(0)?.top ?: 0
    }

    fun restore(listView: ListView) {
        val adapterCount = listView.adapter?.count ?: return
        if (adapterCount <= 0) {
            return
        }
        val safePosition = firstVisiblePosition.coerceIn(0, adapterCount - 1)
        restoreInProgress = true
        listView.setSelectionFromTop(safePosition, firstVisibleTopOffset)
        listView.post {
            restoreInProgress = false
            capture(listView)
        }
    }

    companion object {
        val Saver: Saver<CloudLegacyListScrollState, Any> = listSaver(
            save = { state ->
                listOf(state.firstVisiblePosition, state.firstVisibleTopOffset)
            },
            restore = { restored ->
                CloudLegacyListScrollState(
                    firstVisiblePosition = restored.getOrElse(0) { 0 },
                    firstVisibleTopOffset = restored.getOrElse(1) { 0 },
                )
            },
        )
    }
}

internal fun ListView.restoreCloudLegacyListScroll(
    scrollState: CloudLegacyListScrollState?,
    force: Boolean = false,
) {
    if (scrollState == null || !scrollState.hasPosition) {
        return
    }
    if (force || getTag(R.id.cloud_music_scroll_state) !== scrollState) {
        scrollState.restore(this)
        setTag(R.id.cloud_music_scroll_state, scrollState)
    }
}
