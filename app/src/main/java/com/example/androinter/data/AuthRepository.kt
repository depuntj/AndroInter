package com.example.androinter.data

import com.example.androinter.data.models.ErrorResponse
import com.example.androinter.data.models.LoginResponse
import com.example.androinter.data.models.RegisterResponse
import com.google.gson.Gson
import retrofit2.HttpException
import retrofit2.Response

class AuthRepository(private val apiService: ApiService) {

    suspend fun register(name: String, email: String, password: String): Result<RegisterResponse> {
        return try {
            val response = apiService.register(name, email, password)
            handleResponse(response)
        } catch (e: HttpException) {
            Result.failure(Exception(parseError(e)?.message ?: "Unknown error"))
        }
    }

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(email, password)
            handleResponse(response)
        } catch (e: HttpException) {
            Result.failure(Exception(parseError(e)?.message ?: "Unknown error"))
        }
    }

    private fun <T> handleResponse(response: Response<T>): Result<T> {
        return if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception(parseError(HttpException(response))?.message ?: "Unknown error"))
        }
    }

    private fun parseError(exception: HttpException): ErrorResponse? {
        return try {
            val errorJson = exception.response()?.errorBody()?.string()
            Gson().fromJson(errorJson, ErrorResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
