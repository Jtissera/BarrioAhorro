package com.barrioahorro.app.ui.components.inputs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.domain.usecase.auth.PasswordStrength
import com.barrioahorro.app.ui.theme.AppColors
import com.barrioahorro.app.ui.theme.ErrorRed

private const val SEGMENT_COUNT = 3
private const val ANIMATION_MS = 250

@Composable
fun PasswordStrengthIndicator(
    strength: PasswordStrength,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Seguridad de la contraseña",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.mutedLabel,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(SEGMENT_COUNT) { index ->
                val targetColor = if (index < strength.filledSegments) strength.color() else AppColors.track
                val animatedColor by animateColorAsState(
                    targetValue = targetColor,
                    animationSpec = tween(ANIMATION_MS),
                    label = "strengthSegment$index",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color = animatedColor, shape = RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

private val PasswordStrength.filledSegments: Int
    get() = when (this) {
        PasswordStrength.NONE -> 0
        PasswordStrength.WEAK -> 1
        PasswordStrength.MEDIUM -> 2
        PasswordStrength.STRONG -> 3
    }

private fun PasswordStrength.color(): Color = when (this) {
    PasswordStrength.NONE -> Color.Transparent
    PasswordStrength.WEAK -> ErrorRed
    PasswordStrength.MEDIUM -> AppColors.warning
    PasswordStrength.STRONG -> AppColors.success
}