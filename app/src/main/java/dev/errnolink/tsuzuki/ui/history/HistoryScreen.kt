package dev.errnolink.tsuzuki.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.InsetGroupedList
import dev.errnolink.tsuzuki.designsystem.SearchField
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.ui.shell.ShellAction
import dev.errnolink.tsuzuki.ui.shell.ShellEmpty
import dev.errnolink.tsuzuki.ui.shell.ShellList
import dev.errnolink.tsuzuki.ui.shell.ShellLoading
import eu.kanade.presentation.components.relativeDateText
import dev.errnolink.tsuzuki.ui.history.HistoryItem
import eu.kanade.tachiyomi.ui.history.HistoryScreenModel
import eu.kanade.tachiyomi.ui.history.HistoryScreenModel.HistorySelectionOptions
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.presentation.history.HistoryUiModel
import tachiyomi.domain.history.model.HistoryWithRelations
import java.time.LocalDate

@Composable
fun HistoryScreen(
    state: HistoryScreenModel.State,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChange: (String?) -> Unit,
    onClickCover: (Long) -> Unit,
    onClickResume: (Long, Long) -> Unit,
    onClickFavorite: (Long) -> Unit,
    onDialogChange: (HistoryScreenModel.Dialog?) -> Unit,
    toggleSelectionMode: () -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    onHistorySelected: (HistoryWithRelations, HistorySelectionOptions) -> Unit,
    onFilterClicked: () -> Unit,
    hasActiveFilters: Boolean,
    usePanoramaCover: Boolean,
) {
    BackHandler(state.selectionMode, toggleSelectionMode)
    val navigator = LocalNavigator.current
    var searching by remember { mutableStateOf(false) }
    val models = remember(state.list) { state.getUiModel() }
    ShellList(
        if (state.selectionMode) "${state.selection.size} selected" else "History",
        reselectKey = HistoryTab.key,
        snackbar = snackbarHostState,
        navigateUp = if (navigator?.canPop == true) { { navigator.pop() } } else null,
        itemSpacing = 0.dp,
        actions = {
            if (state.selectionMode) {
                ShellAction("Select all", Icons.Outlined.SelectAll, { onSelectAll(true) })
                ShellAction("Invert selection", Icons.Outlined.FlipToBack, onInvertSelection)
                ShellAction(
                    "Remove selected",
                    Icons.Outlined.Delete,
                    { onDialogChange(HistoryScreenModel.Dialog.Delete(state.selected)) },
                )
                ShellAction("Done", Icons.Outlined.Close, toggleSelectionMode)
            } else {
                ShellAction("Search history", Icons.Outlined.Search, { searching = !searching })
                ShellAction("Filter history", Icons.Outlined.FilterList, onFilterClicked, hasActiveFilters)
                ShellAction("Select history", Icons.Outlined.Checklist, toggleSelectionMode)
                ShellAction("Clear history", Icons.Outlined.Delete, { onDialogChange(HistoryScreenModel.Dialog.DeleteAll) })
            }
        },
    ) {
        if (searching || state.searchQuery != null) {
            item(key = "history-search") {
                SearchField(
                    state.searchQuery.orEmpty(),
                    onSearchQueryChange,
                    {},
                    Modifier.padding(horizontal = TsuzukiTheme.spacing.gutter),
                    placeholder = "Search history",
                )
            }
        }
        when {
            state.isLoading -> item(key = "history-loading") { ShellLoading("Loading reading history…") }
            state.list.isEmpty() -> item(key = "history-empty") {
                ShellEmpty(
                    "Nothing here yet",
                    if (state.searchQuery.isNullOrEmpty()) {
                        "The stories you read will appear here."
                    } else {
                        "No history matches your search."
                    },
                )
            }
            else -> itemsIndexed(
                models,
                key = { _, it ->
                    when (it) {
                        is HistoryUiModel.Header -> "history-date-${it.date}"
                        is HistoryUiModel.Item -> "history-chapter-${it.item.chapterId}"
                    }
                },
                contentType = { _, it -> if (it is HistoryUiModel.Header) "header" else "entry" },
            ) { index, model ->
                when (model) {
                    is HistoryUiModel.Header -> TsuzukiText(
                        relativeDateText(model.date),
                        Modifier.padding(start = 32.dp, top = 20.dp, bottom = 8.dp),
                        TsuzukiTheme.typography.footnote,
                        TsuzukiTheme.colors.secondary,
                    )
                    is HistoryUiModel.Item -> {
                        val entry = model.item
                        val selected = entry.chapterId in state.selection
                            HistoryItem(
                                history = entry,
                                modifier = Modifier.padding(horizontal = 20.dp),
                                shape = RoundedCornerShape(
                                    topStart = if (models.getOrNull(index - 1) !is HistoryUiModel.Item) 14.dp else 0.dp,
                                    topEnd = if (models.getOrNull(index - 1) !is HistoryUiModel.Item) 14.dp else 0.dp,
                                    bottomStart = if (models.getOrNull(index + 1) !is HistoryUiModel.Item) 14.dp else 0.dp,
                                    bottomEnd = if (models.getOrNull(index + 1) !is HistoryUiModel.Item) 14.dp else 0.dp,
                                ),
                                divider = models.getOrNull(index + 1) is HistoryUiModel.Item,
                                onClickCover = { onClickCover(entry.mangaId) },
                                onClick = {
                                    if (state.selectionMode) {
                                        onHistorySelected(entry, HistorySelectionOptions(!selected, false))
                                    } else {
                                        onClickResume(entry.mangaId, entry.chapterId)
                                    }
                                },
                                onLongClick = { onHistorySelected(entry, HistorySelectionOptions(!selected, true)) },
                                onClickDelete = { onDialogChange(HistoryScreenModel.Dialog.Delete(entry)) },
                                onClickFavorite = { onClickFavorite(entry.mangaId) },
                                selected = selected,
                                readProgress = if (!entry.read) "Page ${entry.lastPageRead + 1}" else null,
                                hasUnread = entry.unreadCount > 0,
                                usePanoramaCover = usePanoramaCover,
                            )
                    }
                }
            }
        }
    }
}