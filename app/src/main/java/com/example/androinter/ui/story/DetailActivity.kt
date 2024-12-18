package com.example.androinter.ui.story

import android.os.Bundle
import android.transition.TransitionInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.androinter.R
import com.example.androinter.data.models.ListStoryItem

class DetailActivity : AppCompatActivity() {

    private lateinit var tvName: TextView
    private lateinit var tvDescription: TextView
    private lateinit var ivPhoto: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        postponeEnterTransition()

        tvName = findViewById(R.id.tv_detail_name)
        tvDescription = findViewById(R.id.tv_detail_description)
        ivPhoto = findViewById(R.id.iv_detail_photo)

        val story = intent.getParcelableExtra<ListStoryItem>("story")
        window.sharedElementEnterTransition = TransitionInflater.from(this)
            .inflateTransition(android.R.transition.move)
            .apply {
                duration = 300
            }
        if (story != null) {
            bindData(story)
        } else {
            tvName.text = getString(R.string.error_story_not_found)
            tvDescription.text = getString(R.string.error_story_description_missing)
        }
    }

    private fun bindData(story: ListStoryItem) {
        tvName.text = story.name
        tvDescription.text = story.description

        Glide.with(this)
            .load(story.photoUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .into(ivPhoto)
    }

}
