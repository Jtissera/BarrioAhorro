package com.barrioahorro.app.ui.cuponera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.data.cuponera.CommitmentOverviewDto
import com.barrioahorro.app.data.cuponera.RejectionDto
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.ErrorRed
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.PrimaryContainer
import com.barrioahorro.app.ui.theme.SurfaceSecondary
import com.barrioahorro.app.ui.theme.WarmGray

/** Pantalla del Gate para un comercio con acceso limitado: le indica qué falta ratificar. */
@Composable
fun RestrictedAccessScreen(
    onRatifyCuponera: () -> Unit,
    onRatifyRaffle: () -> Unit,
    onViewProfile: () -> Unit,
    viewModel: MyContributionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream),
    ) {
        ContributionTopBar(title = "Acceso restringido")

        val overview = state.overview
        when {
            overview != null -> RestrictedContent(
                overview = overview,
                onRatifyCuponera = onRatifyCuponera,
                onRatifyRaffle = onRatifyRaffle,
                onViewProfile = onViewProfile,
                modifier = Modifier.weight(1f),
            )

            state.loading -> Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Primary)
            }

            else -> ContributionErrorState(
                message = state.error ?: "No se pudo cargar tu estado.",
                onRetry = viewModel::load,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RestrictedContent(
    overview: CommitmentOverviewDto,
    onRatifyCuponera: () -> Unit,
    onRatifyRaffle: () -> Unit,
    onViewProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ratifyNow: (() -> Unit)? = when {
        overview.rejection?.kind == "SORTEO" && overview.raffle.canRatify -> onRatifyRaffle
        overview.rejection?.kind == "CUPON" && overview.cuponera.canRatify -> onRatifyCuponera
        overview.cuponera.canRatify -> onRatifyCuponera
        overview.raffle.canRatify -> onRatifyRaffle
        else -> null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 64.dp, bottom = 20.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardWhite,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = OnPrimary,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
                val rejection = overview.rejection

                StatusPill("MODO COMERCIANTE", SurfaceSecondary, Ink)

                Text(
                    text = "Tu acceso está limitado",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = InkBlack,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (rejection != null) {
                        "El administrador revisó tu publicación y requiere ajustes para habilitar los beneficios barriales."
                    } else {
                        "Para seguir usando la app, ratificá tu aporte del período."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray,
                    textAlign = TextAlign.Center,
                )

                if (rejection != null) {
                    RejectionCard(rejection)
                } else {
                    if (overview.cuponera.canRatify) PendingRow("Productos de la cuponera")
                    if (overview.raffle.canRatify) PendingRow("Premio del sorteo")
                }

                if (overview.cuponera.canRatify) {
                    PendingRow("Productos de la cuponera")
                }
                if (overview.raffle.canRatify) {
                    PendingRow("Premio del sorteo")
                }

                if (ratifyNow != null) {
                    Button(
                        onClick = ratifyNow,
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) {
                        Text("Ratificar ahora →", fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(onClick = onViewProfile) {
                    Text("Ver mi perfil", color = Ink, fontWeight = FontWeight.SemiBold)
                }

                Text(
                    text = "Mientras tanto podés ver tu perfil, sin editarlo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PendingRow(title: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceSecondary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(
                    text = "Ratificación pendiente",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Lock, contentDescription = null, tint = WarmGray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun RejectionCard(rejection: RejectionDto) {
    Surface(shape = RoundedCornerShape(16.dp), color = SurfaceSecondary, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(rejection.title, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text(
                        "Validación rechazada",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed,
                    )
                }
                Icon(Icons.Default.Lock, contentDescription = null, tint = WarmGray, modifier = Modifier.size(16.dp))
            }
            Surface(shape = RoundedCornerShape(10.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Motivo: ${rejection.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink,
                    modifier = Modifier.padding(10.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Canjes y sorteos inhabilitados hasta ratificar.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ErrorRed,
                )
            }
        }
    }
}