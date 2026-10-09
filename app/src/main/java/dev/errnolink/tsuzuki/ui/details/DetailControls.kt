package dev.errnolink.tsuzuki.ui.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.DetentSheet
import dev.errnolink.tsuzuki.designsystem.GlassContext
import dev.errnolink.tsuzuki.designsystem.GlassSurface
import dev.errnolink.tsuzuki.designsystem.GroupedRow
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import kotlinx.coroutines.delay

@Composable
fun DetailIcon(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp).clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }.padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (enabled) TsuzukiTheme.colors.text else TsuzukiTheme.colors.secondary.copy(alpha = 0.45f))
    }
}

@Composable
fun DetailGlassGroup(modifier: Modifier = Modifier, context: GlassContext? = null, content: @Composable RowScope.() -> Unit) {
    GlassSurface(context, modifier) {
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

@Composable
fun DetailField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TsuzukiText(label, style = TsuzukiTheme.typography.footnote, color = TsuzukiTheme.colors.secondary)
        BasicTextField(
            value = value, onValueChange = onValueChange, singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            textStyle = TsuzukiTheme.typography.body.copy(color = TsuzukiTheme.colors.text),
            cursorBrush = SolidColor(TsuzukiTheme.colors.accent),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(TsuzukiTheme.colors.elevated).padding(14.dp)
                .semantics { contentDescription = label },
        )
    }
}

@Composable
fun DetailChoice(label: String, selected: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    GroupedRow(label, value = if (selected) "Selected" else null, onClick = onClick, enabled = enabled)
}

@Composable
fun DetailDialog(
    title: String,
    onDismissRequest: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    DetentSheet(true, title, onDismissRequest) {
        content()
        PillButton(confirmLabel, onConfirm, Modifier.fillMaxWidth(), enabled = enabled, destructive = destructive)
    }
}

@Composable
fun GlassSnackbar(data: SnackbarData?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = data != null,
        enter = fadeIn(snap()) + slideInVertically(snap()) { it / 2 },
        exit = fadeOut(snap()) + slideOutVertically(snap()) { it / 2 },
        modifier = modifier,
    ) {
        val current = data ?: return@AnimatedVisibility
        LaunchedEffect(current) {
            delay(4000)
            current.dismiss()
        }
        GlassSurface(context = null) {
            Row(
                Modifier.clickable { current.dismiss() }.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TsuzukiText(current.visuals.message, style = TsuzukiTheme.typography.subhead, maxLines = 3)
            }
        }
    }
}
