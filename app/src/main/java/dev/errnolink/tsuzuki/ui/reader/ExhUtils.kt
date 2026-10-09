package dev.errnolink.tsuzuki.ui.reader

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import dev.errnolink.tsuzuki.designsystem.DetentSheet
import dev.errnolink.tsuzuki.designsystem.GroupedRow
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.ui.details.DetailField
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ExhUtils(
    isVisible: Boolean,
    onSetExhUtilsVisibility: (Boolean) -> Unit,
    isAutoScroll: Boolean,
    isAutoScrollEnabled: Boolean,
    onToggleAutoscroll: (Boolean) -> Unit,
    autoScrollFrequency: String,
    onSetAutoScrollFrequency: (String) -> Unit,
    onClickAutoScrollHelp: () -> Unit,
    onClickRetryAll: () -> Unit,
    onClickRetryAllHelp: () -> Unit,
    onClickBoostPage: () -> Unit,
    onClickBoostPageHelp: () -> Unit,
) {
    DetentSheet(isVisible, "Reader tools", { onSetExhUtilsVisibility(false) }) {
        GroupedRow(stringResource(SYMR.strings.eh_autoscroll), checked = isAutoScroll, onCheckedChange = onToggleAutoscroll, enabled = isAutoScrollEnabled)
        var frequency by remember(autoScrollFrequency) { mutableStateOf(autoScrollFrequency) }
        DetailField(frequency, { frequency = it; onSetAutoScrollFrequency(it) }, "Scroll interval (seconds)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        if (!isAutoScrollEnabled) TsuzukiText(stringResource(SYMR.strings.eh_autoscroll_freq_invalid), color = TsuzukiTheme.colors.destructive)
        GroupedRow("About automatic scrolling", chevron = true, onClick = onClickAutoScrollHelp)
        GroupedRow(stringResource(SYMR.strings.eh_retry_all), onClick = onClickRetryAll)
        GroupedRow("About retrying pages", chevron = true, onClick = onClickRetryAllHelp)
        GroupedRow(stringResource(SYMR.strings.eh_boost_page), onClick = onClickBoostPage)
        GroupedRow("About boosting pages", chevron = true, onClick = onClickBoostPageHelp)
    }
}
