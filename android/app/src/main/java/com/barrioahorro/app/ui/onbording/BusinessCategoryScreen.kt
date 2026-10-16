package com.barrioahorro.app.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.SetMeal
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.PrimaryContainer
import com.barrioahorro.app.ui.theme.Secondary
import com.barrioahorro.app.ui.theme.WarmGray
import com.barrioahorro.app.ui.components.feedback.ErrorCard
import com.barrioahorro.app.ui.components.onboarding.ONBOARDING_TOTAL_STEPS
import com.barrioahorro.app.ui.components.onboarding.OnboardingPrimaryButton
import com.barrioahorro.app.ui.components.onboarding.OnboardingTopBar
import java.text.Normalizer

private const val CURRENT_STEP = 2

@Composable
fun BusinessCategoryScreen(
    categories: List<CategoryUi>,
    selectedCategoryId: Int?,
    isLoadingCategories: Boolean,
    isSubmitting: Boolean,
    errorMessage: String?,
    onSelectCategory: (Int) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val background = Cream
    val textSecondary = WarmGray

    Scaffold(containerColor = background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            OnboardingTopBar(currentStep = CURRENT_STEP, onBack = onBack)

            Text(
                text = "PASO $CURRENT_STEP DE $ONBOARDING_TOTAL_STEPS",
                style = MaterialTheme.typography.labelSmall,
                color = textSecondary,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = "¿Cuál es tu rubro?",
                style = MaterialTheme.typography.headlineMedium,
                color = Ink,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "Elegí uno. Lo usamos para que te encuentren al filtrar.",
                style = MaterialTheme.typography.bodyMedium,
                color = textSecondary,
                modifier = Modifier.padding(top = 8.dp),
            )

            ErrorCard(title = errorMessage)

            if (isLoadingCategories) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 20.dp),
                ) {
                    items(categories, key = { it.id }) { category ->
                        CategoryCard(
                            name = category.name,
                            icon = categoryIcon(category.name),
                            isSelected = category.id == selectedCategoryId,
                            onClick = { onSelectCategory(category.id) },
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Secondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Los vecinos podrán encontrar tus ofertas buscando directamente esta categoría.",
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary,
                )
            }

            OnboardingPrimaryButton(
                text = "Siguiente →",
                onClick = onFinish,
                enabled = selectedCategoryId != null && !isSubmitting,
                isLoading = isSubmitting,
            )
        }
    }
}

@Composable
private fun CategoryCard(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val primary = Primary
    val contentColor = if (isSelected) primary else Ink

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) PrimaryContainer else CardWhite,
        border = if (isSelected) BorderStroke(1.5.dp, primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (isSelected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp)
                        .background(primary, CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Seleccionado",
                        tint = OnPrimary,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

/** Los rubros vienen del backend (id + nombre): el ícono se elige por nombre. */
private fun categoryIcon(name: String): ImageVector {
    val normalized = Normalizer.normalize(name.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")

    return when {
        "almacen" in normalized -> Icons.Outlined.Store
        "carnicer" in normalized -> Icons.Outlined.SetMeal
        "verduler" in normalized -> Icons.Outlined.Eco
        "panader" in normalized -> Icons.Outlined.BakeryDining
        "farmacia" in normalized -> Icons.Outlined.LocalPharmacy
        "limpieza" in normalized -> Icons.Outlined.CleaningServices
        "kiosco" in normalized -> Icons.Outlined.Cookie
        else -> Icons.Outlined.Category
    }
}