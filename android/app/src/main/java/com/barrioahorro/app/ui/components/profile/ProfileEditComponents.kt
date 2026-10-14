package com.barrioahorro.app.ui.components.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.R

private const val AWNING_STRIPES = 12

/** Striped awning accent shown at the top of the merchant screens. */
@Composable
fun AwningStripe(modifier: Modifier = Modifier) {
    val red = colorResource(R.color.primary)
    val cream = colorResource(R.color.splash_background)
    Row(modifier = modifier.fillMaxWidth().height(10.dp)) {
        repeat(AWNING_STRIPES) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .background(if (index % 2 == 0) red else cream),
            )
        }
    }
}

/** Top bar of the merchant edit screens: awning stripe, back action and title. */
@Composable
fun ProfileTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().background(colorResource(R.color.splash_background))) {
        AwningStripe()
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 16.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = colorResource(R.color.text_primary),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colorResource(R.color.text_primary),
            )
        }
    }
}

/** Form field header: label on the left and an optional trailing slot (hint, counter, action). */
@Composable
fun FormFieldHeader(
    label: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = colorResource(R.color.text_primary),
        )
        trailing?.invoke()
    }
}

/** Small rounded status pill ("Abierto", "Cerrado", "REQUIERE ATENCIÓN"...). */
@Composable
fun StatusChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
) {
    Row(
        modifier = modifier
            .background(containerColor, CircleShape)
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) {
            Box(modifier = Modifier.size(6.dp).background(dotColor, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = contentColor)
    }
}

/** Red inline hint with a warning icon, used for validation and save errors. */
@Composable
fun ErrorHint(text: String, modifier: Modifier = Modifier) {
    val color = colorResource(R.color.primary)
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

/** Full-width red pill button with an optional leading icon (title typography, like the Figma save CTA). */
@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    height: Dp = 56.dp,
) {
    val onPrimary = colorResource(R.color.on_primary)
    Button(
        onClick = { if (!isLoading) onClick() },
        enabled = enabled,
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(R.color.primary),
            contentColor = onPrimary,
            disabledContainerColor = colorResource(R.color.neutral_inactive),
            disabledContentColor = onPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth().height(height),
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = onPrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Cream pill button used for secondary actions such as "Cancelar". */
@Composable
fun CreamPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(R.color.splash_background),
            contentColor = colorResource(R.color.text_primary),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth().height(height),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

/** Floating bottom surface that holds the main CTA of a form screen. */
@Composable
fun BottomActionSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        color = colorResource(R.color.surface_bottom_bar).copy(alpha = 0.95f),
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, ambientColor = SHADOW_UMBER, spotColor = SHADOW_UMBER),
    ) {
        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        ) {
            content()
        }
    }
}

private val SHADOW_UMBER = Color(0x142E1F17)

@Preview(showBackground = true, backgroundColor = 0xFFFBF6E6)
@Composable
private fun ProfileEditComponentsPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
        AwningStripe()
        FormFieldHeader(label = "Rubro principal") {
            Text("Obligatorio", style = MaterialTheme.typography.bodySmall)
        }
        StatusChip(
            text = "Abierto",
            containerColor = colorResource(R.color.secondary_container),
            contentColor = colorResource(R.color.secondary),
            dotColor = colorResource(R.color.secondary),
        )
        ErrorHint(text = "Ingresá la dirección")
        PrimaryPillButton(text = "Guardar cambios", onClick = {})
        CreamPillButton(text = "Cancelar", onClick = {})
    }
}
