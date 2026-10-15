package com.barrioahorro.app.ui.screens.offers

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.barrioahorro.app.ui.components.BusinessTab

@Composable
fun CreateOfferScreen(
    onClose: () -> Unit,
    onPublished: () -> Unit,
    onTabSelected: (BusinessTab) -> Unit = {},
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
            onTabSelected = onTabSelected,
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
                onUnitsToTakeIncrement = viewModel::incrementUnitsToTake,
                onUnitsToTakeDecrement = viewModel::decrementUnitsToTake,
                onUnitsToPayIncrement = viewModel::incrementUnitsToPay,
                onUnitsToPayDecrement = viewModel::decrementUnitsToPay,
                onWholesalePriceChange = viewModel::setWholesalePrice,
                onMinQuantityChange = viewModel::setMinQuantity,
                onMinQuantityIncrement = viewModel::incrementMinQuantity,
                onMinQuantityDecrement = viewModel::decrementMinQuantity,
                onVigenciaTipoChange = viewModel::setVigenciaTipo,
                onValidUntilChange = viewModel::setValidUntil,
                onBack = viewModel::clearType,
                onSubmit = viewModel::submit,
                onPhotoPicked = viewModel::onPhotoPicked,
                onRemovePhoto = viewModel::removePhoto,
                onTabSelected = onTabSelected,
            )
        }
    }
}

