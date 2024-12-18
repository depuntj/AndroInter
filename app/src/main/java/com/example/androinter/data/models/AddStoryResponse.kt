package com.example.androinter.data.models

import java.io.File

data class AddStoryResponse(
    val description: String,
    val photo: File,
    val lat: Float? = null,
    val lon: Float? = null,
    val error: Boolean,
    val message: String
)
