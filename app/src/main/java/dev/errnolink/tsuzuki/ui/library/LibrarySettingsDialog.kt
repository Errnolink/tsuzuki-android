package dev.errnolink.tsuzuki.ui.library

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import dev.errnolink.tsuzuki.designsystem.DetentSheet
import dev.errnolink.tsuzuki.designsystem.GroupedRow
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.designsystem.SegmentedControl
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.ui.settings.TsuzukiSliderRow
import dev.errnolink.tsuzuki.ui.settings.PreferenceRow
import dev.errnolink.tsuzuki.mangadex.ScanlatorFilterOption
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import dev.errnolink.tsuzuki.ui.settings.TriStateListDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.library.LibrarySettingsScreenModel
import eu.kanade.tachiyomi.util.system.isReleaseBuildType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.toggle
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.model.sort
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.BaseSortItem
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.IconItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SettingsItemsPaddings
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.components.TriStateItem
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun LibrarySettingsDialog(
    onDismissRequest: () -> Unit,
    screenModel: LibrarySettingsScreenModel,
    category: Category?,
    // SY -->
    hasCategories: Boolean,
    // SY <--
    // KMK -->
    categories: List<Category>,
    // KMK <--
) {
    var page by rememberSaveable { mutableStateOf(0) }
    DetentSheet(visible = true, title = "Library options", onDismissRequest = onDismissRequest) {
        SegmentedControl(
            options = listOf(
                stringResource(MR.strings.action_filter),
                stringResource(MR.strings.action_sort),
                stringResource(MR.strings.action_display),
                stringResource(SYMR.strings.group),
            ),
            selectedIndex = page,
            onSelect = { page = it },
        )
        Column {
            when (page) {
                0 -> FilterPage(
                    screenModel = screenModel,
                    // KMK -->
                    categories = categories,
                    // KMK <--
                )
                1 -> SortPage(
                    category = category,
                    screenModel = screenModel,
                )
                2 -> DisplayPage(
                    screenModel = screenModel,
                )
                // SY -->
                3 -> GroupPage(
                    screenModel = screenModel,
                    hasCategories = hasCategories,
                )
                // SY <--
            }
        }
    }
}

@Suppress("UnusedReceiverParameter")
@Composable
private fun ColumnScope.FilterPage(
    screenModel: LibrarySettingsScreenModel,
    categories: List<Category>,
) {
    val filterDownloaded by screenModel.libraryPreferences.filterDownloaded().collectAsState()
    val downloadedOnly by screenModel.preferences.downloadedOnly().collectAsState()
    val autoUpdateMangaRestrictions by screenModel.libraryPreferences.autoUpdateMangaRestrictions().collectAsState()

    LibraryFilterRow(label = stringResource(MR.strings.label_downloaded),
    state = if (downloadedOnly) {
        TriState.ENABLED_IS
    } else {
        filterDownloaded
    },
    enabled = !downloadedOnly,
    onClick = { screenModel.toggleFilter(LibraryPreferences::filterDownloaded) },)
    val filterUnread by screenModel.libraryPreferences.filterUnread().collectAsState()
    LibraryFilterRow(label = stringResource(MR.strings.action_filter_unread),
    state = filterUnread,
    onClick = { screenModel.toggleFilter(LibraryPreferences::filterUnread) },)
    val filterStarted by screenModel.libraryPreferences.filterStarted().collectAsState()
    LibraryFilterRow(label = stringResource(MR.strings.label_started),
    state = filterStarted,
    onClick = { screenModel.toggleFilter(LibraryPreferences::filterStarted) },)
    val filterBookmarked by screenModel.libraryPreferences.filterBookmarked().collectAsState()
    LibraryFilterRow(label = stringResource(MR.strings.action_filter_bookmarked),
    state = filterBookmarked,
    onClick = { screenModel.toggleFilter(LibraryPreferences::filterBookmarked) },)
    val filterCompleted by screenModel.libraryPreferences.filterCompleted().collectAsState()
    LibraryFilterRow(label = stringResource(MR.strings.completed),
    state = filterCompleted,
    onClick = { screenModel.toggleFilter(LibraryPreferences::filterCompleted) },)
    val filterMerged by screenModel.libraryPreferences.filterMerged().collectAsState()
    LibraryFilterRow(
        label = "Merged titles",
        state = filterMerged,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterMerged) },
    )
    val filterMissing by screenModel.libraryPreferences.filterMissingChapters().collectAsState()
    LibraryFilterRow(
        label = "Missing chapters",
        state = filterMissing,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterMissingChapters) },
    )
    val filterUnavailable by screenModel.libraryPreferences.filterUnavailableChapters().collectAsState()
    LibraryFilterRow(
        label = "Unavailable chapters",
        state = filterUnavailable,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterUnavailableChapters) },
    )
    GroupedRow(
        title = "Chapter availability",
        subtitle = "Based on stored chapters and current scanlator exclusions; refresh titles to update. Numbering gaps are estimates, not a network check.",
    )
    val scanlatorFlow = remember { ScanlatorFilterOption.observe() }
    val scanlatorOption by scanlatorFlow.collectAsState(initial = ScanlatorFilterOption.ANY)
    val scope = rememberCoroutineScope()
    GroupedRow(
        title = "Excluded scanlator matching",
        subtitle = if (scanlatorOption == ScanlatorFilterOption.ALL) {
            "Hide collaborations only when all credited groups are excluded"
        } else {
            "Hide collaborations when any credited group is excluded"
        },
        value = scanlatorOption.name,
        onClick = {
            scope.launch {
                ScanlatorFilterOption.set(
                    if (scanlatorOption == ScanlatorFilterOption.ALL) ScanlatorFilterOption.ANY else ScanlatorFilterOption.ALL,
                )
            }
        },
    )
    // TODO: re-enable when custom intervals are ready for stable
    if ((!isReleaseBuildType) && LibraryPreferences.MANGA_OUTSIDE_RELEASE_PERIOD in autoUpdateMangaRestrictions) {
        val filterIntervalCustom by screenModel.libraryPreferences.filterIntervalCustom().collectAsState()
        LibraryFilterRow(label = stringResource(MR.strings.action_filter_interval_custom),
        state = filterIntervalCustom,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterIntervalCustom) },)
    }
    // SY -->
    val filterLewd by screenModel.libraryPreferences.filterLewd().collectAsState()
    LibraryFilterRow(label = stringResource(SYMR.strings.lewd),
    state = filterLewd,
    onClick = { screenModel.toggleFilter(LibraryPreferences::filterLewd) },)
    // SY <--

    // KMK -->
    CategoriesFilter(
        libraryPreferences = screenModel.libraryPreferences,
        categories = categories,
    )
    // KMK <--

    val trackers by screenModel.trackersFlow.collectAsState()
    when (trackers.size) {
        0 -> {
            // No trackers
        }
        1 -> {
            val service = trackers[0]
            val filterTracker by screenModel.libraryPreferences.filterTracking(service.id.toInt()).collectAsState()
            LibraryFilterRow(label = stringResource(MR.strings.action_filter_tracked),
            state = filterTracker,
            onClick = { screenModel.toggleTracker(service.id.toInt()) },)
        }
        else -> {
            LibraryOptionsHeading(MR.strings.action_filter_tracked)
            trackers.map { service ->
                val filterTracker by screenModel.libraryPreferences.filterTracking(service.id.toInt()).collectAsState()
                LibraryFilterRow(label = service.name,
                state = filterTracker,
                onClick = { screenModel.toggleTracker(service.id.toInt()) },)
            }
        }
    }
}

@Suppress("UnusedReceiverParameter")
@Composable
private fun ColumnScope.SortPage(
    category: Category?,
    screenModel: LibrarySettingsScreenModel,
) {
    val trackers by screenModel.trackersFlow.collectAsState()
    // SY -->
    val globalSortMode by screenModel.libraryPreferences.sortingMode().collectAsState()
    val sortingMode = if (screenModel.grouping == LibraryGroup.BY_DEFAULT) {
        category.sort.type
    } else {
        globalSortMode.type
    }
    val sortDescending = if (screenModel.grouping == LibraryGroup.BY_DEFAULT) {
        !category.sort.isAscending
    } else {
        !globalSortMode.isAscending
    }
    val hasSortTags by remember {
        screenModel.libraryPreferences.sortTagsForLibrary().changes()
            .map { it.isNotEmpty() }
    }.collectAsState(initial = screenModel.libraryPreferences.sortTagsForLibrary().get().isNotEmpty())
    // SY <--

    val options = remember(trackers.isEmpty()/* SY --> */, hasSortTags/* SY <-- */) {
        val trackerMeanPair = if (trackers.isNotEmpty()) {
            MR.strings.action_sort_tracker_score to LibrarySort.Type.TrackerMean
        } else {
            null
        }
        // SY -->
        val tagSortPair = if (hasSortTags) {
            SYMR.strings.tag_sorting to LibrarySort.Type.TagList
        } else {
            null
        }
        // SY <--
        listOfNotNull(
            MR.strings.action_sort_alpha to LibrarySort.Type.Alphabetical,
            MR.strings.action_sort_total to LibrarySort.Type.TotalChapters,
            MR.strings.action_sort_last_read to LibrarySort.Type.LastRead,
            MR.strings.action_sort_last_manga_update to LibrarySort.Type.LastUpdate,
            MR.strings.action_sort_unread_count to LibrarySort.Type.UnreadCount,
            MR.strings.action_sort_latest_chapter to LibrarySort.Type.LatestChapter,
            MR.strings.action_sort_chapter_fetch_date to LibrarySort.Type.ChapterFetchDate,
            MR.strings.action_sort_date_added to LibrarySort.Type.DateAdded,
            trackerMeanPair,
            // SY -->
            tagSortPair,
            // SY <--
            MR.strings.action_sort_random to LibrarySort.Type.Random,
        )
    }

    options.map { (titleRes, mode) ->
        if (mode == LibrarySort.Type.Random) {
            LibrarySortRow(
                label = stringResource(titleRes),
                indicator = if (sortingMode == LibrarySort.Type.Random) "↻" else null,
                onClick = { screenModel.setSort(category, mode, LibrarySort.Direction.Ascending) },
            )
            return@map
        }
        LibrarySortRow(
            label = stringResource(titleRes),
            indicator = if (sortingMode == mode) { if (sortDescending) "↓" else "↑" } else null,
            onClick = {
                val isTogglingDirection = sortingMode == mode
                val direction = when {
                    isTogglingDirection -> if (sortDescending) {
                        LibrarySort.Direction.Ascending
                    } else {
                        LibrarySort.Direction.Descending
                    }
                    else -> if (sortDescending) {
                        LibrarySort.Direction.Descending
                    } else {
                        LibrarySort.Direction.Ascending
                    }
                }
                screenModel.setSort(category, mode, direction)
            },
        )
    }
}

private val displayModes = listOf(
    MR.strings.action_display_grid to LibraryDisplayMode.CompactGrid,
    MR.strings.action_display_comfortable_grid to LibraryDisplayMode.ComfortableGrid,
    MR.strings.action_display_list to LibraryDisplayMode.List,
    MR.strings.action_display_cover_only_grid to LibraryDisplayMode.CoverOnlyGrid,
    // KMK -->
    KMR.strings.action_display_comfortable_grid_panorama to LibraryDisplayMode.ComfortableGridPanorama,
    // KMK <--
)

@Suppress("UnusedReceiverParameter")
@Composable
private fun ColumnScope.DisplayPage(
    screenModel: LibrarySettingsScreenModel,
) {
    val displayMode by screenModel.libraryPreferences.displayMode().collectAsState()
    Column {
        displayModes.forEach { (titleRes, mode) ->
            LibrarySortRow(
                label = stringResource(titleRes),
                indicator = if (displayMode == mode) "✓" else null,
                onClick = { screenModel.setDisplayMode(mode) },
            )
        }
    }

    if (displayMode != LibraryDisplayMode.List) {
        val configuration = LocalConfiguration.current
        val columnPreference = remember(configuration.orientation) {
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                screenModel.libraryPreferences.landscapeColumns()
            } else {
                screenModel.libraryPreferences.portraitColumns()
            }
        }
        val columns by columnPreference.collectAsState()
        TsuzukiSliderRow(
            title = stringResource(MR.strings.pref_library_columns),
            value = columns,
            valueString = if (columns > 0) columns.toString() else stringResource(MR.strings.label_auto),
            range = 0..10,
            enabled = true,
            onChange = columnPreference::set,
        )
    }

    LibraryOptionsHeading(MR.strings.overlay_header)
    LibraryToggleRow(label = stringResource(MR.strings.action_display_download_badge),
    pref = screenModel.libraryPreferences.downloadBadge(),)
    LibraryToggleRow(label = stringResource(MR.strings.action_display_unread_badge),
    pref = screenModel.libraryPreferences.unreadBadge(),)
    LibraryToggleRow(label = stringResource(MR.strings.action_display_local_badge),
    pref = screenModel.libraryPreferences.localBadge(),)
    LibraryToggleRow(label = stringResource(MR.strings.action_display_language_badge),
    pref = screenModel.libraryPreferences.languageBadge(),)
    // KMK -->
    val showLang by screenModel.libraryPreferences.languageBadge().collectAsState()
    if (showLang) {
        LibraryToggleRow(label = stringResource(KMR.strings.action_display_language_icon),
        pref = screenModel.libraryPreferences.useLangIcon(),)
    }
    LibraryToggleRow(label = stringResource(KMR.strings.action_display_source_badge),
    pref = screenModel.libraryPreferences.sourceBadge(),)
    // KMK <--
    LibraryToggleRow(label = stringResource(MR.strings.action_display_show_continue_reading_button),
    pref = screenModel.libraryPreferences.showContinueReadingButton(),)

    LibraryOptionsHeading(MR.strings.tabs_header)
    LibraryToggleRow(label = stringResource(MR.strings.action_display_show_tabs),
    pref = screenModel.libraryPreferences.categoryTabs(),)
    // KMK -->
    LibraryToggleRow(label = stringResource(KMR.strings.action_show_hidden_categories),
    pref = screenModel.libraryPreferences.showHiddenCategories(),)
    // KMK <--
    LibraryToggleRow(label = stringResource(MR.strings.action_display_show_number_of_items),
    pref = screenModel.libraryPreferences.categoryNumberOfItems(),)
}

// SY -->
data class GroupMode(
    val int: Int,
    val nameRes: StringResource,
    val drawableRes: Int,
)

private fun groupTypeDrawableRes(type: Int): Int {
    return when (type) {
        LibraryGroup.BY_STATUS -> R.drawable.ic_progress_clock_24dp
        LibraryGroup.BY_TRACK_STATUS -> R.drawable.ic_sync_24dp
        LibraryGroup.BY_SOURCE -> R.drawable.ic_browse_filled_24dp
        LibraryGroup.UNGROUPED -> R.drawable.ic_ungroup_24dp
        else -> R.drawable.ic_label_24dp
    }
}

@Suppress("UnusedReceiverParameter")
@Composable
private fun ColumnScope.GroupPage(
    screenModel: LibrarySettingsScreenModel,
    hasCategories: Boolean,
) {
    val trackers by screenModel.trackersFlow.collectAsState()
    val groups = remember(hasCategories, trackers) {
        buildList {
            add(LibraryGroup.BY_DEFAULT)
            add(LibraryGroup.BY_SOURCE)
            add(LibraryGroup.BY_STATUS)
            if (trackers.isNotEmpty()) {
                add(LibraryGroup.BY_TRACK_STATUS)
            }
            if (hasCategories || screenModel.grouping == LibraryGroup.UNGROUPED) {
                add(LibraryGroup.UNGROUPED)
            }
        }.map {
            GroupMode(
                it,
                LibraryGroup.groupTypeStringRes(it),
                groupTypeDrawableRes(it),
            )
        }.toImmutableList()
    }

    groups.fastForEach {
        LibrarySortRow(
            label = stringResource(it.nameRes),
            indicator = if (it.int == screenModel.grouping) "✓" else null,
            onClick = { screenModel.setGrouping(it.int) },
        )
    }
}
// SY <--

// KMK -->
@Composable
private fun CategoriesFilter(
    libraryPreferences: LibraryPreferences,
    categories: List<Category>,
) {
    val filterCategories by libraryPreferences.filterCategories().collectAsState()

    val filterCategoriesInclude = libraryPreferences.filterCategoriesInclude()
    val filterCategoriesExclude = libraryPreferences.filterCategoriesExclude()
    val included by filterCategoriesInclude.collectAsState()
    val excluded by filterCategoriesExclude.collectAsState()

    var showCategoriesDialog by rememberSaveable { mutableStateOf(false) }
    if (showCategoriesDialog) {
        TriStateListDialog(
            title = stringResource(MR.strings.categories),
            message = stringResource(KMR.strings.pref_library_filter_categories_details),
            items = categories,
            initialChecked = included.mapNotNull { id -> categories.find { it.id.toString() == id } },
            initialInversed = excluded.mapNotNull { id -> categories.find { it.id.toString() == id } },
            itemLabel = { it.visualName },
            onDismissRequest = { showCategoriesDialog = false },
            onValueChanged = { newIncluded, newExcluded ->
                filterCategoriesInclude.set(newIncluded.map { it.id.toString() }.toSet())
                filterCategoriesExclude.set(newExcluded.map { it.id.toString() }.toSet())
                showCategoriesDialog = false
            },
        )
    }

    PreferenceRow(
        title = stringResource(MR.strings.categories),
        value = if (filterCategories) "On" else "Off",
        onClick = { libraryPreferences.filterCategories().toggle() },
        trailing = {
            PillButton(stringResource(MR.strings.action_edit), { showCategoriesDialog = true }, prominent = false)
        },
    )
}
// KMK <--

@Composable
private fun LibrarySortRow(label: String, indicator: String?, onClick: () -> Unit) {
    GroupedRow(title = label, value = indicator, onClick = onClick)
}

@Composable
private fun LibraryFilterRow(label: String, state: TriState, enabled: Boolean = true, onClick: () -> Unit) {
    GroupedRow(
        title = label,
        value = when (state) {
            TriState.DISABLED -> "Any"
            TriState.ENABLED_IS -> "Only"
            TriState.ENABLED_NOT -> "Exclude"
        },
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun LibraryToggleRow(label: String, pref: tachiyomi.core.common.preference.Preference<Boolean>) {
    val checked by pref.collectAsState()
    GroupedRow(title = label, checked = checked, onCheckedChange = pref::set)
}

@Composable
private fun LibraryOptionsHeading(label: StringResource) {
    TsuzukiText(
        stringResource(label),
        Modifier.padding(vertical = 12.dp),
        TsuzukiTheme.typography.headline,
    )
}
