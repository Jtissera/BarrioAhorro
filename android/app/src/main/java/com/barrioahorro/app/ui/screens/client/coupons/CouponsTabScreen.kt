package com.barrioahorro.app.ui.screens.client.coupons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.barrioahorro.app.ui.components.ToldoConFranjas
import com.barrioahorro.app.ui.theme.Cream

/** Pestaña Cupones: toldo + selector Cupones | Sorteos + el contenido de cada sección. */
@Composable
fun CouponsTabScreen(
    onGoToForum: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenRedemption: (Long) -> Unit = {},
    onMyRedemptions: () -> Unit = {},
) {
    var section by rememberSaveable { mutableStateOf(CouponsSection.CUPONES) }

    Column(modifier.fillMaxSize()) {
        ToldoConFranjas(altColor = Cream, modifier = Modifier.fillMaxWidth())
        CouponsSectionToggle(selected = section, onSelect = { section = it })

        when (section) {
            CouponsSection.CUPONES -> CouponsScreen(
                modifier = Modifier.weight(1f),
                onOpenRedemption = onOpenRedemption,
                onMyRedemptions = onMyRedemptions,
            )

            CouponsSection.SORTEOS -> RafflesSection(
                onGoToForum = onGoToForum,
                modifier = Modifier.weight(1f),
            )
        }
    }
}