package dev.errnolink.tsuzuki.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import eu.kanade.tachiyomi.R
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onRestoreBackup: () -> Unit,
) {
    val steps = remember {
        listOf(
            ThemeStep(),
            StorageStep(),
            PermissionStep(),
            GuidesStep(onRestoreBackup = onRestoreBackup),
        )
    }

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val isLastStep = currentStep == steps.lastIndex

    BackHandler(enabled = currentStep != 0, onBack = { currentStep-- })

        Box(
            Modifier
                .fillMaxSize()
                .background(TsuzukiTheme.colors.background)
                .statusBarsPadding(),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
            ) {
                Spacer(Modifier.height(24.dp))
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    androidx.compose.material3.Icon(
                        painter = painterResource(R.drawable.ic_tsuzuki),
                        contentDescription = null,
                        tint = TsuzukiTheme.colors.text,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    TsuzukiText(
                        stringResource(MR.strings.onboarding_heading),
                        Modifier.fillMaxWidth(),
                        TsuzukiTheme.typography.title1,
                    )
                    Spacer(Modifier.height(8.dp))
                    TsuzukiText(
                        stringResource(MR.strings.onboarding_description),
                        Modifier.fillMaxWidth(),
                        TsuzukiTheme.typography.callout,
                        TsuzukiTheme.colors.secondary,
                    )
                }
                Spacer(Modifier.height(20.dp))
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Crossfade(
                        targetState = currentStep,
                        animationSpec = if (TsuzukiTheme.motion.reduced) tween(0) else tween(150),
                        label = "onboarding-step",
                    ) {
                        steps[it].Content()
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    steps.indices.forEach { index ->
                        Box(
                            Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (index == currentStep) 8.dp else 6.dp)
                                .background(
                                    if (index == currentStep) TsuzukiTheme.colors.accent else TsuzukiTheme.colors.separator,
                                    CircleShape,
                                ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                PillButton(
                    label = stringResource(
                        if (isLastStep) MR.strings.onboarding_action_finish else MR.strings.onboarding_action_next,
                    ),
                    onClick = {
                        if (isLastStep) {
                            onComplete()
                        } else {
                            currentStep++
                        }
                    },
                    enabled = steps[currentStep].isComplete,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                )
            }
        }
}
