package dev.errnolink.tsuzuki.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.errnolink.tsuzuki.designsystem.ProgressRing
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.ui.shell.ShellAction
import dev.errnolink.tsuzuki.ui.shell.ShellActionSheet
import dev.errnolink.tsuzuki.ui.shell.ShellContentScaffold
import dev.errnolink.tsuzuki.ui.shell.ShellCover
import dev.errnolink.tsuzuki.ui.shell.ShellEmpty
import dev.errnolink.tsuzuki.ui.shell.ShellSheetAction
import dev.errnolink.tsuzuki.ui.settings.ConfirmSheet
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreenModel
import kotlinx.coroutines.flow.flowOf
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.manga.model.asMangaCover

object DownloadQueueScreen : Screen() {
    private fun readResolve(): Any = DownloadQueueScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel { DownloadQueueScreenModel(navigator = navigator) }
        val groups by model.state.collectAsState()
        val running by model.isDownloaderRunning.collectAsState()
        val queue = remember(groups) { groups.flatMap { group -> group.subItems.map { it.download } } }
        var selected by remember { mutableStateOf<Download?>(null) }
        var sorting by remember { mutableStateOf(false) }
        var clearing by remember { mutableStateOf(false) }
        ShellContentScaffold(
            title = "Downloads",
            navigateUp = navigator::pop,
            actions = {
                if (queue.isNotEmpty()) {
                    ShellAction(if (running) "Pause downloads" else "Resume downloads", if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, {
                        if (running) model.pauseDownloads() else model.startDownloads()
                    })
                    ShellAction("Sort downloads", Icons.AutoMirrored.Outlined.Sort, { sorting = true })
                    ShellAction("Cancel all downloads", Icons.Outlined.MoreHoriz, { clearing = true })
                }
            },
        ) { padding ->
            val listState = rememberLazyListState()
            val reorderable = rememberReorderableLazyListState(listState, padding) { from, to ->
                val fromIndex = queue.indexOfFirst { "download-${it.chapter.id}" == from.key }
                val toIndex = queue.indexOfFirst { "download-${it.chapter.id}" == to.key }
                if (fromIndex >= 0 && toIndex >= 0 && queue[fromIndex].source.id == queue[toIndex].source.id) {
                    model.reorder(queue.toMutableList().apply { add(toIndex, removeAt(fromIndex)) })
                }
            }
            LazyColumn(state = listState, contentPadding = padding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (queue.isEmpty()) {
                    item("downloads-empty") { ShellEmpty("Ready for offline reading", "Downloaded chapters stay on this device. Add chapters from a manga’s chapter list.") }
                } else {
                    item("download-summary") {
                        TsuzukiText(
                            "${queue.size} chapters · ${if (running) "Downloading" else "Paused"}",
                            Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            TsuzukiTheme.typography.subhead,
                            TsuzukiTheme.colors.secondary,
                        )
                    }
                    groups.forEach { group ->
                        item("download-source-${group.id}") {
                            TsuzukiText(group.name, Modifier.padding(horizontal = 20.dp, vertical = 8.dp), TsuzukiTheme.typography.headline)
                        }
                        items(group.subItems, key = { "download-${it.download.chapter.id}" }, contentType = { "download" }) { item ->
                            val download = item.download
                            ReorderableItem(reorderable, "download-${download.chapter.id}") {
                                DownloadRow(
                                    download,
                                    running,
                                    onOpen = { model.showManga(download.manga.id) },
                                    onActions = { selected = download },
                                    dragHandle = Modifier.draggableHandle(),
                                )
                            }
                        }
                    }
                }
            }
        }
        ShellActionSheet(
            sorting,
            "Sort downloads",
            listOf(
                ShellSheetAction("Newest uploads") { model.reorder(queue.sortedWithinSources { it.chapter.dateUpload }.reversedWithinSources()) },
                ShellSheetAction("Oldest uploads") { model.reorder(queue.sortedWithinSources { it.chapter.dateUpload }) },
                ShellSheetAction("Chapter number ascending") { model.reorder(queue.sortedWithinSources { it.chapter.chapterNumber }) },
                ShellSheetAction("Chapter number descending") { model.reorder(queue.sortedWithinSources { it.chapter.chapterNumber }.reversedWithinSources()) },
            ),
            onDismissRequest = { sorting = false },
        )
        selected?.let { download ->
            ShellActionSheet(
                true,
                download.chapter.name,
                listOf(
                    ShellSheetAction("Show manga") { model.showManga(download.manga.id) },
                    ShellSheetAction("Move to top") { model.reorder(queue.moveDownload(download, true)) },
                    ShellSheetAction("Move to bottom") { model.reorder(queue.moveDownload(download, false)) },
                    ShellSheetAction("Move series to top") {
                        val (series, others) = queue.partition { it.manga.id == download.manga.id }
                        model.reorder(series + others)
                    },
                    ShellSheetAction("Move series to bottom") {
                        val (series, others) = queue.partition { it.manga.id == download.manga.id }
                        model.reorder(others + series)
                    },
                    ShellSheetAction("Retry downloads", onClick = model::startDownloads),
                    ShellSheetAction("Cancel chapter", destructive = true) { model.cancel(listOf(download)) },
                    ShellSheetAction("Cancel series", destructive = true) { model.cancel(queue.filter { it.manga.id == download.manga.id }) },
                ),
                onDismissRequest = { selected = null },
            )
        }
        ConfirmSheet(
            visible = clearing,
            title = "Cancel all downloads?",
            message = "Remove ${queue.size} queued chapters. Completed downloads are kept.",
            confirmLabel = "Cancel downloads",
            destructive = true,
            onConfirm = model::clearQueue,
            onDismissRequest = { clearing = false },
        )
    }
}

@Composable
private fun DownloadRow(download: Download, running: Boolean, onOpen: () -> Unit, onActions: () -> Unit, dragHandle: Modifier) {
    val status by download.statusFlow.collectAsState()
    val progressFlow = remember(download, status) {
        if (status == Download.State.DOWNLOADING) download.progressFlow else flowOf(download.progress)
    }
    val progress by progressFlow.collectAsState(initial = download.progress)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(14.dp))
            .background(TsuzukiTheme.colors.grouped).clickable(onClick = onOpen).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.DragHandle, "Reorder download", dragHandle.size(32.dp).padding(6.dp), tint = TsuzukiTheme.colors.secondary)
        ShellCover(download.manga.asMangaCover(), download.manga.title, Modifier.width(40.dp), fixedRatio = 2f / 3f)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            TsuzukiText(download.chapter.name, style = TsuzukiTheme.typography.callout, maxLines = 1, overflow = TextOverflow.Ellipsis)
            TsuzukiText(download.manga.title, style = TsuzukiTheme.typography.footnote, color = TsuzukiTheme.colors.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            TsuzukiText(
                when (status) {
                    Download.State.ERROR -> "Download failed · Retry available"
                    Download.State.DOWNLOADING -> "${download.downloadedImages}/${download.pages?.size ?: 0} pages · $progress%"
                    Download.State.DOWNLOADED -> "Downloaded"
                    else -> if (running) "Queued" else "Paused"
                },
                style = TsuzukiTheme.typography.caption1,
                color = if (status == Download.State.ERROR) TsuzukiTheme.colors.destructive else TsuzukiTheme.colors.secondary,
            )
        }
        if (status == Download.State.DOWNLOADING) ProgressRing(progress / 100f, "Download progress")
        ShellAction("Download actions", Icons.Outlined.MoreHoriz, onActions)
    }
}

private fun <T : Comparable<T>> List<Download>.sortedWithinSources(selector: (Download) -> T): List<Download> =
    groupBy { it.source.id }.values.flatMap { it.sortedBy(selector) }

private fun List<Download>.reversedWithinSources(): List<Download> = groupBy { it.source.id }.values.flatMap { it.reversed() }

private fun List<Download>.moveDownload(download: Download, first: Boolean): List<Download> = groupBy { it.source.id }.values.flatMap { group ->
    if (group.first().source.id != download.source.id) group
    else group.filterNot { it.chapter.id == download.chapter.id }.let { if (first) listOf(download) + it else it + download }
}
