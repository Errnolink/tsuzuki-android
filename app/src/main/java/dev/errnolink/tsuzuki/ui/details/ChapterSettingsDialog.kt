package dev.errnolink.tsuzuki.ui.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.errnolink.tsuzuki.designsystem.DetentSheet
import dev.errnolink.tsuzuki.designsystem.GroupedRow
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.designsystem.SegmentedControl
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.mangadex.MangaDexLanguageFilter
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.manga.model.downloadedFilter
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun ChapterSettingsDialog(
    onDismissRequest: () -> Unit,
    manga: Manga? = null,
    onDownloadFilterChanged: (TriState) -> Unit,
    onUnreadFilterChanged: (TriState) -> Unit,
    onBookmarkedFilterChanged: (TriState) -> Unit,
    scanlatorFilterActive: Boolean,
    onScanlatorFilterClicked: () -> Unit,
    onSortModeChanged: (Long) -> Unit,
    onDisplayModeChanged: (Long) -> Unit,
    onSetAsDefault: (applyToExistingManga: Boolean) -> Unit,
    onResetToDefault: () -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var setDefault by rememberSaveable { mutableStateOf(false) }
    var applyExisting by rememberSaveable { mutableStateOf(false) }
    val downloadedOnlyPreference = remember { Injekt.get<BasePreferences>().downloadedOnly() }
    val downloadedOnly by downloadedOnlyPreference.collectAsState()
    DetentSheet(true, if (setDefault) "Default chapter settings" else "Chapters", onDismissRequest) {
        if (setDefault) {
            TsuzukiText(stringResource(MR.strings.confirm_set_chapter_settings))
            GroupedRow(stringResource(MR.strings.also_set_chapter_settings_for_library), checked = applyExisting, onCheckedChange = { applyExisting = it })
            PillButton("Save defaults", { onSetAsDefault(applyExisting); setDefault = false })
            PillButton("Cancel", { setDefault = false }, prominent = false)
        } else {
            SegmentedControl(listOf("Filter", "Sort", "Display"), page, { page = it })
            when (page) {
                0 -> {
                    if (downloadedOnly) {
                        GroupedRow("Downloaded only", subtitle = "Global mode hides online chapters", checked = true, onCheckedChange = {
                            downloadedOnlyPreference.set(false)
                            onDownloadFilterChanged(TriState.DISABLED)
                        })
                    } else {
                        ChapterFilter("Downloads", manga?.downloadedFilter ?: TriState.DISABLED, onDownloadFilterChanged)
                    }
                    ChapterFilter("Unread", manga?.unreadFilter ?: TriState.DISABLED, onUnreadFilterChanged)
                    ChapterFilter("Bookmarked", manga?.bookmarkedFilter ?: TriState.DISABLED, onBookmarkedFilterChanged)
                    GroupedRow("Scanlators", value = if (scanlatorFilterActive) "Filtered" else "All", chevron = true, onClick = onScanlatorFilterClicked)
                    manga?.let { MangaDexLanguageFilter(it.id) }
                }
                1 -> {
                    listOf(
                        MR.strings.sort_by_source to Manga.CHAPTER_SORTING_SOURCE,
                        MR.strings.sort_by_number to Manga.CHAPTER_SORTING_NUMBER,
                        MR.strings.sort_by_upload_date to Manga.CHAPTER_SORTING_UPLOAD_DATE,
                        MR.strings.action_sort_alpha to Manga.CHAPTER_SORTING_ALPHABET,
                    ).forEach { (label, mode) ->
                        GroupedRow(
                            stringResource(label),
                            value = if (manga?.sorting == mode) if (manga.sortDescending()) "Descending" else "Ascending" else null,
                            onClick = { onSortModeChanged(mode) },
                        )
                    }
                }
                2 -> {
                    val modes = listOf(Manga.CHAPTER_DISPLAY_NAME, Manga.CHAPTER_DISPLAY_NUMBER)
                    SegmentedControl(listOf("Title", "Chapter number"), modes.indexOf(manga?.displayMode).coerceAtLeast(0), { onDisplayModeChanged(modes[it]) })
                }
            }
            GroupedRow("Save as defaults", chevron = true, onClick = { setDefault = true })
            GroupedRow("Reset to defaults", onClick = onResetToDefault)
        }
    }
}

@Composable
private fun ChapterFilter(label: String, value: TriState, onChange: (TriState) -> Unit) {
    TsuzukiText(label, style = TsuzukiTheme.typography.headline)
    val values = listOf(TriState.DISABLED, TriState.ENABLED_IS, TriState.ENABLED_NOT)
    SegmentedControl(listOf("All", "Only", "Exclude"), values.indexOf(value), { onChange(values[it]) })
}
