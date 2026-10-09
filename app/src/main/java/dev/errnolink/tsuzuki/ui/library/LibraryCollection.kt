package dev.errnolink.tsuzuki.ui.library

import exh.source.MERGED_SOURCE_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.manga.model.applyFilter

internal fun <T> Flow<T>.whileLibraryActive(
    visible: Flow<Boolean>,
    restoring: Flow<Boolean>,
    syncing: Flow<Boolean>,
): Flow<T> = combine(visible, restoring, syncing) { isVisible, isRestoring, isSyncing ->
    isVisible && !isRestoring && !isSyncing
}.distinctUntilChanged().flatMapLatest { active ->
    if (active) this else emptyFlow()
}

internal fun matchesMergedFilter(filter: TriState, sourceId: Long): Boolean =
    applyFilter(filter) { sourceId == MERGED_SOURCE_ID }

internal fun matchesMissingChaptersFilter(filter: TriState, missingChapters: Long): Boolean =
    applyFilter(filter) { missingChapters > 0 }

internal fun matchesUnavailableChaptersFilter(filter: TriState, unavailableChapters: Long): Boolean =
    applyFilter(filter) { unavailableChapters > 0 }
