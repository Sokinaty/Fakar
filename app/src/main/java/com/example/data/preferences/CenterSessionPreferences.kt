package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.centerSessionStore by preferencesDataStore(name = "fekr_center_session")

data class CenterSession(
    val isLoggedIn: Boolean = false,
    val role: String = "parent",
    val username: String = "",
    val displayName: String = "",
    val studentId: String = ""
)

class CenterSessionPreferences(private val context: Context) {
    private object Keys {
        val IS_LOGGED_IN = booleanPreferencesKey("session_logged_in")
        val ROLE = stringPreferencesKey("session_role")
        val USERNAME = stringPreferencesKey("session_username")
        val DISPLAY_NAME = stringPreferencesKey("session_display_name")
        val STUDENT_ID = stringPreferencesKey("session_student_id")
    }

    val sessionFlow: Flow<CenterSession> = context.centerSessionStore.data.map { prefs ->
        CenterSession(
            isLoggedIn = prefs[Keys.IS_LOGGED_IN] ?: false,
            role = prefs[Keys.ROLE] ?: "parent",
            username = prefs[Keys.USERNAME] ?: "",
            displayName = prefs[Keys.DISPLAY_NAME] ?: "",
            studentId = prefs[Keys.STUDENT_ID] ?: ""
        )
    }

    suspend fun saveSession(session: CenterSession) {
        context.centerSessionStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = session.isLoggedIn
            prefs[Keys.ROLE] = session.role
            prefs[Keys.USERNAME] = session.username
            prefs[Keys.DISPLAY_NAME] = session.displayName
            prefs[Keys.STUDENT_ID] = session.studentId
        }
    }

    suspend fun clearSession() {
        context.centerSessionStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = false
            prefs[Keys.ROLE] = "parent"
            prefs[Keys.USERNAME] = ""
            prefs[Keys.DISPLAY_NAME] = ""
            prefs[Keys.STUDENT_ID] = ""
        }
    }
}
