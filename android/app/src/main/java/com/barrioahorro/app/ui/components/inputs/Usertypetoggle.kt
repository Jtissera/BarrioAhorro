package com.barrioahorro.app.ui.components.inputs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.domain.model.UserType
import com.barrioahorro.app.ui.theme.AppColors

private val TOGGLE_HEIGHT = 44.dp
private const val SLIDE_ANIMATION_MS = 220

@Composable
fun UserTypeToggle(
    selected: UserType,
    onSelectedChange: (UserType) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .height(TOGGLE_HEIGHT)
            .background(AppColors.toggleBackground, RoundedCornerShape(10.dp))
            .padding(4.dp),
    ) {
        val optionWidth = maxWidth / 2
        val indicatorOffset by animateDpAsState(
            targetValue = if (selected == UserType.CLIENTE) 0.dp else optionWidth,
            animationSpec = tween(SLIDE_ANIMATION_MS),
            label = "userTypeIndicatorOffset",
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(optionWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            UserTypeOption(
                label = "Cliente",
                isSelected = selected == UserType.CLIENTE,
                onClick = { onSelectedChange(UserType.CLIENTE) },
                modifier = Modifier.weight(1f),
            )
            UserTypeOption(
                label = "Comerciante",
                isSelected = selected == UserType.COMERCIO,
                onClick = { onSelectedChange(UserType.COMERCIO) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun UserTypeOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(SLIDE_ANIMATION_MS),
        label = "userTypeContent",
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(PaddingValues(vertical = 10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}