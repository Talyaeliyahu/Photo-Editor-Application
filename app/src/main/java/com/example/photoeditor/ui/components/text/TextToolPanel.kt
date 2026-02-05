package com.example.photoeditor.ui.components

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.photoeditor.R
import com.example.photoeditor.model.TextOverlay

@Composable
fun TextToolPanel(
    overlays: List<TextOverlay>,
    selectedId: String?,
    onAdd: () -> Unit,
    onSelect: (String) -> Unit,
    onDeselect: () -> Unit,
    onTextChange: (String) -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onColorChange: (Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = overlays.firstOrNull { it.id == selectedId }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val context = LocalContext.current

    fun showKeyboard() {
        keyboard?.show()
    }

    fun hideKeyboard() {
        keyboard?.hide()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    LaunchedEffect(selected?.id) {
        if (selected != null) {
            delay(50)
            focusRequester.requestFocus()
            delay(50)
            showKeyboard()
        }
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = selected?.text ?: "",
                    onValueChange = onTextChange,
                    enabled = selected != null,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (selected == null) {
                                Modifier.pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        onAdd()
                                    }
                                }
                            } else Modifier
                        )
                        .onFocusChanged { state ->
                            if (state.isFocused) showKeyboard()
                        }
                        .focusRequester(focusRequester)
                        .focusTarget(),
                    placeholder = { Text(stringResource(R.string.text_hint)) },
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Default
                    )
                )
                IconButton(onClick = {
                    hideKeyboard()
                    onDeselect()
                }) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = stringResource(R.string.done))
                }
            }

            if (selected == null) {
                Text(
                    text = stringResource(R.string.tap_to_add_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Text(
                text = stringResource(R.string.text_color),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val textColors = listOf(
                Color.White,
                Color.Black,
                Color.Red,
                Color(0xFFFF9800),
                Color.Yellow,
                Color.Green,
                Color.Cyan,
                Color.Blue,
                Color(0xFF9C27B0),
                Color(0xFFE91E63),
                Color(0xFF00BCD4),
                Color(0xFF4CAF50),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                textColors.forEach { color ->
                    val colorArgb = color.toArgb()
                    val border = when {
                        (selected?.argb ?: 0) == colorArgb -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        colorArgb == android.graphics.Color.BLACK -> BorderStroke(1.dp, Color.White)
                        else -> null
                    }
                    Surface(
                        shape = CircleShape,
                        color = color,
                        modifier = Modifier.size(28.dp),
                        onClick = { onColorChange(colorArgb) },
                        border = border
                    ) {}
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleBold) {
                        Icon(imageVector = Icons.Default.FormatBold, contentDescription = stringResource(R.string.bold))
                    }
                    IconButton(onClick = onToggleItalic) {
                        Icon(imageVector = Icons.Default.FormatItalic, contentDescription = stringResource(R.string.italic))
                    }
                    IconButton(onClick = onToggleUnderline) {
                        Icon(imageVector = Icons.Default.FormatUnderlined, contentDescription = stringResource(R.string.underline))
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.delete_text))
                }
            }
        }
    }
}

