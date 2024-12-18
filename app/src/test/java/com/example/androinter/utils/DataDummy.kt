package com.example.androinter.utils

import com.example.androinter.data.models.ListStoryItem

object DataDummy {
    fun generateDummyStoryResponse(): List<ListStoryItem> {
        val items: MutableList<ListStoryItem> = arrayListOf()
        for (i in 0..100) {
            val story = ListStoryItem(
                id = "story-$i",
                name = "Name $i",
                description = "Description $i",
                photoUrl = "https://story-api.dicoding.dev/images/stories/photos-$i",
                createdAt = "2022-01-08T06:34:18.598Z",
                lat = -6.8919,
                lon = 107.6089
            )
            items.add(story)
        }
        return items
    }
}