package com.barrioahorro.app.ui.screens.client.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.ui.components.ToldoConFranjas
import com.barrioahorro.app.ui.components.profile.ErrorHint
import com.barrioahorro.app.ui.components.profile.PrimaryPillButton
import com.barrioahorro.app.ui.theme.BadgeOrange
import com.barrioahorro.app.ui.theme.BadgeYellow
import com.barrioahorro.app.ui.theme.BadgeYellowText
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.SurfaceFieldAlt
import com.barrioahorro.app.ui.theme.Tertiary
import com.barrioahorro.app.ui.theme.TertiaryContainer
import com.barrioahorro.app.ui.theme.WarmGray

@Composable
fun ClientProfileScreen(
    onMyPosts: () -> Unit,
    onMyReviews: () -> Unit,
    onEditLocation: () -> Unit,
    onLogout: () -> Unit,
    isLoggingOut: Boolean,
    viewModel: ClientProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Puntos y contadores cambian al publicar, reseñar o comentar: se recargan al volver.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    fun comingSoon(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    Column(Modifier.fillMaxSize().background(Cream).statusBarsPadding()) {
        ToldoConFranjas(stripes = 18, height = 12.dp, altColor = Cream)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = Primary) }

                state.error != null -> {
                    ErrorHint(text = state.error.orEmpty())
                    PrimaryPillButton(text = "Reintentar", onClick = viewModel::refresh, height = 48.dp)
                }

                else -> {
                    HeaderCard(
                        name = state.name,
                        locationLabel = state.locationLabel,
                        onEditLocation = onEditLocation,
                    )

                    PointsCard(
                        points = state.points,
                        onClick = { comingSoon("Próximamente vas a poder ver el detalle de tus puntos") },
                    )

                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            MenuRow(
                                icon = Icons.Outlined.Description,
                                title = "Mis publicaciones",
                                count = state.postsCount,
                                onClick = onMyPosts,
                            )
                            MenuDivider()
                            MenuRow(
                                icon = Icons.Outlined.RateReview,
                                title = "Mis reseñas",
                                count = state.reviewsCount,
                                onClick = onMyReviews,
                            )
                            MenuDivider()
                            MenuRow(
                                icon = Icons.Outlined.ReceiptLong,
                                title = "Historial de canjes",
                                onClick = { comingSoon("Próximamente vas a poder ver tu historial de canjes") },
                            )
                            MenuDivider()
                            MenuRow(
                                icon = Icons.Outlined.ConfirmationNumber,
                                title = "Mis tickets de sorteos",
                                onClick = { comingSoon("Próximamente vas a poder ver tus tickets de sorteos") },
                            )
                            MenuDivider()
                            MenuRow(
                                icon = Icons.Outlined.MyLocation,
                                title = "Mi ubicación",
                                subtitle = state.locationLabel ?: "Configurar ubicación",
                                onClick = onEditLocation,
                            )
                        }
                    }
                }
            }

            LogoutButton(onClick = onLogout, isLoading = isLoggingOut)
        }
    }
}

@Composable
private fun HeaderCard(name: String, locationLabel: String?, onEditLocation: () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            ToldoConFranjas(stripes = 14, height = 8.dp)
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkBlack,
                )
                Row(
                    modifier = if (locationLabel == null) Modifier.clickable(onClick = onEditLocation) else Modifier,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    if (locationLabel != null) {
                        Text(locationLabel, style = MaterialTheme.typography.bodyMedium, color = WarmGray)
                    } else {
                        Text(
                            "Configurar ubicación",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PointsCard(points: Int, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = CardWhite,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(54.dp).background(TertiaryContainer, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(30.dp).background(BadgeYellowText, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Tertiary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (points == 1) "1 punto" else "$points puntos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = InkBlack,
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = CircleShape, color = BadgeYellow) {
                        Text(
                            "Canjeables",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BadgeYellowText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    text = "Ganás puntos con los upvotes que recibís.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                )
            }

            Spacer(Modifier.width(8.dp))

            Surface(
                onClick = onClick,
                shape = CircleShape,
                color = SurfaceFieldAlt,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Ver detalle de puntos",
                        tint = WarmGray,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    count: Int? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(Cream, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(22.dp))
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = WarmGray)
            }
        }

        if (count != null) {
            Surface(shape = CircleShape, color = Cream) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Ink,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 74.dp, end = 16.dp),
        color = SurfaceFieldAlt,
    )
}

@Composable
private fun LogoutButton(onClick: () -> Unit, isLoading: Boolean) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isLoading,
        shape = CircleShape,
        border = BorderStroke(1.5.dp, Primary),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Cream,
            contentColor = Primary,
            disabledContainerColor = Cream,
            disabledContentColor = Primary,
        ),
        modifier = Modifier.padding(top = 6.dp).fillMaxWidth().height(54.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}