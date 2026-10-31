package com.barrioahorro.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Primary

@Composable
fun ToldoConFranjas(
    modifier: Modifier = Modifier,
    stripes: Int = 14,
    height: Dp = 14.dp,
    altColor: Color = CardWhite,
    stripeColor: Color = Primary,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        drawRect(color = altColor)
        if (stripes <= 0) return@Canvas

        val stripeWidth = size.width / stripes
        for (index in 0 until stripes step 2) {
            drawRect(
                color = stripeColor,
                topLeft = Offset(index * stripeWidth, 0f),
                size = Size(stripeWidth, size.height),
            )
        }
    }
}