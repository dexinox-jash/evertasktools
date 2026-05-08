package com.evertask.app.ui.theme.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun HeroChip(
    label: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    AssistChip(
        onClick = onClick,
        label = label,
        modifier = modifier,
        leadingIcon = leadingIcon,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}
