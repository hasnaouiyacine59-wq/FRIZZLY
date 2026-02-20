package com.example.f_rizzly

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    navController: NavController,
    cartItems: List<CartItem>,
    deliveryLocation: Pair<Double, Double>?,
    onPlaceOrder: () -> Unit
) {
    val totalPrice = cartItems.sumOf { it.product.price.replace("$", "").replace("/kg", "").toDouble() * it.quantity }
    val deliveryFee = if (totalPrice > 50) 0.0 else 5.0
    val finalTotal = totalPrice + deliveryFee
    
    var currentStep by remember { mutableStateOf(1) }
    var selectedPayment by remember { mutableStateOf("Cash on Delivery") }
    var deliveryNotes by remember { mutableStateOf("") }
    var isPlacingOrder by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout - Step $currentStep of 2", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (currentStep == 1) navController.popBackStack() 
                        else currentStep = 1
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cardBackground(),
                    titleContentColor = darkText()
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(lightGray())
                .padding(padding)
        ) {
            if (currentStep == 1) {
                // Step 1: Order Summary
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBackground()),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Order Summary",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = darkText()
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                cartItems.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "${item.product.name} × ${String.format("%.1f", item.quantity)}kg",
                                            fontSize = 15.sp,
                                            color = darkText()
                                        )
                                        Text(
                                            "$${String.format("%.2f", item.product.price.replace("$", "").replace("/kg", "").toDouble() * item.quantity)}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = darkText()
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Subtotal", fontSize = 15.sp, color = inactiveGray())
                                    Text("$${String.format("%.2f", totalPrice)}", fontSize = 15.sp, color = darkText())
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Delivery Fee", fontSize = 15.sp, color = inactiveGray())
                                    Text(
                                        if (deliveryFee == 0.0) "FREE" else "$${String.format("%.2f", deliveryFee)}",
                                        fontSize = 15.sp,
                                        color = if (deliveryFee == 0.0) activeGreen() else darkText(),
                                        fontWeight = if (deliveryFee == 0.0) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = activeGreen(), thickness = 2.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Total",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = darkText()
                                    )
                                    Text(
                                        "$${String.format("%.2f", finalTotal)}",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeGreen()
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Next Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shadowElevation = 4.dp,
                    color = cardBackground(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Button(
                        onClick = { currentStep = 2 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = activeGreen()),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "Continue to Delivery & Payment",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Step 2: Delivery & Payment
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Delivery Location Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBackground()),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = activeGreen().copy(alpha = 0.1f),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = activeGreen(),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column {
                                        Text(
                                            "Delivery Location",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = darkText()
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            if (deliveryLocation != null) 
                                                "${String.format("%.4f", deliveryLocation.first)}, ${String.format("%.4f", deliveryLocation.second)}"
                                            else "Location not available",
                                            fontSize = 13.sp,
                                            color = inactiveGray()
                                        )
                                    }
                                }
                                
                                if (deliveryLocation != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .background(lightGreen(), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = activeGreen(),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Map Preview",
                                                fontSize = 14.sp,
                                                color = inactiveGray()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Payment Method Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBackground()),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = orange().copy(alpha = 0.1f),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Payment,
                                                contentDescription = null,
                                                tint = orange(),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Text(
                                        "Payment Method",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = darkText()
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                listOf("Cash on Delivery", "Credit Card", "Debit Card").forEach { method ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedPayment == method,
                                            onClick = { selectedPayment = method },
                                            colors = RadioButtonDefaults.colors(selectedColor = activeGreen())
                                        )
                                        Text(
                                            method,
                                            modifier = Modifier.padding(start = 8.dp),
                                            fontSize = 15.sp,
                                            color = darkText()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Delivery Notes Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBackground()),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Delivery Notes (Optional)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = darkText()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = deliveryNotes,
                                    onValueChange = { deliveryNotes = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Add any special instructions...") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = activeGreen(),
                                        unfocusedBorderColor = Color.LightGray
                                    )
                                )
                            }
                        }
                    }
                }
                
                // Place Order Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shadowElevation = 4.dp,
                    color = cardBackground(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Button(
                        onClick = { 
                            isPlacingOrder = true
                            onPlaceOrder()
                        },
                        enabled = !isPlacingOrder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = activeGreen()),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isPlacingOrder) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Processing...",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = cartItems.size.toString(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                                
                                Text(
                                    "Place Order • $${String.format("%.2f", finalTotal)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.width(36.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
