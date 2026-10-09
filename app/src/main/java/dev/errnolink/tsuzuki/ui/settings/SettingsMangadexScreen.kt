package dev.errnolink.tsuzuki.ui.settings
import eu.kanade.presentation.more.settings.screen.SearchableSettings

import exh.md.MangaDexPreferences
import exh.md.utils.MdLang
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import exh.md.utils.MdConstants
import exh.md.utils.MdUtil
import kotlinx.collections.immutable.toImmutableMap
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object TsuzukiSettingsMangadexScreen : SearchableSettings {
    @Suppress("unused")
    private fun readResolve(): Any = TsuzukiSettingsMangadexScreen

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = SYMR.strings.pref_category_mangadex

    @Composable
    override fun Content() = TsuzukiPreferenceContent()

    override fun isEnabled(): Boolean = MdUtil.getEnabledMangaDexs(Injekt.get()).isNotEmpty()

    @Composable
    override fun getPreferences(): List<Preference> {
        val sourcePreferences: SourcePreferences = remember { Injekt.get() }
        val trackPreferences: TrackPreferences = remember { Injekt.get() }
        val mdex = remember { MdUtil.getEnabledMangaDex(sourcePreferences) } ?: return emptyList()
        val parity = remember { MangaDexPreferences() }
        val multiLanguage by parity.multiLanguage().collectAsState()
        val blockedGroups by parity.blockedGroups().collectAsState()
        val blockedUploaders by parity.blockedUploaders().collectAsState()

        return listOf(
            loginPreference(mdex, trackPreferences),
            preferredMangaDexId(sourcePreferences),
            Preference.PreferenceItem.SwitchPreference(parity.readingSync(), "Sync chapter read markers", "Push and pull read chapters with MangaDex. Off by default."),
            Preference.PreferenceItem.ListPreference(
                parity.autoAddToLibrary(),
                mapOf(0 to "Do not follow automatically", 1 to "Plan to read", 2 to "On hold", 3 to "Reading").toImmutableMap(),
                "On adding to library",
            ),
            Preference.PreferenceItem.SwitchPreference(parity.showRatingFilter(), "Show content-rating search filter"),
            Preference.PreferenceItem.MultiSelectListPreference(
                parity.visibleRatings(), MangaDexPreferences.ratings.associateWith { it.replaceFirstChar(Char::uppercase) }.toImmutableMap(),
                "Visible content ratings", onValueChanged = { it.isNotEmpty() },
            ),
            Preference.PreferenceItem.SwitchPreference(parity.includeUnavailable(), "Include unavailable chapters", "Keep unavailable entries visible; reading them shows an explanation."),
            Preference.PreferenceItem.SwitchPreference(parity.multiLanguage(), "Multi-language chapter list", "Fetch selected languages through one MangaDex source. Refresh chapters after changing."),
            Preference.PreferenceItem.MultiSelectListPreference(
                parity.chapterLanguages(),
                MdLang.entries.associate { it.lang to it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase) }.toImmutableMap(),
                "Chapter languages", enabled = multiLanguage, onValueChanged = { it.isNotEmpty() },
            ),
            Preference.PreferenceItem.MultiSelectListPreference(
                parity.blockedGroups(), blockedGroups.associateWith { parity.blockName(it).get() }.toImmutableMap(),
                "Blocked groups", subtitle = "Uncheck a group to unblock it",
            ),
            Preference.PreferenceItem.MultiSelectListPreference(
                parity.blockedUploaders(), blockedUploaders.associateWith { parity.blockName(it).get() }.toImmutableMap(),
                "Blocked uploaders", subtitle = "Uncheck an uploader to unblock them",
            ),
            Preference.PreferenceItem.SwitchPreference(parity.dynamicCovers(), "Dynamic volume covers", "Use artwork from the last-read volume. Manually selected covers take priority."),
            Preference.PreferenceItem.MultiSelectListPreference(
                parity.autoTrackServices(),
                mapOf("al" to "AniList", "mal" to "MyAnimeList", "kt" to "Kitsu", "mu" to "MangaUpdates").toImmutableMap(),
                "Auto-add linked trackers",
            ),
            Preference.PreferenceItem.MultiSelectListPreference(
                parity.autoTrackRatings(), MangaDexPreferences.ratings.associateWith { it.replaceFirstChar(Char::uppercase) }.toImmutableMap(),
                "Auto-track content ratings", onValueChanged = { it.isNotEmpty() },
            ),
            syncMangaDexIntoThis(sourcePreferences),
            syncLibraryToMangaDex(),
        )
    }

    @Composable
    fun LogoutDialog(
        onDismissRequest: () -> Unit,
        onLogoutRequest: () -> Unit,
    ) {
        ConfirmSheet(
            visible = true,
            title = stringResource(MR.strings.logout),
            confirmLabel = stringResource(MR.strings.logout),
            destructive = true,
            onConfirm = onLogoutRequest,
            onDismissRequest = onDismissRequest,
        )
    }

    @Composable
    fun loginPreference(
        mdex: MangaDex,
        trackPreferences: TrackPreferences,
    ): Preference.PreferenceItem.CustomPreference {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val loggedIn by remember { trackPreferences.trackToken(mdex.mdList) }.collectAsState()
        var logoutDialogOpen by remember { mutableStateOf(false) }
        if (logoutDialogOpen) {
            LogoutDialog(
                onDismissRequest = { logoutDialogOpen = false },
                onLogoutRequest = {
                    logoutDialogOpen = false
                    scope.launchIO {
                        try {
                            if (mdex.logout()) {
                                withUIContext {
                                    context.toast(MR.strings.logout_success)
                                }
                            } else {
                                withUIContext {
                                    context.toast(MR.strings.unknown_error)
                                }
                            }
                        } catch (e: Exception) {
                            logcat(LogPriority.ERROR, e) { "Logout error" }
                            withUIContext {
                                context.toast(MR.strings.unknown_error)
                            }
                        }
                    }
                },
            )
        }
        return Preference.PreferenceItem.CustomPreference(
            title = mdex.name + " Login",
            content = {
                PreferenceRow(
                    title = mdex.name + " Login",
                    divider = false,
                    leading = {
                        Image(
                            imageVector = Icons.Outlined.PeopleAlt,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(dev.errnolink.tsuzuki.designsystem.TsuzukiTheme.colors.secondary),
                        )
                    },
                    onClick = if (loggedIn.isNotEmpty()) {
                        {
                            logoutDialogOpen = true
                        }
                    } else {
                        {
                            context.openInBrowser(
                                MdConstants.Login.authUrl(MdUtil.getPkceChallengeCode()),
                                forceDefaultBrowser = true,
                            )
                        }
                    },
                )
            },
        )
    }

    @Composable
    fun preferredMangaDexId(
        sourcePreferences: SourcePreferences,
    ): Preference.PreferenceItem.ListPreference<String> {
        return Preference.PreferenceItem.ListPreference(
            preference = sourcePreferences.preferredMangaDexId(),
            entries = MdUtil.getEnabledMangaDexs(sourcePreferences)
                .associate { it.id.toString() to it.toString() }
                .toImmutableMap(),
            title = stringResource(SYMR.strings.mangadex_preffered_source),
            subtitle = stringResource(SYMR.strings.mangadex_preffered_source_summary),
        )
    }

    @Composable
    fun SyncMangaDexDialog(
        onDismissRequest: () -> Unit,
        onSelectionConfirmed: (List<String>) -> Unit,
    ) {
        val resources = LocalResources.current
        val items = remember(resources) {
            resources.getStringArray(R.array.md_follows_options)
                .drop(1)
        }
        val selection = remember {
            List(items.size) { index ->
                index == 0 || index == 5
            }.toMutableStateList()
        }
        MultiChoiceSheet(
            visible = true,
            title = stringResource(SYMR.strings.mangadex_sync_follows_to_library),
            doneLabel = stringResource(MR.strings.action_ok),
            onDone = { onSelectionConfirmed(items.filterIndexed { index, _ -> selection[index] }) },
            options = items.mapIndexed { index, followOption ->
                Triple(followOption, selection.getOrNull(index) ?: false) { checked ->
                    selection[index] = checked
                }
            },
            onDismissRequest = onDismissRequest,
        )
    }

    @Composable
    fun syncMangaDexIntoThis(sourcePreferences: SourcePreferences): Preference.PreferenceItem.TextPreference {
        val context = LocalContext.current
        var dialogOpen by remember { mutableStateOf(false) }
        if (dialogOpen) {
            SyncMangaDexDialog(
                onDismissRequest = { dialogOpen = false },
                onSelectionConfirmed = { items ->
                    dialogOpen = false
                    val options = context.resources.getStringArray(R.array.md_follows_options).drop(1)
                    sourcePreferences.mangadexSyncToLibraryIndexes().set(
                        items.mapNotNull { item -> options.indexOf(item).takeIf { it >= 0 }?.let { (it + 1).toString() } }.toSet(),
                    )
                    LibraryUpdateJob.startNow(
                        context,
                        target = LibraryUpdateJob.Target.SYNC_FOLLOWS,
                    )
                },
            )
        }
        return Preference.PreferenceItem.TextPreference(
            title = stringResource(SYMR.strings.mangadex_sync_follows_to_library),
            subtitle = stringResource(SYMR.strings.mangadex_sync_follows_to_library_summary),
            onClick = { dialogOpen = true },
        )
    }

    @Composable
    fun syncLibraryToMangaDex(): Preference.PreferenceItem.TextPreference {
        val context = LocalContext.current
        return Preference.PreferenceItem.TextPreference(
            title = stringResource(SYMR.strings.mangadex_push_favorites_to_mangadex),
            subtitle = stringResource(SYMR.strings.mangadex_push_favorites_to_mangadex_summary),
            onClick = {
                LibraryUpdateJob.startNow(
                    context,
                    target = LibraryUpdateJob.Target.PUSH_FAVORITES,
                )
            },
        )
    }
}
