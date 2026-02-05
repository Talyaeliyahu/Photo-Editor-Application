package com.example.photoeditor.ui.components

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.photoeditor.R

@Composable
fun AdjustmentPanel(
    values: Map<AdjustmentType, Float>,
    selectedAdjustment: AdjustmentType?,
    onAdjustmentClick: (AdjustmentType) -> Unit,
    onValueChange: (AdjustmentType, Float) -> Unit,
    onReset: () -> Unit,
    onConfirmAdjustment: ((AdjustmentType, Float) -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedAdjustment == null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AdjustmentsGrid(onAdjustmentClick = onAdjustmentClick)

                        OutlinedButton(
                            onClick = onReset,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.reset_all))
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedAdjustment != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    selectedAdjustment?.let { adjustment ->
                        AdjustmentSliderPanel(
                            adjustmentType = adjustment,
                            initialValue = values[adjustment] ?: 0f,
                            onValueChange = { value -> onValueChange(adjustment, value) },
                            onConfirm = { value ->
                                onConfirmAdjustment?.invoke(adjustment, value)
                                onAdjustmentClick(adjustment)
                            },
                            onCancel = { originalValue ->
                                onValueChange(adjustment, originalValue)
                                onAdjustmentClick(adjustment)
                            }
                        )
                    }
                }
            }
        }
    }
}

