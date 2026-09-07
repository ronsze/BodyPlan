package kr.sdbk.bodyplan.core.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bodyplan")

/**
 * 행이 아니라 값 하나씩 두는 설정. Room에 넣지 않는 이유다.
 *
 * 제공자와 키는 한 벌이라 함께 쓰고 함께 지운다.
 */
@Singleton
class AppPreferences
@Inject
constructor(context: Context) {
    private val store = context.dataStore

    val aiProvider: Flow<String?> = store.data.map { it[AI_PROVIDER] }

    val aiToken: Flow<String?> = store.data.map { it[AI_TOKEN] }

    suspend fun getAiProvider(): String? = aiProvider.first()

    suspend fun getAiToken(): String? = aiToken.first()

    suspend fun saveAiCredential(provider: String, token: String) {
        store.edit {
            it[AI_PROVIDER] = provider
            it[AI_TOKEN] = token
        }
    }

    suspend fun clearAiCredential() {
        store.edit {
            it.remove(AI_PROVIDER)
            it.remove(AI_TOKEN)
        }
    }
}

private val AI_PROVIDER = stringPreferencesKey("ai_provider")
private val AI_TOKEN = stringPreferencesKey("ai_token")
