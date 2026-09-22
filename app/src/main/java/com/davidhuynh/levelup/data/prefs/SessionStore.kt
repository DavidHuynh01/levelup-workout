package com.davidhuynh.levelup.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.davidhuynh.levelup.domain.logic.RestTimer
import com.davidhuynh.levelup.domain.model.Session
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.util.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration

interface SessionStore {
    /** Emits null when there is no session or the stored one has expired. */
    val session: Flow<Session?>

    suspend fun save(userId: String, token: String, ttl: Duration = DEFAULT_TTL)

    /** Slides the expiry forward, so an active user is never logged out mid-use. */
    suspend fun touch(ttl: Duration = DEFAULT_TTL)

    suspend fun clear()

    companion object {
        val DEFAULT_TTL: Duration = Duration.ofDays(30)
    }
}

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore("levelup_session")

/**
 * DataStore rather than EncryptedSharedPreferences: that library was deprecated in 2025
 * and brought keyset corruption and main-thread I/O with it. Nothing secret is kept here
 * anyway — the password hash lives in the database and the plaintext is never stored.
 */
class DataStoreSessionStore(
    private val context: Context,
    private val clock: AppClock,
) : SessionStore {

    override val session: Flow<Session?> = context.sessionDataStore.data.map { prefs ->
        val userId = prefs[KEY_USER_ID] ?: return@map null
        val token = prefs[KEY_TOKEN] ?: return@map null
        val createdAt = prefs[KEY_CREATED_AT] ?: 0L
        val expiresAt = prefs[KEY_EXPIRES_AT] ?: 0L
        val candidate = Session(userId, token, createdAt, expiresAt)
        if (candidate.isExpiredAt(clock.nowMillis())) null else candidate
    }

    override suspend fun save(userId: String, token: String, ttl: Duration) {
        val now = clock.nowMillis()
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
            prefs[KEY_TOKEN] = token
            prefs[KEY_CREATED_AT] = now
            prefs[KEY_EXPIRES_AT] = now + ttl.toMillis()
        }
    }

    override suspend fun touch(ttl: Duration) {
        context.sessionDataStore.edit { prefs ->
            if (prefs[KEY_USER_ID] == null) return@edit
            prefs[KEY_EXPIRES_AT] = clock.nowMillis() + ttl.toMillis()
        }
    }

    override suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }

    private companion object {
        val KEY_USER_ID = stringPreferencesKey("session_user_id")
        val KEY_TOKEN = stringPreferencesKey("session_token")
        val KEY_CREATED_AT = longPreferencesKey("session_created_at")
        val KEY_EXPIRES_AT = longPreferencesKey("session_expires_at")
    }
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("levelup_settings")

/** Display preferences that are per device rather than per account. */
class UserPreferencesStore(private val context: Context) {

    val weightUnit: Flow<WeightUnit> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_WEIGHT_UNIT]?.let { name ->
            WeightUnit.entries.firstOrNull { it.name == name }
        } ?: WeightUnit.LB
    }

    suspend fun setWeightUnit(unit: WeightUnit) {
        context.settingsDataStore.edit { it[KEY_WEIGHT_UNIT] = unit.name }
    }

    /** Remembered between sessions: most people rest the same length every workout. */
    val restSeconds: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_REST_SECONDS] ?: RestTimer.DEFAULT_SECONDS
    }

    suspend fun setRestSeconds(seconds: Int) {
        context.settingsDataStore.edit { it[KEY_REST_SECONDS] = seconds }
    }

    private companion object {
        val KEY_WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val KEY_REST_SECONDS = intPreferencesKey("rest_seconds")
    }
}
