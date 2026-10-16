package com.barrioahorro.app.ui.screens.business

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.MutedLabel
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.WarmGray
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.barrioahorro.app.R
import com.barrioahorro.app.ui.components.BusinessBottomBar
import com.barrioahorro.app.ui.components.BusinessTab
import com.barrioahorro.app.ui.components.profile.ErrorHint
import com.barrioahorro.app.ui.components.profile.PrimaryPillButton
import com.barrioahorro.app.ui.screens.onboarding.DayScheduleUi
import androidx.compose.ui.graphics.Color

@Composable
fun BusinessProfileScreen(
    onEditData: () -> Unit,
    onEditPhotos: () -> Unit,
    viewModel: BusinessProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val modifier = Modifier.fillMaxSize()
    val error = state.error

    when {
        state.isLoading -> Box(modifier, Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }

        error != null -> Column(
            modifier = modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ErrorHint(text = error)
            Spacer(Modifier.height(16.dp))
            PrimaryPillButton(text = "Reintentar", onClick = viewModel::load, height = 48.dp)
        }

        else -> ProfileBody(state, onEditData, onEditPhotos, modifier)
    }
}

@Composable
private fun ProfileBody(
    state: BusinessProfileUiState,
    onEditData: () -> Unit,
    onEditPhotos: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Cover(state.coverUrl)

        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileCard {
                Text(
                    text = state.businessName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                )
                if (state.categoryName.isNotBlank()) {
                    Text(
                        text = state.categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Primary,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EditButton("Editar datos", Icons.Outlined.Edit, onEditData, Modifier.weight(1f))
                EditButton("Editar foto", Icons.Outlined.CameraAlt, onEditPhotos, Modifier.weight(1f))
            }

            ProfileCard {
                InfoBlock("Dirección", state.direccion.ifBlank { "Sin dirección cargada" })
                if (state.descripcion.isNotBlank()) InfoBlock("Descripción", state.descripcion)
                Schedule(state.schedule)
            }
        }
    }
}

@Composable
private fun Cover(url: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color(0xFFF3ECE4)),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Outlined.Storefront,
                contentDescription = null,
                tint = MutedLabel,
                modifier = Modifier.size(56.dp),
            )
        }
    }
}

@Composable
private fun ProfileCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }
}

@Composable
private fun EditButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    val primary = Primary
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, primary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = primary),
        modifier = modifier.height(48.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoBlock(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = WarmGray,
        )
    }
}

@Composable
private fun Schedule(days: List<DayScheduleUi>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Horarios",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
        days.forEach { day ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink,
                )
                Text(
                    text = if (day.enabled && day.slots.isNotEmpty()) {
                        day.slots.joinToString(" y ") { "${it.horaInicio.take(5)} – ${it.horaFin.take(5)}" }
                    } else {
                        "Cerrado"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray,
                )
            }
        }
    }
}