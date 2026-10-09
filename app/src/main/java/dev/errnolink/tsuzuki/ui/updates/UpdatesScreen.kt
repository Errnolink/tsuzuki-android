package dev.errnolink.tsuzuki.ui.updates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import cafe.adriel.voyager.navigator.LocalNavigator
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.ui.shell.ShellActionSheet
import dev.errnolink.tsuzuki.ui.shell.ShellSheetAction
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import eu.kanade.tachiyomi.data.LibraryUpdateStatus
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import dev.errnolink.tsuzuki.ui.shell.ShellAction
import dev.errnolink.tsuzuki.ui.shell.ShellCaption
import dev.errnolink.tsuzuki.ui.shell.ShellEmpty
import dev.errnolink.tsuzuki.ui.shell.ShellList
import dev.errnolink.tsuzuki.ui.shell.ShellLoading
import dev.errnolink.tsuzuki.ui.shell.ShellRefresh
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel.UpdateSelectionOptions
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import eu.kanade.presentation.updates.UpdatesUiModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.domain.library.service.LibraryPreferences.ChapterSwipeAction
import kotlin.time.Duration.Companion.seconds

@Composable
fun UpdateScreen(
    state: UpdatesScreenModel.State,
    snackbarHostState: SnackbarHostState,
    lastUpdated: Long,
    preserveReadingPosition: Boolean,
    onClickCover: (UpdatesItem) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    onCalendarClicked: () -> Unit,
    onUpdateLibrary: () -> Boolean,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
    onMultiBookmarkClicked: (List<UpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<UpdatesItem>, read: Boolean) -> Unit,
    onMultiDeleteClicked: (List<UpdatesItem>) -> Unit,
    updateSwipeStartAction: ChapterSwipeAction,
    updateSwipeEndAction: ChapterSwipeAction,
    onUpdateSwipe: (UpdatesItem, ChapterSwipeAction) -> Unit,
    onUpdateSelected: (UpdatesItem, UpdateSelectionOptions) -> Unit,
    onOpenChapter: (UpdatesItem) -> Unit,
    onFilterClicked: () -> Unit,
    hasActiveFilters: Boolean,
    usePanoramaCover: Boolean,
    collapseToggle: (key: String) -> Unit,
) {
    BackHandler(enabled = state.selectionMode, onBack = { onSelectAll(false) })
    val navigator = LocalNavigator.current
    val isRefreshing by remember { Injekt.get<LibraryUpdateStatus>().isRunning }.collectAsState()
    var actionsVisible by remember { mutableStateOf(false) }
    val uiModels = remember(state.items) { state.getUiModel() }
    val startRefresh: () -> Unit = { onUpdateLibrary(); Unit }
    ShellActionSheet(
        visible = actionsVisible,
        title = "Selected chapters",
        actions = listOf(
            ShellSheetAction("Mark as read") { onMultiMarkAsReadClicked(state.selected, true) },
            ShellSheetAction("Mark as unread") { onMultiMarkAsReadClicked(state.selected, false) },
            ShellSheetAction("Bookmark") { onMultiBookmarkClicked(state.selected, true) },
            ShellSheetAction("Remove bookmark") { onMultiBookmarkClicked(state.selected, false) },
            ShellSheetAction("Download") { onDownloadChapter(state.selected, ChapterDownloadAction.START) },
            ShellSheetAction("Download next") { onDownloadChapter(state.selected, ChapterDownloadAction.START_NOW) },
            ShellSheetAction("Delete downloads", destructive = true) { onMultiDeleteClicked(state.selected) },
        ),
        onDismissRequest = { actionsVisible = false },
    )
    ShellRefresh(refreshing = isRefreshing, enabled = !state.selectionMode, onRefresh = startRefresh) {
        ShellList(
            if (state.selectionMode) "${state.selected.size} selected" else "Updates",
            reselectKey = UpdatesTab.key,
            snackbar = snackbarHostState,
            navigateUp = if (navigator?.canPop == true) { { navigator.pop() } } else null,
            itemSpacing = 0.dp,
            actions = {
                if (state.selectionMode) {
                    ShellAction("Select all", Icons.Outlined.SelectAll, { onSelectAll(true) })
                    ShellAction("Invert selection", Icons.Outlined.FlipToBack, onInvertSelection)
                    ShellAction("Done", Icons.Outlined.Close, { onSelectAll(false) })
                } else {
                    ShellAction("Filter updates", Icons.Outlined.FilterList, onFilterClicked, hasActiveFilters)
                    ShellAction("Upcoming chapters", Icons.Outlined.CalendarMonth, onCalendarClicked)
                    ShellAction("Update library", Icons.Outlined.Refresh, startRefresh)
                }
            },
        ) {
            if (state.selectionMode) {
                item("updates-selection-actions") {
                    PillButton("Actions", { actionsVisible = true }, Modifier.padding(horizontal = 20.dp))
                }
            }
            when {
                state.isLoading -> item(key = "updates-loading") { ShellLoading("Checking for new chapters…") }
                state.items.isEmpty() -> item(key = "updates-empty") {
                    ShellEmpty(
                        "No chapter updates",
                        "New chapters from your library appear here. Pull down or tap the refresh action to check now.",
                    )
                }
                else -> {
                    updatesLastUpdatedItem(lastUpdated)
                    updatesUiItems(
                        uiModels = uiModels,
                        expandedState = state.expandedState,
                        collapseToggle = collapseToggle,
                        usePanoramaCover = usePanoramaCover,
                        selectionMode = state.selectionMode,
                        preserveReadingPosition = preserveReadingPosition,
                        onUpdateSelected = onUpdateSelected,
                        onClickCover = onClickCover,
                        onClickUpdate = onOpenChapter,
                        onDownloadChapter = onDownloadChapter,
                        updateSwipeStartAction = updateSwipeStartAction,
                        updateSwipeEndAction = updateSwipeEndAction,
                        onUpdateSwipe = onUpdateSwipe,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.updatesLastUpdatedItem(lastUpdated: Long) {
    if (lastUpdated <= 0L) return
    item(key = "updates-last-updated") {
        ShellCaption(
            "Updated ${eu.kanade.presentation.util.relativeTimeSpanString(lastUpdated)}",
            Modifier.padding(horizontal = 20.dp),
        )
    }
}