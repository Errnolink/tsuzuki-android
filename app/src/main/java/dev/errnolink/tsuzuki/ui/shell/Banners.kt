package dev.errnolink.tsuzuki.ui.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.GlassSurface
import dev.errnolink.tsuzuki.designsystem.Banner
import dev.errnolink.tsuzuki.designsystem.NoticeTone
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.math.RoundingMode
import java.text.NumberFormat

val LocalAppStateBottomPadding = androidx.compose.runtime.staticCompositionLocalOf { 0.dp }

@Composable
fun WarningBanner(
    textRes: StringResource,
    modifier: Modifier = Modifier,
) {
    Banner(
        stringResource(textRes),
        modifier = modifier.padding(horizontal = TsuzukiTheme.spacing.gutter, vertical = TsuzukiTheme.spacing.small),
        tone = NoticeTone.Error,
    )
}

private val percentFormatter = NumberFormat.getPercentInstance().apply {
    roundingMode = RoundingMode.DOWN
    maximumFractionDigits = 0
}

@Composable
fun AppStateBanners(
    downloadedOnlyMode: Boolean,
    incognitoMode: Boolean,
    indexing: Boolean,
    restoring: Boolean,
    syncing: Boolean,
    updating: Boolean,
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    if (!downloadedOnlyMode && !incognitoMode && !indexing && !restoring && !syncing && !updating) return
    val labels = buildList {
        if (downloadedOnlyMode) add(stringResource(MR.strings.label_downloaded_only))
        if (incognitoMode) add(stringResource(MR.strings.pref_incognito_mode))
        val activity = when {
            updating -> stringResource(MR.strings.updating_library)
            syncing -> stringResource(MR.strings.syncing_library)
            restoring -> stringResource(MR.strings.restoring_backup)
            indexing -> stringResource(MR.strings.download_notifier_cache_renewal)
            else -> null
        }
        if (activity != null) add(activity + (progress?.let { " ${percentFormatter.format(it)}" } ?: ""))
    }
    GlassSurface(context = null, modifier = modifier) {
        TsuzukiText(
            labels.joinToString(" · "),
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = TsuzukiTheme.typography.caption2,
            color = TsuzukiTheme.colors.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

