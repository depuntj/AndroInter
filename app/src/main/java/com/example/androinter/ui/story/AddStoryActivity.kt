package com.example.androinter.ui.story

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.androinter.R
import com.example.androinter.data.RetrofitInstance
import com.example.androinter.data.SessionManager
import com.example.androinter.data.StoryRepository
import com.example.androinter.databinding.ActivityAddstoryBinding
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*

class AddStoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddstoryBinding
    private var currentImageUri: Uri? = null
    private lateinit var sessionManager: SessionManager
    private lateinit var repository: StoryRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            val storageGranted = if (android.os.Build.VERSION.SDK_INT >= 33) {
                permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
            } else {
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
            }

            if (locationGranted) {
                startLocationUpdates()
            }

            if (cameraGranted && storageGranted) {
                showImageSourceDialog()
            } else {
                showToast("Required permissions are not granted.")
            }
        }

    private val launcherGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                currentImageUri = it
                showImage()
            }
        }

    // Camera launcher
    private val launcherCamera =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess) {
                showImage()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddstoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        repository = StoryRepository(RetrofitInstance.getApiService(""))
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.add_story)
        }

        setupActions()
    }

    private fun setupActions() {
        binding.ivPhoto.setOnClickListener {
            checkPermissionsAndShowDialog()
        }

        binding.buttonAdd.setOnClickListener {
            if (binding.switchLocation.isChecked) {
                fetchAndUploadWithLocation()
            } else {
                uploadStory()
            }
        }

        binding.switchLocation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startLocationUpdates()
            } else {
                stopLocationUpdates()
                currentLocation = null
                updateLocationText()
            }
        }
    }
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(object : LocationCallback() {})
        Log.d("AddStory", "Location updates stopped")
    }

    private fun checkPermissionsAndShowDialog() {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

        if (checkPermissionsGranted(permissions)) {
            showImageSourceDialog()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun checkPermissionsGranted(permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    5000
                ).build()

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        locationResult.lastLocation?.let {
                            currentLocation = it // Update current location
                            updateLocationText() // Update UI
                            Log.d("AddStory", "Location Updated: Lat=${it.latitude}, Lon=${it.longitude}")
                        }
                    }
                }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                Log.e("AddStory", "Error requesting location updates: ${e.message}")
                showToast("Error requesting location updates")
            }
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun showImageSourceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Select Image Source")
            .setItems(arrayOf("Camera", "Gallery")) { _, which ->
                when (which) {
                    0 -> startCamera()
                    1 -> startGallery()
                }
            }
            .show()
    }

    private fun startGallery() {
        launcherGallery.launch("image/*")
    }

    private fun startCamera() {
        try {
            val imageFile = createCustomTempFile(applicationContext)
            val uri = FileProvider.getUriForFile(
                this@AddStoryActivity,
                "${applicationContext.packageName}.provider",
                imageFile
            )
            currentImageUri = uri
            launcherCamera.launch(uri)
        } catch (e: Exception) {
            showToast("Failed to open camera: ${e.message}")
        }
    }

    private fun showImage() {
        val uri = currentImageUri
        if (uri != null) {
            binding.ivPhoto.setImageURI(uri)
        }
    }

    private fun fetchAndUploadWithLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        uploadStory()
    }

    private fun updateLocationText() {
        binding.tvLocation.text = currentLocation?.let {
            String.format(Locale.getDefault(), "Location: %.6f, %.6f", it.latitude, it.longitude)
        } ?: "Location not available"
    }

    private fun uploadStory() {
        val description = binding.edAddDescription.text.toString().trim()

        if (description.isEmpty()) {
            binding.edAddDescription.error = "Description is required"
            return
        }

        val currentUri = currentImageUri
        if (currentUri == null) {
            showToast("Please select an image")
            return
        }

        val imageFile = getImageFile()
        if (imageFile == null) {
            showToast("Failed to process image")
            return
        }

        val latitude = currentLocation?.latitude?.toFloat()
        val longitude = currentLocation?.longitude?.toFloat()

        if (binding.switchLocation.isChecked && (latitude == null || longitude == null)) {
            showToast("Location is not yet available")
            return
        }

        Log.d("AddStory", "Uploading Story: Lat=$latitude, Lon=$longitude")

        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.buttonAdd.isEnabled = false

                val token = sessionManager.authToken.firstOrNull()
                if (token != null) {
                    val success = repository.addStory(
                        token = token,
                        description = description,
                        photo = imageFile,
                        lat = if (binding.switchLocation.isChecked) latitude else null,
                        lon = if (binding.switchLocation.isChecked) longitude else null
                    )
                    if (success) {
                        showToast("Story uploaded successfully")
                        Log.d("AddStory", "Uploading story with Lat=${currentLocation?.latitude} and Lon=${currentLocation?.longitude}")
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        showToast("Failed to upload story")
                    }
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.buttonAdd.isEnabled = true
            }
        }
    }

    private fun getImageFile(): File? {
        val currentUri = currentImageUri ?: return null
        val contentResolver: ContentResolver = applicationContext.contentResolver
        val myFile = createCustomTempFile(applicationContext)

        return try {
            contentResolver.openInputStream(currentUri)?.use { inputStream ->
                FileOutputStream(myFile).use { outputStream ->
                    val buf = ByteArray(1024)
                    var len: Int
                    while (inputStream.read(buf).also { len = it } > 0) {
                        outputStream.write(buf, 0, len)
                    }
                }
            }
            myFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun createCustomTempFile(context: Context): File {
        val timestamp = System.currentTimeMillis()
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("STORY_$timestamp", ".jpg", storageDir)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        fun getIntent(homeActivity: HomeActivity): Intent {
            return Intent(homeActivity, AddStoryActivity::class.java)
        }
    }
}
