package dev.errnolink.tsuzuki.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.errnolink.tsuzuki.designsystem.DetentSheet
import dev.errnolink.tsuzuki.designsystem.GroupedRow
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.category.visualName
import dev.errnolink.tsuzuki.ui.settings.ConfirmSheet
import dev.errnolink.tsuzuki.ui.settings.TextEntrySheet
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun CategoryCreateDialog(
    onDismissRequest: () -> Unit,
    onCreate: (String) -> Unit,
    categories: ImmutableList<String>,
    title: String = stringResource(MR.strings.action_add_category),
    extraMessage: String? = null,
    alreadyExistsError: StringResource = MR.strings.error_category_exists,
) {
    TextEntrySheet(
        visible = true,
        title = title,
        initialValue = "",
        confirmLabel = stringResource(MR.strings.action_add),
        errorText = stringResource(alreadyExistsError),
        supportsConfirm = { it.isNotBlank() && it !in categories },
        onConfirm = { onCreate(it); true },
        onDismissRequest = onDismissRequest,
        message = extraMessage,
        placeholder = stringResource(MR.strings.name),
    )
}

@Composable
fun CategoryRenameDialog(
    onDismissRequest: () -> Unit,
    onRename: (String) -> Unit,
    categories: ImmutableList<String>,
    category: String,
) {
    TextEntrySheet(
        visible = true,
        title = stringResource(MR.strings.action_rename_category),
        initialValue = category,
        confirmLabel = stringResource(MR.strings.action_ok),
        errorText = stringResource(MR.strings.error_category_exists),
        supportsConfirm = { it.isNotBlank() && it != category && it !in categories },
        onConfirm = { onRename(it); true },
        onDismissRequest = onDismissRequest,
        placeholder = stringResource(MR.strings.name),
    )
}

@Composable
fun CategoryDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    category: String = "",
    title: String = stringResource(MR.strings.delete_category),
    text: String = stringResource(MR.strings.delete_category_confirmation, category),
) {
    ConfirmSheet(true, title, text, stringResource(MR.strings.action_delete), true, onDelete, onDismissRequest)
}

@Composable
fun ChangeCategoryDialog(
    initialSelection: ImmutableList<CheckboxState<Category>>,
    onDismissRequest: () -> Unit,
    onEditCategories: () -> Unit,
    onConfirm: (List<Long>, List<Long>) -> Unit,
) {
    var selection by remember(initialSelection) { mutableStateOf(initialSelection.toList()) }
    DetentSheet(true, stringResource(MR.strings.action_move_category), onDismissRequest) {
        if (selection.isEmpty()) {
            TsuzukiText(stringResource(MR.strings.information_empty_category_dialog))
        } else {
            selection.forEachIndexed { index, checkbox ->
                GroupedRow(
                    title = checkbox.value.visualName,
                    value = when (checkbox) {
                        is CheckboxState.State.Checked, is CheckboxState.TriState.Include -> "Included"
                        is CheckboxState.State.None, is CheckboxState.TriState.None -> "Not included"
                        else -> "Mixed"
                    },
                    onClick = {
                        selection = selection.toMutableList().also { it[index] = checkbox.next() }
                    },
                )
            }
            PillButton(stringResource(MR.strings.action_ok), {
                onConfirm(
                    selection.filter { it is CheckboxState.State.Checked || it is CheckboxState.TriState.Include }.map { it.value.id },
                    selection.filter { it is CheckboxState.State.None || it is CheckboxState.TriState.None }.map { it.value.id },
                )
                onDismissRequest()
            })
        }
        PillButton(stringResource(MR.strings.action_edit_categories), { onDismissRequest(); onEditCategories() }, prominent = false)
    }
}
