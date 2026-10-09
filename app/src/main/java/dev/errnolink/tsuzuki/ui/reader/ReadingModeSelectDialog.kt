package dev.errnolink.tsuzuki.ui.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.errnolink.tsuzuki.designsystem.DetentSheet
import dev.errnolink.tsuzuki.designsystem.GroupedRow
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.manga.model.readingMode
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ReadingModeSelectDialog(
    onDismissRequest: () -> Unit,
    screenModel: ReaderSettingsScreenModel,
    onChange: (StringResource) -> Unit,
) {
    val manga by screenModel.mangaFlow.collectAsState()
    var selected by remember { mutableStateOf(ReadingMode.fromPreference(manga?.readingMode?.toInt())) }
    DetentSheet(true, "Reading mode", onDismissRequest) {
        ReadingMode.entries.forEach { mode ->
            GroupedRow(stringResource(mode.stringRes), value = if (mode == selected) "Selected" else null, onClick = { selected = mode })
        }
        PillButton("Apply", {
            screenModel.onChangeReadingMode(selected)
            onChange(selected.stringRes)
            onDismissRequest()
        })
    }
}
