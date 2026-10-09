package dev.errnolink.tsuzuki.mangadex

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.errnolink.tsuzuki.designsystem.ContextMenu
import dev.errnolink.tsuzuki.designsystem.ContextMenuAction
import dev.errnolink.tsuzuki.designsystem.DetentSheet
import dev.errnolink.tsuzuki.designsystem.GroupedRow
import dev.errnolink.tsuzuki.designsystem.InsetGroupedList
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import exh.md.dto.RelationshipDto
import exh.md.handlers.MangaDexFeatures
import exh.md.handlers.MangaDexStatistics
import exh.md.service.MangaDexFeatureRequests
import exh.md.service.MangaDexService
import exh.md.utils.MdUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat

private data class DetailsState(
    val source: MangaDexContext? = null,
    val statistics: MangaDexStatistics? = null,
    val creators: List<RelationshipDto> = emptyList(),
    val error: String? = null,
)

@Composable
fun MangaDexDetailsSection(mangaId: Long) {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    var retry by remember { mutableIntStateOf(0) }
    val state by produceState(DetailsState(), mangaId, retry) {
        withContext(Dispatchers.IO) {
            val resolved = resolveMangaDex(mangaId) ?: return@withContext
            value = DetailsState(source = resolved)
            try {
                val id = MdUtil.getMangaId(resolved.manga.url)
                val manga = MangaDexService(resolved.source.client, MangaDexFeatureRequests.headers).viewManga(id).data
                value = value.copy(creators = manga.relationships.filter { it.type == "author" || it.type == "artist" }.distinctBy { it.id })
                value = value.copy(statistics = MangaDexFeatures(resolved.source).statistics(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                value = value.copy(error = e.message ?: "MangaDex statistics could not be loaded")
            }
        }
    }
    if (state.source == null) return
    var showDistribution by remember { mutableStateOf(false) }
    val numbers = remember { NumberFormat.getIntegerInstance() }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        InsetGroupedList(title = "MangaDex") {
            state.statistics?.let { stats ->
                GroupedRow("Rating", value = stats.rating?.let { String.format("%.2f / 10", it) } ?: "Not rated", chevron = true, onClick = { showDistribution = true })
                GroupedRow("Follows", value = stats.follows?.let(numbers::format) ?: "Not available")
                GroupedRow(
                    "Comments", value = numbers.format(stats.comments), chevron = stats.threadId != null,
                    onClick = stats.threadId?.let { thread ->
                        { context.startActivity(WebViewActivity.newIntent(context, "https://forums.mangadex.org/threads/$thread", state.source!!.source.id, "MangaDex comments")) }
                    },
                )
                stats.estimatedMissing?.let { missing ->
                    GroupedRow("Estimated missing chapters", value = numbers.format(missing), subtitle = "Based on MangaDex aggregate numbering; specials and volume resets may affect this estimate.", divider = stats.unavailable > 0)
                }
                if (stats.unavailable > 0) GroupedRow("Unavailable chapters", value = numbers.format(stats.unavailable), divider = false)
            }
            state.error?.let { GroupedRow("Statistics unavailable", subtitle = it, value = "Retry", divider = false, onClick = { retry++ }) }
            if (state.statistics == null && state.error == null) GroupedRow("Loading statistics…", divider = false)
        }
        if (state.creators.isNotEmpty()) InsetGroupedList(title = "Creators") {
            state.creators.forEachIndexed { index, creator ->
                val name = creator.attributes?.name ?: creator.id
                val browse = { navigator.push(BrowseSourceScreen(state.source!!.source.id, "author:${creator.id}")); Unit }
                ContextMenu(
                    name,
                    listOf(
                        ContextMenuAction("Browse titles", onClick = browse),
                        ContextMenuAction("Copy name") { (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Creator", name)) },
                    ),
                    onClick = browse,
                ) {
                    GroupedRow(name, subtitle = creator.type.replaceFirstChar(Char::uppercase), chevron = true, divider = index != state.creators.lastIndex)
                }
            }
        }
    }
    state.statistics?.let { stats ->
        DetentSheet(showDistribution, "Rating distribution", onDismissRequest = { showDistribution = false }) {
            val max = stats.distribution.values.maxOrNull()?.coerceAtLeast(1) ?: 1
            stats.distribution.entries.sortedByDescending { it.key }.forEach { (rating, votes) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TsuzukiText(rating.toString(), Modifier.width(24.dp), TsuzukiTheme.typography.footnote)
                    Box(Modifier.weight(1f).height(8.dp).background(TsuzukiTheme.colors.elevated)) {
                        Box(Modifier.fillMaxWidth(votes.toFloat() / max).height(8.dp).background(TsuzukiTheme.colors.accent))
                    }
                    TsuzukiText(numbers.format(votes), Modifier.width(54.dp), TsuzukiTheme.typography.caption1)
                }
            }
        }
    }
}
