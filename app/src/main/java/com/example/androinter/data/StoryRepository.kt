package com.example.androinter.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.liveData
import com.example.androinter.data.models.ListStoryItem
import com.example.androinter.utils.StoryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class StoryRepository(private val apiService: ApiService) {
    suspend fun getStories(token: String): List<ListStoryItem>? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getStories("Bearer $token")
                if (response.isSuccessful) {
                    val storyResponse = response.body()
                    if (storyResponse?.error == false) {
                        storyResponse.listStory
                    } else {
                        null
                    }
                } else {
                    Log.e("StoryRepository", "Error: ${response.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("StoryRepository", "Exception: ${e.message}")
                null
            }
        }
    }
    suspend fun addStory(
        token: String,
        description: String,
        photo: File,
        lat: Float? = null,
        lon: Float? = null
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
                val photoBody = photo.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val photoPart = MultipartBody.Part.createFormData("photo", photo.name, photoBody)

                val latBody = lat?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                val lonBody = lon?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = apiService.addStory(
                    token = "Bearer $token",
                    description = descriptionBody,
                    photo = photoPart,
                    lat = latBody,
                    lon = lonBody
                )

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    !responseBody?.error!!
                } else {
                    Log.e("StoryRepository", "Error: ${response.errorBody()?.string()}")
                    false
                }
            } catch (e: Exception) {
                Log.e("StoryRepository", "Exception: ${e.message}", e)
                false
            }
        }
    }
    suspend fun getStoriesWithLocation(token: String): List<ListStoryItem>? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getStoriesWithLocation("Bearer $token", location = 1)
                if (response.isSuccessful) {
                    val storyResponse = response.body()
                    if (storyResponse?.error == false) {
                        storyResponse.listStory
                    } else {
                        null
                    }
                } else {
                    Log.e("StoryRepository", "Error: ${response.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("StoryRepository", "Exception: ${e.message}")
                null
            }
        }
    }
    fun getStoriesPaging(token: String): LiveData<PagingData<ListStoryItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                StoryPagingSource(apiService, token)
            }
        ).liveData
    }
}
