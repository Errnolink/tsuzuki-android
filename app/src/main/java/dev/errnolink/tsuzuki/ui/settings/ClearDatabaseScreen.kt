package dev.errnolink.tsuzuki.ui.settings

import dev.errnolink.tsuzuki.ui.shell.ShellAction
import dev.errnolink.tsuzuki.ui.shell.ShellContentScaffold
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.MaterialTheme
import dev.errnolink.tsuzuki.designsystem.TsuzukiText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchUI
import tachiyomi.core.common.util.lang.toLong
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.data.Database
import tachiyomi.domain.source.interactor.GetSourcesWithNonLibraryManga
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.model.SourceWithCount
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LazyColumnWithAction
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.selectedBackground
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TsuzukiClearDatabaseScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel { ClearDatabaseScreenModel() }
        val state by model.state.collectAsState()
        val scope = rememberCoroutineScope()

        when (val s = state) {
            is ClearDatabaseScreenModel.State.Loading -> LoadingScreen()
            is ClearDatabaseScreenModel.State.Ready -> {
                if (s.showConfirmation) {
                    var keepReadManga by remember { mutableStateOf(true) }
                    SettingsAlert(title = {
                        Text(text = stringResource(MR.strings.are_you_sure))
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                        ) {
                            Text(text = stringResource(MR.strings.clear_database_text))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(MR.strings.clear_db_exclude_read),
                                    modifier = Modifier.weight(1f),
                                )
                                SettingsSwitch(checked = keepReadManga,
                                onCheckedChange = { keepReadManga = it },)
                            }
                            if (!keepReadManga) {
                                Text(
                                    text = stringResource(MR.strings.clear_database_history_warning),
                                    style = TsuzukiTheme.typography.footnote,
                                    color = TsuzukiTheme.colors.destructive,
                                )
                            }
                        }
                    },
                    onDismissRequest = model::hideConfirmation,
                    confirmButton = {
                        SettingsActionButton(onClick = {
                            scope.launchUI {
                                model.removeMangaBySourceId(keepReadManga)
                                model.clearSelection()
                                model.hideConfirmation()
                                context.toast(MR.strings.clear_database_completed)
                            }
                        },) { Text(text = stringResource(MR.strings.action_ok)) }
                    },
                    dismissButton = {
                        SettingsActionButton(onClick = model::hideConfirmation) { Text(text = stringResource(MR.strings.action_cancel)) }
                    },)
                }

                ShellContentScaffold(
                    title = stringResource(MR.strings.pref_clear_database),
                    navigateUp = navigator::pop,
                    actions = {
                        if (s.items.isNotEmpty()) {
                            ShellAction("Select all", Icons.Outlined.SelectAll, model::selectAll)
                            ShellAction("Invert selection", Icons.Outlined.FlipToBack, model::invertSelection)
                        }
                    },
                ) { contentPadding ->
                    if (s.items.isEmpty()) {
                        EmptyScreen(
                            message = stringResource(MR.strings.database_clean),
                            modifier = Modifier.padding(contentPadding),
                        )
                    } else {
                        SettingsListWithAction(
                            contentPadding = contentPadding,
                            actionLabel = stringResource(MR.strings.action_delete),
                            actionEnabled = s.selection.isNotEmpty(),
                            onClickAction = model::showConfirmation,
                        ) {
                            items(s.items) { sourceWithCount ->
                                ClearDatabaseItem(
                                    source = sourceWithCount.source,
                                    count = sourceWithCount.count,
                                    isSelected = s.selection.contains(sourceWithCount.id),
                                    onClickSelect = { model.toggleSelection(sourceWithCount.source) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ClearDatabaseItem(
        source: Source,
        count: Long,
        isSelected: Boolean,
        onClickSelect: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .selectedBackground(isSelected)
                .clickable(onClick = onClickSelect)
                .padding(horizontal = 8.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceIcon(source = source)
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
            ) {
                Text(
                    text = source.visualName,
                    style = TsuzukiTheme.typography.callout,
                )
                Text(text = stringResource(MR.strings.clear_database_source_item_count, count))
            }
            SettingsCheckmark(checked = isSelected,
            onCheckedChange = { onClickSelect() },)
        }
    }
}

private class ClearDatabaseScreenModel : StateScreenModel<ClearDatabaseScreenModel.State>(State.Loading) {
    private val getSourcesWithNonLibraryManga: GetSourcesWithNonLibraryManga = Injekt.get()
    private val database: Database = Injekt.get()

    init {
        screenModelScope.launchIO {
            getSourcesWithNonLibraryManga.subscribe()
                .collectLatest { list ->
                    mutableState.update { old ->
                        val items = list.sortedBy { it.name }
                        when (old) {
                            State.Loading -> State.Ready(items)
                            is State.Ready -> old.copy(items = items)
                        }
                    }
                }
        }
    }

    suspend fun removeMangaBySourceId(keepReadManga: Boolean) = withNonCancellableContext {
        val state = state.value as? State.Ready ?: return@withNonCancellableContext
        database.mangasQueries.deleteNonLibraryManga(state.selection, keepReadManga.toLong())
        database.historyQueries.removeResettedHistory()
    }

    fun toggleSelection(source: Source) = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        val mutableList = state.selection.toMutableList()
        if (mutableList.contains(source.id)) {
            mutableList.remove(source.id)
        } else {
            mutableList.add(source.id)
        }
        state.copy(selection = mutableList)
    }

    fun clearSelection() = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(selection = emptyList())
    }

    fun selectAll() = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(selection = state.items.fastMap { it.id })
    }

    fun invertSelection() = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(
            selection = state.items
                .fastMap { it.id }
                .filterNot { it in state.selection },
        )
    }

    fun showConfirmation() = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(showConfirmation = true)
    }

    fun hideConfirmation() = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(showConfirmation = false)
    }

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data class Ready(
            val items: List<SourceWithCount>,
            val selection: List<Long> = emptyList(),
            val showConfirmation: Boolean = false,
        ) : State
    }
}
