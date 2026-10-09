package dev.errnolink.tsuzuki.ui.shell

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.TsuzukiIcons
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab

@Composable
fun ShellTabIcon(tab: Tab) {
    val icon = when (tab) {
        LibraryTab -> TsuzukiIcons.Library
        UpdatesTab -> TsuzukiIcons.Updates
        HistoryTab -> TsuzukiIcons.History
        BrowseTab -> TsuzukiIcons.Browse
        MoreTab -> TsuzukiIcons.More
        else -> return
    }
    Icon(icon, null, Modifier.size(23.dp), tint = TsuzukiTheme.colors.text)
}
