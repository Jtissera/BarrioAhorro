package com.barrioahorro.app.ui.components.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.R


@Composable
fun ErrorCard(
    title: String?,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val error = colorResource(R.color.error)
    val container = colorResource(R.color.primary_container)
    val shape = RoundedCornerShape(10.dp)


    var lastTitle by remember { mutableStateOf("") }
    if (title != null) lastTitle = title

    AnimatedVisibility(
        visible = title != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(container)
                .border(1.dp, error.copy(alpha = 0.25f), shape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = lastTitle,
                style = MaterialTheme.typography.labelMedium,
                color = error,
            )
            if (actionText != null && onActionClick != null) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium,
                    color = error,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(onClick = onActionClick),
                )
            }
        }
    }
}