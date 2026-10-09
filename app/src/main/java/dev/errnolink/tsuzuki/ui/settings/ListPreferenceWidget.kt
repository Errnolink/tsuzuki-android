package dev.errnolink.tsuzuki.ui.settings

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme

@Composable
fun <T> ListPreferenceWidget(
    value: T,
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    entries: Map<out T, String>,
    onValueChange: (T) -> Unit,
    divider: Boolean = true,
) {
    var isDialogShown by remember { mutableStateOf(false) }

    PreferenceRow(
        title = title,
        modifier = Modifier.preferenceHighlight(),
        subtitle = subtitle?.takeUnless { it == entries[value] },
        value = entries[value],
        chevron = true,
        divider = divider,
        leading = icon?.let {
            {
                Icon(it, null, Modifier.size(24.dp), tint = TsuzukiTheme.colors.secondary)
            }
        },
        onClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        ChoiceSheet(
            visible = true,
            title = title,
            entries = entries.map { it.key to it.value },
            selected = value,
            onDismissRequest = { isDialogShown = false },
            onSelect = {
                onValueChange(it)
                isDialogShown = false
            },
        )
    }
}
