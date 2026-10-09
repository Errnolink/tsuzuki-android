package dev.errnolink.tsuzuki.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(options.isNotEmpty() && selectedIndex in options.indices)
    val colors = TsuzukiTheme.colors
    val motion = TsuzukiTheme.motion
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val position by animateFloatAsState(
        selectedIndex.toFloat(), if (motion.reduced) snap() else motion.tabSpring(), label = "Segment selection",
    )
    BoxWithConstraints(modifier.clip(RoundedCornerShape(12.dp)).background(colors.separator).padding(3.dp)) {
        val cellWidth = maxWidth / options.size
        val cellPx = with(LocalDensity.current) { cellWidth.toPx() }
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier.width(cellWidth).fillMaxHeight().graphicsLayer {
                    translationX = position.coerceIn(0f, options.lastIndex.toFloat()) * cellPx * if (rtl) -1f else 1f
                }.background(colors.grouped, RoundedCornerShape(9.dp)),
            )
        }
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            options.forEachIndexed { index, label ->
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                        .selectable(index == selectedIndex, role = Role.RadioButton, onClick = { onSelect(index) })
                        .heightIn(min = 44.dp).padding(horizontal = 8.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TsuzukiText(label, style = TsuzukiTheme.typography.subhead)
                }
            }
        }
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Title or MangaDex link",
    enabled: Boolean = true,
) {
    val colors = TsuzukiTheme.colors
    val shape = RoundedCornerShape(TsuzukiCorners.field)
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    var draft by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    LaunchedEffect(value, focused) {
        if (!focused && draft.text != value) draft = TextFieldValue(value, TextRange(value.length))
    }
    Row(
        modifier.clip(shape).background(colors.grouped).heightIn(min = TsuzukiSpacing.touchTarget)
            .padding(start = 14.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(19.dp)) {
            drawCircle(colors.secondary, radius = size.width * 0.31f, center = Offset(size.width * 0.39f, size.height * 0.39f), style = Stroke(1.8.dp.toPx()))
            drawLine(colors.secondary, Offset(size.width * 0.64f, size.height * 0.64f), Offset(size.width, size.height), 1.8.dp.toPx())
        }
        BasicTextField(
            value = draft,
            onValueChange = {
                val changed = draft.text != it.text
                draft = it
                if (changed) onValueChange(it.text)
            },
            enabled = enabled,
            modifier = Modifier.weight(1f).padding(vertical = 12.dp)
                .onFocusChanged { focused = it.isFocused }
                .semantics { contentDescription = placeholder },
            textStyle = TsuzukiTheme.typography.body.copy(color = colors.text),
            cursorBrush = SolidColor(colors.accent), singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(); focusManager.clearFocus() }),
            decorationBox = { inner ->
                Box {
                    if (draft.text.isEmpty()) TsuzukiText(placeholder, color = colors.secondary, maxLines = 1)
                    inner()
                }
            },
        )
        if (draft.text.isNotEmpty()) IconButton(
            "Clear search",
            { draft = TextFieldValue(""); onValueChange("") },
            enabled = enabled,
        ) { CloseMark() }
    }
}

@Composable
internal fun CloseMark() {
    val color = LocalTsuzukiContentColor.current
    Canvas(Modifier.size(16.dp)) {
        drawLine(color, Offset.Zero, Offset(size.width, size.height), 1.8.dp.toPx())
        drawLine(color, Offset(size.width, 0f), Offset(0f, size.height), 1.8.dp.toPx())
    }
}
