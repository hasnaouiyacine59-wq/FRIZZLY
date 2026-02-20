// BACKUP OF MY ORDERS UI DESIGN - Created: 2026-02-18

@Composable
fun OrdersScreen(navController: NavHostController, orders: List<Order>, onCancelOrder: (Order) -> Unit, onRefresh: () -> Unit) {
    var isLoading by remember { mutableStateOf(false) }
    
    // Force recomposition by using orders directly without remember
    val uniqueOrders = orders.distinctBy { it.orderId }
    
    android.util.Log.d("OrdersScreen", "🔄 Recomposing with ${orders.size} orders, ${uniqueOrders.size} unique")
    uniqueOrders.take(3).forEach { order ->
        android.util.Log.d("OrdersScreen", "  - ${order.orderId}: ${order.status}")
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGray())
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = activeGreen(),
            shadowElevation = 4.dp
        ) {
            Text(
                text = "My Orders (${uniqueOrders.size})",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        }
        
        if (isLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(3) {
                    ShimmerOrderCard()
                }
            }
        } else if (uniqueOrders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = inactiveGray()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No orders yet",
                        fontSize = 18.sp,
                        color = inactiveGray()
                    )
                }
            }
        } else {
            android.util.Log.d("OrdersScreen", "🎨 Rendering ${uniqueOrders.size} order cards")
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    count = uniqueOrders.size,
                    key = { index -> "${uniqueOrders[index].orderId}_${uniqueOrders[index].status}" }
                ) { index ->
                    OrderCard(order = uniqueOrders[index], navController = navController, onCancelOrder = onCancelOrder)
                }
            }
        }
    }
}

@Composable
fun ShimmerOrderCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground()),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(20.dp)
                        .background(inactiveGray().copy(alpha = shimmerAlpha), RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(20.dp)
                        .background(inactiveGray().copy(alpha = shimmerAlpha), RoundedCornerShape(10.dp))
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(16.dp)
                    .background(inactiveGray().copy(alpha = shimmerAlpha), RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(16.dp)
                        .background(inactiveGray().copy(alpha = shimmerAlpha), RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(18.dp)
                        .background(inactiveGray().copy(alpha = shimmerAlpha), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
fun OrderCard(order: Order, navController: NavHostController, onCancelOrder: (Order) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Order Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order #${order.orderId.takeLast(8)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = order.getFormattedDate(),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = order.getStatusColor().copy(alpha = 0.2f)
                ) {
                    Text(
                        text = order.getStatusText(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = order.getStatusColor(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Order Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${order.items.size} items",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "$${String.format("%.2f", order.totalAmount)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeGreen()
                )
            }
            
            // Expandable Items List
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                
                order.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.product.name} x ${item.quantity}kg",
                            fontSize = 14.sp
                        )
                        Text(
                            text = item.product.price,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                if (order.deliveryLocation != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Delivery: ${String.format("%.4f", order.deliveryLocation.first)}, ${String.format("%.4f", order.deliveryLocation.second)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                // Order Details Button
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { navController.navigate(Screen.OrderDetails.createRoute(order.orderId)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Filled.Receipt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Order Details")
                }
                
                // Cancel Order Button (only for PENDING orders)
                if (order.status == OrderStatus.PENDING) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onCancelOrder(order) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Icon(Icons.Filled.Cancel, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel Order")
                    }
                }
            }
            
            // Expand/Collapse Button
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (expanded) "Show Less" else "View Details")
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            }
        }
    }
}
