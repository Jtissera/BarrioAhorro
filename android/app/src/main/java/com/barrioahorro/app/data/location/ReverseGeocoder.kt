package com.barrioahorro.app.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class ReverseGeocodeResult(
    val street: String?,
    val locality: String?,
)

@Singleton
class ReverseGeocoder @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val geocoder by lazy { Geocoder(context, Locale("es", "AR")) }

    /** Devuelve calle + altura y localidad para el punto, o null si el servicio no pudo resolverlo. */
    suspend fun addressFor(latitude: Double, longitude: Double): ReverseGeocodeResult? {
        if (!Geocoder.isPresent()) return null

        val address: Address? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine<Address?> { cont ->
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (cont.isActive) cont.resume(addresses.firstOrNull())
                            }

                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(null)
                            }
                        },
                    )
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                }
            }
        } catch (e: IOException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }

        return address?.toResult()
    }

    private fun Address.toResult(): ReverseGeocodeResult? {
        val calle = thoroughfare?.takeIf { it.isNotBlank() }
        val altura = subThoroughfare?.takeIf { it.isNotBlank() }
        val street = calle?.let { listOfNotNull(it, altura).joinToString(" ") }
        val localidad = (locality ?: subAdminArea)?.takeIf { it.isNotBlank() }

        if (street == null && localidad == null) return null
        return ReverseGeocodeResult(street = street, locality = localidad)
    }
}
