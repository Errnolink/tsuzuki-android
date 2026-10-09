package dev.errnolink.tsuzuki.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.DetentSheet
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.designsystem.TsuzukiIcons
import dev.errnolink.tsuzuki.ui.details.DetailField
import dev.errnolink.tsuzuki.ui.details.DetailIcon

@Composable
fun ChapterNavigator(
    isRtl: Boolean,
    isVerticalSlider: Boolean,
    onNextChapter: () -> Unit,
    enabledNext: Boolean,
    onPreviousChapter: () -> Unit,
    enabledPrevious: Boolean,
    currentPage: Int,
    currentPageText: String,
    totalPages: Int,
    onPageIndexChange: (Int) -> Unit,
    enabled: Boolean,
) {
    var preview by remember { mutableStateOf<Int?>(null) }
    var showJump by remember { mutableStateOf(false) }
    var pageText by remember(currentPage) { mutableStateOf(currentPage.toString()) }
    val readout = if (totalPages > 0) "${preview?.toString() ?: currentPageText} of $totalPages" else "No pages"
    val pageReadout: @Composable () -> Unit = {
        TsuzukiText(
            readout,
            Modifier.clickable(enabled = enabled && totalPages > 0, role = Role.Button, onClick = { pageText = currentPage.toString(); showJump = true })
                .padding(horizontal = 12.dp, vertical = 12.dp),
            TsuzukiTheme.typography.subhead,
        )
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        if (isVerticalSlider) {
            Column(Modifier.fillMaxHeight().padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                DetailIcon("Previous chapter", TsuzukiIcons.Back, onPreviousChapter, Modifier.rotate(90f), enabled && enabledPrevious)
                pageReadout()
                ReaderSlider(
                    currentPage, 1..totalPages.coerceAtLeast(1), { onPageIndexChange(it - 1) },
                    Modifier.weight(1f), vertical = true, enabled = enabled && totalPages > 1, onPreview = { preview = it },
                )
                DetailIcon("Next chapter", TsuzukiIcons.Forward, onNextChapter, Modifier.rotate(90f), enabled && enabledNext)
            }
        } else {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DetailIcon(
                        if (isRtl) "Next chapter" else "Previous chapter", TsuzukiIcons.Back,
                        if (isRtl) onNextChapter else onPreviousChapter, enabled = enabled && if (isRtl) enabledNext else enabledPrevious,
                    )
                    androidx.compose.foundation.layout.Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { pageReadout() }
                    DetailIcon(
                        if (isRtl) "Previous chapter" else "Next chapter", TsuzukiIcons.Forward,
                        if (isRtl) onPreviousChapter else onNextChapter, enabled = enabled && if (isRtl) enabledPrevious else enabledNext,
                    )
                }
                ReaderSlider(
                    currentPage, 1..totalPages.coerceAtLeast(1), { onPageIndexChange(it - 1) },
                    reversed = isRtl, enabled = enabled && totalPages > 1, onPreview = { preview = it },
                )
            }
        }
    }
    DetentSheet(showJump, "Go to page", { showJump = false }) {
        val target = pageText.toIntOrNull()
        DetailField(pageText, { pageText = it }, "Page (1–$totalPages)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        if (target == null || target !in 1..totalPages) {
            TsuzukiText("Enter a page from 1 to $totalPages.", color = TsuzukiTheme.colors.destructive)
        }
        PillButton("Go", {
            target?.takeIf { it in 1..totalPages }?.let { onPageIndexChange(it - 1); showJump = false }
        }, enabled = target != null && target in 1..totalPages)
    }
}
