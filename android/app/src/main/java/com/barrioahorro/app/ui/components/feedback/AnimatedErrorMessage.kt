package com.barrioahorro.app.ui.components.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.barrioahorro.app.ui.theme.ErrorRed


@Composable
fun AnimatedErrorMessage(
    message: String?,
    modifier: Modifier = Modifier,
) {
    var lastMessage by remember { mutableStateOf(message) }
    LaunchedEffect(message) {
        if (message != null) lastMessage = message
    }

    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        Text(
            text = lastMessage.orEmpty(),
            color = ErrorRed,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}