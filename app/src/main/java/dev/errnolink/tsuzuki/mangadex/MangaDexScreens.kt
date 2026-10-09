package dev.errnolink.tsuzuki.mangadex

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import dev.errnolink.tsuzuki.ui.shell.ShellList
import dev.errnolink.tsuzuki.ui.shell.ShellMasonry
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import dev.errnolink.tsuzuki.designsystem.Banner
import dev.errnolink.tsuzuki.designsystem.ContextMenu
import dev.errnolink.tsuzuki.designsystem.ContextMenuAction
import dev.errnolink.tsuzuki.designsystem.CoverArrivalState
import dev.errnolink.tsuzuki.designsystem.CoverImage
import dev.errnolink.tsuzuki.designsystem.DetentSheet
import dev.errnolink.tsuzuki.designsystem.GroupedRow
import dev.errnolink.tsuzuki.designsystem.InsetGroupedList
import dev.errnolink.tsuzuki.designsystem.NoticeTone
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.designsystem.TsuzukiCorners
import dev.errnolink.tsuzuki.designsystem.TsuzukiIcons
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.designsystem.rememberCoverArrivalState
import eu.kanade.core.util.ifSourcesLoaded
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import exh.md.dto.CustomListDto
import exh.md.handlers.MangaDexArtwork
import exh.md.handlers.MangaDexFeedEntry
import kotlinx.coroutines.flow.distinctUntilChanged
import tachiyomi.domain.manga.model.Manga
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class MangaDexFeedScreen : Screen() {

    @Composable
    override fun Content() {
        if (!ifSourcesLoaded()) {
            MangaDexScreenLoading("MangaDex Feed")
            return
        }
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { MangaDexScreenModel(MangaDexDestination.Feed) }
        val state by screenModel.state.collectAsState()
        MangaDexNavigationEffect(screenModel) { navigation ->
            when (navigation) {
                is MangaDexNavigation.Title -> navigator.push(MangaScreen(navigation.id, true))
                is MangaDexNavigation.Chapter -> context.startActivity(
                    ReaderActivity.newIntent(context, navigation.mangaId, navigation.chapterId),
                )
            }
        }
        MangaDexResumeEffect(screenModel::onResume)
        FeedContent(state, navigator::pop, screenModel)
    }

    @Composable
    private fun FeedContent(state: MangaDexState, onBack: () -> Unit, screenModel: MangaDexScreenModel) {
        val listState = rememberLazyListState()
        LaunchedEffect(listState, state.hasNext) {
            snapshotFlow {
                val info = listState.layoutInfo
                (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
            }.distinctUntilChanged().collect { (last, total) ->
                if (total > 0 && last >= total - 4) screenModel.loadMore()
            }
        }
        val arrivals = rememberCoverArrivalState()
        Box(Modifier.fillMaxSize()) {
            ShellList(
                title = "MangaDex Feed",
                navigateUp = onBack,
                state = listState,
                itemSpacing = 0.dp,
                actions = {
                    if (!state.working) {
                        MangaDexChromeAction("Refresh feed", Icons.Outlined.Refresh, screenModel::refresh)
                    }
                },
            ) {
                FeedItems(state, arrivals, screenModel)
            }
            if (state.entries.isNotEmpty() && (state.error != null || state.notice != null)) {
                Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter).navigationBarsPadding()) {
                    MangaDexNoticeOverlay(state)
                }
            }
        }
    }

    private fun LazyListScope.FeedItems(state: MangaDexState, arrivals: CoverArrivalState, screenModel: MangaDexScreenModel) {
        when {
            state.loading -> item(key = "feed-loading") { MangaDexLoading("Loading your feed…") }
            !state.sourceAvailable -> item(key = "feed-unavailable") {
                MangaDexEmptyState("MangaDex is unavailable", "Enable a MangaDex source in Browse first, then come back.")
            }
            !state.signedIn -> item(key = "feed-signin") { MangaDexSignInPrompt() }
            state.entries.isEmpty() -> item(key = "feed-error-or-empty") {
                val error = state.error
                if (error != null) {
                    MangaDexEmptyState(
                        "Feed unavailable",
                        error,
                        action = { PillButton("Try again", screenModel::refresh) },
                    )
                } else {
                    MangaDexEmptyState(
                        "No chapters yet",
                        "New chapters from manga you follow on MangaDex will show up here.",
                    )
                }
            }
            else -> {
                val groups = state.entries.groupBy { it.day() }
                groups.forEach { (day, entries) ->
                    item(key = "feed-day-$day", contentType = "day-header") {
                        TsuzukiText(
                            feedDayLabel(day),
                            Modifier.padding(start = 32.dp, top = 20.dp, bottom = 8.dp),
                            TsuzukiTheme.typography.footnote,
                            TsuzukiTheme.colors.secondary,
                        )
                    }
                    itemsIndexed(entries, key = { _, it -> "feed-${it.chapter.id}" }) { index, entry ->
                        FeedEntryRow(
                            entry, arrivals, screenModel,
                            shape = RoundedCornerShape(
                                topStart = if (index == 0) 14.dp else 0.dp, topEnd = if (index == 0) 14.dp else 0.dp,
                                bottomStart = if (index == entries.lastIndex) 14.dp else 0.dp,
                                bottomEnd = if (index == entries.lastIndex) 14.dp else 0.dp,
                            ),
                            divider = index < entries.lastIndex,
                        )
                    }
                }
                if (state.hasNext) item(key = "feed-more") { MangaDexLoading("Loading more…") }
            }
        }
    }

    @Composable
    private fun FeedEntryRow(entry: MangaDexFeedEntry, arrivals: CoverArrivalState, screenModel: MangaDexScreenModel, shape: CornerBasedShape, divider: Boolean) {
        val colors = TsuzukiTheme.colors
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                .clip(shape)
                .background(colors.grouped)
                .drawBehind {
                    if (divider) drawLine(colors.separator, Offset(84.dp.toPx(), size.height), Offset(size.width, size.height), 0.5.dp.toPx())
                }
                .clickable { screenModel.open(entry, chapter = true) }
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MangaDexRemoteCover(
                url = entry.coverUrl?.let { "$it.512.jpg" },
                contentDescription = entry.mangaTitle,
                modifier = Modifier.height(96.dp),
                arrivals = arrivals,
                onClick = { screenModel.open(entry, chapter = false) },
            )
            Column(Modifier.weight(1f).heightIn(min = 96.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                TsuzukiText(
                    entry.mangaTitle,
                    Modifier.clickable { screenModel.open(entry, chapter = false) },
                    TsuzukiTheme.typography.headline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                TsuzukiText(
                    entry.chapterName,
                    style = TsuzukiTheme.typography.subhead,
                    color = TsuzukiTheme.colors.secondary,
                )
                if (entry.groups.isNotBlank()) {
                    TsuzukiText(
                        entry.groups,
                        style = TsuzukiTheme.typography.footnote,
                        color = TsuzukiTheme.colors.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TsuzukiText(
                    entry.publishedAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = TsuzukiTheme.typography.caption1,
                    color = TsuzukiTheme.colors.accent,
                )
            }
        }
    }
}

internal fun feedDayLabel(day: LocalDate): String {
    val today = LocalDate.now()
    return when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
}

class MangaDexListsScreen : Screen() {

    @Composable
    override fun Content() {
        if (!ifSourcesLoaded()) {
            MangaDexScreenLoading("MD Lists")
            return
        }
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { MangaDexScreenModel(MangaDexDestination.Lists) }
        val state by screenModel.state.collectAsState()
        MangaDexResumeEffect(screenModel::onResume)
        ListsContent(
            state,
            navigator::pop,
            screenModel,
            onOpenList = { list -> navigator.push(MangaDexListScreen(list.id, list.attributes.name)) },
        )
    }

    @Composable
    private fun ListsContent(
        state: MangaDexState,
        onBack: () -> Unit,
        screenModel: MangaDexScreenModel,
        onOpenList: (CustomListDto) -> Unit,
    ) {
        val listState = rememberLazyListState()
        Box(Modifier.fillMaxSize()) {
            ShellList(
                title = "MD Lists",
                navigateUp = onBack,
                state = listState,
                actions = {
                    if (!state.working) {
                        MangaDexChromeAction("Refresh lists", Icons.Outlined.Refresh, screenModel::refresh)
                    }
                },
            ) {
                when {
                    state.loading -> item(key = "lists-loading") { MangaDexLoading("Loading your lists…") }
                    !state.sourceAvailable -> item(key = "lists-unavailable") {
                        MangaDexEmptyState("MangaDex is unavailable", "Enable a MangaDex source in Browse first, then come back.")
                    }
                    !state.signedIn -> item(key = "lists-signin") { MangaDexSignInPrompt() }
                    state.lists.isEmpty() -> item(key = "lists-error-or-empty") {
                        val error = state.error
                        if (error != null) {
                            MangaDexEmptyState(
                                "Lists unavailable",
                                error,
                                action = { PillButton("Try again", screenModel::refresh) },
                            )
                        } else {
                            MangaDexEmptyState("No MD Lists", "Custom lists you create on MangaDex will appear here.")
                        }
                    }
                    else -> item(key = "lists-rows") {
                        InsetGroupedList(footer = "Custom lists from your MangaDex account.") {
                            state.lists.forEachIndexed { index, list ->
                                GroupedRow(
                                    title = list.attributes.name,
                                    subtitle = list.attributes.visibility.replaceFirstChar { it.uppercase() },
                                    value = list.mangaIds.takeIf { it.isNotEmpty() }?.let { "${it.size} titles" },
                                    chevron = true,
                                    divider = index < state.lists.lastIndex,
                                    onClick = { onOpenList(list) },
                                )
                            }
                        }
                    }
                }
            }
            if (state.lists.isNotEmpty() && (state.error != null || state.notice != null)) {
                Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter).navigationBarsPadding()) {
                    MangaDexNoticeOverlay(state)
                }
            }
        }
    }
}

internal class MangaDexListScreen(
    private val listId: String,
    private val listName: String,
) : Screen() {

    @Composable
    override fun Content() {
        if (!ifSourcesLoaded()) {
            MangaDexScreenLoading(listName)
            return
        }
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { MangaDexScreenModel(MangaDexDestination.ListManga, listId = listId) }
        val state by screenModel.state.collectAsState()
        MangaDexResumeEffect(screenModel::onResume)
        ListMangaContent(state, listName, navigator::pop, screenModel, onOpen = { navigator.push(MangaScreen(it, true)) })
    }

    @Composable
    private fun ListMangaContent(
        state: MangaDexState,
        title: String,
        onBack: () -> Unit,
        screenModel: MangaDexScreenModel,
        onOpen: (Long) -> Unit,
    ) {
        val gridState = rememberLazyStaggeredGridState()
        LaunchedEffect(gridState, state.hasNext) {
            snapshotFlow {
                val info = gridState.layoutInfo
                (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
            }.distinctUntilChanged().collect { (last, total) ->
                if (total > 0 && last >= total - 6) screenModel.loadMore()
            }
        }
        val arrivals = rememberCoverArrivalState()
        Box(Modifier.fillMaxSize()) {
            ShellMasonry(
                title = title,
                state = gridState,
                navigateUp = onBack,
                columns = StaggeredGridCells.Adaptive(150.dp),
            ) {
            when {
                state.loading -> fullLineItem("list-loading") { MangaDexLoading("Loading $title…") }
                !state.sourceAvailable -> fullLineItem("list-unavailable") {
                    MangaDexEmptyState("MangaDex is unavailable", "Enable a MangaDex source in Browse first, then come back.")
                }
                state.titles.isEmpty() -> fullLineItem("list-error-or-empty") {
                    val error = state.error
                    if (error != null) {
                        MangaDexEmptyState(
                            "List unavailable",
                            error,
                            action = { PillButton("Try again", screenModel::refresh) },
                        )
                    } else {
                        MangaDexEmptyState("Empty list", "This MD List has no titles yet.")
                    }
                }
                else -> {
                    staggeredItems(state.titles, key = { it.id }) { manga ->
                        ListMangaTile(manga, arrivals, screenModel, onOpen)
                    }
                    if (state.hasNext) fullLineItem("list-more") { MangaDexLoading("Loading more…") }
                }
            }
        }
            if (state.titles.isNotEmpty() && (state.error != null || state.notice != null)) {
                Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter).navigationBarsPadding()) { MangaDexNoticeOverlay(state) }
            }
            }
    }

    @Composable
    private fun ListMangaTile(
        manga: Manga,
        arrivals: CoverArrivalState,
        screenModel: MangaDexScreenModel,
        onOpen: (Long) -> Unit,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            ContextMenu(
                label = manga.title,
                actions = listOf(
                    ContextMenuAction(
                        label = if (manga.favorite) "Already in library" else "Add to library",
                        enabled = !manga.favorite,
                        icon = { Icon(TsuzukiIcons.Library, null, tint = TsuzukiTheme.colors.text) },
                        onClick = { screenModel.addToLibrary(manga) },
                    ),
                    ContextMenuAction("Open details", icon = { Icon(TsuzukiIcons.Book, null, tint = TsuzukiTheme.colors.text) }) { onOpen(manga.id) },
                ),
                onClick = { onOpen(manga.id) },
            ) {
                MangaDexRemoteCover(
                    url = manga.thumbnailUrl?.let { "$it.512.jpg" },
                    contentDescription = manga.title,
                    modifier = Modifier.fillMaxWidth(),
                    arrivals = arrivals,
                )
            }
            TsuzukiText(
                manga.title,
                style = TsuzukiTheme.typography.subhead,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            LibraryCapsule(manga.favorite) { screenModel.addToLibrary(manga) }
        }
    }
}

@Composable
private fun LibraryCapsule(inLibrary: Boolean, onAdd: () -> Unit) {
    val colors = TsuzukiTheme.colors
    val label = if (inLibrary) "In library" else "Add to Library"
    Box(
        Modifier.clip(RoundedCornerShape(percent = 50))
            .background(if (inLibrary) colors.elevated else colors.accent)
            .clickable(enabled = !inLibrary, onClick = onAdd)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        TsuzukiText(
            label,
            style = TsuzukiTheme.typography.footnote,
            color = if (inLibrary) colors.secondary else colors.onAccent,
        )
    }
}

class MangaDexCoverGalleryScreen(private val mangaId: Long) : Screen() {

    @Composable
    override fun Content() {
        if (!ifSourcesLoaded()) {
            MangaDexScreenLoading("Cover Gallery")
            return
        }
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { MangaDexScreenModel(MangaDexDestination.Covers, mangaId = mangaId) }
        val state by screenModel.state.collectAsState()
        var selected by remember { mutableStateOf<MangaDexArtwork?>(null) }
        GalleryContent(
            state,
            navigator::pop,
            screenModel,
            selected,
            onSelect = { selected = it },
        )
    }

    @Composable
    private fun GalleryContent(
        state: MangaDexState,
        onBack: () -> Unit,
        screenModel: MangaDexScreenModel,
        selected: MangaDexArtwork?,
        onSelect: (MangaDexArtwork?) -> Unit,
    ) {
        val arrivals = rememberCoverArrivalState()
        val context = LocalContext.current
        Box(Modifier.fillMaxSize()) {
            ShellMasonry(
                title = "Cover Gallery",
                navigateUp = onBack,
                columns = StaggeredGridCells.Adaptive(150.dp),
                actions = { PillButton("Reset", screenModel::resetCover, prominent = false, enabled = !state.working) },
            ) {
                when {
                    state.loading -> fullLineItem("gallery-loading") { MangaDexLoading("Loading cover art…") }
                    state.covers.isEmpty() -> fullLineItem("gallery-error-or-empty") {
                        val error = state.error
                        if (error != null) {
                            MangaDexEmptyState(
                                "Cover art unavailable",
                                error,
                                action = { PillButton("Try again", screenModel::refresh) },
                            )
                        } else {
                            MangaDexEmptyState("No cover art", "This title has no cover artwork on MangaDex.")
                        }
                    }
                    else -> {
                        fullLineItem("gallery-header") {
                            Column(Modifier.padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                state.manga?.let { manga ->
                                    TsuzukiText(
                                        manga.title,
                                        style = TsuzukiTheme.typography.title3,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                TsuzukiText(
                                    "${state.covers.size} covers · tap one to use it as your library cover",
                                    style = TsuzukiTheme.typography.footnote,
                                    color = TsuzukiTheme.colors.secondary,
                                )
                            }
                        }
                        staggeredItems(state.covers, key = { it.id }) { artwork ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                MangaDexRemoteCover(
                                    url = artwork.thumbnailUrl,
                                    contentDescription = artwork.label,
                                    modifier = Modifier.fillMaxWidth(),
                                    arrivals = arrivals,
                                    onClick = { onSelect(artwork) },
                                )
                                TsuzukiText(artwork.label, style = TsuzukiTheme.typography.headline)
                                artwork.locale?.let {
                                    TsuzukiText(it.uppercase(), style = TsuzukiTheme.typography.caption1, color = TsuzukiTheme.colors.secondary)
                                }
                            }
                        }
                    }
                }
            }
            if (state.covers.isNotEmpty() && (state.error != null || state.notice != null)) {
                Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter).navigationBarsPadding()) { MangaDexNoticeOverlay(state) }
            }
            if (selected != null) {
                CoverSheet(
                    selected!!,
                    state,
                    onUse = {
                        screenModel.useCover(selected!!)
                        onSelect(null)
                    },
                    onSave = { screenModel.saveCover(context, selected, false); onSelect(null) },
                    onShare = { screenModel.saveCover(context, selected, true); onSelect(null) },
                    onDismiss = { onSelect(null) },
                )
            }
        }
    }

    @Composable
    private fun CoverSheet(
        artwork: MangaDexArtwork,
        state: MangaDexState,
        onUse: () -> Unit,
        onSave: () -> Unit,
        onShare: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        DetentSheet(
            visible = true,
            title = artwork.label,
            onDismissRequest = onDismiss,
        ) {
            var loaded by remember(artwork.id) { mutableStateOf(false) }
            var ratio by remember(artwork.id) { mutableStateOf(2f / 3f) }
            CoverImage(
                imageKey = artwork.url,
                aspectRatio = ratio,
                loaded = loaded,
                arrivals = rememberCoverArrivalState(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            ) {
                AsyncImage(
                    model = artwork.url,
                    contentDescription = artwork.label,
                    modifier = Modifier.matchParentSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    onState = { result ->
                        if (result is AsyncImagePainter.State.Success) {
                            val image = result.result.image
                            if (image.width > 0 && image.height > 0) ratio = image.width.toFloat() / image.height
                            loaded = true
                        }
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
            if (artwork.description.isNotBlank()) {
                TsuzukiText(
                    artwork.description,
                    style = TsuzukiTheme.typography.footnote,
                    color = TsuzukiTheme.colors.secondary,
                )
                Spacer(Modifier.height(12.dp))
            }
            val favorite = state.manga?.favorite == true
            PillButton(
                label = "Set as Library Cover",
                onClick = onUse,
                enabled = favorite && !state.working,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Save image", onSave, prominent = false, enabled = !state.working, modifier = Modifier.weight(1f))
                PillButton("Share image", onShare, prominent = false, enabled = !state.working, modifier = Modifier.weight(1f))
            }
            if (!favorite) {
                Banner(
                    "Add this title to your library first to use a custom cover.",
                    tone = NoticeTone.Neutral,
                )
            } else {
                TsuzukiText(
                    "Replaces the cover shown in your Tsuzuki library only.",
                    style = TsuzukiTheme.typography.caption1,
                    color = TsuzukiTheme.colors.secondary,
                )
            }
        }
    }
}

private inline fun LazyStaggeredGridScope.fullLineItem(
    key: Any,
    crossinline content: @Composable () -> Unit,
) {
    item(key = key, span = StaggeredGridItemSpan.FullLine) { content() }
}

@Composable
private fun MangaDexScreenLoading(title: String) {
    val navigator = LocalNavigator.currentOrThrow
    ShellList(title = title, navigateUp = { navigator.pop() }) {
        item("source-loading") { MangaDexLoading("Loading MangaDex…") }
    }
}
