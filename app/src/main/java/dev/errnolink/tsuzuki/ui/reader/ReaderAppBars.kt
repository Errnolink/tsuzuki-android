package dev.errnolink.tsuzuki.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.GlassContext
import dev.errnolink.tsuzuki.designsystem.GlassSurface
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import kotlinx.collections.immutable.ImmutableSet

// SY -->
enum class NavBarType {
    VerticalRight,
    VerticalLeft,
    Bottom,
}
// SY <--

@Composable
fun ReaderAppBars(
    visible: Boolean,
    glass: GlassContext,

    mangaTitle: String?,
    chapterTitle: String?,
    navigateUp: () -> Unit,
    onClickTopAppBar: () -> Unit,
    bookmarked: Boolean,
    onToggleBookmarked: () -> Unit,
    onOpenInWebView: (() -> Unit)?,
    onOpenInBrowser: (() -> Unit)?,
    onShare: (() -> Unit)?,

    viewer: Viewer?,
    onNextChapter: () -> Unit,
    enabledNext: Boolean,
    onPreviousChapter: () -> Unit,
    enabledPrevious: Boolean,
    currentPage: Int,
    totalPages: Int,
    onPageIndexChange: (Int) -> Unit,

    readingMode: ReadingMode,
    onClickReadingMode: () -> Unit,
    orientation: ReaderOrientation,
    onClickOrientation: () -> Unit,
    cropEnabled: Boolean,
    onClickCropBorder: () -> Unit,
    onClickSettings: () -> Unit,
    // SY -->
    isExhToolsVisible: Boolean,
    onSetExhUtilsVisibility: (Boolean) -> Unit,
    isAutoScroll: Boolean,
    isAutoScrollEnabled: Boolean,
    onToggleAutoscroll: (Boolean) -> Unit,
    autoScrollFrequency: String,
    onSetAutoScrollFrequency: (String) -> Unit,
    onClickAutoScrollHelp: () -> Unit,
    onClickRetryAll: () -> Unit,
    onClickRetryAllHelp: () -> Unit,
    onClickBoostPage: () -> Unit,
    onClickBoostPageHelp: () -> Unit,
    navBarType: NavBarType,
    currentPageText: String,
    enabledButtons: ImmutableSet<String>,
    currentReadingMode: ReadingMode,
    dualPageSplitEnabled: Boolean,
    doublePages: Boolean,
    onClickChapterList: () -> Unit,
    onClickPageLayout: () -> Unit,
    onClickShiftPage: () -> Unit,
    // SY <--
) {
    val isRtl = viewer is R2LPagerViewer
    val duration = if (TsuzukiTheme.motion.reduced) 0 else 160
    val slide = with(LocalDensity.current) { if (TsuzukiTheme.motion.reduced) 0 else 6.dp.roundToPx() }
    Box(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .then(if (visible) Modifier else Modifier.clearAndSetSemantics {}),
    ) {
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(tween(duration)) + slideInVertically(tween(duration)) { -slide },
            exit = fadeOut(tween(duration)) + slideOutVertically(tween(duration)) { -slide },
        ) {
            Box {
                ReaderTopBar(
                    enabled = visible,
                    mangaTitle = mangaTitle,
                    chapterTitle = chapterTitle,
                    navigateUp = navigateUp,
                    onTitleClick = onClickTopAppBar,
                    bookmarked = bookmarked,
                    onToggleBookmarked = onToggleBookmarked,
                    onToolsClick = { onSetExhUtilsVisibility(true) },
                )
            }
        }
        if (navBarType != NavBarType.Bottom) {
            AnimatedVisibility(
                visible,
                modifier = Modifier.align(if (navBarType == NavBarType.VerticalLeft) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(top = 76.dp, bottom = 64.dp),
                enter = fadeIn(tween(duration)),
                exit = fadeOut(tween(duration)),
            ) {
                GlassSurface(null, Modifier.heightIn(max = 480.dp), shape = RoundedCornerShape(28.dp)) {
                    key(chapterTitle, viewer) {
                        ChapterNavigator(
                            isRtl, true, onNextChapter, enabledNext, onPreviousChapter, enabledPrevious,
                            currentPage, currentPageText, totalPages, onPageIndexChange, enabled = visible,
                        )
                    }
                }
            }
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(duration)) + slideInVertically(tween(duration)) { slide },
            exit = fadeOut(tween(duration)) + slideOutVertically(tween(duration)) { slide },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (navBarType == NavBarType.Bottom) {
                    GlassSurface(null, Modifier.fillMaxWidth(), shape = RoundedCornerShape(34.dp)) {
                        key(chapterTitle, viewer) {
                            ChapterNavigator(
                                isRtl, false, onNextChapter, enabledNext, onPreviousChapter, enabledPrevious,
                                currentPage, currentPageText, totalPages, onPageIndexChange, enabled = visible,
                            )
                        }
                    }
                }
                GlassSurface(null) {
                    ReaderBottomBar(
                        enabled = visible,
                        readingMode = readingMode,
                        onClickReadingMode = onClickReadingMode,
                        orientation = orientation,
                        onClickOrientation = onClickOrientation,
                        cropEnabled = cropEnabled,
                        onClickCropBorder = onClickCropBorder,
                        onClickSettings = onClickSettings,
                        enabledButtons = enabledButtons,
                        currentReadingMode = currentReadingMode,
                        dualPageSplitEnabled = dualPageSplitEnabled,
                        doublePages = doublePages,
                        onClickChapterList = onClickChapterList,
                        onClickWebView = onOpenInWebView,
                        onClickBrowser = onOpenInBrowser,
                        onClickShare = onShare,
                        onClickPageLayout = onClickPageLayout,
                        onClickShiftPage = onClickShiftPage,
                    )
                }
            }
        }
    }
    ExhUtils(
        isVisible = isExhToolsVisible,
        onSetExhUtilsVisibility = onSetExhUtilsVisibility,
        isAutoScroll = isAutoScroll,
        isAutoScrollEnabled = isAutoScrollEnabled,
        onToggleAutoscroll = onToggleAutoscroll,
        autoScrollFrequency = autoScrollFrequency,
        onSetAutoScrollFrequency = onSetAutoScrollFrequency,
        onClickAutoScrollHelp = onClickAutoScrollHelp,
        onClickRetryAll = onClickRetryAll,
        onClickRetryAllHelp = onClickRetryAllHelp,
        onClickBoostPage = onClickBoostPage,
        onClickBoostPageHelp = onClickBoostPageHelp,
    )
}
