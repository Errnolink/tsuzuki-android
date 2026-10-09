package dev.errnolink.tsuzuki.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import eu.kanade.presentation.more.onboarding.OnboardingStep

internal class GuidesStep(
    private val onRestoreBackup: () -> Unit,
) : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val handler = LocalUriHandler.current

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TsuzukiText(
                stringResource(MR.strings.onboarding_guides_new_user, stringResource(MR.strings.app_name)),
                Modifier.padding(horizontal = 4.dp),
                TsuzukiTheme.typography.callout,
                TsuzukiTheme.colors.secondary,
            )
            PillButton(
                label = stringResource(MR.strings.getting_started_guide),
                onClick = { handler.openUri(GETTING_STARTED_URL) },
                prominent = false,
                modifier = Modifier.fillMaxWidth(),
            )

            TsuzukiText(
                stringResource(MR.strings.onboarding_guides_returning_user, stringResource(MR.strings.app_name)),
                Modifier.padding(horizontal = 4.dp),
                TsuzukiTheme.typography.callout,
                TsuzukiTheme.colors.secondary,
            )
            PillButton(
                label = stringResource(MR.strings.pref_restore_backup),
                onClick = onRestoreBackup,
                prominent = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

const val GETTING_STARTED_URL = "https://komikku-app.github.io/docs/guides/getting-started"
