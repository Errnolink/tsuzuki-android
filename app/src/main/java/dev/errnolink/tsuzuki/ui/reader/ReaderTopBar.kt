package dev.errnolink.tsuzuki.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import dev.errnolink.tsuzuki.designsystem.GlassSurface
import dev.errnolink.tsuzuki.designsystem.TsuzukiIcons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.ui.details.DetailIcon

@Composable
fun ReaderTopBar(
    enabled: Boolean,
    mangaTitle: String?,
    chapterTitle: String?,
    navigateUp: () -> Unit,
    onTitleClick: () -> Unit,
    bookmarked: Boolean,
    onToggleBookmarked: () -> Unit,
    onToolsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GlassSurface(null, Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DetailIcon("Back", TsuzukiIcons.Back, navigateUp, enabled = enabled)
                Column(Modifier.weight(1f).clickable(enabled = enabled, onClick = onTitleClick).padding(end = 12.dp, top = 8.dp, bottom = 8.dp)) {
                    TsuzukiText(mangaTitle.orEmpty(), style = TsuzukiTheme.typography.headline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    TsuzukiText(chapterTitle.orEmpty(), style = TsuzukiTheme.typography.caption1, color = TsuzukiTheme.colors.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        GlassSurface(null) {
            Row {
                DetailIcon(if (bookmarked) "Remove bookmark" else "Bookmark chapter", if (bookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, onToggleBookmarked, enabled = enabled)
                DetailIcon("Reader tools", Icons.Outlined.MoreHoriz, onToolsClick, enabled = enabled)
            }
        }
    }
}