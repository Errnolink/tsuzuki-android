package dev.errnolink.tsuzuki.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.DetentSheet
import dev.errnolink.tsuzuki.designsystem.EmptyState
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.ui.details.MangaChapterListItem
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.reader.chapter.ReaderChapterItem
import eu.kanade.tachiyomi.util.lang.toRelativeString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ChapterListDialog(
    onDismissRequest: () -> Unit,
    chapters: ImmutableList<ReaderChapterItem>,
    onClickChapter: (Chapter) -> Unit,
    onBookmark: (Chapter) -> Unit,
    dateRelativeTime: Boolean,
    onDownloadAction: ((Chapter, ChapterDownloadAction) -> Unit)? = null,
) {
    val context = LocalContext.current
    val state = rememberLazyListState(chapters.indexOfFirst { it.isCurrent }.coerceAtLeast(0))
    val downloadManager: DownloadManager = remember { Injekt.get() }
    val queue by downloadManager.queueState.collectAsState()
    DetentSheet(true, "Chapters", onDismissRequest, scrollContent = false) {
        if (chapters.isEmpty()) {
            dev.errnolink.tsuzuki.ui.shell.ShellEmpty("No chapters", "The source has no chapters available for this title.")
        } else {
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(TsuzukiTheme.colors.grouped),
            ) {
                items(chapters, key = { it.chapter.id }, contentType = { "chapter" }) { item ->
                    val active = queue.find { it.chapter.id == item.chapter.id }
                    val progress = active?.let {
                        remember(item.chapter.id) {
                            downloadManager.progressFlow().filter { it.chapter.id == item.chapter.id }.map { it.progress }
                        }.collectAsState(0).value
                    } ?: 0
                    val downloaded = item.manga.isLocal() || downloadManager.isChapterDownloaded(
                        item.chapter.name, item.chapter.scanlator, item.chapter.url, item.manga.ogTitle, item.manga.source,
                    )
                    val downloadState = active?.status ?: if (downloaded) Download.State.DOWNLOADED else Download.State.NOT_DOWNLOADED
                    MangaChapterListItem(
                        title = item.chapter.name,
                        date = item.chapter.dateUpload.takeIf { it > 0L }?.let {
                            LocalDate.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()).toRelativeString(context, dateRelativeTime, item.dateFormat)
                        },
                        readProgress = if (item.isCurrent) "Reading now" else null,
                        scanlator = item.chapter.scanlator,
                        sourceName = null,
                        read = item.chapter.read,
                        bookmark = item.chapter.bookmark,
                        selected = item.isCurrent,
                        downloadIndicatorEnabled = onDownloadAction != null,
                        downloadStateProvider = { downloadState },
                        downloadProgressProvider = { progress },
                        chapterSwipeStartAction = LibraryPreferences.ChapterSwipeAction.ToggleBookmark,
                        chapterSwipeEndAction = LibraryPreferences.ChapterSwipeAction.ToggleBookmark,
                        onLongClick = { onBookmark(item.chapter) },
                        onClick = { onClickChapter(item.chapter) },
                        onDownloadClick = onDownloadAction?.let { callback -> { callback(item.chapter, it) } },
                        onChapterSwipe = { onBookmark(item.chapter) },
                        shape = RoundedCornerShape(0.dp),
                    )
                }
            }
        }
    }
}
