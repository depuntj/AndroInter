package com.example.androinter.data

import kotlinx.coroutines.flow.firstOrNull
import android.content.Context
import kotlinx.coroutines.runBlocking

object Injection {
    fun provideRepository(context: Context): StoryRepository {
        val sessionManager = SessionManager(context)
        val token = runBlocking { sessionManager.authToken.firstOrNull().orEmpty() }
        val apiService = RetrofitInstance.getApiService(token)
        return StoryRepository(apiService)
    }
}
