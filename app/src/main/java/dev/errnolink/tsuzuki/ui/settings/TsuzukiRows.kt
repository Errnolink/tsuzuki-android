package dev.errnolink.tsuzuki.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.TsuzukiCorners
import dev.errnolink.tsuzuki.designsystem.TsuzukiSpacing
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

internal val PrefsHorizontalPadding = 16.dp
internal val PrefsVerticalPadding = 12.dp
internal val TrailingWidgetBuffer = 16.dp

@Composable
internal fun Modifier.preferenceHighlight(): Modifier {
    val highlighted = LocalPreferenceHighlighted.current
    var flag by remember { mutableStateOf(false) }
    LaunchedEffect(highlighted) {
        if (highlighted) {
            flag = true
            delay(2.seconds)
            flag = false
        }
    }
    val color by animateColorAsState(
        if (flag) TsuzukiTheme.colors.selectedFill.copy(alpha = 0.6f) else Color.Transparent,
        if (flag) tween(150) else tween(400),
        label = "preference-highlight",
    )
    return this.background(color)
}

@Composable
internal fun PreferenceRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    chevron: Boolean = false,
    enabled: Boolean = true,
    divider: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = TsuzukiTheme.colors
    val stackValue = androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(enabled = enabled && onClick != null, role = Role.Button) { onClick?.invoke() }
                .padding(horizontal = PrefsHorizontalPadding, vertical = PrefsVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            leading?.invoke()
            Column(Modifier.weight(1f), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp)) {
                TsuzukiText(title, color = if (enabled) colors.text else colors.secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                subtitle?.let {
                    TsuzukiText(it, Modifier, TsuzukiTheme.typography.footnote, colors.secondary, maxLines = 10)
                }
                if (stackValue && value != null) {
                    TsuzukiText(value, Modifier, TsuzukiTheme.typography.subhead, colors.secondary)
                }
            }
            if (!stackValue && value != null) {
                Box(Modifier.weight(0.8f), contentAlignment = Alignment.CenterEnd) {
                    TsuzukiText(
                        value,
                        style = TsuzukiTheme.typography.subhead,
                        color = colors.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            trailing?.invoke()
            if (chevron) Chevron()
        }
        if (divider) {
            Spacer(
                Modifier
                    .padding(start = PrefsHorizontalPadding)
                    .fillMaxWidth()
                    .height(TsuzukiSpacing.hairline)
                    .background(colors.separator),
            )
        }
    }
}

@Composable
internal fun Chevron() {
    val color = TsuzukiTheme.colors.secondary
    val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    Canvas(Modifier.size(width = 8.dp, height = 14.dp)) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(if (rtl) size.width else 0f, 0f)
            lineTo(if (rtl) 0f else size.width, size.height / 2f)
            lineTo(if (rtl) size.width else 0f, size.height)
        }
        drawPath(path, color, style = Stroke(1.7.dp.toPx()))
    }
}

@Composable
internal fun CheckMark() {
    val color = TsuzukiTheme.colors.accent
    Canvas(Modifier.size(width = 14.dp, height = 11.dp)) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, size.height * 0.55f)
            lineTo(size.width * 0.36f, size.height)
            lineTo(size.width, 0f)
        }
        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

@Composable
internal fun MinusMark() {
    val color = TsuzukiTheme.colors.destructive
    Canvas(Modifier.size(width = 14.dp, height = 11.dp)) {
        drawLine(
            color,
            androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
            androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
            2.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

@Composable
internal fun BaseTextFieldRow(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
) {
    val colors = TsuzukiTheme.colors
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TsuzukiCorners.field))
            .background(colors.grouped)
            .heightIn(min = 44.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = TsuzukiTheme.typography.body.copy(color = colors.text),
            cursorBrush = SolidColor(colors.accent),
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            decorationBox = { inner ->
                Box {
                    if (value.text.isEmpty() && placeholder != null) {
                        TsuzukiText(placeholder, color = colors.secondary, maxLines = 1)
                    }
                    inner()
                }
            },
        )
    }
}
