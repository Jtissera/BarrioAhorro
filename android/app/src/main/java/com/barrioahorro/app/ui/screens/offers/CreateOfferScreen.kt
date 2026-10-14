package com.barrioahorro.app.ui.screens.offers

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CreateOfferScreen(
    onClose: () -> Unit,
    onPublished: () -> Unit,
    viewModel: CreateOfferViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CreateOfferEvent.Published -> onPublished()
            }
        }
    }

    when (state.selectedType) {
        null -> OfferTypeSelectorScreen(
            onSelect = viewModel::selectType,
            onBack = onClose,
        )
        else -> {
            BackHandler { viewModel.clearType() }
            OfferFormScreen(
                state = state,
                onProductNameChange = viewModel::setProductName,
                onPriceChange = viewModel::setPrice,
                onDiscountChange = viewModel::setDiscount,
                onIncrementQuantity = viewModel::incrementQuantity,
                onDecrementQuantity = viewModel::decrementQuantity,
                onValidUntilChange = viewModel::setValidUntil,
                onBack = viewModel::clearType,
                onSubmit = viewModel::submit,
            )
        }
    }
}

