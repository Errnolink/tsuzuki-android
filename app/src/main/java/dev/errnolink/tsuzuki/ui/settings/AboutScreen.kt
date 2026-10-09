package dev.errnolink.tsuzuki.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.errnolink.tsuzuki.designsystem.InsetGroupedList
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.more.LogoHeader
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.util.CrashLogUtil
import eu.kanade.tachiyomi.util.lang.toDateTimestampString
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.presentation.more.settings.screen.about.OpenSourceLicensesScreen
import eu.kanade.tachiyomi.util.system.isDebugBuildType
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LinkIcon
import tachiyomi.presentation.core.icons.CustomIcons
import tachiyomi.presentation.core.icons.Discord
import tachiyomi.presentation.core.icons.Github
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class TsuzukiAboutScreen : Screen() {
    @Suppress("unused")
    private fun readResolve(): Any = TsuzukiAboutScreen

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val handleBack = LocalBackPress.current
        val navigator = LocalNavigator.currentOrThrow
        SettingsScaffold(
            title = stringResource(MR.strings.pref_category_about),
            navigateUp = handleBack?.let { { it() } },
        ) {
            item(key = "about-header") {
                LogoHeader()
            }
            item(key = "about-rows") {
                InsetGroupedList {
                    PreferenceRow(
                        title = stringResource(MR.strings.version),
                        subtitle = getVersionName(withBuildDate = true),
                        divider = true,
                        onClick = {
                            val deviceInfo = CrashLogUtil(context).getDebugInfo()
                            context.copyToClipboard("Debug information", deviceInfo)
                        },
                    )
                    GroupDivider()
                    PreferenceRow(
                        title = stringResource(MR.strings.help_translate),
                        divider = true,
                        onClick = {
                            uriHandler.openUri(
                                "https://hosted.weblate.org/engage/komikku-app/",
                            )
                        },
                    )
                    GroupDivider()
                    PreferenceRow(
                        title = stringResource(MR.strings.licenses),
                        divider = true,
                        onClick = { navigator.push(OpenSourceLicensesScreen()) },
                    )
                    GroupDivider()
                    PreferenceRow(
                        title = stringResource(MR.strings.privacy_policy),
                        divider = false,
                        onClick = { uriHandler.openUri("https://komikku-app.github.io/privacy/") },
                    )
                }
            }
            item(key = "about-links") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    LinkIcon(
                        label = stringResource(MR.strings.website),
                        icon = Icons.Outlined.Public,
                        url = "https://komikku-app.github.io",
                    )
                    LinkIcon(
                        label = "Discord",
                        icon = CustomIcons.Discord,
                        url = "https://discord.gg/85jB7V5AJR",
                    )
                    LinkIcon(
                        label = "GitHub",
                        icon = CustomIcons.Github,
                        url = "https://github.com/komikku-app",
                    )
                }
            }
        }
    }

    companion object {
        fun getVersionName(withBuildDate: Boolean): String {
            return when {
                isDebugBuildType -> {
                    "Debug ${BuildConfig.COMMIT_SHA}".let {
                        if (withBuildDate) {
                            "$it (${getFormattedBuildTime()})"
                        } else {
                            it
                        }
                    }
                }

                isPreviewBuildType -> {
                    "Beta r${BuildConfig.COMMIT_COUNT}".let {
                        if (withBuildDate) {
                            "$it (${BuildConfig.COMMIT_SHA}, ${getFormattedBuildTime()})"
                        } else {
                            "$it (${BuildConfig.COMMIT_SHA})"
                        }
                    }
                }

                else -> {
                    "Stable ${BuildConfig.VERSION_NAME}".let {
                        if (withBuildDate) {
                            "$it (${getFormattedBuildTime()})"
                        } else {
                            it
                        }
                    }
                }
            }
        }

        internal fun getFormattedBuildTime(): String {
            return try {
                LocalDateTime.ofInstant(
                    Instant.parse(BuildConfig.BUILD_TIME),
                    ZoneId.systemDefault(),
                )
                    .toDateTimestampString(
                        UiPreferences.dateFormat(
                            Injekt.get<UiPreferences>().dateFormat().get(),
                        ),
                    )
            } catch (_: Exception) {
                BuildConfig.BUILD_TIME
            }
        }
    }
}
