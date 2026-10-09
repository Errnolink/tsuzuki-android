package dev.errnolink.tsuzuki.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.ContextMenu
import dev.errnolink.tsuzuki.designsystem.ContextMenuAction
import dev.errnolink.tsuzuki.designsystem.PillButton
import tachiyomi.domain.history.model.HistoryWithRelations
import java.text.DateFormat
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.ui.shell.ShellCover

@Composable
fun HistoryItem(
    history: HistoryWithRelations,
    onClickCover: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickDelete: () -> Unit,
    onClickFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean,
    readProgress: String?,
    hasUnread: Boolean,
    usePanoramaCover: Boolean,
    coverRatio: MutableFloatState = remember { mutableFloatStateOf(1f) },
    shape: CornerBasedShape = RoundedCornerShape(14.dp),
    divider: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val colors = TsuzukiTheme.colors
    val longClick = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onLongClick()
    }
    Row(
        modifier = modifier.fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.selectedFill else colors.grouped)
            .combinedClickable(onClick = onClick, onLongClick = longClick)
            .drawBehind {
                if (divider) drawLine(colors.separator, Offset(72.dp.toPx(), size.height), Offset(size.width, size.height), 0.5.dp.toPx())
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TsuzukiTheme.spacing.medium),
    ) {
        ContextMenu(
            label = history.title,
            actions = listOf(
                ContextMenuAction(
                    if (history.coverData.isMangaFavorite) "Remove from library" else "Add to library",
                    icon = { Icon(Icons.Outlined.FavoriteBorder, null, tint = colors.text) },
                    destructive = history.coverData.isMangaFavorite,
                    onClick = onClickFavorite,
                ),
                ContextMenuAction("Remove history", icon = { Icon(Icons.Outlined.Delete, null, tint = colors.destructive) }, destructive = true, onClick = onClickDelete),
            ),
            onClick = onClickCover,
        ) {
            ShellCover(history.coverData, history.title, Modifier.width(48.dp), fixedRatio = if (usePanoramaCover) null else 2f / 3f)
        }
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            TsuzukiText(
                history.title,
                style = TsuzukiTheme.typography.headline,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (history.read && readProgress == null) colors.secondary else colors.text,
            )
            TsuzukiText(
                if (history.chapterNumber >= 0) "Chapter ${history.chapterNumber.toString().removeSuffix(".0")}" else "Chapter",
                style = TsuzukiTheme.typography.footnote,
                color = colors.secondary,
            )
            if (readProgress != null) {
                TsuzukiText(
                    readProgress,
                    style = TsuzukiTheme.typography.caption1,
                    color = if (hasUnread) colors.accent else colors.secondary,
                )
            }
            val time = remember(history.readAt) { history.readAt?.let { DateFormat.getTimeInstance(DateFormat.SHORT).format(it) } }
            if (time != null) {
                TsuzukiText(time, style = TsuzukiTheme.typography.caption1, color = colors.secondary)
            }
        }
        PillButton(if (selected) "Selected" else "Resume", onClick, prominent = false)
    }
}

