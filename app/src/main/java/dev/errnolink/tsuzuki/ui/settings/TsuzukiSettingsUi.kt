package dev.errnolink.tsuzuki.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas as SliderCanvas
import dev.errnolink.tsuzuki.designsystem.DetentSheet
import dev.errnolink.tsuzuki.designsystem.GroupedRow
import dev.errnolink.tsuzuki.designsystem.InsetGroupedList
import dev.errnolink.tsuzuki.designsystem.LargeTitleScaffold
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.rememberGlassContext
import dev.errnolink.tsuzuki.ui.shell.LocalShellGlass
import kotlinx.coroutines.launch

@Composable
fun SettingsScaffold(
    title: String,
    navigateUp: (() -> Unit)?,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    itemSpacing: androidx.compose.ui.unit.Dp = dev.errnolink.tsuzuki.designsystem.TsuzukiSpacing.section,
    content: LazyListScope.() -> Unit,
) {
    val shellGlass = LocalShellGlass.current
    LargeTitleScaffold(
        title = title,
        modifier = modifier,
        state = state,
        glassContext = shellGlass ?: rememberGlassContext(),
        navigationIcon = {
            if (navigateUp != null) {
                SettingsIconButton("Back", navigateUp) { BackMark() }
            }
        },
        actions = actions,
        bottomBar = bottomBar,
        bottomPadding = dev.errnolink.tsuzuki.ui.shell.shellBottomPadding(),
        itemSpacing = itemSpacing,
        content = content,
    )
}

@Composable
fun SettingsIconButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
) {
    dev.errnolink.tsuzuki.designsystem.IconButton(
        label = label,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
    )
}

@Composable
internal fun BackMark() {
    val color = TsuzukiTheme.colors.accent
    val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current ==
        androidx.compose.ui.unit.LayoutDirection.Rtl
    SliderCanvas(Modifier.size(width = 11.dp, height = 19.dp)) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(if (rtl) 0f else size.width, 0f)
            lineTo(if (rtl) size.width else 0f, size.height / 2f)
            lineTo(if (rtl) 0f else size.width, size.height)
        }
        drawPath(
            path,
            color,
            style = Stroke(
                2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
    }
}

@Composable
fun GroupDivider() {
    Spacer(
        Modifier
            .padding(start = 16.dp)
            .fillMaxWidth()
            .height(dev.errnolink.tsuzuki.designsystem.TsuzukiSpacing.hairline)
            .background(TsuzukiTheme.colors.separator),
    )
}

@Composable
fun <T> ChoiceSheet(
    visible: Boolean,
    title: String,
    entries: List<Pair<T, String>>,
    selected: T?,
    onDismissRequest: () -> Unit,
    onSelect: (T) -> Unit,
) {
    DetentSheet(visible = visible, title = title, onDismissRequest = onDismissRequest) {
        InsetGroupedList {
            entries.forEachIndexed { index, (key, label) ->
                if (index > 0) GroupDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable(role = Role.RadioButton) {
                            onDismissRequest()
                            onSelect(key)
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TsuzukiText(label, Modifier.weight(1f))
                    if (selected == key) CheckMark()
                }
            }
        }
    }
}

@Composable
fun MultiChoiceSheet(
    visible: Boolean,
    title: String,
    footer: String? = null,
    doneLabel: String? = null,
    onDone: (() -> Unit)? = null,
    options: List<Triple<String, Boolean, (Boolean) -> Unit>>,
    onDismissRequest: () -> Unit,
) {
    DetentSheet(visible = visible, title = title, onDismissRequest = onDismissRequest) {
        InsetGroupedList {
            options.forEachIndexed { index, (label, checked, onChange) ->
                if (index > 0) GroupDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable(role = Role.Checkbox) { onChange(!checked) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TsuzukiText(label, Modifier.weight(1f))
                    if (checked) CheckMark()
                }
            }
        }
        footer?.let {
            TsuzukiText(
                it,
                Modifier.padding(horizontal = 16.dp),
                TsuzukiTheme.typography.footnote,
                TsuzukiTheme.colors.secondary,
            )
        }
        if (doneLabel != null && onDone != null) {
            PillButton(doneLabel, onDone, prominent = false, modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
fun TriStateChoiceSheet(
    visible: Boolean,
    title: String,
    message: String? = null,
    doneLabel: String,
    options: List<Triple<String, Int, (Int) -> Unit>>,
    onDismissRequest: () -> Unit,
    onDone: () -> Unit,
) {
    DetentSheet(visible = visible, title = title, onDismissRequest = onDismissRequest) {
        message?.let {
            TsuzukiText(it, Modifier.padding(horizontal = 16.dp), TsuzukiTheme.typography.footnote, TsuzukiTheme.colors.secondary)
        }
        InsetGroupedList {
            options.forEachIndexed { index, (label, state, onCycle) ->
                if (index > 0) GroupDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable(role = Role.Checkbox) { onCycle((state + 1) % 3) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TsuzukiText(label, Modifier.weight(1f))
                    when (state) {
                        1 -> CheckMark()
                        2 -> MinusMark()
                    }
                }
            }
        }
        PillButton(doneLabel, onDone, prominent = false, modifier = Modifier.align(Alignment.End))
    }
}

@Composable
fun TextEntrySheet(
    visible: Boolean,
    title: String,
    initialValue: String,
    confirmLabel: String,
    errorText: String?,
    supportsConfirm: (String) -> Boolean,
    onConfirm: suspend (String) -> Boolean,
    onDismissRequest: () -> Unit,
    message: String? = null,
    placeholder: String? = null,
) {
    if (!visible) return
    val scope = rememberCoroutineScope()
    var fieldValue by rememberSaveable(visible, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialValue))
    }
    var saving by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    DetentSheet(visible = true, title = title, onDismissRequest = onDismissRequest) {
        message?.let {
            TsuzukiText(
                it,
                Modifier.padding(horizontal = 16.dp),
                TsuzukiTheme.typography.footnote,
                TsuzukiTheme.colors.secondary,
            )
        }
        BaseTextFieldRow(
            value = fieldValue,
            onValueChange = { fieldValue = it },
            modifier = Modifier.focusRequester(focusRequester),
            placeholder = placeholder,
        )
        if (fieldValue.text.isNotEmpty() && errorText != null && !supportsConfirm(fieldValue.text)) {
            TsuzukiText(
                errorText,
                Modifier.padding(horizontal = 16.dp),
                TsuzukiTheme.typography.footnote,
                TsuzukiTheme.colors.destructive,
            )
        }
        Row(Modifier.align(Alignment.End)) {
            PillButton(
                confirmLabel,
                enabled = supportsConfirm(fieldValue.text) && !saving,
                onClick = {
                    scope.launch {
                        saving = true
                        if (onConfirm(fieldValue.text)) onDismissRequest()
                        saving = false
                    }
                },
            )
        }
    }
}

@Composable
fun ConfirmSheet(
    visible: Boolean,
    title: String,
    message: String? = null,
    confirmLabel: String,
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    DetentSheet(visible = visible, title = title, onDismissRequest = onDismissRequest) {
        message?.let {
            TsuzukiText(it, Modifier.padding(horizontal = 16.dp), TsuzukiTheme.typography.body, TsuzukiTheme.colors.secondary)
        }
        PillButton(
            label = confirmLabel,
            onClick = { onDismissRequest(); onConfirm() },
            destructive = destructive,
        )
    }
}

@Composable
fun TsuzukiSliderRow(
    title: String,
    value: Int,
    valueString: String,
    range: IntProgression,
    enabled: Boolean,
    onChange: (Int) -> Unit,
) {
    val colors = TsuzukiTheme.colors
    val first = range.first
    val last = range.last
    val span = (last - first).coerceAtLeast(1)
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp).alpha(if (enabled) 1f else 0.45f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TsuzukiText(title, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            TsuzukiText(valueString, Modifier, TsuzukiTheme.typography.subhead, colors.secondary)
        }
        var trackWidth by remember { mutableStateOf(1f) }
        var dragging by remember { mutableStateOf(false) }
        fun valueAt(x: Float): Int = first + ((x / trackWidth).coerceIn(0f, 1f) * span).let { if (range.step > 1) (it / range.step).toInt() * range.step else kotlin.math.round(it).toInt() }
        SliderCanvas(
            Modifier
                .fillMaxWidth()
                .height(32.dp)
                .semantics {
                    contentDescription = title
                    progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(
                        (value - first).toFloat() / span,
                        0f..1f,
                    )
                }
                .pointerInput(enabled, first, span, range.step) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(onPress = { onChange(valueAt(it.x)) })
                }
                .pointerInput(enabled, first, span, range.step) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, _ ->
                        change.consume()
                        onChange(valueAt(change.position.x))
                    }
                },
        ) {
            trackWidth = size.width
            val fraction = ((value - first).toFloat() / span).coerceIn(0f, 1f)
            val knobRadius = if (dragging) 15.dp.toPx() else 13.dp.toPx()
            val centerY = size.height / 2f
            drawRoundRect(
                colors.separator,
                topLeft = androidx.compose.ui.geometry.Offset(0f, centerY - 2.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(size.width, 4.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            )
            drawRoundRect(
                colors.accent,
                topLeft = androidx.compose.ui.geometry.Offset(0f, centerY - 2.dp.toPx()),
                size = androidx.compose.ui.geometry.Size((size.width * fraction).coerceAtLeast(knobRadius), 4.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            )
            drawCircle(colors.grouped, knobRadius, androidx.compose.ui.geometry.Offset((size.width * fraction).coerceIn(knobRadius, size.width - knobRadius), centerY))
            drawCircle(
                colors.separator.copy(alpha = 0.6f),
                knobRadius,
                androidx.compose.ui.geometry.Offset((size.width * fraction).coerceIn(knobRadius, size.width - knobRadius), centerY),
                style = Stroke(0.5.dp.toPx()),
            )
        }
    }
}
