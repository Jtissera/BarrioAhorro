package com.barrioahorro.app.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class ClientLocation(
    val latitude: Double,
    val longitude: Double,
    val label: String,
)

private val KEY_LATITUDE = doublePreferencesKey("client_latitude")
private val KEY_LONGITUDE = doublePreferencesKey("client_longitude")
private val KEY_LABEL = stringPreferencesKey("client_location_label")
private val KEY_DECIDED = booleanPreferencesKey("client_location_decided")

/** Ubicación del vecino: vive solo en el dispositivo y se borra al cerrar sesión. */
class ClientLocationDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val location: Flow<ClientLocation?> = dataStore.data.map { prefs ->
        val lat = prefs[KEY_LATITUDE]
        val lng = prefs[KEY_LONGITUDE]
        if (lat != null && lng != null) {
            ClientLocation(lat, lng, prefs[KEY_LABEL] ?: "Mi ubicación")
        } else {
            null
        }
    }

    /** true si el vecino ya pasó por el onboarding, aunque haya elegido no compartir ubicación. */
    val decided: Flow<Boolean> = dataStore.data.map { it[KEY_DECIDED] == true }

    suspend fun save(latitude: Double, longitude: Double, label: String) {
        dataStore.edit { prefs ->
            prefs[KEY_LATITUDE] = latitude
            prefs[KEY_LONGITUDE] = longitude
            prefs[KEY_LABEL] = label
            prefs[KEY_DECIDED] = true
        }
    }

    suspend fun markDeclined() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_LATITUDE)
            prefs.remove(KEY_LONGITUDE)
            prefs.remove(KEY_LABEL)
            prefs[KEY_DECIDED] = true
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_LATITUDE)
            prefs.remove(KEY_LONGITUDE)
            prefs.remove(KEY_LABEL)
            prefs.remove(KEY_DECIDED)
        }
    }
}