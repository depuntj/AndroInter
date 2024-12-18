package com.example.androinter.ui.views

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.PagingData
import androidx.recyclerview.widget.ListUpdateCallback
import com.example.androinter.utils.DataDummy
import com.example.androinter.utils.MainDispatcherRule
import com.example.androinter.utils.StoryPagingSource
import com.example.androinter.data.StoryAdapter
import com.example.androinter.data.StoryRepository
import com.example.androinter.data.models.ListStoryItem
import com.example.androinter.utils.getOrAwaitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class StoryViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    private lateinit var storyRepository: StoryRepository

    private val dummyToken = "dummy_token"

    @Test
    fun `when Get Story Should Not Null and Return Success`() = runTest {
        println("Starting test: when Get Story Should Not Null and Return Success")

        val dummyStory = DataDummy.generateDummyStoryResponse()
        println("Generated ${dummyStory.size} dummy stories")

        val data: PagingData<ListStoryItem> = StoryPagingSource.snapshot(dummyStory)
        val expectedStory = MutableLiveData<PagingData<ListStoryItem>>()
        expectedStory.value = data

        Mockito.`when`(storyRepository.getStoriesPaging(dummyToken)).thenReturn(expectedStory)
        println("Repository mock setup completed")

        val storyViewModel = StoryViewModel(storyRepository)
        val actualStory: PagingData<ListStoryItem> = storyViewModel.getStoriesPaging(dummyToken).getOrAwaitValue()

        val differ = AsyncPagingDataDiffer(
            diffCallback = StoryAdapter.DIFF_CALLBACK,
            updateCallback = noopListUpdateCallback,
            workerDispatcher = Dispatchers.Main
        )
        differ.submitData(actualStory)
        println("Submitted data to differ")

        Assert.assertNotNull("Differ snapshot should not be null", differ.snapshot())
        Assert.assertEquals("Story size should match", dummyStory.size, differ.snapshot().size)
        Assert.assertEquals("First story should match", dummyStory[0], differ.snapshot()[0])
        println("All assertions passed")
    }

    @Test
    fun `when Get Story Empty Should Return No Data`() = runTest {
        println("Starting test: when Get Story Empty Should Return No Data")

        val data: PagingData<ListStoryItem> = PagingData.from(emptyList())
        val expectedStory = MutableLiveData<PagingData<ListStoryItem>>()
        expectedStory.value = data

        Mockito.`when`(storyRepository.getStoriesPaging(dummyToken)).thenReturn(expectedStory)
        println("Repository mock setup completed")

        val storyViewModel = StoryViewModel(storyRepository)
        val actualStory: PagingData<ListStoryItem> = storyViewModel.getStoriesPaging(dummyToken).getOrAwaitValue()

        val differ = AsyncPagingDataDiffer(
            diffCallback = StoryAdapter.DIFF_CALLBACK,
            updateCallback = noopListUpdateCallback,
            workerDispatcher = Dispatchers.Main
        )
        differ.submitData(actualStory)
        println("Submitted data to differ")

        Assert.assertEquals("Story list should be empty", 0, differ.snapshot().size)
        println("Empty list assertion passed")
    }

    private val noopListUpdateCallback = object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {}
        override fun onRemoved(position: Int, count: Int) {}
        override fun onMoved(fromPosition: Int, toPosition: Int) {}
        override fun onChanged(position: Int, count: Int, payload: Any?) {}
    }
}