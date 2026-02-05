package com.example.photoeditor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.photoeditor.R

/**
 * Top action bar with Undo/Redo/Toggle/Save/New buttons
 */
@Composable
fun TopActionBar(
    canUndo: Boolean,
    canRedo: Boolean,
    showOriginal: Boolean,
    isSaving: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggle: () -> Unit,
    onSave: () -> Unit,
    onNew: () -> Unit,
    width: androidx.compose.ui.unit.Dp = 72.dp,
    flipUndoRedoForRtl: Boolean = false,
    modifier: Modifier = Modifier
) {
    val iconFlip = if (flipUndoRedoForRtl) Modifier.graphicsLayer { scaleX = -1f } else Modifier
    // Slightly gray button backgrounds (requested) so they stand out on black UI.
    val buttonBgEnabled = Color(0xFF2E2E2E)
    // Disabled was too dark on black; make it a bit brighter.
    val buttonBgDisabled = Color(0xFF242424)
    // Selected/active state: slightly brighter than enabled.
    val buttonBgSelected = Color(0xFF3D3D3D)

    Surface(
        modifier = modifier
            .width(width)
            .padding(6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Undo button with text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.size(48.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (canUndo) {
                            buttonBgEnabled
                        } else {
                            buttonBgDisabled
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = stringResource(R.string.undo),
                                modifier = Modifier.size(24.dp).then(iconFlip),
                                tint = if (canUndo) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                }
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.undo),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = if (canUndo) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )
            }
            
            // Redo button with text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.size(48.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (canRedo) {
                            buttonBgEnabled
                        } else {
                            buttonBgDisabled
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Redo,
                                contentDescription = stringResource(R.string.redo),
                                modifier = Modifier.size(24.dp).then(iconFlip),
                                tint = if (canRedo) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                }
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.redo),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = if (canRedo) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )
            }
            
            // Toggle button
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(48.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (showOriginal) {
                        buttonBgSelected
                    } else {
                        buttonBgEnabled
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = stringResource(R.string.toggle_original_edited),
                            modifier = Modifier.size(24.dp),
                            tint = if (showOriginal) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
            
            // Save button
            IconButton(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier.size(48.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (!isSaving) {
                        buttonBgEnabled
                    } else {
                        buttonBgDisabled
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = stringResource(R.string.save),
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            
            // Cancel button
            IconButton(
                onClick = onNew,
                modifier = Modifier.size(48.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Horizontal version of the action bar.
 * Useful for wide/landscape images so the controls don't cover the image content.
 */
@Composable
fun TopActionBarHorizontal(
    canUndo: Boolean,
    canRedo: Boolean,
    showOriginal: Boolean,
    isSaving: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggle: () -> Unit,
    onSave: () -> Unit,
    onNew: () -> Unit,
    flipUndoRedoForRtl: Boolean = false,
    modifier: Modifier = Modifier
) {
    val iconFlip = if (flipUndoRedoForRtl) Modifier.graphicsLayer { scaleX = -1f } else Modifier
    // Slightly gray button backgrounds (requested) so they stand out on black UI.
    val buttonBgEnabled = Color(0xFF2E2E2E)
    // Disabled was too dark on black; make it a bit brighter.
    val buttonBgDisabled = Color(0xFF242424)
    // Selected/active state: slightly brighter than enabled.
    val buttonBgSelected = Color(0xFF3D3D3D)

    val cardShape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, cardShape),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(48.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (canUndo) {
                        buttonBgEnabled
                    } else {
                        buttonBgDisabled
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = stringResource(R.string.undo),
                            modifier = Modifier.size(24.dp).then(iconFlip),
                            tint = if (canUndo) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            }
                        )
                    }
                }
            }

            // Redo
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(48.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (canRedo) {
                        buttonBgEnabled
                    } else {
                        buttonBgDisabled
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = stringResource(R.string.redo),
                            modifier = Modifier.size(24.dp).then(iconFlip),
                            tint = if (canRedo) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            }
                        )
                    }
                }
            }

            // Toggle original/edited
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(48.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (showOriginal) {
                        buttonBgSelected
                    } else {
                        buttonBgEnabled
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = stringResource(R.string.toggle_original_edited),
                            modifier = Modifier.size(24.dp),
                            tint = if (showOriginal) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }

            // Save
            IconButton(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier.size(48.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (!isSaving) {
                        buttonBgEnabled
                    } else {
                        buttonBgDisabled
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = stringResource(R.string.save),
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Cancel / New
            IconButton(
                onClick = onNew,
                modifier = Modifier.size(48.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}
