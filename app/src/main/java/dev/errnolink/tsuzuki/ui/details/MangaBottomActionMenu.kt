package dev.errnolink.tsuzuki.ui.details

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.GlassContext
import eu.kanade.tachiyomi.R

@Composable
fun MangaBottomActionMenu(
    visible: Boolean,
    modifier: Modifier = Modifier,
    glass: GlassContext? = null,
    onBookmarkClicked: (() -> Unit)? = null,
    onRemoveBookmarkClicked: (() -> Unit)? = null,
    onMarkAsReadClicked: (() -> Unit)? = null,
    onMarkAsUnreadClicked: (() -> Unit)? = null,
    onMarkPreviousAsReadClicked: (() -> Unit)? = null,
    onDownloadClicked: (() -> Unit)? = null,
    onDeleteClicked: (() -> Unit)? = null,
) {
    if (!visible) return
    DetailGlassGroup(
        context = glass,
        modifier = modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            onBookmarkClicked?.let { DetailIcon("Bookmark", Icons.Outlined.BookmarkAdd, it) }
            onRemoveBookmarkClicked?.let { DetailIcon("Remove bookmark", Icons.Outlined.BookmarkRemove, it) }
            onMarkAsReadClicked?.let { DetailIcon("Mark read", Icons.Outlined.DoneAll, it) }
            onMarkAsUnreadClicked?.let { DetailIcon("Mark unread", Icons.Outlined.RemoveDone, it) }
            onMarkPreviousAsReadClicked?.let { DetailIcon("Mark previous read", ImageVector.vectorResource(R.drawable.ic_done_prev_24dp), it) }
            onDownloadClicked?.let { DetailIcon("Download", Icons.Outlined.Download, it) }
            onDeleteClicked?.let { DetailIcon("Delete downloads", Icons.Outlined.Delete, it) }
        }
    }
}

