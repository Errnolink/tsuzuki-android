package dev.errnolink.tsuzuki.ui.settings
import eu.kanade.presentation.more.settings.screen.about.AboutScreen

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.errnolink.tsuzuki.designsystem.InsetGroupedList
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.more.settings.screen.SettingsBrowseScreen
import eu.kanade.presentation.more.settings.screen.SettingsDownloadScreen
import eu.kanade.presentation.more.settings.screen.SettingsLibraryScreen
import eu.kanade.presentation.more.settings.screen.SettingsReaderScreen
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import exh.assets.EhAssets

import exh.assets.ehassets.MangadexLogo
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource

object TsuzukiSettingsMainScreen : Screen() {
    @Suppress("unused")
    private fun readResolve(): Any = TsuzukiSettingsMainScreen

    @Composable
    override fun Content() {
        Content(twoPane = false)
    }

    @Composable
    fun Content(twoPane: Boolean) {
        val navigator = LocalNavigator.currentOrThrow
        val backPress = LocalBackPress.currentOrThrow
        val state = rememberLazyListState()
        val visibleSections = sections.map { section ->
            Section(section.items.filter { it.screen !is SearchableSettings || it.screen.isEnabled() })
        }.filter { it.items.isNotEmpty() }

        SettingsScaffold(
            title = stringResource(MR.strings.label_settings),
            navigateUp = backPress::invoke,
            state = state,
            actions = {
                SettingsIconButton(stringResource(MR.strings.action_search), {
                    navigator.navigate(TsuzukiSettingsSearchScreen(), twoPane)
                }) {
                    Icon(Icons.Outlined.Search, null, Modifier.size(22.dp), tint = TsuzukiTheme.colors.text)
                }
            },
        ) {
            visibleSections.forEach { section ->
                item(key = "settings-main-${section.items.first().titleRes.hashCode()}") {
                    InsetGroupedList {
                        section.items.forEachIndexed { i, item ->
                            if (i > 0) GroupDivider()
                            PreferenceRow(
                                title = stringResource(item.titleRes),
                                subtitle = item.formatSubtitle(),
                                chevron = true,
                                enabled = true,
                                divider = false,
                                leading = {
                                    Icon(item.icon, null, Modifier.size(24.dp), tint = TsuzukiTheme.colors.secondary)
                                },
                                onClick = { navigator.navigate(item.screen, twoPane) },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun Navigator.navigate(screen: VoyagerScreen, twoPane: Boolean) {
        if (twoPane) replaceAll(screen) else push(screen)
    }

    private data class Item(
        val titleRes: StringResource,
        val subtitleRes: StringResource,
        val formatSubtitle: @Composable () -> String = { stringResource(subtitleRes) },
        val icon: ImageVector,
        val screen: VoyagerScreen,
    )

    private data class Section(val items: List<Item>)

    private val sections = listOf(
        Section(
            items = listOf(
                Item(
                    titleRes = MR.strings.pref_category_appearance,
                    subtitleRes = MR.strings.pref_appearance_summary,
                    icon = Icons.Outlined.Palette,
                    screen = TsuzukiSettingsAppearanceScreen,
                ),
                Item(
                    titleRes = MR.strings.pref_category_library,
                    subtitleRes = MR.strings.pref_library_summary,
                    icon = Icons.Outlined.CollectionsBookmark,
                    screen = SettingsLibraryScreen,
                ),
                Item(
                    titleRes = MR.strings.pref_category_reader,
                    subtitleRes = MR.strings.pref_reader_summary,
                    icon = Icons.AutoMirrored.Outlined.ChromeReaderMode,
                    screen = SettingsReaderScreen,
                ),
                Item(
                    titleRes = MR.strings.pref_category_downloads,
                    subtitleRes = MR.strings.pref_downloads_summary,
                    icon = Icons.Outlined.GetApp,
                    screen = SettingsDownloadScreen,
                ),
            ),
        ),
        Section(
            items = listOf(
                Item(
                    titleRes = MR.strings.pref_category_tracking,
                    subtitleRes = MR.strings.pref_tracking_summary,
                    icon = Icons.Outlined.Sync,
                    screen = TsuzukiSettingsTrackingScreen,
                ),
                Item(
                    titleRes = MR.strings.browse,
                    subtitleRes = MR.strings.pref_browse_summary,
                    icon = Icons.Outlined.Explore,
                    screen = SettingsBrowseScreen,
                ),
            ),
        ),
        Section(
            items = listOf(
                Item(
                    titleRes = MR.strings.label_data_storage,
                    subtitleRes = MR.strings.pref_backup_summary,
                    icon = Icons.Outlined.SettingsBackupRestore,
                    screen = TsuzukiSettingsDataScreen,
                ),
                Item(
                    titleRes = MR.strings.pref_category_security,
                    subtitleRes = MR.strings.pref_security_summary,
                    icon = Icons.Outlined.Security,
                    screen = TsuzukiSettingsSecurityScreen,
                ),
                // SY -->
                Item(
                    titleRes = SYMR.strings.pref_category_mangadex,
                    subtitleRes = SYMR.strings.pref_mangadex_summary,
                    icon = EhAssets.MangadexLogo,
                    screen = TsuzukiSettingsMangadexScreen,
                ),
                // SY <--
            ),
        ),
        Section(
            items = listOf(
                Item(
                    titleRes = MR.strings.pref_category_advanced,
                    subtitleRes = MR.strings.pref_advanced_summary,
                    icon = Icons.Outlined.Code,
                    screen = TsuzukiSettingsAdvancedScreen,
                ),
                Item(
                    titleRes = MR.strings.pref_category_about,
                    subtitleRes = StringResource(0),
                    formatSubtitle = {
                        "${stringResource(MR.strings.app_name)} ${AboutScreen.getVersionName(withBuildDate = false)}"
                    },
                    icon = Icons.Outlined.Info,
                    screen = AboutScreen(),
                ),
            ),
        ),
    )
}
