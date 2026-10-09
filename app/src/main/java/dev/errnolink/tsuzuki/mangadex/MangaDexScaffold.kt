package dev.errnolink.tsuzuki.mangadex

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.GlassContext
import dev.errnolink.tsuzuki.designsystem.GlassSurface
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.designsystem.glassSource
import dev.errnolink.tsuzuki.designsystem.rememberGlassContext

@Composable
internal fun MangaDexListScaffold(
    title: String,
    onBack: () -> Unit,
    state: LazyListState = rememberLazyListState(),
    actions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val glass = rememberGlassContext()
    val collapsed by remember(state) { derivedStateOf { state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 80 } }
    Box(Modifier.fillMaxSize().background(TsuzukiTheme.colors.background).safeDrawingPadding()) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize().glassSource(glass),
            contentPadding = PaddingValues(top = 72.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "mangadex-large-title") {
                TsuzukiText(title, Modifier.padding(horizontal = 20.dp), TsuzukiTheme.typography.largeTitle)
            }
            content()
        }
        MangaDexNavigationBar(title, collapsed, glass, onBack, actions)
    }
}

@Composable
internal fun MangaDexNavigationBar(
    title: String,
    collapsed: Boolean,
    glass: GlassContext,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    GlassSurface(glass, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MangaDexBackAction(onBack)
            if (collapsed) TsuzukiText(title, Modifier.weight(1f), TsuzukiTheme.typography.headline, maxLines = 1, overflow = TextOverflow.Ellipsis)
            else Spacer(Modifier.weight(1f))
            actions()
        }
    }
}
