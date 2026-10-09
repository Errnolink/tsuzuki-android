package dev.errnolink.tsuzuki.ui.shell

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.designsystem.SearchField
import dev.errnolink.tsuzuki.designsystem.SegmentedControl
import eu.kanade.presentation.components.AppBar
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.ui.browse.BulkFavoriteScreenModel
import eu.kanade.tachiyomi.ui.browse.feed.FeedScreenModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TabbedScreen(
    titleRes: StringResource,
    tabs: ImmutableList<TabContent>,
    state: PagerState = rememberPagerState { tabs.size },
    searchQuery: String? = null,
    onChangeSearchQuery: (String?) -> Unit = {},
    feedScreenModel: FeedScreenModel,
    bulkFavoriteScreenModel: BulkFavoriteScreenModel,
    onGlobalSearch: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val feedState by feedScreenModel.state.collectAsState()
    val selection by bulkFavoriteScreenModel.state.collectAsState()
    var globalQuery by rememberSaveable { mutableStateOf("") }
    val tab = tabs[state.currentPage.coerceIn(tabs.indices)]
    ShellContentScaffold(
        title = if (selection.selectionMode) "${selection.selection.size} selected" else stringResource(titleRes),
        snackbar = snackbar,
        actions = {
            if (selection.selectionMode) {
                ShellAction("Select all", Icons.Outlined.SelectAll, {
                    feedState.items.orEmpty().flatMap { it.results.orEmpty() }.forEach(bulkFavoriteScreenModel::select)
                })
                ShellAction("Invert selection", Icons.Outlined.FlipToBack, {
                    bulkFavoriteScreenModel.reverseSelection(feedState.items.orEmpty().flatMap { it.results.orEmpty() })
                })
                ShellAction("Done", Icons.Outlined.Close, bulkFavoriteScreenModel::toggleSelectionMode)
            } else {
                ShellToolbarActions(tab.actions)
            }
        },
        header = {
            if (selection.selectionMode) {
                PillButton("Add to library", bulkFavoriteScreenModel::addFavorite, Modifier.padding(horizontal = 20.dp))
            } else {
                SearchField(
                    value = if (tab.searchEnabled) searchQuery.orEmpty() else globalQuery,
                    onValueChange = { if (tab.searchEnabled) onChangeSearchQuery(it) else globalQuery = it },
                    onSearch = { if (!tab.searchEnabled && globalQuery.isNotBlank()) onGlobalSearch(globalQuery) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    placeholder = if (tab.searchEnabled) "Search extensions" else "Title or MangaDex link",
                )
            }
            SegmentedControl(
                options = tabs.map { stringResource(it.titleRes) + (it.badgeNumber?.takeIf { count -> count > 0 }?.let { count -> " ($count)" } ?: "") },
                selectedIndex = state.currentPage,
                onSelect = { scope.launch { state.scrollToPage(it) } },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        },
    ) { padding ->
        HorizontalPager(state = state, modifier = Modifier.fillMaxSize()) { page ->
            tabs[page].content(padding, snackbar)
        }
    }
}

data class TabContent(
    val titleRes: StringResource,
    val badgeNumber: Int? = null,
    val searchEnabled: Boolean = false,
    val actions: ImmutableList<AppBar.AppBarAction> = persistentListOf(),
    val content: @Composable (contentPadding: PaddingValues, snackbarHostState: SnackbarHostState) -> Unit,
)
