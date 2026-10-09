package dev.errnolink.tsuzuki.ui.library

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.errnolink.tsuzuki.designsystem.ContextMenu
import dev.errnolink.tsuzuki.designsystem.ContextMenuAction
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.designsystem.SearchField
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.ui.shell.LocalShellBottomPadding
import dev.errnolink.tsuzuki.ui.shell.ShellAction
import dev.errnolink.tsuzuki.ui.shell.ShellCaption
import dev.errnolink.tsuzuki.ui.shell.ShellEmpty
import dev.errnolink.tsuzuki.ui.shell.ShellLoading
import dev.errnolink.tsuzuki.ui.shell.ShellMangaTile
import dev.errnolink.tsuzuki.ui.shell.ShellMasonry
import dev.errnolink.tsuzuki.ui.shell.ShellCover
import dev.errnolink.tsuzuki.ui.shell.ShellRefresh
import dev.errnolink.tsuzuki.ui.shell.ShellSnackbar
import dev.errnolink.tsuzuki.ui.shell.ShellSheetAction
import dev.errnolink.tsuzuki.ui.settings.ChoiceSheet
import eu.kanade.tachiyomi.data.LibraryUpdateStatus
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel
import eu.kanade.tachiyomi.ui.library.LibraryTab
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.model.HistoryWithRelations
import eu.kanade.tachiyomi.ui.library.LibraryItem
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun TsuzukiLibraryScreen(
    state: LibraryScreenModel.State,
    screenModel: LibraryScreenModel,
    snackbar: SnackbarHostState,
    onRefresh: () -> Unit,
    onOpen: (Long) -> Unit,
    onResume: (Long, Long) -> Unit,
    onBrowse: () -> Unit,
    selectionActions: @Composable () -> Unit,
) {
    LifecycleResumeEffect(screenModel) {
        screenModel.setLibraryVisible(true)
        onPauseOrDispose { screenModel.setLibraryVisible(false) }
    }
    val historyFlow = remember { Injekt.get<GetHistory>().subscribe("", null, true, false) }
    val history by historyFlow.collectAsStateWithLifecycle(emptyList(), minActiveState = Lifecycle.State.RESUMED)
    val recent = remember(history) { history.asSequence().distinctBy { it.mangaId }.take(12).toList() }
    val refreshing by remember { Injekt.get<LibraryUpdateStatus>().isRunning }.collectAsState()
    val preferences = remember { Injekt.get<LibraryPreferences>() }
    val displayMode by preferences.displayMode().collectAsState()
    val orientation = LocalConfiguration.current.orientation
    val columnPreference = remember(orientation) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) preferences.landscapeColumns() else preferences.portraitColumns()
    }
    val columnCount by columnPreference.collectAsState()
    val grid = rememberLazyStaggeredGridState()
    val category = state.activeCategory
    val libraryItems = remember(state.libraryData, state.groupedFavorites, category) {
        category?.let(state::getItemsForCategory).orEmpty()
    }
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var categoriesVisible by rememberSaveable { mutableStateOf(false) }
    if (categoriesVisible) {
        ChoiceSheet(
            visible = true,
            title = "Library categories",
            entries = state.displayedCategories.mapIndexed { index, item ->
                index to item.name.ifBlank { "Default" }
            },
            selected = state.coercedActiveCategoryIndex,
            onDismissRequest = { categoriesVisible = false },
            onSelect = screenModel::updateActiveCategoryIndex,
        )
    }
    Box(Modifier.fillMaxSize()) {
        ShellRefresh(refreshing, !state.selectionMode, onRefresh) {
            ShellMasonry(
                if (state.selectionMode) "${state.selection.size} selected" else "Library",
                grid,
                LibraryTab.key,
                actions = {
                    if (state.selectionMode) {
                        ShellAction("Select all", Icons.Outlined.SelectAll, screenModel::selectAll)
                        ShellAction("Invert selection", Icons.Outlined.FlipToBack, screenModel::invertSelection)
                        ShellAction("Done", Icons.Outlined.Close, screenModel::clearSelection)
                    } else {
                        ShellAction("Search library", Icons.Outlined.Search, { searchVisible = !searchVisible })
                        ShellAction("Filter and sort", Icons.Outlined.Tune, screenModel::showSettingsDialog, state.hasActiveFilters)
                    }
                },
                columns = when {
                    displayMode == LibraryDisplayMode.List -> StaggeredGridCells.Fixed(1)
                    columnCount > 0 -> StaggeredGridCells.Fixed(columnCount)
                    else -> StaggeredGridCells.Adaptive(140.dp)
                },
            ) {
                if (searchVisible || !state.searchQuery.isNullOrEmpty()) {
                    item(key = "library-search", span = StaggeredGridItemSpan.FullLine) {
                        SearchField(
                            state.searchQuery.orEmpty(),
                            screenModel::search,
                            {},
                            Modifier,
                            placeholder = "Search your library",
                        )
                    }
                }
                if (state.displayedCategories.size > 1) {
                    item(key = "library-categories", span = StaggeredGridItemSpan.FullLine) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PillButton(
                                "${category?.name?.ifBlank { "Default" } ?: "Library"} ▾",
                                { categoriesVisible = true },
                                prominent = false,
                            )
                            ShellCaption("${libraryItems.size} titles", Modifier.padding(start = 12.dp))
                        }
                    }
                }
                if (!state.selectionMode && state.searchQuery.isNullOrEmpty() && recent.isNotEmpty()) {
                    item(key = "library-continue", span = StaggeredGridItemSpan.FullLine) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            TsuzukiText("Continue reading", style = TsuzukiTheme.typography.title2)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(recent, key = { it.chapterId }, contentType = { "continue" }) { entry ->
                                    ContinueItem(entry) { onResume(entry.mangaId, entry.chapterId) }
                                }
                            }
                        }
                    }
                }
                if (state.selectionMode) {
                    item(key = "library-selection", span = StaggeredGridItemSpan.FullLine) { selectionActions() }
                }
                when {
                    state.isLoading -> item(key = "library-loading", span = StaggeredGridItemSpan.FullLine) {
                        ShellLoading("Loading your library…")
                    }
                    state.isLibraryEmpty -> item(key = "library-empty", span = StaggeredGridItemSpan.FullLine) {
                        ShellEmpty("Your next story starts here", "Add manga from Browse to build your library.") {
                            PillButton("Browse manga", onBrowse)
                        }
                    }
                    libraryItems.isEmpty() -> item(key = "library-no-results", span = StaggeredGridItemSpan.FullLine) {
                        ShellEmpty("No matching manga", "Try another category, search, or filter.")
                    }
                    else -> libraryGrid(libraryItems, state, category, screenModel, displayMode, onOpen)
                }
            }
        }
        ShellSnackbar(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = LocalShellBottomPadding.current + 12.dp))
    }
}

private fun LazyStaggeredGridScope.libraryGrid(
    libraryItems: List<LibraryItem>,
    state: LibraryScreenModel.State,
    category: Category?,
    screenModel: LibraryScreenModel,
    displayMode: LibraryDisplayMode,
    onOpen: (Long) -> Unit,
) {
    items(libraryItems, key = { it.id }, contentType = { displayMode }) { item ->
        val manga = item.libraryManga
        val selectOnly = {
            screenModel.clearSelection()
            if (category != null) screenModel.toggleSelection(category, manga)
        }
        ContextMenu(
            manga.manga.title,
            listOf(
                ContextMenuAction("Select", icon = { Icon(Icons.Outlined.SelectAll, null, tint = TsuzukiTheme.colors.text) }) { if (category != null) screenModel.toggleRangeSelection(category, manga) },
                ContextMenuAction("Mark as read", icon = { Icon(Icons.Outlined.Check, null, tint = TsuzukiTheme.colors.text) }) { selectOnly(); screenModel.markReadSelection(true) },
                ContextMenuAction("Mark as unread", icon = { Icon(Icons.Outlined.RemoveDone, null, tint = TsuzukiTheme.colors.text) }) { selectOnly(); screenModel.markReadSelection(false) },
                ContextMenuAction("Change categories", icon = { Icon(Icons.Outlined.FolderOpen, null, tint = TsuzukiTheme.colors.text) }, group = 1) { selectOnly(); screenModel.openChangeCategoryDialog() },
                ContextMenuAction("Remove from library", icon = { Icon(Icons.Outlined.DeleteOutline, null, tint = TsuzukiTheme.colors.destructive) }, group = 2, destructive = true) { selectOnly(); screenModel.openDeleteMangaDialog() },
            ),
            onClick = {
                if (state.selectionMode && category != null) {
                    screenModel.toggleSelection(category, manga)
                } else {
                    onOpen(manga.id)
                }
            },
        ) {
            val selected = item.id in state.selection
            when (displayMode) {
                LibraryDisplayMode.List -> Row(
                    Modifier.background(
                        if (selected) TsuzukiTheme.colors.selectedFill else TsuzukiTheme.colors.grouped,
                        RoundedCornerShape(14.dp),
                    ).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShellCover(manga.manga.asMangaCover(), manga.manga.title, Modifier.width(48.dp), fixedRatio = 2f / 3f)
                    Column(Modifier.weight(1f)) {
                        TsuzukiText(manga.manga.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        ShellCaption("${manga.unreadCount} unread")
                    }
                }
                LibraryDisplayMode.CoverOnlyGrid -> Box(
                    Modifier.background(
                        if (selected) TsuzukiTheme.colors.selectedFill else androidx.compose.ui.graphics.Color.Transparent,
                        RoundedCornerShape(14.dp),
                    ).padding(if (selected) 4.dp else 0.dp),
                ) {
                    ShellCover(manga.manga.asMangaCover(), manga.manga.title)
                }
                else -> ShellMangaTile(manga.manga, selected, item.unreadCount, item.downloadCount)
            }
        }
    }
}

@Composable
private fun ContinueItem(entry: HistoryWithRelations, onClick: () -> Unit) {
    Row(
        Modifier.width(320.dp)
            .background(TsuzukiTheme.colors.grouped, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShellCover(entry.coverData, entry.title, Modifier.width(48.dp), fixedRatio = 2f / 3f)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            TsuzukiText(
                entry.title,
                style = TsuzukiTheme.typography.subhead,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TsuzukiText(
                "Ch. ${entry.chapterNumber.toString().removeSuffix(".0")} · Page ${entry.lastPageRead + 1}",
                style = TsuzukiTheme.typography.caption1,
                color = TsuzukiTheme.colors.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PillButton("Resume", onClick, prominent = false)
    }
}

@Composable
fun ShellLibraryActions(
    visible: Boolean,
    onChangeCategoryClicked: () -> Unit,
    onMarkAsReadClicked: () -> Unit,
    onMarkAsUnreadClicked: () -> Unit,
    onDownloadClicked: ((eu.kanade.presentation.manga.DownloadAction) -> Unit)?,
    onDeleteClicked: () -> Unit,
    onMigrateClicked: () -> Unit,
    onMergeClicked: () -> Unit,
    onSelectionUpdateClicked: () -> Unit,
    onClickCollectRecommendations: (() -> Unit)?,
    onClickAddToMangaDex: (() -> Unit)?,
    onClickResetInfo: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    var expanded by remember { mutableStateOf(false) }
    Row(modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PillButton("Actions", { expanded = true })
        PillButton("Categories", onChangeCategoryClicked, prominent = false)
    }
    val actions = buildList {
        add(ShellSheetAction("Change categories", onClick = onChangeCategoryClicked))
        add(ShellSheetAction("Mark as read", onClick = onMarkAsReadClicked))
        add(ShellSheetAction("Mark as unread", onClick = onMarkAsUnreadClicked))
        onDownloadClicked?.let {
            eu.kanade.presentation.manga.DownloadAction.entries.forEach { choice ->
                add(
                    ShellSheetAction(
                        choice.name.lowercase().replace('_', ' ').replaceFirstChar { c -> c.uppercaseChar() },
                        onClick = { it(choice) },
                    ),
                )
            }
        }
        add(ShellSheetAction("Update selected", onClick = onSelectionUpdateClicked))
        add(ShellSheetAction("Migrate", onClick = onMigrateClicked))
        add(ShellSheetAction("Merge", onClick = onMergeClicked))
        onClickCollectRecommendations?.let { add(ShellSheetAction("Collect recommendations", onClick = it)) }
        onClickAddToMangaDex?.let { add(ShellSheetAction("Add to MangaDex", onClick = it)) }
        onClickResetInfo?.let { add(ShellSheetAction("Reset information", onClick = it)) }
        add(ShellSheetAction("Remove from library", destructive = true, onClick = onDeleteClicked))
    }
    dev.errnolink.tsuzuki.ui.shell.ShellActionSheet(
        visible = expanded,
        title = "Selected manga",
        actions = actions,
        onDismissRequest = { expanded = false },
    )
}
