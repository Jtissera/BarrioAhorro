package com.barrioahorro.app.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
private val KEY_USER_TYPE = stringPreferencesKey("user_type")


class AuthTokenDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val accessToken: Flow<String?> = dataStore.data.map { it[KEY_ACCESS_TOKEN] }

    suspend fun currentToken(): String? = accessToken.first()

    suspend fun saveSession(accessToken: String, userType: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_USER_TYPE] = userType
        }
    }

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_USER_TYPE)
        }
    }
}