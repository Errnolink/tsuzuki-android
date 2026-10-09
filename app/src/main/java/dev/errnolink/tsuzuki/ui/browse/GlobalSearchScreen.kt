package dev.errnolink.tsuzuki.ui.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.designsystem.SearchField
import dev.errnolink.tsuzuki.designsystem.SegmentedControl
import dev.errnolink.tsuzuki.ui.shell.ShellAction
import dev.errnolink.tsuzuki.ui.shell.ShellCaption
import dev.errnolink.tsuzuki.ui.shell.ShellContentScaffold
import eu.kanade.domain.source.model.installedExtension
import eu.kanade.presentation.browse.components.GlobalSearchToolbar
import eu.kanade.presentation.components.BulkSelectionToolbar
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.browse.BulkFavoriteScreenModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchScreenModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.collections.immutable.ImmutableMap
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.domain.source.model.Source as DomainSource

@Composable
fun GlobalSearchScreen(
    state: SearchScreenModel.State,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onToggleResults: () -> Unit,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (Source) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
    // KMK -->
    bulkFavoriteScreenModel: BulkFavoriteScreenModel,
    hasPinnedSources: Boolean,
    // KMK <--
) {
    // KMK -->
    val bulkFavoriteState by bulkFavoriteScreenModel.state.collectAsState()
    // KMK <--

    ShellContentScaffold(
        title = if (bulkFavoriteState.selectionMode) "${bulkFavoriteState.selection.size} selected" else "Search",
        navigateUp = navigateUp,
        actions = {
            if (bulkFavoriteState.selectionMode) {
                ShellAction("Select all", Icons.Outlined.SelectAll, {
                    state.filteredItems.values.filterIsInstance<SearchItemResult.Success>()
                        .flatMap { it.result }.forEach(bulkFavoriteScreenModel::select)
                })
                ShellAction("Invert selection", Icons.Outlined.FlipToBack, {
                    bulkFavoriteScreenModel.reverseSelection(
                        state.filteredItems.values.filterIsInstance<SearchItemResult.Success>().flatMap { it.result },
                    )
                })
                ShellAction("Done", Icons.Outlined.Close, bulkFavoriteScreenModel::toggleSelectionMode)
            } else {
                ShellAction("Select manga", Icons.Outlined.Checklist, bulkFavoriteScreenModel::toggleSelectionMode)
            }
        },
        header = {
            if (bulkFavoriteState.selectionMode) {
                PillButton("Add to library", bulkFavoriteScreenModel::addFavorite, Modifier.padding(horizontal = 20.dp))
            } else {
                SearchField(
                    state.searchQuery.orEmpty(),
                    onChangeSearchQuery,
                    { onSearch(state.searchQuery.orEmpty()) },
                    Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    placeholder = "Title or MangaDex link",
                )
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    if (hasPinnedSources) {
                        SegmentedControl(
                            options = listOf("All sources", "Pinned"),
                            selectedIndex = if (state.sourceFilter == SourceFilter.All) 0 else 1,
                            onSelect = { onChangeSearchFilter(if (it == 0) SourceFilter.All else SourceFilter.PinnedOnly) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    PillButton("Has results", onToggleResults, prominent = state.onlyShowHasResults)
                }
                if (state.progress < state.total) ShellCaption(
                    "Searching ${state.progress} of ${state.total} sources",
                    Modifier.padding(horizontal = 20.dp),
                )
            }
        },
    ) { paddingValues ->
        GlobalSearchContent(
            items = state.filteredItems,
            contentPadding = paddingValues,
            getManga = getManga,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
            // KMK -->
            selection = bulkFavoriteState.selection,
            // KMK <--
        )
    }
}

@Composable
internal fun GlobalSearchContent(
    items: ImmutableMap<Source, SearchItemResult>,
    contentPadding: PaddingValues,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (Source) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
    fromSourceId: Long? = null,
    // KMK -->
    selection: List<Manga>,
    // KMK <--
) {
    LazyColumn(
        contentPadding = contentPadding,
    ) {
        items.forEach { (source, result) ->
            item(key = "global-search-${source.id}") {
                // KMK -->
                val domainSource = DomainSource(
                    source.id,
                    "",
                    "",
                    supportsLatest = false,
                    isStub = false,
                )
                // KMK <--

                GlobalSearchResultItem(
                    title = (
                        fromSourceId?.let {
                            "▶ ${source.name}".takeIf { source.id == fromSourceId }
                        } ?: source.name
                        ) +
                        // KMK -->
                        (
                            domainSource.installedExtension?.let { extension ->
                                " (${extension.name})".takeIf { extension.name != source.name }
                            } ?: ""
                            ),
                    // KMK <--
                    subtitle = LocaleHelper.getLocalizedDisplayName(source.lang),
                    onClick = { onClickSource(source) },
                ) {
                    when (result) {
                        SearchItemResult.Loading -> {
                            GlobalSearchLoadingResultItem()
                        }
                        is SearchItemResult.Success -> {
                            GlobalSearchCardRow(
                                titles = result.result,
                                getManga = getManga,
                                onClick = onClickItem,
                                onLongClick = onLongClickItem,
                                // KMK -->
                                selection = selection,
                                // KMK <--
                            )
                        }
                        is SearchItemResult.Error -> {
                            GlobalSearchErrorResultItem(message = result.throwable.message)
                        }
                    }
                }
            }
        }
    }
}
