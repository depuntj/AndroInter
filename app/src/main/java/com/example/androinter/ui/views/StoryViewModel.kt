package com.example.androinter.ui.views

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.androinter.data.StoryRepository
import com.example.androinter.data.models.ListStoryItem
import kotlinx.coroutines.launch

class StoryViewModel(private val repository: StoryRepository) : ViewModel() {

    private val _stories = MutableLiveData<List<ListStoryItem>?>()
    val stories: MutableLiveData<List<ListStoryItem>?> = _stories

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _storiesWithLocation = MutableLiveData<List<ListStoryItem>?>()
    val storiesWithLocation: LiveData<List<ListStoryItem>?> = _storiesWithLocation

    fun getStoriesPaging(token: String): LiveData<PagingData<ListStoryItem>> {
        return repository.getStoriesPaging(token).cachedIn(viewModelScope)
    }


    fun fetchStories(token: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.getStories(token)
            if (result != null) {
                _stories.value = result
            } else {
                _errorMessage.value = "Failed to fetch stories."
            }
            _isLoading.value = false
        }
    }
    fun fetchStoriesWithLocation(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val stories = repository.getStoriesWithLocation(token)
                _storiesWithLocation.value = stories
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}
