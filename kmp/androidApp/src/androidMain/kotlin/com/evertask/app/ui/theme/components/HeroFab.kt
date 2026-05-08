package com.evertask.app.ui.theme.components

import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HeroFab(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {}
) {
    ExtendedFloatingActionButton(
        text = { Text(text) },
        icon = icon,
        onClick = onClick,
        modifier = modifier
    )
}
