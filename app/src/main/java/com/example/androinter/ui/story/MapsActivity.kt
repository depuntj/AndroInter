package com.example.androinter.ui.story

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.androinter.R
import com.example.androinter.data.Injection
import com.example.androinter.data.SessionManager
import com.example.androinter.databinding.ActivityMapsBinding
import com.example.androinter.ui.views.StoryViewModel
import com.example.androinter.ui.views.StoryViewModelFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var sessionManager: SessionManager

    private val viewModel: StoryViewModel by viewModels {
        StoryViewModelFactory(Injection.provideRepository(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.story_maps)
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMapToolbarEnabled = true
        }

        try {
            val success = mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style)
            )
            if (!success) {
                Toast.makeText(this, "Failed to apply map style", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Resources.NotFoundException) {
            Toast.makeText(this, "Map style resource not found: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        getStoriesWithLocation()
    }

    private fun getStoriesWithLocation() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val token = sessionManager.authToken.firstOrNull()

                if (token != null) {
                    viewModel.fetchStoriesWithLocation(token)
                    viewModel.storiesWithLocation.observe(this@MapsActivity) { stories ->
                        binding.progressBar.visibility = View.GONE

                        stories?.forEach { story ->
                            if (story.lat != null && story.lon != null) {
                                val latLng = LatLng(story.lat, story.lon)
                                Log.d("MapsActivity", "Adding Marker: Lat=${story.lat}, Lon=${story.lon}")
                                mMap.addMarker(
                                    MarkerOptions()
                                        .position(latLng)
                                        .title(story.name)
                                        .snippet(story.description)
                                )
                            }
                        }
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@MapsActivity, "Please login first", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@MapsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
