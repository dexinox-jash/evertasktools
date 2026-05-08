package com.evertask.app.ui.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class HeroButtonVariant {
    PRIMARY,
    SECONDARY,
    ERROR
}

@Composable
fun HeroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: HeroButtonVariant = HeroButtonVariant.PRIMARY,
    enabled: Boolean = true
) {
    val containerColor = when (variant) {
        HeroButtonVariant.PRIMARY -> MaterialTheme.colorScheme.primary
        HeroButtonVariant.SECONDARY -> MaterialTheme.colorScheme.surface
        HeroButtonVariant.ERROR -> MaterialTheme.colorScheme.error
    }

    val contentColor = when (variant) {
        HeroButtonVariant.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        HeroButtonVariant.SECONDARY -> MaterialTheme.colorScheme.onSurface
        HeroButtonVariant.ERROR -> MaterialTheme.colorScheme.onError
    }

    val border = if (variant == HeroButtonVariant.SECONDARY) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    } else {
        null
    }

    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.outline
        ),
        border = border,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}
