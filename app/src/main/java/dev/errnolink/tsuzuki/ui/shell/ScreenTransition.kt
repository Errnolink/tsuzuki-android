package dev.errnolink.tsuzuki.ui.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.Navigator
import dev.errnolink.tsuzuki.designsystem.LocalNavigationMotion
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import kotlinx.coroutines.delay

private val NavigationEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Composable
fun rememberTabNavigationMotion(key: String): Boolean {
    val settledKey by produceState(key, key) {
        delay(350)
        value = key
    }
    return LocalNavigationMotion.current || (!TsuzukiTheme.motion.reduced && settledKey != key)
}

@Composable
fun TsuzukiScreenTransition(navigator: Navigator, modifier: Modifier = Modifier) {
    val transition = updateTransition(navigator.lastItem, label = "navigation")
    val moving = transition.currentState != transition.targetState || transition.isRunning
    val duration = if (TsuzukiTheme.motion.reduced) 0 else 350
    val forward = navigator.lastEvent != StackEvent.Pop
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    CompositionLocalProvider(LocalNavigationMotion provides moving) {
        transition.AnimatedContent(
            modifier = modifier,
            contentKey = { it.key },
            transitionSpec = {
                (fadeIn(tween(duration, easing = NavigationEasing)) togetherWith
                    fadeOut(tween(duration, easing = NavigationEasing))).using(null)
            },
        ) { screen ->
            val offset by this.transition.animateFloat(
                transitionSpec = { tween(duration, easing = NavigationEasing) },
                label = "navigation-translation",
            ) { state ->
                when (state) {
                    EnterExitState.PreEnter -> if (forward) 0.12f else -0.04f
                    EnterExitState.Visible -> 0f
                    EnterExitState.PostExit -> if (forward) -0.04f else 0.12f
                }
            }
            Box(Modifier.fillMaxSize().graphicsLayer { translationX = size.width * offset * direction }) {
                navigator.saveableState("screen-transition-${screen.key}", screen) {
                    screen.Content()
                }
            }
        }
    }
}
