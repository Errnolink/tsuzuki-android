package dev.errnolink.tsuzuki.ui.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.errnolink.tsuzuki.designsystem.DetentSheet
import dev.errnolink.tsuzuki.designsystem.GroupedRow
import dev.errnolink.tsuzuki.designsystem.IconButton
import dev.errnolink.tsuzuki.designsystem.InsetGroupedList
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.designsystem.SearchField
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import eu.kanade.domain.source.model.installedExtension
import eu.kanade.presentation.browse.components.SourceIcon
import dev.errnolink.tsuzuki.ui.shell.ShellEmpty
import dev.errnolink.tsuzuki.ui.shell.ShellLoading
import dev.errnolink.tsuzuki.mangadex.MangaDexDestination
import dev.errnolink.tsuzuki.mangadex.openMangaDexDestination
import eu.kanade.tachiyomi.ui.browse.source.SourcesScreenModel
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Listing
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.isLocal
import eu.kanade.presentation.browse.SourceUiModel

@Composable
fun SourcesScreen(
    state: SourcesScreenModel.State,
    contentPadding: PaddingValues,
    onClickItem: (Source, Listing) -> Unit,
    onClickPin: (Source) -> Unit,
    onLongClickItem: (Source) -> Unit,
    modifier: Modifier = Modifier,
    onChangeSearchQuery: (String?) -> Unit,
    showMangaDexDestinations: Boolean = false,
) {
    val navigator = LocalNavigator.currentOrThrow
    BackHandler(enabled = !state.searchQuery.isNullOrBlank()) { onChangeSearchQuery("") }
    LazyColumn(modifier = modifier, contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        if (showMangaDexDestinations) {
            item("mangadex-destinations") {
                InsetGroupedList(title = "MangaDex") {
                    GroupedRow("MangaDex home", chevron = true, onClick = { openMangaDexDestination(navigator, MangaDexDestination.Home) })
                    GroupedRow("MangaDex feed", chevron = true, onClick = { openMangaDexDestination(navigator, MangaDexDestination.Feed) })
                    GroupedRow("My MangaDex lists", chevron = true, onClick = { openMangaDexDestination(navigator, MangaDexDestination.Lists) }, divider = false)
                }
            }
        }
        item("source-search") {
            SearchField(
                state.searchQuery.orEmpty(),
                onChangeSearchQuery,
                {},
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                placeholder = stringResource(KMR.strings.action_search_for_source),
            )
        }
        when {
            state.isLoading -> item("sources-loading") { ShellLoading("Loading sources…") }
            state.isEmpty -> item("sources-empty") {
                ShellEmpty("No sources found", "Install an extension or change your source filters.")
            }
            else -> itemsIndexed(
                state.items,
                key = { _, it -> when (it) {
                    is SourceUiModel.Header -> "sources-header-${it.language}-${it.isCategory}"
                    is SourceUiModel.Item -> "source-${it.source.key()}"
                } },
                contentType = { _, it -> if (it is SourceUiModel.Header) "header" else "source" },
            ) { index, model ->
                when (model) {
                    is SourceUiModel.Header -> TsuzukiText(
                        if (model.isCategory) model.language else LocaleHelper.getSourceDisplayName(model.language, LocalContext.current),
                        Modifier.padding(start = 36.dp, top = 16.dp, bottom = 8.dp),
                        TsuzukiTheme.typography.footnote,
                        TsuzukiTheme.colors.secondary,
                    )
                    is SourceUiModel.Item -> SourceRow(
                        model.source, state.showLatest, state.showPin, onClickItem, onLongClickItem, onClickPin,
                        shape = RoundedCornerShape(
                            topStart = if (state.items.getOrNull(index - 1) !is SourceUiModel.Item) 14.dp else 0.dp,
                            topEnd = if (state.items.getOrNull(index - 1) !is SourceUiModel.Item) 14.dp else 0.dp,
                            bottomStart = if (state.items.getOrNull(index + 1) !is SourceUiModel.Item) 14.dp else 0.dp,
                            bottomEnd = if (state.items.getOrNull(index + 1) !is SourceUiModel.Item) 14.dp else 0.dp,
                        ),
                        divider = state.items.getOrNull(index + 1) is SourceUiModel.Item,
                    )
                }
            }
        }
        if (showMangaDexDestinations) {
            item("source-migrations") {
                InsetGroupedList(title = "Manage") {
                    GroupedRow(stringResource(MR.strings.label_migration), chevron = true, onClick = { navigator.push(SourceMigrationScreen()) }, divider = false)
                }
            }
        }
    }
}

@Composable
private fun SourceRow(
    source: Source,
    showLatest: Boolean,
    showPin: Boolean,
    onClickItem: (Source, Listing) -> Unit,
    onLongClickItem: (Source) -> Unit,
    onClickPin: (Source) -> Unit,
    shape: CornerBasedShape,
    divider: Boolean,
) {
    val colors = TsuzukiTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .clip(shape).background(colors.grouped)
            .combinedClickable(onClick = { onClickItem(source, Listing.Popular) }, onLongClick = { onLongClickItem(source) })
            .drawBehind {
                if (divider) drawLine(colors.separator, Offset(56.dp.toPx(), size.height), Offset(size.width, size.height), 0.5.dp.toPx())
            }
            .heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SourceIcon(source = source, modifier = Modifier.size(32.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            TsuzukiText(source.visualName, style = TsuzukiTheme.typography.callout, maxLines = 1, overflow = TextOverflow.Ellipsis)
            TsuzukiText(
                LocaleHelper.getSourceDisplayName(source.lang, LocalContext.current),
                style = TsuzukiTheme.typography.footnote,
                color = colors.secondary,
                maxLines = 1,
            )
        }
        if (showPin) {
            val pinned = Pin.Pinned in source.pin
            IconButton(if (pinned) "Unpin source" else "Pin source", { onClickPin(source) }) {
                Icon(Icons.Outlined.PushPin, null, Modifier.size(18.dp), tint = if (pinned) colors.accent else colors.secondary)
            }
        }
        if (showLatest && source.supportsLatest) {
            PillButton(stringResource(MR.strings.latest), { onClickItem(source, Listing.Latest) }, prominent = false)
        }
    }
}

@Composable
fun SourceOptionsDialog(
    source: Source,
    onClickPin: () -> Unit,
    onClickDisable: () -> Unit,
    onClickSetCategories: (() -> Unit)?,
    onClickToggleDataSaver: (() -> Unit)?,
    onDismiss: () -> Unit,
    onClickSettings: (() -> Unit)? = null,
) {
    DetentSheet(true, source.visualName, onDismiss) {
        GroupedRow(stringResource(if (Pin.Pinned in source.pin) MR.strings.action_unpin else MR.strings.action_pin), onClick = onClickPin)
        if (!source.isLocal()) GroupedRow(stringResource(MR.strings.action_disable), onClick = onClickDisable)
        onClickSetCategories?.let { GroupedRow(stringResource(MR.strings.categories), onClick = it) }
        onClickToggleDataSaver?.let {
            GroupedRow(stringResource(if (source.isExcludedFromDataSaver) SYMR.strings.data_saver_stop_exclude else SYMR.strings.data_saver_exclude), onClick = it)
        }
        if (onClickSettings != null && !source.isLocal()) {
            GroupedRow(stringResource(MR.strings.label_extension_info), onClick = onClickSettings, divider = false)
        }
    }
}

@Composable
fun SourceCategoriesDialog(
    source: Source,
    categories: ImmutableList<String>,
    onClickCategories: (List<String>) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val selected = remember(source) { mutableStateListOf<String>().also { it.addAll(source.categories) } }
    DetentSheet(true, source.visualName, onDismissRequest) {
        categories.forEach { category ->
            GroupedRow(category, checked = category in selected, onCheckedChange = {
                if (it) selected.add(category) else selected.remove(category)
            })
        }
        PillButton(stringResource(MR.strings.action_ok), { onClickCategories(selected.toList()); onDismissRequest() })
    }
}

