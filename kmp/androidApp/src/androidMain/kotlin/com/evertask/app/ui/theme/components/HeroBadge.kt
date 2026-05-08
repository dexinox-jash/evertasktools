package com.evertask.app.ui.theme.components

import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroBadge(count: Int) {
    Badge(containerColor = MaterialTheme.colorScheme.error) {
        Text(text = count.toString(), color = MaterialTheme.colorScheme.onError)
    }
}
