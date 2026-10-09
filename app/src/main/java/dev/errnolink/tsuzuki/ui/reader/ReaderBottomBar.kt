package dev.errnolink.tsuzuki.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.errnolink.tsuzuki.designsystem.ContextMenuAction
import dev.errnolink.tsuzuki.designsystem.GlassMenu
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.ui.details.DetailIcon
import eu.kanade.tachiyomi.ui.reader.setting.ReaderBottomButton
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import kotlinx.collections.immutable.ImmutableSet

@Composable
fun ReaderBottomBar(
    enabledButtons: ImmutableSet<String>,
    enabled: Boolean,
    readingMode: ReadingMode,
    onClickReadingMode: () -> Unit,
    orientation: ReaderOrientation,
    onClickOrientation: () -> Unit,
    cropEnabled: Boolean,
    onClickCropBorder: () -> Unit,
    onClickSettings: () -> Unit,
    currentReadingMode: ReadingMode,
    dualPageSplitEnabled: Boolean,
    doublePages: Boolean,
    onClickChapterList: () -> Unit,
    onClickWebView: (() -> Unit)?,
    onClickBrowser: (() -> Unit)?,
    onClickShare: (() -> Unit)?,
    onClickPageLayout: () -> Unit,
    onClickShiftPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    val actions = buildList {
        if (ReaderBottomButton.WebView.isIn(enabledButtons)) onClickWebView?.let { add(readerMenuAction("Open in web view", dev.errnolink.tsuzuki.designsystem.TsuzukiIcons.Globe, it)) }
        if (ReaderBottomButton.Browser.isIn(enabledButtons)) onClickBrowser?.let { add(readerMenuAction("Open in browser", dev.errnolink.tsuzuki.designsystem.TsuzukiIcons.Browse, it)) }
        if (ReaderBottomButton.Share.isIn(enabledButtons)) onClickShare?.let { add(readerMenuAction("Share", dev.errnolink.tsuzuki.designsystem.TsuzukiIcons.Share, it)) }
        if (ReaderBottomButton.ReadingMode.isIn(enabledButtons)) add(readerMenuAction("Reading mode", dev.errnolink.tsuzuki.designsystem.TsuzukiIcons.Book, onClickReadingMode))
        if (ReaderBottomButton.Rotation.isIn(enabledButtons)) add(readerMenuAction("Orientation", orientation.icon, onClickOrientation))
        val cropButton = when (currentReadingMode) {
            ReadingMode.WEBTOON -> ReaderBottomButton.CropBordersWebtoon
            ReadingMode.CONTINUOUS_VERTICAL -> ReaderBottomButton.CropBordersContinuesVertical
            else -> ReaderBottomButton.CropBordersPager
        }
        if (cropButton.isIn(enabledButtons)) add(readerMenuAction(if (cropEnabled) "Keep page borders" else "Crop page borders", Icons.Outlined.Crop, onClickCropBorder))
        if (!dualPageSplitEnabled && ReaderBottomButton.PageLayout.isIn(enabledButtons) && ReadingMode.isPagerType(currentReadingMode.flagValue)) {
            add(readerMenuAction("Page layout", Icons.Outlined.MenuBook, onClickPageLayout))
        }
        if (doublePages) add(readerMenuAction("Shift double pages", Icons.Outlined.SwapHoriz, onClickShiftPage))
    }
    Row(modifier) {
        DetailIcon("Chapters", dev.errnolink.tsuzuki.designsystem.TsuzukiIcons.Chapters, onClickChapterList, enabled = enabled)
        DetailIcon("Reading settings", dev.errnolink.tsuzuki.designsystem.TsuzukiIcons.Settings, onClickSettings, enabled = enabled)
        if (actions.isNotEmpty()) {
            Box {
                DetailIcon("More reading actions", Icons.Outlined.MoreHoriz, { menu = true }, enabled = enabled)
                GlassMenu(menu && enabled, "Reading actions", actions, { menu = false })
            }
        }
    }
}

private fun readerMenuAction(label: String, icon: ImageVector, onClick: () -> Unit) = ContextMenuAction(
    label,
    icon = { Icon(icon, null, tint = TsuzukiTheme.colors.text) },
    onClick = onClick,
)
