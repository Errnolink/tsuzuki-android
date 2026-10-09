package dev.errnolink.tsuzuki.ui.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import tachiyomi.core.common.preference.TriState
import eu.kanade.domain.manga.model.downloadedFilter
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import dev.errnolink.tsuzuki.designsystem.LocalNavigationMotion
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.Banner
import dev.errnolink.tsuzuki.designsystem.DetentSheet
import dev.errnolink.tsuzuki.designsystem.GroupedRow
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.designsystem.TsuzukiCorners
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.designsystem.ContextMenuAction
import dev.errnolink.tsuzuki.designsystem.FloatingTopBar
import dev.errnolink.tsuzuki.designsystem.GlassMenu
import dev.errnolink.tsuzuki.designsystem.TsuzukiIcons
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import dev.errnolink.tsuzuki.designsystem.glassSource
import dev.errnolink.tsuzuki.designsystem.rememberGlassContext
import dev.errnolink.tsuzuki.mangadex.MangaDexDetailsSection
import dev.errnolink.tsuzuki.mangadex.MangaDexChapterMenu
import dev.errnolink.tsuzuki.mangadex.rememberMangaDexChapterPresentation
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.presentation.manga.components.SearchMetadataChips
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.getNameForMangaInfo
import eu.kanade.tachiyomi.source.isIncognitoModeEnabled
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.ui.manga.ChapterList
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel
import eu.kanade.tachiyomi.util.system.copyToClipboard
import exh.source.MERGED_SOURCE_ID
import exh.source.getMainSource
import exh.ui.metadata.adapters.MangaDexDescription
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.isLocal
import java.time.Instant

@Composable
fun MangaScreen(
    state: MangaScreenModel.State.Success,
    snackbarHostState: SnackbarHostState,
    nextUpdate: Instant?,
    isTabletUi: Boolean,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    navigateUp: () -> Unit,
    onChapterClicked: (Chapter) -> Unit,
    onDownloadChapter: ((List<ChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,
    onTagSearch: (String) -> Unit,
    onFilterButtonClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueReading: () -> Unit,
    continueChapter: Chapter?,
    onSearch: (query: String, global: Boolean) -> Unit,
    onCoverClicked: () -> Unit,
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onEditFetchIntervalClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    onEditNotesClicked: () -> Unit,
    onMetadataViewerClicked: () -> Unit,
    onEditInfoClicked: () -> Unit,
    onRecommendClicked: () -> Unit,
    onMergedSettingsClicked: () -> Unit,
    onMergeClicked: () -> Unit,
    onMergeWithAnotherClicked: () -> Unit,
    onMultiBookmarkClicked: (List<Chapter>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<Chapter>, markAsRead: Boolean) -> Unit,
    onMarkPreviousAsReadClicked: (Chapter) -> Unit,
    onMultiDeleteClicked: (List<Chapter>) -> Unit,
    onChapterSwipe: (ChapterList.Item, LibraryPreferences.ChapterSwipeAction) -> Unit,
    onChapterSelected: (ChapterList.Item, Boolean, Boolean) -> Unit,
    onAllChapterSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    getMangaState: @Composable (Manga) -> State<Manga>,
    onClickSourceSettingsClicked: (() -> Unit)?,
    onClearManga: () -> Unit,
    onOpenMangaFolder: (() -> Unit)?,
    onRelatedMangasScreenClick: () -> Unit,
    onRelatedMangaClick: (Manga) -> Unit,
    onRelatedMangaLongClick: (Manga) -> Unit,
    librarySearch: (query: String) -> Unit,
    onSourceClick: () -> Unit,
    coverRatio: MutableFloatState,
    onCoversClicked: (() -> Unit)? = null,
    onPaletteClicked: (() -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val glass = rememberGlassContext()
    val context = LocalContext.current
    val moving = LocalNavigationMotion.current
    var detailsReady by remember(state.manga.id) { mutableStateOf(false) }
    LaunchedEffect(moving) {
        if (!moving) {
            withFrameNanos { }
            detailsReady = true
        }
    }
    val dex = rememberMangaDexChapterPresentation(state.manga.id, remember(state.chapters) { state.chapters.map { it.chapter } })
    val chapters = remember(state.processedChapters, dex) { state.processedChapters.filter { dex.isVisible(it.chapter) } }
    val chapterListItems = if (chapters.size == state.processedChapters.size) state.chapterListItems else chapters
    val visibleContinueChapter = continueChapter?.takeIf(dex::isVisible)
        ?: chapters.asReversed().firstOrNull { !it.chapter.read && dex.accessMessage(it.chapter) == null }?.chapter
    val selected = remember(state.chapters) { state.chapters.filter { it.selected } }
    val collapseOffset = with(LocalDensity.current) { 48.dp.roundToPx() }
    val collapsed by remember(collapseOffset) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > collapseOffset }
    }
    var menu by remember { mutableStateOf(false) }
    var menuAtTop by remember { mutableStateOf(true) }
    var downloads by remember { mutableStateOf(false) }
    val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val top = statusBarInset + 56.dp
    val activeFilters = buildList {
        when (state.manga.downloadedFilter) {
            TriState.ENABLED_IS -> add("Downloaded only")
            TriState.ENABLED_NOT -> add("Not downloaded")
            TriState.DISABLED -> Unit
        }
        when (state.manga.unreadFilter) {
            TriState.ENABLED_IS -> add("Unread only")
            TriState.ENABLED_NOT -> add("Read only")
            TriState.DISABLED -> Unit
        }
        when (state.manga.bookmarkedFilter) {
            TriState.ENABLED_IS -> add("Bookmarked only")
            TriState.ENABLED_NOT -> add("Not bookmarked")
            TriState.DISABLED -> Unit
        }
        if (state.scanlatorFilterActive) add("Scanlator filter")
        if (dex.selected.isNotEmpty()) add(dex.selected.joinToString { it.uppercase() })
        if (chapters.size != state.processedChapters.size) add("MangaDex filters")
    }.joinToString(" · ")
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + if (selected.isEmpty()) 24.dp else 88.dp
    val nextUpdateText = nextUpdate?.let { "Next update: ${relativeDateText(it.toEpochMilli())}" }
    val menuActions = buildList {
        onCoversClicked?.takeIf { state.source.getMainSource<MetadataSource<*, *>>() is MangaDex }?.let {
            add(detailMenuAction("Covers", Icons.Outlined.Collections, onClick = it))
        }
        onPaletteClicked?.let { add(detailMenuAction("Palette", Icons.Outlined.Palette, group = 1, onClick = it)) }
        add(detailMenuAction(
            if (state.manga.source == MERGED_SOURCE_ID) "Sources and merge settings" else "Merge with another source",
            Icons.Outlined.Link,
            onClick = if (state.manga.source == MERGED_SOURCE_ID) onMergedSettingsClicked else onMergeClicked,
        ))
        if (state.showMergeWithAnother) add(detailMenuAction("Merge with another title", Icons.Outlined.Link, onClick = onMergeWithAnotherClicked))
        onDownloadActionClicked?.let { add(detailMenuAction("Download chapters", Icons.Outlined.Download, onClick = { downloads = true })) }
        add(detailMenuAction("Refresh", Icons.Outlined.Sync, onClick = onRefresh))
        add(detailMenuAction("Notes", Icons.Outlined.Notes, group = 1, onClick = onEditNotesClicked))
        if (state.manga.favorite) add(detailMenuAction("Edit title information", Icons.Outlined.Edit, group = 1, onClick = onEditInfoClicked))
        onEditCategoryClicked?.let { add(detailMenuAction("Categories", Icons.Outlined.Category, group = 1, onClick = it)) }
        onEditFetchIntervalClicked?.let { add(detailMenuAction("Update schedule", Icons.Outlined.CalendarMonth, subtitle = nextUpdateText, group = 1, onClick = it)) }
        add(detailMenuAction("Related titles", Icons.Outlined.Collections, group = 2, onClick = onRelatedMangasScreenClick))
        add(detailMenuAction("Recommendations", Icons.Outlined.StarOutline, group = 2, onClick = onRecommendClicked))
        if (state.meta != null) add(detailMenuAction("Source metadata", Icons.Outlined.Info, group = 2, onClick = onMetadataViewerClicked))
        onWebViewLongClicked?.let { add(detailMenuAction("Copy link", Icons.Outlined.ContentCopy, group = 3, onClick = it)) }
        onClickSourceSettingsClicked?.let { add(detailMenuAction("Source settings", Icons.Outlined.Settings, group = 3, onClick = it)) }
        onMigrateClicked?.let { add(detailMenuAction("Migrate title", Icons.Outlined.SwapHoriz, group = 3, onClick = it)) }
        onOpenMangaFolder?.let { add(detailMenuAction("Open download folder", Icons.Outlined.FolderOpen, group = 3, onClick = it)) }
        add(detailMenuAction("Clear title data", Icons.Outlined.DeleteOutline, group = 4, destructive = true, onClick = onClearManga))
    }
    BackHandler(selected.isNotEmpty()) { onAllChapterSelected(false) }
    Box(Modifier.fillMaxSize().background(TsuzukiTheme.colors.background)) {
        PullRefresh(
            refreshing = state.isRefreshingData, onRefresh = onRefresh, enabled = selected.isEmpty(),
            indicatorPadding = PaddingValues(top = top),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().glassSource(glass),
                contentPadding = PaddingValues(top = top, bottom = bottom),
            ) {
                item("hero") {
                    MangaInfoBox(
                        isTabletUi = isTabletUi, appBarPadding = 0.dp, manga = state.manga,
                        sourceName = state.source.getNameForMangaInfo(state.mergedData?.sources),
                        isStubSource = state.source is StubSource,
                        isSourceIncognito = state.source.isIncognitoModeEnabled(),
                        onCoverClick = onCoverClicked, doSearch = onSearch, librarySearch = librarySearch,
                        onSourceClick = onSourceClick, coverRatio = coverRatio,
                    )
                }
                item("actions") {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PillButton(
                            label = when {
                                visibleContinueChapter == null && activeFilters.isNotEmpty() -> "No matching chapters"
                                visibleContinueChapter == null -> "No unread chapters"
                                visibleContinueChapter.lastPageRead > 0 || state.chapters.any { it.chapter.read } ->
                                    continueChapterLabel(visibleContinueChapter)
                                else -> "Start reading"
                            },
                            onClick = {
                                visibleContinueChapter?.let { chapter ->
                                    val downloaded = chapters.firstOrNull { it.id == chapter.id }?.isDownloaded == true
                                    val message = if (downloaded) null else dex.accessMessage(chapter)
                                    if (message != null) context.toast(message)
                                    else if (chapter == continueChapter) onContinueReading()
                                    else onChapterClicked(chapter)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = visibleContinueChapter != null,
                        )
                        DetailGlassGroup(modifier = Modifier.fillMaxWidth()) {
                            DetailLabeledAction(
                                if (state.manga.favorite) "In Library" else "Add",
                                if (state.manga.favorite) Icons.Outlined.Check else Icons.Outlined.Add,
                                onAddToLibraryClicked,
                                Modifier.weight(1f),
                                active = state.manga.favorite,
                            )
                            DetailLabeledAction("Tracking", Icons.Outlined.Sync, onTrackingClicked, Modifier.weight(1f))
                            onWebViewClicked?.let {
                                DetailLabeledAction("Web", TsuzukiIcons.Globe, it, Modifier.weight(1f))
                            }
                            Box(Modifier.weight(1f)) {
                                DetailLabeledAction("More", Icons.Outlined.MoreHoriz, { menuAtTop = false; menu = true }, Modifier.fillMaxWidth())
                                GlassMenu(menu && !menuAtTop, state.manga.title, menuActions, { menu = false })
                            }
                        }
                    }
                }
                item("description") {
                    ExpandableMangaDescription(
                        defaultExpandState = false, description = state.manga.description,
                        tagsProvider = { state.manga.genre }, notes = state.manga.notes,
                        onTagSearch = onTagSearch, onCopyTagToClipboard = { context.copyToClipboard(it, it) },
                        onEditNotes = onEditNotesClicked, doSearch = onSearch,
                        searchMetadataChips = remember(state.meta, state.manga.genre) { SearchMetadataChips(state.meta, state.source.id, state.manga.genre) },
                    )
                }
                if (!detailsReady) return@LazyColumn
                if (state.source.getMainSource<MetadataSource<*, *>>() is MangaDex || state.manga.source == MERGED_SOURCE_ID) {
                    item("mangadex-details") { MangaDexDetailsSection(state.manga.id) }
                }
                item("chapters") {
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            TsuzukiText("Chapters", style = TsuzukiTheme.typography.title2)
                            TsuzukiText("${chapters.size} chapters" + activeFilters.takeIf { it.isNotEmpty() }?.let { " · $it" }.orEmpty(), style = TsuzukiTheme.typography.footnote, color = TsuzukiTheme.colors.secondary)
                        }
                        DetailIcon("Filter and sort chapters", Icons.Outlined.FilterList, onFilterButtonClicked)
                    }
                }
                if (chapters.isEmpty()) {
                    item("empty") {
                        Banner(
                            when {
                                state.isRefreshingData -> "Loading chapters…"
                                activeFilters.isNotEmpty() -> "No chapters match $activeFilters. Change your filters to see other chapters."
                                else -> "No chapters found. Refresh this title to check for new chapters."
                            },
                            Modifier.padding(20.dp),
                            actionLabel = if (activeFilters.isNotEmpty()) "Filters" else "Refresh",
                            onAction = if (activeFilters.isNotEmpty()) onFilterButtonClicked else onRefresh,
                        )
                    }
                }
                itemsIndexed(
                    chapterListItems,
                    key = { _, item -> when (item) { is ChapterList.Item -> "chapter-${item.id}"; is ChapterList.MissingCount -> "gap-${item.id}" } },
                    contentType = { _, item -> item is ChapterList.Item },
                ) { index, item ->
                    when (item) {
                        is ChapterList.MissingCount -> TsuzukiText("${item.count} missing chapters", Modifier.padding(horizontal = 36.dp, vertical = 12.dp), TsuzukiTheme.typography.footnote, TsuzukiTheme.colors.secondary)
                        is ChapterList.Item -> {
                            val haptic = LocalHapticFeedback.current
                            val child = state.mergedData?.manga?.get(item.chapter.mangaId)
                            val items = chapterListItems
                            val roundedTop = index == 0 || items.getOrNull(index - 1) !is ChapterList.Item
                            val roundedBottom = index == items.lastIndex || items.getOrNull(index + 1) !is ChapterList.Item
                            MangaChapterListItem(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                shape = when {
                                    roundedTop && roundedBottom -> ChapterRowShape
                                    roundedTop -> ChapterRowTopShape
                                    roundedBottom -> ChapterRowBottomShape
                                    else -> ChapterRowMiddleShape
                                },
                                title = if (state.manga.displayMode == Manga.CHAPTER_DISPLAY_NUMBER) stringResource(MR.strings.display_mode_chapter, formatChapterNumber(item.chapter.chapterNumber)) else item.chapter.name,
                                date = item.chapter.dateUpload.takeIf { it > 0 }?.let { relativeDateText(it) },
                                readProgress = item.chapter.lastPageRead.takeIf { it > 0 && (!item.chapter.read || state.alwaysShowReadingProgress) }?.let { "Page ${it + 1}" },
                                scanlator = item.chapter.scanlator,
                                unavailable = dex.unavailable(item.chapter),
                                languageLabel = dex.languageLabel(item.chapter),
                                sourceName = item.sourceName ?: child?.let { manga -> state.mergedData.sources.firstOrNull { it.id == manga.source }?.name ?: "Source ${manga.source}" },
                                read = item.chapter.read, bookmark = item.chapter.bookmark, selected = item.selected,
                                downloadIndicatorEnabled = selected.isEmpty() && !(child ?: state.manga).isLocal(),
                                downloadStateProvider = { item.downloadState }, downloadProgressProvider = { item.downloadProgress },
                                chapterSwipeStartAction = chapterSwipeStartAction, chapterSwipeEndAction = chapterSwipeEndAction,
                                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onChapterSelected(item, !item.selected, true) },
                                onClick = {
                                    if (selected.isNotEmpty()) onChapterSelected(item, !item.selected, false)
                                    else {
                                        val message = if (item.isDownloaded) null else dex.accessMessage(item.chapter)
                                        if (message != null) context.toast(message) else onChapterClicked(item.chapter)
                                    }
                                },
                                onDownloadClick = onDownloadChapter?.let { action -> { action(listOf(item), it) } },
                                onChapterSwipe = { onChapterSwipe(item, it) },
                                actions = {
                                    val source = child?.let { manga -> state.mergedData.sources.firstOrNull { it.id == manga.source } } ?: state.source
                                    if (selected.isEmpty() && source.getMainSource<MetadataSource<*, *>>() is MangaDex) {
                                        MangaDexChapterMenu(item.chapter)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
        FloatingTopBar(
            title = if (selected.isNotEmpty()) "${selected.size} selected" else state.manga.title,
            collapsed = collapsed || selected.isNotEmpty(),
            context = glass,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
            navigationIcon = {
                DetailIcon(if (selected.isEmpty()) "Back" else "Cancel selection", if (selected.isEmpty()) TsuzukiIcons.Back else Icons.Outlined.Close, { if (selected.isEmpty()) navigateUp() else onAllChapterSelected(false) })
            },
            actions = {
                if (selected.isNotEmpty()) {
                    DetailIcon("Select all", Icons.Outlined.SelectAll, { onAllChapterSelected(true) })
                    DetailIcon("Invert selection", Icons.Outlined.FlipToBack, onInvertSelection)
                } else {
                    onShareClicked?.let { DetailIcon("Share", TsuzukiIcons.Share, it) }
                    Box {
                        DetailIcon("More title actions", Icons.Outlined.MoreHoriz, { menuAtTop = true; menu = true })
                        GlassMenu(menu && menuAtTop, state.manga.title, menuActions, { menu = false })
                    }
                }
            },
        )
        MangaBottomActionMenu(
            visible = selected.isNotEmpty(), modifier = Modifier.align(Alignment.BottomCenter),
            glass = glass,
            onBookmarkClicked = { onMultiBookmarkClicked(selected.map { it.chapter }, true) }.takeIf { selected.any { !it.chapter.bookmark } },
            onRemoveBookmarkClicked = { onMultiBookmarkClicked(selected.map { it.chapter }, false) }.takeIf { selected.all { it.chapter.bookmark } },
            onMarkAsReadClicked = { onMultiMarkAsReadClicked(selected.map { it.chapter }, true) }.takeIf { selected.any { !it.chapter.read } },
            onMarkAsUnreadClicked = { onMultiMarkAsReadClicked(selected.map { it.chapter }, false) }.takeIf { selected.any { it.chapter.read || it.chapter.lastPageRead > 0 } },
            onMarkPreviousAsReadClicked = { onMarkPreviousAsReadClicked(selected.single().chapter) }.takeIf { selected.size == 1 },
            onDownloadClicked = onDownloadChapter?.let { action -> { action(selected, ChapterDownloadAction.START) } }?.takeIf { selected.any { !it.isDownloaded } },
            onDeleteClicked = { onMultiDeleteClicked(selected.map { it.chapter }) }.takeIf { selected.any { it.isDownloaded } },
        )
        GlassSnackbar(
            data = snackbarHostState.currentSnackbarData,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottom),
        )
    }
    DetentSheet(downloads, "Download chapters", { downloads = false }) {
        DownloadAction.entries.forEach { option ->
            val label = when (option) {
                DownloadAction.NEXT_1_CHAPTER -> "Next chapter"
                DownloadAction.NEXT_5_CHAPTERS -> "Next 5 chapters"
                DownloadAction.NEXT_10_CHAPTERS -> "Next 10 chapters"
                DownloadAction.NEXT_25_CHAPTERS -> "Next 25 chapters"
                DownloadAction.UNREAD_CHAPTERS -> "Unread chapters"
                DownloadAction.BOOKMARKED_CHAPTERS -> "Bookmarked chapters"
            }
            GroupedRow(label, onClick = { downloads = false; onDownloadActionClicked?.invoke(option) })
        }
    }
}

@Composable
private fun DetailLabeledAction(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier, active: Boolean = false) {
    val colors = TsuzukiTheme.colors
    val tint = Color(if (colors.isDark) 0xFF0091FF else 0xFF0088FF)
    Column(
        modifier.clip(RoundedCornerShape(50))
            .background(if (active) tint.copy(alpha = 0.22f) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = TsuzukiTheme.colors.text)
        TsuzukiText(label, style = TsuzukiTheme.typography.caption1, maxLines = 1)
    }
}

private fun detailMenuAction(
    label: String,
    icon: ImageVector,
    subtitle: String? = null,
    group: Int = 0,
    destructive: Boolean = false,
    onClick: () -> Unit,
) = ContextMenuAction(
    label = label,
    icon = { Icon(icon, null, Modifier.size(20.dp), tint = if (destructive) TsuzukiTheme.colors.destructive else TsuzukiTheme.colors.text) },
    subtitle = subtitle,
    group = group,
    destructive = destructive,
    onClick = onClick,
)

typealias MetadataDescriptionComposable = @Composable (state: MangaScreenModel.State.Success, openMetadataViewer: () -> Unit, search: (String) -> Unit) -> Unit

private val ChapterRowShape = RoundedCornerShape(TsuzukiCorners.cover)
private val ChapterRowTopShape = RoundedCornerShape(topStart = TsuzukiCorners.cover, topEnd = TsuzukiCorners.cover)
private val ChapterRowBottomShape = RoundedCornerShape(bottomStart = TsuzukiCorners.cover, bottomEnd = TsuzukiCorners.cover)
private val ChapterRowMiddleShape = RoundedCornerShape(0.dp)

@Composable
fun metadataDescription(source: Source): MetadataDescriptionComposable? {
    val metadataSource = remember(source.id) { source.getMainSource<MetadataSource<*, *>>() }
    return remember(metadataSource) {
        when (metadataSource) {
            is MangaDex -> { state, open, _ -> MangaDexDescription(state, open) }
            else -> null
        }
    }
}
