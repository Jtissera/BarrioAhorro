package com.barrioahorro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.R
import androidx.compose.ui.graphics.Color

@Composable
fun ToldoConFranjas(
    modifier: Modifier = Modifier,
    stripes: Int = 14,
    height: Dp = 14.dp,
    altColor: Color = colorResource(R.color.surface_card),
) {
    val red = colorResource(R.color.primary)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        repeat(stripes) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (index % 2 == 0) red else altColor),
            )
        }
    }
}