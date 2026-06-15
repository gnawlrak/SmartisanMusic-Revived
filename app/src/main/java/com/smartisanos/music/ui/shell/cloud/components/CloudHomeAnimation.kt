package com.smartisanos.music.ui.shell.cloud.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * 云音乐首页区块的错峰入场动画。
 *
 * 首次进入组合时延迟 [delayMillis]，然后以淡入 + 上滑 12dp 的方式出现。
 * 用于首页 Banner 下方的各个分区卡片，形成层次感的加载节奏。
 */
@Composable
internal fun CloudHomeAnimatedSection(
    index: Int,
    modifier: Modifier = Modifier,
    delayMillisPerItem: Int = 60,
    content: @Composable () -> Unit,
) {
    val visibleState = remember {
        MutableTransitionState(false)
    }
    LaunchedEffect(Unit) {
        val delay = index * delayMillisPerItem.toLong()
        if (delay > 0) {
            kotlinx.coroutines.delay(delay)
        }
        visibleState.targetState = true
    }
    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(220)) +
            slideInVertically(
                animationSpec = tween(280, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 12 },
            ),
    ) {
        content()
    }
}
