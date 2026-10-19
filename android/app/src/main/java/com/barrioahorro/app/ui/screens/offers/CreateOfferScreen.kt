package com.barrioahorro.app.ui.screens.offers

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CreateOfferScreen(
    onClose: () -> Unit,
    onBackToOffers: () -> Unit,
    viewModel: CreateOfferViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.publishedOfferId != null) {
        BackHandler { onBackToOffers() }
        OfferPublishedScreen(
            state = state,
            onBackToOffers = onBackToOffers,
        )
        return
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
            )
        }
    }
}