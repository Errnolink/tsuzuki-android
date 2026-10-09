package dev.errnolink.tsuzuki.ui.onboarding

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.InsetGroupedList
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import eu.kanade.presentation.more.settings.screen.SettingsDataScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import tachiyomi.domain.storage.service.StorageManager.Companion.directoryAccessible
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.presentation.more.onboarding.OnboardingStep

internal class StorageStep : OnboardingStep {

    private val storagePref = Injekt.get<StoragePreferences>().baseStorageDirectory()

    private var _isComplete by mutableStateOf(false)

    override val isComplete: Boolean
        get() = _isComplete

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val handler = LocalUriHandler.current

        val pickStorageLocation = SettingsDataScreen.storageLocationPicker(storagePref)

        val storageDir by storagePref.collectAsState()
        var locationValid by remember(storageDir) {
            mutableStateOf(directoryAccessible(context, storageDir))
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TsuzukiText(
                stringResource(
                    MR.strings.onboarding_storage_info,
                    stringResource(MR.strings.app_name),
                    SettingsDataScreen.storageLocationText(storagePref),
                ),
                Modifier.padding(horizontal = 4.dp),
                TsuzukiTheme.typography.callout,
                TsuzukiTheme.colors.secondary,
            )

            PillButton(
                label = stringResource(MR.strings.onboarding_storage_action_select),
                onClick = {
                    try {
                        pickStorageLocation.launch(null)
                    } catch (e: ActivityNotFoundException) {
                        context.toast(MR.strings.file_picker_error)
                    }
                },
                prominent = false,
                modifier = Modifier.fillMaxWidth(),
            )

            TsuzukiText(
                stringResource(MR.strings.onboarding_storage_help_info, stringResource(MR.strings.app_name)),
                Modifier.padding(horizontal = 4.dp),
                TsuzukiTheme.typography.callout,
                TsuzukiTheme.colors.secondary,
            )
            PillButton(
                label = stringResource(MR.strings.onboarding_storage_help_action),
                onClick = { handler.openUri(SettingsDataScreen.HELP_URL) },
                prominent = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        LaunchedEffect(storageDir) {
            storagePref.changes()
                .collectLatest {
                    locationValid = directoryAccessible(context, storageDir)
                    _isComplete = locationValid
                }
        }
    }
}
