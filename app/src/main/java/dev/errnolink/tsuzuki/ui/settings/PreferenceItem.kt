package dev.errnolink.tsuzuki.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import dev.errnolink.tsuzuki.designsystem.TsuzukiCorners
import dev.errnolink.tsuzuki.designsystem.TsuzukiSpacing
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import java.util.Locale
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import kotlin.time.Duration.Companion.seconds

val LocalPreferenceHighlighted = compositionLocalOf(structuralEqualityPolicy()) { false }
val LocalPreferenceMinHeight = compositionLocalOf(structuralEqualityPolicy()) { 56.dp }

@Composable
fun PreferenceScaffold(
    titleRes: StringResource,
    actions: @Composable RowScope.() -> Unit = {},
    onBackPressed: (() -> Unit)? = null,
    itemsProvider: @Composable () -> List<Preference>,
) {
    val preferences = itemsProvider()
    val sections = remember(preferences) { preferenceSections(preferences) }
    val state = rememberLazyListState()
    val highlightKey = SearchableSettings.highlightKey
    LaunchedEffect(sections, highlightKey) {
        if (highlightKey != null) {
            var index = 1
            for (section in sections) {
                val row = section.items.indexOfFirst { it.title == highlightKey }
                if (section.title == highlightKey || row >= 0) {
                    delay(0.5.seconds)
                    state.scrollToItem(if (row >= 0) index + row + 1 else index)
                    break
                }
                index += section.items.size + 1
            }
            SearchableSettings.highlightKey = null
        }
    }
    SettingsScaffold(
        title = stringResource(titleRes),
        navigateUp = onBackPressed,
        actions = actions,
        state = state,
        itemSpacing = 0.dp,
    ) {
        renderPreferenceSections(sections, highlightKey)
    }
}

@Composable
fun PreferenceScreen(
    items: List<Preference>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val sections = remember(items) { preferenceSections(items) }
    LazyColumn(
        modifier = modifier,
        state = rememberLazyListState(),
        contentPadding = contentPadding,
    ) {
        renderPreferenceSections(sections, SearchableSettings.highlightKey)
    }
}

@Composable
fun SearchableSettings.TsuzukiPreferenceContent() {
    val handleBack = eu.kanade.presentation.util.LocalBackPress.current
    PreferenceScaffold(
        titleRes = getTitleRes(),
        onBackPressed = if (handleBack != null) handleBack::invoke else null,
        actions = { AppBarAction() },
        itemsProvider = { getPreferences() },
    )
}

private data class PreferenceSection(val title: String?, val items: List<Preference.PreferenceItem<*, *>>)

private fun preferenceSections(items: List<Preference>): List<PreferenceSection> = buildList {
    var rows = mutableListOf<Preference.PreferenceItem<*, *>>()
    fun flushRows() {
        if (rows.isNotEmpty()) {
            add(PreferenceSection(null, rows))
            rows = mutableListOf()
        }
    }
    items.forEach { preference ->
        if (preference.enabled) {
            when (preference) {
                is Preference.PreferenceGroup -> {
                    flushRows()
                    val visible = preference.preferenceItems.filter { it.enabled }
                    if (visible.isNotEmpty()) add(PreferenceSection(preference.title.takeIf { it.isNotBlank() }, visible))
                }
                is Preference.PreferenceItem<*, *> -> rows.add(preference)
            }
        }
    }
    flushRows()
}

private fun LazyListScope.renderPreferenceSections(sections: List<PreferenceSection>, highlightKey: String?) {
    sections.forEachIndexed { index, section ->
        val groupKey = "settings-group-$index-${section.title}"
        item(key = groupKey, contentType = "settings-header") {
            Column {
                Spacer(Modifier.height(TsuzukiSpacing.section))
                section.title?.let { title ->
                    val locale = Locale.getDefault()
                    val label = remember(title, locale) { title.uppercase(locale) }
                    TsuzukiText(
                        label,
                        Modifier.padding(start = TsuzukiSpacing.gutter + 16.dp, end = TsuzukiSpacing.gutter + 16.dp, bottom = 8.dp),
                        TsuzukiTheme.typography.sectionHeader,
                        TsuzukiTheme.colors.secondary,
                    )
                }
            }
        }
        items(
            count = section.items.size,
            key = { row -> "$groupKey-$row-${section.items[row].title}" },
            contentType = { row -> section.items[row]::class },
        ) { row ->
            val shape = remember(row, section.items.size) {
                RoundedCornerShape(
                    topStart = if (row == 0) TsuzukiCorners.group else 0.dp,
                    topEnd = if (row == 0) TsuzukiCorners.group else 0.dp,
                    bottomStart = if (row == section.items.lastIndex) TsuzukiCorners.group else 0.dp,
                    bottomEnd = if (row == section.items.lastIndex) TsuzukiCorners.group else 0.dp,
                )
            }
            Column(
                Modifier.padding(horizontal = TsuzukiSpacing.gutter).fillMaxWidth()
                    .clip(shape).background(TsuzukiTheme.colors.grouped),
            ) {
                if (row > 0) GroupDivider()
                PreferenceItem(item = section.items[row], highlightKey = highlightKey)
            }
        }
    }
}

@Composable
internal fun PreferenceItem(
    item: Preference.PreferenceItem<*, *>,
    highlightKey: String?,
) {
    val scope = rememberCoroutineScope()
    val enabled = item.enabled
    val highlighted = item.title == highlightKey
    androidx.compose.animation.AnimatedVisibility(
        visible = enabled,
        enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
        content = {
            CompositionLocalProvider(LocalPreferenceHighlighted provides highlighted) {
                when (item) {
                    is Preference.PreferenceItem.SwitchPreference -> {
                        val value by item.preference.collectAsState()
                        SwitchPreferenceWidget(
                            title = item.title,
                            subtitle = item.subtitle,
                            icon = item.icon,
                            checked = value,
                            divider = false,
                            onCheckedChanged = { newValue ->
                                scope.launch {
                                    if (item.onValueChanged(newValue)) {
                                        item.preference.set(newValue)
                                    }
                                }
                            },
                        )
                    }
                    is Preference.PreferenceItem.SliderPreference -> {
                        TsuzukiSliderRow(
                            title = item.title,
                            value = item.value,
                            valueString = item.valueString.takeUnless { it.isNullOrEmpty() } ?: item.value.toString(),
                            range = item.valueRange,
                            enabled = item.enabled,
                            onChange = {
                                scope.launch {
                                    item.onValueChanged(it)
                                }
                            },
                        )
                    }
                    is Preference.PreferenceItem.ListPreference<*> -> {
                        val value by item.preference.collectAsState()
                        ListPreferenceWidget(
                            value = value,
                            title = item.title,
                            subtitle = item.internalSubtitleProvider(value, item.entries),
                            icon = item.icon,
                            entries = item.entries,
                            divider = false,
                            onValueChange = { newValue ->
                                scope.launch {
                                    if (item.internalOnValueChanged(newValue!!)) {
                                        item.internalSet(newValue)
                                    }
                                }
                            },
                        )
                    }
                    is Preference.PreferenceItem.BasicListPreference -> {
                        ListPreferenceWidget(
                            value = item.value,
                            title = item.title,
                            subtitle = item.subtitleProvider(item.value, item.entries),
                            icon = item.icon,
                            entries = item.entries,
                            divider = false,
                            onValueChange = { scope.launch { item.onValueChanged(it) } },
                        )
                    }
                    is Preference.PreferenceItem.MultiSelectListPreference -> {
                        val values by item.preference.collectAsState()
                        MultiSelectListPreferenceWidget(
                            preference = item,
                            values = values,
                            divider = false,
                            onValuesChange = { newValues ->
                                scope.launch {
                                    if (item.onValueChanged(newValues)) {
                                        item.preference.set(newValues.toMutableSet())
                                    }
                                }
                            },
                        )
                    }
                    is Preference.PreferenceItem.EditTextPreference -> {
                        val values by item.preference.collectAsState()
                        EditTextPreferenceWidget(
                            title = item.title,
                            subtitle = item.subtitle,
                            icon = item.icon,
                            value = values,
                            divider = false,
                            onConfirm = {
                                val accepted = item.onValueChanged(it)
                                if (accepted) item.preference.set(it)
                                accepted
                            },
                        )
                    }
                    is Preference.PreferenceItem.TextPreference -> {
                        TextPreferenceWidget(
                            title = item.title,
                            subtitle = item.subtitle,
                            icon = item.icon,
                            divider = false,
                            onPreferenceClick = item.onClick,
                        )
                    }
                    is Preference.PreferenceItem.TrackerPreference -> {
                        val isLoggedIn by item.tracker.let { tracker ->
                            tracker.isLoggedInFlow.collectAsState(tracker.isLoggedIn)
                        }
                        TrackingPreferenceWidget(
                            tracker = item.tracker,
                            checked = isLoggedIn,
                            onClick = { if (isLoggedIn) item.logout() else item.login() },
                        )
                    }
                    is Preference.PreferenceItem.InfoPreference -> {
                        InfoWidget(text = item.title)
                    }
                    is Preference.PreferenceItem.CustomPreference -> {
                        item.content()
                    }
                }
            }
        },
    )
}
