package com.gloster.yaplan.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gloster.yaplan.data.ChecklistItem
import com.gloster.yaplan.ui.components.FieldLabel
import com.gloster.yaplan.ui.components.TaskCheckbox
import com.gloster.yaplan.ui.theme.PlexMono
import com.gloster.yaplan.ui.theme.Yap

/**
 * "Not": free text and tickable lines (e.g. a shopping list) in one box. Typing in the last row and
 * pressing Enter adds a line and keeps the keyboard open for the next one.
 */
@Composable
fun NoteEditor(
    note: String,
    items: List<ChecklistItem>,
    newItemText: String,
    actions: TaskFormActions,
    fieldColor: Color,
    modifier: Modifier = Modifier,
) {
    val addFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(4.dp)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FieldLabel("Not")
            Text(
                " (isteğe bağlı)",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal),
                color = Yap.TextMuted,
            )
            if (items.isNotEmpty()) {
                Text(
                    "${items.count { it.isDone }}/${items.size}",
                    modifier = Modifier.semantics {
                        contentDescription = "${items.size} maddeden ${items.count { it.isDone }} tanesi alındı"
                    },
                    fontFamily = PlexMono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Yap.TextMuted,
                )
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(fieldColor, shape)
                .border(1.dp, Yap.OutlineStrong, shape)
                .padding(vertical = 2.dp),
        ) {
            ItemTextField(
                value = note,
                onValueChange = actions::setNote,
                placeholder = "Ayrıntı ekle",
                imeAction = ImeAction.Default,
                onIme = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Not" },
                minLines = 2,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            )
            HorizontalDivider(color = Yap.Divider)
            items.forEach { item ->
                key(item.id) {
                    ItemRow(
                        item = item,
                        onText = { actions.setItemText(item.id, it) },
                        onDone = { actions.setItemDone(item.id, it) },
                        onRemove = { actions.removeItem(item.id) },
                        onNext = { addFocus.requestFocus() },
                    )
                }
                HorizontalDivider(Modifier.padding(start = 48.dp), color = Yap.Divider)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Yap.TextMuted, modifier = Modifier.size(20.dp))
                }
                ItemTextField(
                    value = newItemText,
                    onValueChange = actions::setNewItemText,
                    placeholder = if (items.isEmpty()) "Madde ekle (ör. Süt)" else "Madde ekle",
                    imeAction = ImeAction.Done,
                    onIme = {
                        if (newItemText.isBlank()) focusManager.clearFocus() else actions.addNewItem()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                        .focusRequester(addFocus)
                        .semantics { contentDescription = "Listeye madde ekle" },
                )
            }
        }
    }
}

@Composable
private fun ItemRow(
    item: ChecklistItem,
    onText: (String) -> Unit,
    onDone: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(48.dp)
                .fillMaxHeight()
                .defaultMinSize(minHeight = 48.dp)
                .toggleable(value = item.isDone, role = Role.Checkbox, onValueChange = onDone)
                .clearAndSetSemantics {
                    contentDescription = (if (item.isDone) "Alınmadı olarak işaretle: " else "Alındı olarak işaretle: ") + item.text
                },
            contentAlignment = Alignment.Center,
        ) {
            TaskCheckbox(checked = item.isDone, overdue = false)
        }
        ItemTextField(
            value = item.text,
            onValueChange = onText,
            placeholder = "Boş madde",
            imeAction = ImeAction.Next,
            onIme = onNext,
            done = item.isDone,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Maddeyi sil: ${item.text}",
                tint = Yap.TextFaint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ItemTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    imeAction: ImeAction,
    onIme: () -> Unit,
    modifier: Modifier = Modifier,
    done: Boolean = false,
    minLines: Int = 1,
    contentPadding: PaddingValues = PaddingValues(),
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = minLines == 1,
        minLines = minLines,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 15.sp,
            color = if (done) Yap.TextFaint else Yap.Text,
            textDecoration = if (done) TextDecoration.LineThrough else null,
        ),
        cursorBrush = SolidColor(Yap.Accent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onIme() }, onNext = { onIme() }),
        decorationBox = { inner ->
            Box(Modifier.padding(contentPadding)) {
                if (value.isEmpty()) {
                    Text(placeholder, fontSize = 15.sp, color = Yap.TextFaint)
                }
                inner()
            }
        },
    )
}
