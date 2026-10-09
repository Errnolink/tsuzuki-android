package dev.errnolink.tsuzuki.ui.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import dev.errnolink.tsuzuki.designsystem.GlassMenu
import dev.errnolink.tsuzuki.designsystem.TsuzukiIcons
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.ContextMenu
import dev.errnolink.tsuzuki.designsystem.ContextMenuAction
import dev.errnolink.tsuzuki.designsystem.DetentSheet
import dev.errnolink.tsuzuki.designsystem.GroupedRow
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.presentation.manga.components.MarkdownRender
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.manga.components.SearchMetadataChips
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MangaInfoBox(
    isTabletUi: Boolean,
    appBarPadding: Dp,
    manga: Manga,
    sourceName: String,
    isStubSource: Boolean,
    isSourceIncognito: Boolean,
    onCoverClick: () -> Unit,
    doSearch: (query: String, global: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    librarySearch: (query: String) -> Unit,
    onSourceClick: () -> Unit,
    coverRatio: MutableFloatState,
) {
    Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = appBarPadding + 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        MangaCover.Book(
            data = manga, modifier = Modifier.width(114.dp), shape = RoundedCornerShape(8.dp),
            contentDescription = stringResource(MR.strings.manga_cover), onClick = onCoverClick,
            onCoverLoaded = { _, result ->
                coverRatio.floatValue = result.result.image.height.toFloat() / result.result.image.width
            },
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            ContextMenu(
                manga.title,
                listOf(
                    ContextMenuAction("Search library", icon = { Icon(TsuzukiIcons.Library, null, tint = TsuzukiTheme.colors.text) }) { librarySearch(manga.title) },
                    ContextMenuAction("Search all sources", icon = { Icon(TsuzukiIcons.Globe, null, tint = TsuzukiTheme.colors.text) }) { doSearch(manga.title, true) },
                ),
                onClick = { doSearch(manga.title, true) },
            ) {
                TsuzukiText(manga.title, style = TsuzukiTheme.typography.title2, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            manga.author?.takeIf { it.isNotBlank() }?.let {
                TsuzukiText(it, Modifier.clickable { doSearch(it, true) }, TsuzukiTheme.typography.subhead, TsuzukiTheme.colors.secondary)
            }
            manga.artist?.takeIf { it.isNotBlank() && it != manga.author }?.let {
                TsuzukiText(it, Modifier.clickable { doSearch(it, true) }, TsuzukiTheme.typography.footnote, TsuzukiTheme.colors.secondary)
            }
            val status = when (manga.status.toInt()) {
                SManga.ONGOING -> MR.strings.ongoing
                SManga.COMPLETED -> MR.strings.completed
                SManga.LICENSED -> MR.strings.licensed
                SManga.PUBLISHING_FINISHED -> MR.strings.publishing_finished
                SManga.CANCELLED -> MR.strings.cancelled
                SManga.ON_HIATUS -> MR.strings.on_hiatus
                else -> null
            }
            status?.let { TsuzukiText(stringResource(it), style = TsuzukiTheme.typography.footnote, color = TsuzukiTheme.colors.secondary) }
            TsuzukiText(sourceName + if (isStubSource) " · Unavailable" else "", Modifier.clickable(onClick = onSourceClick), TsuzukiTheme.typography.footnote, TsuzukiTheme.colors.accent)
            if (isSourceIncognito) TsuzukiText(stringResource(MR.strings.pref_incognito_mode), style = TsuzukiTheme.typography.caption1, color = TsuzukiTheme.colors.secondary)
        }
    }
}

@Composable
fun ExpandableMangaDescription(
    defaultExpandState: Boolean,
    description: String?,
    tagsProvider: () -> List<String>?,
    notes: String,
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (String) -> Unit,
    onEditNotes: () -> Unit,
    searchMetadataChips: SearchMetadataChips?,
    doSearch: (query: String, global: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(description) { mutableStateOf(defaultExpandState) }
    var descriptionOverflows by remember(description) { mutableStateOf(false) }
    Column(modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val tags = tagsProvider().orEmpty()
        if (tags.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (if (expanded) tags else tags.take(4)).forEach { tag ->
                TagChip(tag.substringAfter(':').trim(), tag, onTagSearch, onCopyTagToClipboard, doSearch)
            }
            if (!expanded && tags.size > 4) TsuzukiText("+${tags.size - 4}", Modifier.clickable { expanded = true }.padding(7.dp), TsuzukiTheme.typography.footnote, TsuzukiTheme.colors.secondary)
        }
        if (expanded && searchMetadataChips != null) {
            searchMetadataChips.tags.forEach { (namespace, values) ->
                TsuzukiText(namespace, style = TsuzukiTheme.typography.footnote, color = TsuzukiTheme.colors.secondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    values.forEach { tag ->
                        TagChip(tag.text.substringAfter(':').trim(), tag.search, onTagSearch, onCopyTagToClipboard, doSearch)
                    }
                }
            }
        }
        if (!description.isNullOrBlank()) {
            if (expanded) {
                SelectionContainer { MarkdownRender(content = description) }
            } else {
                BasicText(
                    text = description,
                    style = TsuzukiTheme.typography.callout.copy(color = TsuzukiTheme.colors.secondary),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { descriptionOverflows = it.hasVisualOverflow },
                )
            }
            if (expanded || descriptionOverflows) {
                TsuzukiText(
                    if (expanded) "Show less" else "Read more",
                    Modifier.clickable { expanded = !expanded }.padding(vertical = 8.dp),
                    TsuzukiTheme.typography.headline,
                    TsuzukiTheme.colors.accent,
                )
            }
        }
        if (notes.isNotBlank()) GroupedRow("Notes", subtitle = if (expanded) notes else notes.lineSequence().firstOrNull(), onClick = onEditNotes, chevron = true)
    }
}

@Composable
private fun TagChip(
    label: String,
    query: String,
    onSearch: (String) -> Unit,
    onCopy: (String) -> Unit,
    onGlobalSearch: (String, Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = TsuzukiTheme.colors
    Box {
        TsuzukiText(
            label,
            Modifier.clip(RoundedCornerShape(10.dp)).background(colors.elevated)
                .clickable { expanded = true }.padding(vertical = 7.dp, horizontal = 10.dp),
            TsuzukiTheme.typography.footnote,
            colors.secondary,
        )
        GlassMenu(
            expanded,
            label,
            listOf(
                ContextMenuAction("Search this source", icon = { Icon(Icons.Outlined.Search, null, tint = colors.text) }) { onSearch(query) },
                ContextMenuAction("Search all sources", icon = { Icon(TsuzukiIcons.Globe, null, tint = colors.text) }) { onGlobalSearch(query, true) },
                ContextMenuAction("Copy tag", icon = { Icon(Icons.Outlined.ContentCopy, null, tint = colors.text) }) { onCopy(query) },
            ),
            onDismissRequest = { expanded = false },
        )
    }
}
