package com.barrioahorro.app.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Paso 2 de 4", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "¿A qué se dedica?", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Elegí el rubro que mejor lo describe. Así aparece en las búsquedas correctas.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (isLoadingCategories) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(categories) { category ->
                    val isSelected = category.id == selectedCategoryId
                    if (isSelected) {
                        Button(onClick = { onSelectCategory(category.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(category.name)
                        }
                    } else {
                        OutlinedButton(onClick = { onSelectCategory(category.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(category.name)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack, enabled = !isSubmitting, modifier = Modifier.width(56.dp)) {
                Text("<")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onFinish,
                enabled = selectedCategoryId != null && !isSubmitting,
                modifier = Modifier.weight(1f),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                } else {
                    Text("Continuar")
                }
            }
        }
    }
}