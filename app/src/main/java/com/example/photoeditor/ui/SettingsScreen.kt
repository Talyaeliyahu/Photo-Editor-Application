package com.example.photoeditor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.photoeditor.R
import com.example.photoeditor.ui.theme.LightBlueHeader
import com.example.photoeditor.ui.theme.SelectedAccentBlue
import com.example.photoeditor.utils.LocaleHelper

/**
 * Settings screen with language selection and app version
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedLanguage = LocaleHelper.getSavedLanguage(context)
    var selectedLanguage by remember { mutableStateOf(savedLanguage) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val appVersionName = remember {
        try {
            @Suppress("DEPRECATION")
            val pkgInfo = if (android.os.Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pkgInfo.versionName ?: "-"
        } catch (_: Exception) {
            "-"
        }
    }

    fun languageLabel(code: String): String {
        return when (code) {
            "he" -> context.getString(R.string.hebrew)
            "en" -> context.getString(R.string.english)
            else -> context.getString(R.string.system_default)
        }
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightBlueHeader,
                    titleContentColor = Color(0xFF1A237E),
                    navigationIconContentColor = Color(0xFF1A237E)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.language_settings),
                    style = MaterialTheme.typography.titleSmall,
                    color = SelectedAccentBlue,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.language_settings), color = Color(0xFF212121)) },
                    supportingContent = { Text(languageLabel(selectedLanguage), color = Color(0xFF616161)) },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF757575)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLanguageDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    color = Color(0xFFE0E0E0)
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.app_details),
                    style = MaterialTheme.typography.titleSmall,
                    color = SelectedAccentBlue,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.app_version), color = Color(0xFF212121)) },
                    supportingContent = { Text(appVersionName, color = Color(0xFF616161)) }
                )
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.select_language), color = Color(0xFF212121)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LanguageChoiceRow(
                        label = stringResource(R.string.system_default),
                        isSelected = selectedLanguage == "system",
                        onClick = {
                            selectedLanguage = "system"
                            onLanguageChange("system")
                            showLanguageDialog = false
                        }
                    )
                    LanguageChoiceRow(
                        label = stringResource(R.string.hebrew),
                        isSelected = selectedLanguage == "he",
                        onClick = {
                            selectedLanguage = "he"
                            onLanguageChange("he")
                            showLanguageDialog = false
                        }
                    )
                    LanguageChoiceRow(
                        label = stringResource(R.string.english),
                        isSelected = selectedLanguage == "en",
                        onClick = {
                            selectedLanguage = "en"
                            onLanguageChange("en")
                            showLanguageDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel), color = SelectedAccentBlue)
                }
            }
        )
    }
}

@Composable
private fun LanguageChoiceRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF212121)
        )
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = SelectedAccentBlue,
                unselectedColor = Color(0xFF757575)
            )
        )
    }
}
