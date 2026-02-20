package com.example.f_rizzly

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun OrderSuccessScreen(
    navController: NavController,
    orderId: String
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    val checkmarkProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
    )
    
    LaunchedEffect(Unit) {
        delay(100)
        animationPlayed = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4CAF50).copy(alpha = 0.1f),
                        Color.White
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated success icon
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale),
                contentAlignment = Alignment.Center
            ) {
                // Outer circle with gradient
                Surface(
                    modifier = Modifier.size(160.dp),
                    shape = CircleShape,
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                ) {}
                
                // Middle circle
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                ) {}
                
                // Inner circle with icon
                Surface(
                    modifier = Modifier.size(90.dp),
                    shape = CircleShape,
                    color = Color(0xFF4CAF50),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Animated checkmark
                        Canvas(modifier = Modifier.size(50.dp)) {
                            val path = Path().apply {
                                moveTo(size.width * 0.2f, size.height * 0.5f)
                                lineTo(
                                    size.width * 0.2f + (size.width * 0.2f * checkmarkProgress),
                                    size.height * 0.5f + (size.height * 0.2f * checkmarkProgress)
                                )
                                if (checkmarkProgress > 0.5f) {
                                    lineTo(
                                        size.width * 0.4f + (size.width * 0.4f * (checkmarkProgress - 0.5f) * 2),
                                        size.height * 0.7f - (size.height * 0.5f * (checkmarkProgress - 0.5f) * 2)
                                    )
                                }
                            }
                            drawPath(
                                path = path,
                                color = Color.White,
                                style = Stroke(width = 8.dp.toPx())
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Success text
            Text(
                "Order Placed!",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF4CAF50),
                modifier = Modifier.scale(scale)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Successfully",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier.scale(scale)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Order ID card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Order ID",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        orderId,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Your order is being prepared",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        "We'll notify you when it's on the way",
                        fontSize = 12.sp,
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Action buttons
            Button(
                onClick = {
                    navController.navigate(Screen.Orders.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(scale),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Receipt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Track Order", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(scale),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continue Shopping", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            }
        }
    }
}
