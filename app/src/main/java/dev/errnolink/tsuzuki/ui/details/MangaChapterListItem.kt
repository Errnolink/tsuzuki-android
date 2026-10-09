package dev.errnolink.tsuzuki.ui.details

import eu.kanade.presentation.manga.components.ChapterDownloadAction
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import dev.errnolink.tsuzuki.designsystem.TsuzukiCorners
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileDownloadOff
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.download.model.Download
import me.saket.swipe.SwipeableActionsBox
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.SECONDARY_ALPHA
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.selectedBackground

@Composable
fun MangaChapterListItem(
    title: String,
    date: String?,
    readProgress: String?,
    scanlator: String?,
    // SY -->
    sourceName: String?,
    // SY <--
    read: Boolean,
    bookmark: Boolean,
    selected: Boolean,
    downloadIndicatorEnabled: Boolean,
    downloadStateProvider: () -> Download.State,
    downloadProgressProvider: () -> Int,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onDownloadClick: ((ChapterDownloadAction) -> Unit)?,
    onChapterSwipe: (LibraryPreferences.ChapterSwipeAction) -> Unit,
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(TsuzukiCorners.cover),
    actions: (@Composable () -> Unit)? = null,
    unavailable: Boolean = false,
    languageLabel: String? = null,
) {
    val colors = TsuzukiTheme.colors
    val state = downloadStateProvider()
    val swipeStart = remember(chapterSwipeStartAction, read, bookmark, state, colors) {
        getSwipeAction(chapterSwipeStartAction, read, bookmark, state, colors.elevated) { onChapterSwipe(chapterSwipeStartAction) }
    }
    val swipeEnd = remember(chapterSwipeEndAction, read, bookmark, state, colors) {
        getSwipeAction(chapterSwipeEndAction, read, bookmark, state, colors.elevated) { onChapterSwipe(chapterSwipeEndAction) }
    }
    SwipeableActionsBox(
        modifier = modifier.clip(shape),
        startActions = listOfNotNull(swipeStart), endActions = listOfNotNull(swipeEnd),
        swipeThreshold = swipeActionThreshold, backgroundUntilSwipeThreshold = colors.grouped,
    ) {
        Column(
            Modifier.background(if (selected) colors.accent.copy(alpha = 0.12f) else colors.grouped)
                .semantics { this.selected = selected }
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).alpha(if (unavailable) 0.45f else 1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (selected) Icon(Icons.Outlined.Done, "Selected", Modifier.size(18.dp), tint = colors.accent)
                        if (bookmark) Icon(Icons.Filled.Bookmark, "Bookmarked", Modifier.size(16.dp), tint = colors.accent)
                        TsuzukiText(title, style = TsuzukiTheme.typography.callout, color = if (read) colors.secondary else colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TsuzukiText(
                        listOfNotNull(
                            sourceName?.takeIf { it.isNotBlank() },
                            if (unavailable) "Unavailable" else null,
                            languageLabel,
                            if (read) "Read" else readProgress ?: "Unread",
                            date,
                            scanlator?.takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                        style = TsuzukiTheme.typography.caption1,
                        color = colors.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                actions?.invoke()
                ChapterDownloadIndicator(
                    enabled = downloadIndicatorEnabled && onDownloadClick != null,
                    downloadStateProvider = downloadStateProvider,
                    downloadProgressProvider = downloadProgressProvider,
                    onClick = { onDownloadClick?.invoke(it) },
                )
            }
            Spacer(Modifier.padding(start = 16.dp).fillMaxWidth().height(0.5.dp).background(colors.separator))
        }
    }
}

internal fun getSwipeAction(
    action: LibraryPreferences.ChapterSwipeAction,
    read: Boolean,
    bookmark: Boolean,
    downloadState: Download.State,
    background: Color,
    onSwipe: () -> Unit,
): me.saket.swipe.SwipeAction? {
    return when (action) {
        LibraryPreferences.ChapterSwipeAction.ToggleRead -> swipeAction(
            icon = if (!read) Icons.Outlined.Done else Icons.Outlined.RemoveDone,
            background = background,
            isUndo = read,
            onSwipe = onSwipe,
        )
        LibraryPreferences.ChapterSwipeAction.ToggleBookmark -> swipeAction(
            icon = if (!bookmark) Icons.Outlined.BookmarkAdd else Icons.Outlined.BookmarkRemove,
            background = background,
            isUndo = bookmark,
            onSwipe = onSwipe,
        )
        LibraryPreferences.ChapterSwipeAction.Download -> swipeAction(
            icon = when (downloadState) {
                Download.State.NOT_DOWNLOADED, Download.State.ERROR -> Icons.Outlined.Download
                Download.State.QUEUE, Download.State.DOWNLOADING -> Icons.Outlined.FileDownloadOff
                Download.State.DOWNLOADED -> Icons.Outlined.Delete
            },
            background = background,
            onSwipe = onSwipe,
        )
        LibraryPreferences.ChapterSwipeAction.Disabled -> null
    }
}

internal fun swipeAction(
    onSwipe: () -> Unit,
    icon: ImageVector,
    background: Color,
    isUndo: Boolean = false,
): me.saket.swipe.SwipeAction {
    return me.saket.swipe.SwipeAction(
        icon = {
            Icon(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(IndicatorSize),
                imageVector = icon,
                tint = contentColorFor(background),
                contentDescription = null,
            )
        },
        background = background,
        onSwipe = onSwipe,
        isUndo = isUndo,
    )
}

internal val swipeActionThreshold = 56.dp
