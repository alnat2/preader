package com.example.preader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.preader.domain.ReadingPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val PAGES_KEY = stringPreferencesKey("pages_history")
    
    // Configured JSON to ignore unknown keys for backwards compatibility
    private val json = Json { ignoreUnknownKeys = true }

    val pagesFlow: Flow<List<ReadingPage>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[PAGES_KEY] ?: "[]"
        try {
            json.decodeFromString<List<ReadingPage>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addOrUpdatePage(page: ReadingPage) {
        context.dataStore.edit { preferences ->
            val jsonString = preferences[PAGES_KEY] ?: "[]"
            val currentPages = try {
                json.decodeFromString<List<ReadingPage>>(jsonString).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            val existingIndex = currentPages.indexOfFirst { it.id == page.id }
            if (existingIndex != -1) {
                // Update existing page but preserve firstOpenedAt to maintain order
                val existingPage = currentPages[existingIndex]
                currentPages[existingIndex] = page.copy(firstOpenedAt = existingPage.firstOpenedAt)
            } else {
                currentPages.add(page)
            }

            // Sort by firstOpenedAt (first open - first display implies descending or ascending? 
            // "first open - first display" means oldest first or newest first? 
            // Usually history is newest first, but "first open - first display" translates to sorting by firstOpenedAt ascending or descending. 
            // Actually, "Порядок списка: first open - first display. Файлы отображаются в том порядке, в котором они были впервые добавлены в историю. Повторное открытие файла не меняет его позицию в списке."
            // This means we just append and keep the original insertion order, which is sorting by firstOpenedAt ascending, or simply appending.
            
            // To ensure strict order according to firstOpenedAt:
            currentPages.sortBy { it.firstOpenedAt }

            preferences[PAGES_KEY] = json.encodeToString(currentPages)
        }
    }

    suspend fun resetProgress(pageId: String) {
        context.dataStore.edit { preferences ->
            val jsonString = preferences[PAGES_KEY] ?: "[]"
            val currentPages = try {
                json.decodeFromString<List<ReadingPage>>(jsonString).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            val existingIndex = currentPages.indexOfFirst { it.id == pageId }
            if (existingIndex != -1) {
                currentPages[existingIndex] = currentPages[existingIndex].copy(
                    positionRatio = 0.0,
                    progressPercent = 0
                )
                preferences[PAGES_KEY] = json.encodeToString(currentPages)
            }
        }
    }

    suspend fun deletePage(pageId: String) {
        context.dataStore.edit { preferences ->
            val jsonString = preferences[PAGES_KEY] ?: "[]"
            val currentPages = try {
                json.decodeFromString<List<ReadingPage>>(jsonString).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            val existingIndex = currentPages.indexOfFirst { it.id == pageId }
            if (existingIndex != -1) {
                currentPages.removeAt(existingIndex)
                preferences[PAGES_KEY] = json.encodeToString(currentPages)
            }
        }
    }
}
