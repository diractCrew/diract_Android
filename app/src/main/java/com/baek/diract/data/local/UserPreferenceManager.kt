package com.baek.diract.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/*
    - 저장: userPreferenceManager.saveLastTeamspaceId("teamspace123")
    - 읽기: userPreferenceManager.lastTeamspaceId.first() (suspend) 또는 Flow로 collect
    - 초기화: userPreferenceManager.clear() (팀스페이스 삭제로 인해 조회가 안되거나, 로그아웃 할때)

 */
private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val lastTeamspaceId: Flow<String?> = context.userPrefsDataStore.data.map { prefs ->
        prefs[LAST_TEAMSPACE_ID_KEY]
    }

    suspend fun saveLastTeamspaceId(teamspaceId: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[LAST_TEAMSPACE_ID_KEY] = teamspaceId
        }
    }

    suspend fun clearLastTeamspace() {
        context.userPrefsDataStore.edit {
            it.remove(LAST_TEAMSPACE_ID_KEY)
        }
    }

    companion object {
        private val LAST_TEAMSPACE_ID_KEY = stringPreferencesKey("last_teamspace_id")
    }
}
