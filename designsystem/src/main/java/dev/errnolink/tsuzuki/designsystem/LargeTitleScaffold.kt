package dev.errnolink.tsuzuki.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun LargeTitleScaffold(
    title: String,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    glassContext: GlassContext = rememberGlassContext(),
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    bottomPadding: Dp = 0.dp,
    itemSpacing: Dp = TsuzukiSpacing.section,
    content: LazyListScope.() -> Unit,
) {
    val colors = TsuzukiTheme.colors
    val density = LocalDensity.current
    var titleHeight by remember { mutableIntStateOf(0) }
    var navigationHeight by remember { mutableIntStateOf(0) }
    var bottomHeight by remember { mutableIntStateOf(0) }
    val collapsed by remember(state) {
        derivedStateOf {
            state.firstVisibleItemIndex > 0 || (titleHeight > 0 && state.firstVisibleItemScrollOffset >= titleHeight)
        }
    }
    Box(
        modifier.fillMaxSize().background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)).statusBarsPadding(),
    ) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize().floatingNavigationScroll(glassContext).glassSource(glassContext),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            contentPadding = PaddingValues(
                top = with(density) { navigationHeight.toDp() },
                bottom = max(with(density) { bottomHeight.toDp().value }, bottomPadding.value).dp + TsuzukiSpacing.section,
            ),
        ) {
            item(key = "tsuzuki-large-title") {
                TsuzukiText(
                    title,
                    Modifier.fillMaxWidth().onSizeChanged { titleHeight = it.height }
                        .padding(horizontal = TsuzukiSpacing.gutter, vertical = 8.dp)
                        .then(if (collapsed) Modifier.clearAndSetSemantics {} else Modifier),
                    TsuzukiTheme.typography.largeTitle,
                )
            }
            content()
        }
        FloatingTopBar(
            title = title,
            collapsed = collapsed,
            context = glassContext,
            modifier = Modifier.onSizeChanged { navigationHeight = it.height },
            navigationIcon = navigationIcon,
            actions = actions,
        )
        Box(
            Modifier.align(Alignment.BottomCenter).onSizeChanged { bottomHeight = it.height }
                .navigationBarsPadding()
                .then(if (bottomBar != null) Modifier.padding(horizontal = 20.dp, vertical = 8.dp) else Modifier),
        ) {
            bottomBar?.invoke()
        }
    }
}
