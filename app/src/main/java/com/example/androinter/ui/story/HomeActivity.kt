package com.example.androinter.ui.story

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androinter.R
import com.example.androinter.data.Injection
import com.example.androinter.data.SessionManager
import com.example.androinter.data.StoryAdapter
import com.example.androinter.data.models.ListStoryItem
import com.example.androinter.ui.auth.LoginActivity
import com.example.androinter.ui.views.StoryViewModel
import com.example.androinter.ui.views.StoryViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var adapter: StoryAdapter
    private val viewModel: StoryViewModel by viewModels {
        StoryViewModelFactory(Injection.provideRepository(this))
    }
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story)

        sessionManager = SessionManager(this)
        setupButtons()
        setupRecyclerView()
        loadStories()

        findViewById<FloatingActionButton>(R.id.fab_add_story).setOnClickListener {
            startAddStory.launch(AddStoryActivity.getIntent(this))
        }

        findViewById<Button>(R.id.btn_logout).setOnClickListener {
            logout()
        }
    }

    private fun setupRecyclerView() {
        adapter = StoryAdapter { story ->
            navigateToDetail(story)
        }
        findViewById<RecyclerView>(R.id.rv_story_list).apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = this@HomeActivity.adapter
            layoutAnimation = AnimationUtils.loadLayoutAnimation(
                context,
                R.anim.fall_down
            )
        }
    }

    private fun loadStories() {
        lifecycleScope.launch {
            val token = sessionManager.authToken.firstOrNull()
            if (!token.isNullOrEmpty()) {
                Log.d("HomeActivity", "Using token: $token")
                viewModel.getStoriesPaging(token).observe(this@HomeActivity) { pagingData ->
                    adapter.submitData(lifecycle, pagingData)
                }
            } else {
                Log.e("HomeActivity", "Token is null or empty. Redirecting to login.")
                logout()
            }
        }
    }

    private fun setNewLocale(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btn_logout).setOnClickListener {
            logout()
        }

        findViewById<ImageButton>(R.id.btn_change_language).setOnClickListener {
            showLanguageDialog()
        }

        findViewById<ImageButton>(R.id.btn_maps).setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Indonesia")
        AlertDialog.Builder(this)
            .setTitle("Select Language")
            .setItems(languages) { _, which ->
                val locale = when (which) {
                    0 -> "en"
                    1 -> "in"
                    else -> "en"
                }
                setNewLocale(locale)
            }
            .show()
    }

    private fun navigateToDetail(story: ListStoryItem) {
        val intent = Intent(this, DetailActivity::class.java)
        intent.putExtra("story", story)
        startActivity(intent)
    }

    private fun logout() {
        lifecycleScope.launch {
            sessionManager.clearAuthToken()
            startActivity(Intent(this@HomeActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    private val startAddStory = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            refreshStories()
        }
    }

    private fun refreshStories() {
        lifecycleScope.launch {
            val token = sessionManager.authToken.firstOrNull()
            if (!token.isNullOrEmpty()) {
                adapter.refresh()
            }
        }
    }
}