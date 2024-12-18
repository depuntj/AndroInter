package com.example.androinter.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.Binder
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.bumptech.glide.Glide
import com.example.androinter.R
import com.example.androinter.data.RetrofitInstance
import com.example.androinter.data.SessionManager
import com.example.androinter.data.StoryRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

class StackRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private val stories = mutableListOf<StoryItem>()
    private val sessionManager = SessionManager(context)

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val identityToken = Binder.clearCallingIdentity()
        try {
            runBlocking {
                val token = sessionManager.authToken.firstOrNull()
                if (!token.isNullOrEmpty()) {
                    val storyRepository = StoryRepository(RetrofitInstance.getApiService(token))
                    val fetchedStories = storyRepository.getStories(token)

                    stories.clear()
                    fetchedStories?.forEach { story ->
                        try {
                            val bitmap = Glide.with(context)
                                .asBitmap()
                                .load(story.photoUrl)
                                .submit()
                                .get()

                            stories.add(
                                StoryItem(
                                    story.name,
                                    story.description,
                                    bitmap
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identityToken)
        }
    }

    override fun onDestroy() {
        stories.clear()
    }

    override fun getCount(): Int = stories.size

    override fun getViewAt(position: Int): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_item)
        if (position < stories.size) {
            val story = stories[position]
            rv.setImageViewBitmap(R.id.widget_item_image, story.image)
            rv.setTextViewText(R.id.widget_item_name, story.name)
            rv.setTextViewText(R.id.widget_item_description, story.description)
        }
        return rv
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    data class StoryItem(
        val name: String,
        val description: String,
        val image: Bitmap
    )
}