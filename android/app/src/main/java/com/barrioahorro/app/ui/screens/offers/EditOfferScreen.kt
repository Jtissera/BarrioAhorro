package com.barrioahorro.app.ui.screens.offers

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary

@Composable
fun EditOfferScreen(
    onClose: () -> Unit,
    onBackToOffers: () -> Unit,
    viewModel: CreateOfferViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event == CreateOfferEvent.Deleted) {
                Toast.makeText(context, "Oferta eliminada", Toast.LENGTH_SHORT).show()
                onClose()
            }
        }
    }

    if (state.publishedOfferId != null) {
        BackHandler { onBackToOffers() }
        OfferPublishedScreen(
            state = state,
            onBackToOffers = onBackToOffers,
        )
        return
    }

    when {
        state.isLoadingOffer -> Box(
            modifier = Modifier.fillMaxSize().background(Cream),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(color = Primary) }

        state.loadError != null -> Column(
            modifier = Modifier.fillMaxSize().background(Cream).statusBarsPadding().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(text = state.loadError.orEmpty(), color = Ink, textAlign = TextAlign.Center)
            Button(
                onClick = viewModel::retryLoad,
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
            ) { Text("Reintentar") }
            TextButton(onClick = onClose) { Text("Volver", color = Ink) }
        }

        else -> OfferFormScreen(
            state = state,
            isEditing = true,
            onDelete = viewModel::delete,
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
            onBack = onClose,
            onSubmit = viewModel::submit,
            onPhotoPicked = viewModel::onPhotoPicked,
            onRemovePhoto = viewModel::removePhoto,
        )
    }
}