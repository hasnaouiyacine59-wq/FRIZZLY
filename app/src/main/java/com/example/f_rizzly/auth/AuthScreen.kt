package com.example.f_rizzly.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.f_rizzly.R
import com.example.f_rizzly.Screen

@Composable
fun AuthScreen(navController: NavController, authViewModel: AuthViewModel = viewModel()) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.observeAsState()
    val authError by authViewModel.authError.observeAsState()
    var isLoading by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            authViewModel.handleGoogleSignInResult(result.data)
        } else {
            isLoading = false
            if (result.resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(context, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            isLoading = false
            Toast.makeText(context, "Welcome ${currentUser?.displayName}!", Toast.LENGTH_SHORT).show()
            navController.navigate(Screen.Home.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(authError) {
        authError?.let {
            isLoading = false
            Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
            authViewModel.clearAuthError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF667eea),
                        Color(0xFF764ba2)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Logo
            FrizzlyLogo()
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "FRIZZLY",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Fresh Groceries Delivered",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Google Sign In Button
            Button(
                onClick = {
                    isLoading = true
                    authViewModel.signInWithGoogle(googleSignInLauncher)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(28.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF667eea),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Connecting...", fontSize = 16.sp, color = Color(0xFF667eea))
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.googleg_standard_color_18),
                        contentDescription = "Google",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Sign in with Google", fontSize = 16.sp, color = Color(0xFF667eea), fontWeight = FontWeight.SemiBold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Get fresh fruits & vegetables\ndelivered to your door",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun FrizzlyLogo() {
    val infiniteTransition = rememberInfiniteTransition(label = "logo")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Canvas(
        modifier = Modifier
            .size(150.dp)
            .scale(scale)
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2
        
        // Circle background
        drawCircle(
            color = Color.White,
            radius = radius,
            center = center
        )
        
        // Basket shape
        val basketPath = Path().apply {
            moveTo(center.x - radius * 0.5f, center.y - radius * 0.2f)
            lineTo(center.x - radius * 0.4f, center.y + radius * 0.4f)
            lineTo(center.x + radius * 0.4f, center.y + radius * 0.4f)
            lineTo(center.x + radius * 0.5f, center.y - radius * 0.2f)
            close()
        }
        
        drawPath(
            path = basketPath,
            color = Color(0xFF667eea)
        )
        
        // Fruits (circles)
        drawCircle(
            color = Color(0xFFFF6B6B),
            radius = radius * 0.15f,
            center = Offset(center.x - radius * 0.25f, center.y - radius * 0.3f)
        )
        drawCircle(
            color = Color(0xFF4ECDC4),
            radius = radius * 0.15f,
            center = Offset(center.x + radius * 0.25f, center.y - radius * 0.3f)
        )
        drawCircle(
            color = Color(0xFFFFA500),
            radius = radius * 0.15f,
            center = Offset(center.x, center.y - radius * 0.5f)
        )
    }
}
