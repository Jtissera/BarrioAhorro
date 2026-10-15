package com.barrioahorro.app.ui.screens.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.remote.api.BusinessApiService
import com.barrioahorro.app.data.remote.dto.OfferResponseDto
import com.barrioahorro.app.domain.repository.IOfferRepository
import com.barrioahorro.app.domain.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

enum class OfferTabFilter {
    VIGENTES, FINALIZADAS
}

data class MyOfferItemUi(
    val id: Long,
    val productName: String,
    val photoUrl: String?,
    val badgeText: String,
    val isYellowBadge: Boolean = false,
    val priceMain: String,
    val priceUnit: String? = null,
    val businessName: String,
    val categoryName: String,
    val distanceText: String = "a 250 m",
    val vigenciaText: String,
    val confirmedCountText: String = "0 vecinos confirmaron",
    val isVigente: Boolean,
    val isActiva: Boolean,
)

data class MyOffersUiState(
    val isLoading: Boolean = true,
    val businessName: String = "",
    val categoryName: String = "",
    val selectedTab: OfferTabFilter = OfferTabFilter.VIGENTES,
    val allOffers: List<MyOfferItemUi> = emptyList(),
    val activeCount: Int = 0,
    val finishedCount: Int = 0,
    val filteredOffers: List<MyOfferItemUi> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class MyOffersViewModel @Inject constructor(
    private val businessApiService: BusinessApiService,
    private val offerRepository: IOfferRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyOffersUiState())
    val uiState: StateFlow<MyOffersUiState> = _uiState.asStateFlow()

    private val esAR = Locale.forLanguageTag("es-AR")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Info del comercio
            var bName = _uiState.value.businessName
            var catName = _uiState.value.categoryName
            runCatching {
                val bRes = businessApiService.getMyBusiness()
                if (bRes.isSuccessful) {
                    val b = bRes.body()
                    bName = b?.businessName.orEmpty()
                    catName = b?.categoryName.orEmpty()
                }
            }

            // 2. Ofertas del comercio
            when (val offersResult = offerRepository.getMyOffers()) {
                is Result.Success -> {
                    val rawOffers = offersResult.value
                    val mapped = rawOffers.map { dto ->
                        mapOfferDtoToUi(dto, bName, catName)
                    }

                    val active = mapped.filter { it.isVigente && it.isActiva }
                    val finished = mapped.filter { !it.isVigente || !it.isActiva }

                    _uiState.update { current ->
                        val currentTab = current.selectedTab
                        current.copy(
                            isLoading = false,
                            businessName = bName,
                            categoryName = catName,
                            allOffers = mapped,
                            activeCount = active.size,
                            finishedCount = finished.size,
                            filteredOffers = if (currentTab == OfferTabFilter.VIGENTES) active else finished,
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            businessName = bName,
                            categoryName = catName,
                            errorMessage = "No se pudieron cargar las ofertas",
                        )
                    }
                }
            }
        }
    }

    fun selectTab(tab: OfferTabFilter) {
        _uiState.update { current ->
            val list = if (tab == OfferTabFilter.VIGENTES) {
                current.allOffers.filter { it.isVigente && it.isActiva }
            } else {
                current.allOffers.filter { !it.isVigente || !it.isActiva }
            }
            current.copy(
                selectedTab = tab,
                filteredOffers = list,
            )
        }
    }

    private fun mapOfferDtoToUi(
        dto: OfferResponseDto,
        bName: String,
        catName: String,
    ): MyOfferItemUi {
        val tipo = dto.tipoOferta.uppercase()

        var badge = "PROMO"
        var isYellow = false
        var price = "$ —"
        var unit: String? = null

        when {
            tipo.contains("PORCENTA") -> {
                val p = dto.detallePorcentaje
                badge = if (p != null) "-${p.porcentajeDescuento}%" else "-%"
                val finalP = p?.precioFinal ?: p?.precioOriginal
                if (finalP != null) price = formatMoney(finalP)
            }
            tipo.contains("2X1") || tipo.contains("DOS") -> {
                val d = dto.detalle2x1
                badge = if (d != null) "${d.unidadesALlevar}X${d.unidadesAPagar}" else "2X1"
                if (d?.precioUnitario != null) {
                    price = formatMoney(d.precioUnitario)
                    unit = "c/u"
                }
            }
            tipo.contains("MAYOR") -> {
                val m = dto.detallePorMayor
                badge = if (m != null) "Desde ${m.cantidadMinima} u." else "X MAYOR"
                isYellow = true
                if (m?.precioUnitarioMayorista != null) {
                    price = formatMoney(m.precioUnitarioMayorista)
                    unit = "c/u"
                }
            }
            tipo.contains("CANTIDAD") -> {
                val c = dto.detalleCantidad
                badge = if (c != null) "${ordinal(c.cantidadRequerida)} AL ${c.porcentajeDescuentoUltimaUnidad}%" else "PROMO"
                if (c?.precioUnitario != null) {
                    price = formatMoney(c.precioUnitario)
                }
            }
        }

        val vigencia = formatVigencia(dto.vigenciaTipo, dto.vigenteHasta)

        return MyOfferItemUi(
            id = dto.id,
            productName = dto.nombreProducto,
            photoUrl = dto.fotoUrl,
            badgeText = badge,
            isYellowBadge = isYellow,
            priceMain = price,
            priceUnit = unit,
            businessName = bName.ifBlank { "Mi Comercio" },
            categoryName = catName.ifBlank { "Comercio" },
            vigenciaText = vigencia,
            confirmedCountText = "0 vecinos confirmaron",
            isVigente = dto.vigente,
            isActiva = dto.activa,
        )
    }

    private fun formatVigencia(vigenciaTipo: String, vigenteHasta: String?): String {
        return when (vigenciaTipo.uppercase()) {
            "HASTA_AGOTAR_STOCK" -> "Hasta agotar stock"
            "SOLO_HOY" -> "Hasta hoy 23:59"
            "TODA_LA_SEMANA" -> "Toda la semana"
            "FECHA_ESPECIFICA" -> {
                if (vigenteHasta == null) return "Fecha a definir"
                runCatching {
                    val odt = OffsetDateTime.parse(vigenteHasta)
                    val date = odt.toLocalDate()
                    val today = LocalDate.now()
                    val time = odt.format(timeFormatter)
                    when {
                        date == today -> "Hasta hoy $time"
                        date == today.plusDays(1) -> "Hasta mañana $time"
                        date <= today.plusDays(6) -> {
                            val dayName = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, esAR)
                                .replaceFirstChar { it.lowercase() }
                            "Hasta el $dayName"
                        }
                        else -> "Hasta el ${date.format(DateTimeFormatter.ofPattern("dd/MM"))}"
                    }
                }.getOrDefault("Fecha a definir")
            }
            else -> "Hasta agotar stock"
        }
    }

    private fun formatMoney(value: BigDecimal): String {
        val nf = NumberFormat.getNumberInstance(esAR).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        return "$ " + nf.format(value)
    }

    private fun ordinal(n: Short): String = when (n.toInt()) {
        1, 3 -> "${n}RA"
        2 -> "2DA"
        4, 5, 6 -> "${n}TA"
        7, 10 -> "${n}MA"
        8 -> "8VA"
        9 -> "9NA"
        else -> "${n}ª"
    }
}
