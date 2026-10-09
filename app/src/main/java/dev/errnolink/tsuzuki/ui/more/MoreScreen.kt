package dev.errnolink.tsuzuki.ui.more

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.GroupedRow
import dev.errnolink.tsuzuki.designsystem.InsetGroupedList
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.ui.settings.GroupDivider
import dev.errnolink.tsuzuki.ui.settings.SettingsScaffold
import dev.errnolink.tsuzuki.ui.settings.PreferenceRow
import dev.errnolink.tsuzuki.ui.settings.SwitchPreferenceWidget
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import tachiyomi.core.common.Constants
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector

@Composable
fun MoreScreen(
    downloadQueueStateProvider: () -> DownloadQueueState,
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    // SY -->
    showNavUpdates: Boolean,
    showNavHistory: Boolean,
    // SY <--
    onClickDownloadQueue: () -> Unit,
    onClickCategories: () -> Unit,
    onClickStats: () -> Unit,
    onClickBackupAndRestore: () -> Unit,
    onClickSettings: () -> Unit,
    onClickAbout: () -> Unit,
    onClickUpdates: () -> Unit,
    onClickHistory: () -> Unit,
    // KMK -->
    onClickLibraryUpdateErrors: () -> Unit,
    // KMK <--
) {
    val uriHandler = LocalUriHandler.current

    SettingsScaffold(
        title = stringResource(MR.strings.label_more),
        navigateUp = null,
    ) {
        item(key = "more-modes") {
            InsetGroupedList {
                SwitchPreferenceWidget(
                    title = stringResource(MR.strings.label_downloaded_only),
                    subtitle = stringResource(MR.strings.downloaded_only_summary),
                    icon = Icons.Outlined.CloudOff,
                    checked = downloadedOnly,
                    onCheckedChanged = onDownloadedOnlyChange,
                    divider = false,
                )
                GroupDivider()
                SwitchPreferenceWidget(
                    title = stringResource(MR.strings.pref_incognito_mode),
                    subtitle = stringResource(MR.strings.pref_incognito_mode_summary),
                    icon = rememberAnimatedVectorPainter(
                        AnimatedImageVector.animatedVectorResource(R.drawable.anim_incognito),
                        incognitoMode,
                    ),
                    checked = incognitoMode,
                    onCheckedChanged = onIncognitoModeChange,
                    divider = false,
                )
            }
        }
        item(key = "more-library") {
            InsetGroupedList {
                if (!showNavUpdates) {
                    MoreRow(
                        title = stringResource(MR.strings.label_recent_updates),
                        icon = Icons.Outlined.NewReleases,
                        divider = true,
                        onClick = onClickUpdates,
                    )
                }
                if (!showNavHistory) {
                    MoreRow(
                        title = stringResource(MR.strings.label_recent_manga),
                        icon = Icons.Outlined.History,
                        divider = true,
                        onClick = onClickHistory,
                    )
                }
                val downloadQueueState = downloadQueueStateProvider()
                MoreRow(
                    title = stringResource(MR.strings.label_download_queue),
                    value = when (downloadQueueState) {
                        DownloadQueueState.Stopped -> null
                        is DownloadQueueState.Paused -> {
                            val pending = downloadQueueState.pending
                            if (pending == 0) {
                                stringResource(MR.strings.paused)
                            } else {
                                "${stringResource(MR.strings.paused)} • ${
                                    pluralStringResource(
                                        MR.plurals.download_queue_summary,
                                        count = pending,
                                        pending,
                                    )
                                }"
                            }
                        }
                        is DownloadQueueState.Downloading -> {
                            val pending = downloadQueueState.pending
                            pluralStringResource(MR.plurals.download_queue_summary, count = pending, pending)
                        }
                    },
                    icon = Icons.Outlined.GetApp,
                    divider = true,
                    onClick = onClickDownloadQueue,
                )
                MoreRow(
                    title = stringResource(MR.strings.categories),
                    icon = Icons.AutoMirrored.Outlined.Label,
                    divider = true,
                    onClick = onClickCategories,
                )
                MoreRow(
                    title = stringResource(MR.strings.label_stats),
                    icon = Icons.Outlined.QueryStats,
                    divider = true,
                    onClick = onClickStats,
                )
                // KMK -->
                MoreRow(
                    title = stringResource(KMR.strings.option_label_library_update_errors),
                    icon = Icons.Outlined.NewReleases,
                    divider = true,
                    onClick = onClickLibraryUpdateErrors,
                )
                // KMK <--
            }
        }
        item(key = "more-backup") {
            InsetGroupedList {
                MoreRow(
                    title = stringResource(MR.strings.label_backup),
                    icon = Icons.Outlined.Storage,
                    divider = false,
                    onClick = onClickBackupAndRestore,
                )
            }
        }
        item(key = "more-about") {
            InsetGroupedList {
                MoreRow(
                    title = stringResource(MR.strings.label_settings),
                    icon = Icons.Outlined.Settings,
                    divider = true,
                    onClick = onClickSettings,
                )
                MoreRow(
                    title = stringResource(MR.strings.pref_category_about),
                    icon = Icons.Outlined.Info,
                    divider = true,
                    onClick = onClickAbout,
                )
                MoreRow(
                    title = stringResource(MR.strings.label_help),
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    divider = false,
                    onClick = { uriHandler.openUri(Constants.URL_HELP) },
                )
            }
        }
    }
}

@Composable
private fun MoreRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    divider: Boolean,
    value: String? = null,
    onClick: () -> Unit,
) {
    PreferenceRow(
        title = title,
        value = value,
        chevron = true,
        divider = divider,
        leading = {
            androidx.compose.material3.Icon(icon, null, Modifier.size(24.dp), tint = TsuzukiTheme.colors.secondary)
        },
        onClick = onClick,
    )
}
