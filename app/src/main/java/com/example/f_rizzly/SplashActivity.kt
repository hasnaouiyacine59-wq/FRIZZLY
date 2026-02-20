package com.example.f_rizzly

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.f_rizzly.ui.theme.FRIZZLYTheme

class SplashActivity : ComponentActivity() {
    private var permissionStep = 0 // 0 = phone, 1 = location, 2 = done
    private var statusCallback: ((String) -> Unit)? = null
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        android.util.Log.d("SplashActivity", "Permission result: step=$permissionStep, permissions=$permissions")
        when (permissionStep) {
            0 -> {
                statusCallback?.invoke("Phone permissions granted")
                android.util.Log.d("SplashActivity", "Phone done, requesting location permissions...")
                permissionStep = 1
                requestLocationPermissions()
            }
            1 -> {
                statusCallback?.invoke("Location permissions granted")
                android.util.Log.d("SplashActivity", "Location done, navigating to main...")
                navigateToMain()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            FRIZZLYTheme {
                SplashScreenWithPermissions(
                    onSplashComplete = {
                        checkAndRequestPermissions()
                    },
                    onStatusUpdate = { status ->
                        statusCallback = status
                    }
                )
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        statusCallback?.invoke("Requesting phone permissions...")
        requestPhonePermissions()
    }
    
    private fun requestLocationPermissions() {
        statusCallback?.invoke("Requesting location permissions...")
        android.util.Log.d("SplashActivity", "requestLocationPermissions() called")
        val permissionsToRequest = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        
        android.util.Log.d("SplashActivity", "Location permissions to request: $permissionsToRequest")
        if (permissionsToRequest.isNotEmpty()) {
            permissionStep = 1
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            android.util.Log.d("SplashActivity", "All location permissions already granted")
            navigateToMain()
        }
    }
    
    private fun requestPhonePermissions() {
        android.util.Log.d("SplashActivity", "requestPhonePermissions() called")
        val permissionsToRequest = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_PHONE_NUMBERS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CALL_PHONE)
        }
        
        // Notifications (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        android.util.Log.d("SplashActivity", "Phone permissions to request: $permissionsToRequest")
        if (permissionsToRequest.isNotEmpty()) {
            permissionStep = 0
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            android.util.Log.d("SplashActivity", "All phone permissions already granted")
            requestLocationPermissions()
        }
    }
    
    private fun navigateToMain() {
        // Check if notifications are enabled
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            // Open notification settings
            val intent = Intent().apply {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    }
                    else -> {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = android.net.Uri.parse("package:$packageName")
                    }
                }
            }
            startActivity(intent)
        }
        
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
