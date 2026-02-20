package com.example.f_rizzly

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    val context = LocalContext.current
    var statusText by remember { mutableStateOf("Initializing...") }
    var hasInternet by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(500)
        
        // Check network
        statusText = "Checking network..."
        delay(300)
        hasInternet = isNetworkAvailable(context)
        
        if (!hasInternet) {
            statusText = "No internet connection"
            delay(2000)
            return@LaunchedEffect
        }
        
        statusText = "Network connected"
        delay(500)
        
        statusText = "Ready"
        delay(300)
        onSplashComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4CAF50),
                        Color(0xFF66BB6A),
                        Color(0xFF81C784)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (hasInternet) Icons.Filled.ShoppingCart else Icons.Filled.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color.White
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "FRIZZLY",
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (hasInternet) "Fresh Groceries Delivered" else "No Internet Connection",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = statusText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}


@Composable
fun SplashScreenWithPermissions(
    onSplashComplete: () -> Unit,
    onStatusUpdate: ((String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var statusText by remember { mutableStateOf("Initializing...") }
    var hasInternet by remember { mutableStateOf(true) }
    var currentStep by remember { mutableStateOf(0) }
    val totalSteps = 4 // Network, Phone, Location, Ready
    
    // Register status callback
    LaunchedEffect(Unit) {
        onStatusUpdate { newStatus ->
            statusText = newStatus
            when {
                newStatus.contains("Network connected") -> currentStep = 1
                newStatus.contains("phone permissions", ignoreCase = true) -> currentStep = 2
                newStatus.contains("location permissions", ignoreCase = true) -> currentStep = 3
                newStatus.contains("granted") && currentStep == 3 -> currentStep = 4
            }
        }
    }
    
    LaunchedEffect(Unit) {
        delay(500)
        
        // Check network
        statusText = "Checking network..."
        delay(300)
        hasInternet = isNetworkAvailable(context)
        
        if (!hasInternet) {
            statusText = "No internet connection"
            delay(2000)
            return@LaunchedEffect
        }
        
        statusText = "Network connected"
        currentStep = 1
        delay(500)
        
        statusText = "Checking permissions..."
        delay(300)
        onSplashComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4CAF50),
                        Color(0xFF66BB6A),
                        Color(0xFF81C784)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (hasInternet) Icons.Filled.ShoppingCart else Icons.Filled.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color.White
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "FRIZZLY",
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Fresh Groceries Delivered",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Step indicator
            Text(
                text = "Step $currentStep of $totalSteps",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = currentStep.toFloat() / totalSteps.toFloat(),
                modifier = Modifier
                    .width(200.dp)
                    .height(6.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = statusText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
