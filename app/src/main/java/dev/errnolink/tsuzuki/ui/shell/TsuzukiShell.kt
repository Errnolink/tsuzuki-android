package dev.errnolink.tsuzuki.ui.shell

import android.util.LruCache
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.rememberConstraintsSizeResolver
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import androidx.compose.ui.platform.LocalContext
import coil3.decode.DataSource
import dev.errnolink.tsuzuki.designsystem.Banner
import dev.errnolink.tsuzuki.designsystem.CoverArrivalState
import dev.errnolink.tsuzuki.designsystem.CoverImage
import dev.errnolink.tsuzuki.designsystem.DetentSheet
import dev.errnolink.tsuzuki.designsystem.EmptyState
import dev.errnolink.tsuzuki.designsystem.GlassContext
import dev.errnolink.tsuzuki.designsystem.FloatingTopBar
import dev.errnolink.tsuzuki.designsystem.floatingNavigationScroll
import dev.errnolink.tsuzuki.designsystem.TsuzukiIcons
import dev.errnolink.tsuzuki.designsystem.IconButton
import dev.errnolink.tsuzuki.designsystem.LargeTitleScaffold
import dev.errnolink.tsuzuki.designsystem.ProgressRing
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.designsystem.glassSource
import dev.errnolink.tsuzuki.designsystem.rememberCoverArrivalState
import dev.errnolink.tsuzuki.designsystem.rememberGlassContext
import eu.kanade.tachiyomi.ui.home.HomeScreen
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.manga.model.asMangaCover

val LocalShellBottomPadding = staticCompositionLocalOf { 0.dp }
val LocalShellGlass = staticCompositionLocalOf<GlassContext?> { null }
val LocalShellArrivals = staticCompositionLocalOf<CoverArrivalState?> { null }
private val coverRatios = LruCache<String, Float>(1024)

class ShellSheetAction(val label: String, val destructive: Boolean = false, val onClick: () -> Unit)

@Composable
fun shellBottomPadding(): androidx.compose.ui.unit.Dp = maxOf(
    LocalShellBottomPadding.current,
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
) + LocalAppStateBottomPadding.current

@Composable
fun ShellAction(label: String, icon: ImageVector, onClick: () -> Unit, active: Boolean = false) {
    IconButton(label, onClick) {
        Icon(icon, null, tint = if (active) TsuzukiTheme.colors.accent else TsuzukiTheme.colors.text)
    }
}

@Composable
fun ShellList(
    title: String,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    reselectKey: String? = null,
    navigateUp: (() -> Unit)? = null,
    snackbar: SnackbarHostState? = null,
    actions: @Composable RowScope.() -> Unit = {},
    itemSpacing: androidx.compose.ui.unit.Dp = TsuzukiTheme.spacing.section,
    content: LazyListScope.() -> Unit,
) {
    val clearance = shellBottomPadding()
    val shellGlass = LocalShellGlass.current
    val glass = shellGlass ?: rememberGlassContext()
    LaunchedEffect(reselectKey, state) {
        HomeScreen.reselectEvents.collect { if (it == reselectKey) state.scrollToItem(0) }
    }
    Box(modifier.fillMaxSize()) {
        LargeTitleScaffold(
            title,
            state = state,
            glassContext = glass,
            navigationIcon = {
                if (navigateUp != null) ShellAction("Back", TsuzukiIcons.Back, navigateUp)
            },
            actions = actions,
            bottomPadding = clearance,
            itemSpacing = itemSpacing,
            content = content,
        )
        ShellSnackbar(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = clearance + 12.dp))
    }
}

@Composable
fun ShellSnackbar(state: SnackbarHostState?, modifier: Modifier = Modifier) {
    val data = state?.currentSnackbarData ?: return
    LaunchedEffect(data) {
        if (data.visuals.duration != SnackbarDuration.Indefinite) {
            delay(if (data.visuals.actionLabel == null) 4000L else 10000L)
            data.dismiss()
        }
    }
    Banner(
        data.visuals.message,
        modifier.padding(horizontal = TsuzukiTheme.spacing.gutter),
        actionLabel = data.visuals.actionLabel,
        onAction = data.visuals.actionLabel?.let { { data.performAction() } },
        onDismiss = data::dismiss,
    )
}

@Composable
fun ShellActionSheet(
    visible: Boolean,
    title: String,
    actions: List<ShellSheetAction>,
    onDismissRequest: () -> Unit,
) {
    if (!visible) return
    DetentSheet(visible = true, title = title, onDismissRequest = onDismissRequest) {
        actions.forEachIndexed { index, action ->
            ShellSheetRow(
                action.label,
                destructive = action.destructive,
                divider = index < actions.lastIndex,
                onClick = { onDismissRequest(); action.onClick() },
            )
        }
    }
}

@Composable
private fun ShellSheetRow(label: String, destructive: Boolean, divider: Boolean, onClick: () -> Unit) {
    val colors = TsuzukiTheme.colors
    Column {
        Row(
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = TsuzukiTheme.spacing.touchTarget)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TsuzukiText(label, color = if (destructive) colors.destructive else colors.text)
        }
        if (divider) {
            Spacer(
                Modifier.padding(start = 16.dp).fillMaxWidth()
                    .height(TsuzukiTheme.spacing.hairline)
                    .background(colors.separator),
            )
        }
    }
}

@Composable
fun ShellCaption(text: String, modifier: Modifier = Modifier) {
    TsuzukiText(
        text,
        modifier.padding(vertical = TsuzukiTheme.spacing.tiny),
        TsuzukiTheme.typography.footnote,
        TsuzukiTheme.colors.secondary,
    )
}

@Composable
fun ShellEmpty(title: String, description: String, action: (@Composable () -> Unit)? = null) {
    EmptyState(title, description, icon = {
        Icon(Icons.Outlined.MenuBook, null, Modifier.size(42.dp), tint = TsuzukiTheme.colors.secondary)
    }, action = action)
}

@Composable
fun ShellLoading(label: String = "Loading…") {
    Row(
        Modifier.fillMaxWidth().padding(TsuzukiTheme.spacing.section),
        horizontalArrangement = Arrangement.spacedBy(TsuzukiTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressRing(0f, label)
        TsuzukiText(label, color = TsuzukiTheme.colors.secondary)
    }
}

@Composable
fun ShellCover(cover: MangaCover, title: String, modifier: Modifier = Modifier, fixedRatio: Float? = null) {
    val key = "${cover.sourceId}:${cover.mangaId}:${cover.url}:${cover.lastModified}"
    val arrivals = LocalShellArrivals.current ?: rememberCoverArrivalState()
    var ratio by remember(key) {
        mutableFloatStateOf(
            cover.ratio?.takeIf { it.isFinite() && it > 0f }
                ?: coverRatios.get(key)
                ?: (2f / 3f),
        )
    }
    var loaded by remember(key) { mutableStateOf(false) }
    var cached by remember(key) { mutableStateOf(false) }
    var failed by remember(key) { mutableStateOf(false) }
    val context = LocalContext.current
    val sizeResolver = rememberConstraintsSizeResolver()
    val request = remember(cover, context, sizeResolver) {
        ImageRequest.Builder(context)
            .data(cover)
            .size(sizeResolver)
            .precision(Precision.INEXACT)
            .crossfade(false)
            .build()
    }
    Box(modifier) {
        CoverImage(key, fixedRatio ?: ratio, loaded, arrivals, cached = cached, label = title, modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = request,
                contentDescription = title,
                modifier = Modifier.fillMaxSize().then(sizeResolver),
                contentScale = ContentScale.Crop,
                onSuccess = {
                    val image = it.result.image
                    if (image.width > 0 && image.height > 0) {
                        ratio = cover.ratio?.takeIf { value -> value.isFinite() && value > 0f }
                            ?: (image.width.toFloat() / image.height)
                        coverRatios.put(key, ratio)
                        cover.ratio = ratio
                    }
                    cached = it.result.dataSource == DataSource.MEMORY_CACHE
                    loaded = true
                },
                onError = { failed = true },
            )
        }
        if (failed) {
            Icon(
                Icons.Outlined.MenuBook,
                "Cover unavailable",
                Modifier.align(Alignment.Center).size(28.dp),
                tint = TsuzukiTheme.colors.secondary,
            )
        }
    }
}

@Composable
fun ShellMangaTile(
    manga: Manga,
    selected: Boolean,
    unread: Long = 0,
    downloads: Long = 0,
    modifier: Modifier = Modifier,
) {
    val colors = TsuzukiTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box {
            ShellCover(manga.asMangaCover(), manga.title, Modifier.fillMaxWidth())
            if (selected) {
                Box(
                    Modifier.align(Alignment.TopStart).padding(7.dp).size(26.dp)
                        .background(colors.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Check, "Selected", Modifier.size(17.dp), tint = colors.onAccent)
                }
            }
            Row(
                Modifier.align(Alignment.TopEnd).padding(7.dp),
                horizontalArrangement = Arrangement.spacedBy(TsuzukiTheme.spacing.tiny),
            ) {
                if (downloads > 0) ShellBadge("↓ $downloads")
                if (unread > 0) ShellBadge(unread.toString())
            }
        }
        TsuzukiText(manga.title, style = TsuzukiTheme.typography.subhead, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ShellBadge(label: String) {
    TsuzukiText(
        label,
        Modifier.clip(RoundedCornerShape(7.dp))
            .background(TsuzukiTheme.colors.elevated)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        TsuzukiTheme.typography.caption2,
    )
}

@Composable
fun ShellMasonry(
    title: String,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    reselectKey: String? = null,
    navigateUp: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    columns: StaggeredGridCells = StaggeredGridCells.Adaptive(140.dp),
    content: LazyStaggeredGridScope.() -> Unit,
) {
    val colors = TsuzukiTheme.colors
    val shellGlass = LocalShellGlass.current
    val glass = shellGlass ?: rememberGlassContext()
    val clearance = shellBottomPadding()
    var titleHeight by remember { mutableIntStateOf(0) }
    var navigationHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val collapsed by remember(state) {
        derivedStateOf {
            state.firstVisibleItemIndex > 0 ||
                (titleHeight > 0 && state.firstVisibleItemScrollOffset >= titleHeight)
        }
    }
    LaunchedEffect(reselectKey, state) {
        HomeScreen.reselectEvents.collect { if (it == reselectKey) state.scrollToItem(0) }
    }
    Box(Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        LazyVerticalStaggeredGrid(
            columns = columns,
            state = state,
            modifier = Modifier.fillMaxSize().floatingNavigationScroll(glass).glassSource(glass),
            horizontalArrangement = Arrangement.spacedBy(TsuzukiTheme.spacing.medium),
            verticalItemSpacing = TsuzukiTheme.spacing.section,
            contentPadding = PaddingValues(
                start = TsuzukiTheme.spacing.gutter,
                end = TsuzukiTheme.spacing.gutter,
                top = with(density) { navigationHeight.toDp() } + 8.dp,
                bottom = clearance + TsuzukiTheme.spacing.section,
            ),
        ) {
            item(key = "shell-large-title", span = StaggeredGridItemSpan.FullLine) {
                TsuzukiText(
                    title,
                    Modifier.fillMaxWidth().onSizeChanged { titleHeight = it.height },
                    TsuzukiTheme.typography.largeTitle,
                )
            }
            content()
        }
        FloatingTopBar(
            title = title,
            collapsed = collapsed,
            context = glass,
            modifier = Modifier.onSizeChanged { navigationHeight = it.height },
            navigationIcon = {
                if (navigateUp != null) ShellAction("Back", TsuzukiIcons.Back, navigateUp)
            },
            actions = actions,
        )
    }
}

@Composable
fun ShellRefresh(
    refreshing: Boolean,
    enabled: Boolean = true,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit,
) {
    val threshold = with(LocalDensity.current) { 88.dp.toPx() }
    var pulled by remember { mutableFloatStateOf(0f) }
    val currentRefresh by rememberUpdatedState(onRefresh)
    val connection = remember(enabled, refreshing, threshold) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0 && pulled > 0 && source == NestedScrollSource.UserInput) {
                    val consumed = available.y.coerceAtLeast(-pulled)
                    pulled += consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!enabled || refreshing || available.y <= 0 || source != NestedScrollSource.UserInput) return Offset.Zero
                pulled = (pulled + available.y * 0.5f).coerceAtMost(threshold * 1.5f)
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pulled == 0f) return Velocity.Zero
                if (pulled >= threshold) currentRefresh()
                pulled = 0f
                return Velocity(0f, available.y)
            }
        }
    }
    Box(Modifier.fillMaxSize().nestedScroll(connection)) {
        content()
        if (pulled > 0 || refreshing) {
            Box(
                Modifier.align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = TsuzukiTheme.spacing.small)
                    .clip(RoundedCornerShape(24.dp))
                    .background(TsuzukiTheme.colors.elevated)
                    .padding(10.dp),
            ) {
                ProgressRing(
                    if (refreshing) 0f else pulled / threshold,
                    if (refreshing) "Updating library" else "Pull to update",
                )
            }
        }
    }
}

@Composable
fun ShellFilterRow(
    label: String,
    state: tachiyomi.core.common.preference.TriState,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    dev.errnolink.tsuzuki.designsystem.GroupedRow(
        title = label,
        value = when (state) {
            tachiyomi.core.common.preference.TriState.DISABLED -> "Any"
            tachiyomi.core.common.preference.TriState.ENABLED_IS -> "Include"
            tachiyomi.core.common.preference.TriState.ENABLED_NOT -> "Exclude"
        },
        enabled = enabled,
        onClick = onClick,
    )
}
