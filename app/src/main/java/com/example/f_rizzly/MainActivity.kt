package com.example.f_rizzly

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.awaitCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.core.content.ContextCompat
import android.annotation.SuppressLint
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.f_rizzly.auth.AuthViewModel
import com.example.f_rizzly.auth.AuthScreen
import com.example.f_rizzly.ui.theme.FRIZZLYTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.max
import com.example.f_rizzly.R

class MainActivity : ComponentActivity() {
    private var initialRoute: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Verify Firebase is initialized
        try {
            FirebaseAuth.getInstance()
            android.util.Log.d("MainActivity", "✅ Firebase initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Firebase initialization failed", e)
            Toast.makeText(this, "Firebase initialization failed", Toast.LENGTH_LONG).show()
            return
        }
        
        // Get notification data from intent
        handleIntent(intent)
        
        // Start background notification service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(android.content.Intent(this, NotificationListenerService::class.java))
        } else {
            startService(android.content.Intent(this, NotificationListenerService::class.java))
        }
        
        // Request only essential permissions at startup
        val permissions = mutableListOf<String>()
        
        // Essential: Location for delivery
        permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        
        // Essential: Phone Call
        permissions.add(android.Manifest.permission.CALL_PHONE)
        
        // Essential: Notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // Request essential permissions only
        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), 1001)
        }
        
        // Check API health on startup
        lifecycleScope.launch {
            val result = ApiService.checkHealth()
            result.onSuccess { response ->
                android.util.Log.d("API", "Health check successful: $response")
            }.onFailure { error ->
                android.util.Log.e("API", "Health check failed: ${error.message}")
            }
        }
        
        setContent {
            FRIZZLYTheme {
                MainScreen(initialRoute = initialRoute)
            }
        }
    }
    
    private fun handleIntent(intent: Intent?) {
        val notificationType = intent?.getStringExtra("notification_type")
        val orderId = intent?.getStringExtra("order_id")
        android.util.Log.d("MainActivity", "Intent extras: type=$notificationType, orderId=$orderId")
        
        initialRoute = when {
            notificationType == "order" && orderId != null -> {
                android.util.Log.d("MainActivity", "Setting route to order: $orderId")
                Screen.OrderDetails.createRoute(orderId)
            }
            else -> null
        }
    }
}

// --- Colors and Styles ---
// Theme-aware color helpers
@Composable
fun activeGreen() = MaterialTheme.colorScheme.primary

@Composable
fun lightGreen() = MaterialTheme.colorScheme.primaryContainer

@Composable
fun inactiveGray() = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun lightGray() = MaterialTheme.colorScheme.background

@Composable
fun darkText() = MaterialTheme.colorScheme.onSurface

@Composable
fun orange() = MaterialTheme.colorScheme.secondary

@Composable
fun cardBackground() = MaterialTheme.colorScheme.surface

@Composable
fun surfaceVariant() = MaterialTheme.colorScheme.surfaceVariant

// --- Navigation ---
sealed class Screen(
    val route: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
) {
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Categories : Screen("categories", "Categories", Icons.Filled.Apps, Icons.Outlined.Apps)
    object Panier : Screen("panier", "Panier", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart)
    object Orders : Screen("orders", "Orders", Icons.Filled.Receipt, Icons.Outlined.Receipt)
    object Profile : Screen("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
    object Notifications : Screen("notifications", "Notifications", Icons.Filled.Notifications, Icons.Outlined.Notifications)
    object Auth : Screen("auth", "Auth", Icons.Filled.Lock, Icons.Outlined.Lock)
    object Checkout : Screen("checkout", "Checkout", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart)
    object OrderDetails : Screen("order_details/{orderId}", "Order Details", Icons.Filled.Receipt, Icons.Outlined.Receipt) {
        fun createRoute(orderId: String) = "order_details/$orderId"
    }
    object OrderSuccess : Screen("order_success/{orderId}", "Order Success", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle) {
        fun createRoute(orderId: String) = "order_success/$orderId"
    }
}

val bottomNavItems = listOf(Screen.Home, Screen.Categories, Screen.Panier, Screen.Orders, Screen.Profile)

/** Delivery location captured at checkout success; use for delivery. */
data class DeliveryLocation(val latitude: Double, val longitude: Double)

@Composable
fun NavigationGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    context: Context,
    cartItems: List<CartItem>,
    deliveryLocation: Pair<Double, Double>?,
    orders: List<Order>,
    notifications: List<AppNotification>,
    notificationCount: Int,
    onAddToCart: (Product, Double) -> Unit,
    onDeleteItem: (CartItem) -> Unit,
    onModifyItem: (CartItem, Double) -> Unit,
    onCheckoutWithLocation: (latitude: Double, longitude: Double) -> Unit,
    onClearCart: () -> Unit,
    onPlaceOrder: suspend (Order) -> String?,
    onSignOut: () -> Unit,
    onRefreshOrders: () -> Unit,
    onCancelOrder: (Order) -> Unit,
    onSyncOrders: () -> Unit,
    onMarkNotificationAsRead: (String) -> Unit,
    onClearAllNotifications: () -> Unit
) {
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        Screen.Home.route
    } else {
        Screen.Auth.route
    }
    
    NavHost(navController, startDestination = startDestination, modifier = modifier) {
        composable(Screen.Home.route) {
            HomeScreen(
                cartItems = cartItems,
                orders = orders,
                notificationCount = notificationCount,
                onAddToCart = onAddToCart,
                onModifyItem = onModifyItem,
                navController = navController,
                onRefreshOrders = onRefreshOrders
            )
        }
        composable(Screen.Categories.route) {
            CategoriesScreen(
                cartItems = cartItems,
                onAddToCart = onAddToCart,
                onModifyItem = onModifyItem
            )
        }
        composable(Screen.Panier.route) {
            PanierScreen(
                cartItems = cartItems,
                onDeleteItem = onDeleteItem,
                onModifyItem = onModifyItem,
                onCheckoutWithLocation = onCheckoutWithLocation,
                navController = navController
            )
        }
        composable(Screen.Checkout.route) {
            val scope = rememberCoroutineScope()
            CheckoutScreen(
                navController = navController,
                cartItems = cartItems,
                deliveryLocation = deliveryLocation,
                onPlaceOrder = {
                    scope.launch {
                        val tempOrderId = "TEMP${System.currentTimeMillis()}"
                        val totalAmount = cartItems.sumOf { 
                            it.product.price.replace("$", "").replace("/kg", "").toDouble() * it.quantity 
                        }
                        val order = Order(
                            orderId = tempOrderId,
                            items = cartItems.map { it.copy() },
                            totalAmount = totalAmount,
                            deliveryLocation = deliveryLocation,
                            status = OrderStatus.PENDING
                        )
                        
                        // Wait for real order ID from API
                        val realOrderId = onPlaceOrder(order)
                        onClearCart()
                        
                        // Navigate with real order ID
                        navController.navigate(Screen.OrderSuccess.createRoute(realOrderId ?: tempOrderId)) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    }
                }
            )
        }
        composable(Screen.OrderSuccess.route) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: "UNKNOWN"
            OrderSuccessScreen(navController = navController, orderId = orderId)
        }
        composable(Screen.Orders.route) { 
            OrdersScreen(
                navController = navController,
                orders = orders, 
                onCancelOrder = onCancelOrder,
                onRefresh = onSyncOrders
            )
        }
        composable(Screen.Profile.route) { 
            com.example.f_rizzly.profile.ProfileScreen(
                navController = navController,
                onSignOut = onSignOut
            )
        }
        composable(Screen.Notifications.route) {
            NotificationsScreen(
                navController = navController, 
                context = context,
                notifications = notifications,
                onMarkAsRead = onMarkNotificationAsRead,
                onClearAll = onClearAllNotifications
            )
        }
        composable(Screen.OrderDetails.route) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId")
            OrderDetailsScreen(navController = navController, orderId = orderId, orders = orders)
        }
        composable(Screen.Auth.route) { AuthScreen(navController = navController) }
    }
}

// --- Home Screen and its Components ---

private fun Product.isSameAs(other: Product) =
    name == other.name && price == other.price && imageResId == other.imageResId

@Composable
fun HomeScreen(
    cartItems: List<CartItem>, 
    orders: List<Order>,
    notificationCount: Int,
    onAddToCart: (Product, Double) -> Unit, 
    onModifyItem: (CartItem, Double) -> Unit, 
    navController: NavController,
    onRefreshOrders: () -> Unit
) {
    val context = LocalContext.current
    val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    var showQuantityPopup by remember { mutableStateOf(false) }
    var selectedProductForQuantity by remember { mutableStateOf<Product?>(null) }
    var selectedCartItemForModification by remember { mutableStateOf<CartItem?>(null) }

    val onProductActionClick: (Product, CartItem?) -> Unit = { product, cartItem ->
        if (cartItem != null) {
            selectedCartItemForModification = cartItem
        } else {
            selectedProductForQuantity = product
        }
        showQuantityPopup = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.8f
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item { 
                TopBar(
                    cartItemCount = cartItems.size,
                    notificationCount = notificationCount,
                    onCartClick = { navController.navigate(Screen.Panier.route) },
                    onNotificationClick = { navController.navigate(Screen.Notifications.route) }
                ) 
            }
            item {
                TodaysPicksSection(
                    cartItems = cartItems,
                    onProductActionClick = onProductActionClick
                )
            }
            item { OfferBannersSection() }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        if (showQuantityPopup) {
            if (selectedProductForQuantity != null) {
                QuantitySelectionPopup(
                    product = selectedProductForQuantity!!,
                    initialQuantity = 1.0,
                    onConfirmAddToCart = onAddToCart,
                    onDismiss = { showQuantityPopup = false; selectedProductForQuantity = null }
                )
            } else if (selectedCartItemForModification != null) {
                QuantitySelectionPopup(
                    product = selectedCartItemForModification!!.product,
                    initialQuantity = selectedCartItemForModification!!.quantity,
                    existingCartItem = selectedCartItemForModification, // Pass existing cart item
                    onConfirmAddToCart = { product, newQuantity ->
                        onModifyItem(selectedCartItemForModification!!, newQuantity)
                        showQuantityPopup = false
                        selectedCartItemForModification = null
                    },
                    onDismiss = { showQuantityPopup = false; selectedCartItemForModification = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(cartItemCount: Int, notificationCount: Int, onCartClick: () -> Unit, onNotificationClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "FRIZZLY",
            color = activeGreen(),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Notification Bell
            BadgedBox(
                badge = {
                    if (notificationCount > 0) {
                        Badge(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ) { Text(notificationCount.toString(), fontWeight = FontWeight.Bold) }
                    }
                }
            ) {
                IconButton(onClick = onNotificationClick) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        tint = darkText(),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            // Cart
            BadgedBox(
                badge = {
                    if (cartItemCount > 0) {
                        Badge(
                            containerColor = orange(),
                            contentColor = Color.White
                        ) { Text(cartItemCount.toString(), fontWeight = FontWeight.Bold) }
                    }
                }
            ) {
                IconButton(onClick = onCartClick) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingBag,
                        contentDescription = "Cart",
                        tint = darkText(),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBar() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = lightGray(),
        tonalElevation = 0.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = inactiveGray()
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Search fruits & vegetables", color = inactiveGray())
        }
    }
}

@Composable
fun FilterChips() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Chip("Fruits", Icons.Filled.Spa) }
        item { Chip("Vegetables", Icons.Filled.EnergySavingsLeaf) }
        item { Chip("Organic", Icons.Filled.VerifiedUser) }
        item { Chip("Seasonal", Icons.Filled.WbSunny) }
    }
}

@Composable
fun Chip(text: String, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBackground(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = text, tint = activeGreen(), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, fontSize = 14.sp, color = darkText())
        }
    }
}

@Composable
fun FilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) activeGreen() else cardBackground(),
        border = BorderStroke(1.dp, if (isSelected) activeGreen() else MaterialTheme.colorScheme.outline),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else darkText()
            )
        }
    }
}

@Composable
fun DeliveryBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = lightGreen())
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Fresh & Fast Delivery", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = darkText())
                Text("Get it delivered to your door!", fontSize = 14.sp, color = inactiveGray())
                Spacer(modifier = Modifier.height(8.dp))
                Surface(shape = CircleShape, color = activeGreen()) {
                    Text("FREE DELIVERY", color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 10.sp)
                }
            }
            Icon(
                imageVector = Icons.Filled.LocalShipping,
                contentDescription = "Delivery",
                tint = activeGreen(),
                modifier = Modifier.size(70.dp)
            )
        }
    }
}

@Composable
fun TodaysPicksSection(
    cartItems: List<CartItem>,
    onProductActionClick: (Product, CartItem?) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Today's Fresh Picks",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = darkText()
            )
            Text(
                "See All >",
                color = activeGreen(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sampleProducts.size) { index ->
                val product = sampleProducts[index]
                val cartItem = cartItems.find { it.product.isSameAs(product) }
                ProductCard(
                    product = product,
                    isInCart = cartItem != null,
                    cartItem = cartItem,
                    onProductActionClick = onProductActionClick
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    isInCart: Boolean = false,
    cartItem: CartItem? = null,
    onProductActionClick: (Product, CartItem?) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground()),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .width(160.dp)
            .height(220.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Product Image - Optimized
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUrl.isNotEmpty()) {
                    coil.compose.AsyncImage(
                        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                            .data(product.imageUrl)
                            .crossfade(true)
                            .memoryCacheKey(product.name)
                            .diskCacheKey(product.name)
                            .build(),
                        contentDescription = product.name,
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Fit
                    )
                } else if (product.imageResId != 0) {
                    Image(
                        painter = painterResource(id = product.imageResId),
                        contentDescription = product.name,
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = Color.LightGray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = product.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = darkText(),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            
            Text(
                text = product.price,
                fontSize = 14.sp,
                color = activeGreen(),
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { onProductActionClick(product, cartItem) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isInCart) orange() else activeGreen()
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = if (isInCart) Icons.Filled.Edit else Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isInCart) "Edit" else "Add",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun QuantitySelectionPopup(
    product: Product,
    initialQuantity: Double = 1.0,
    existingCartItem: CartItem? = null,
    onConfirmAddToCart: (Product, Double) -> Unit,
    onDismiss: () -> Unit,
    onRemoveFromCart: ((CartItem) -> Unit)? = null
) {
    var selectedQuantity by remember { mutableStateOf(initialQuantity) }
    val pricePerKg = remember { 
        product.price
            .replace("€", "")
            .replace("$", "")
            .replace("/kg", "")
            .trim()
            .toDoubleOrNull() ?: 0.0 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingCartItem != null) "Modify Quantity for ${product.name}" else "Select Quantity for ${product.name}") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = product.imageResId),
                        contentDescription = product.name,
                        modifier = Modifier.size(110.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(product.price, color = inactiveGray(), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedQuantity = max(0.5, selectedQuantity - 0.5) },
                        enabled = selectedQuantity > 0.5
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
                    }
                    Text(
                        text = "${String.format("%.1f", selectedQuantity)}kg",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    IconButton(
                        onClick = { selectedQuantity += 0.5 }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase quantity")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val totalPrice = pricePerKg * selectedQuantity
                Text(
                    text = "Total: $${String.format("%.2f", totalPrice)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeGreen()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmAddToCart(product, selectedQuantity)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = activeGreen())
            ) {
                Text(if (existingCartItem != null) "Update Panier" else "Add to Panier")
            }
        },
        dismissButton = {
            Row {
                if (existingCartItem != null && onRemoveFromCart != null) {
                    TextButton(onClick = {
                        onRemoveFromCart(existingCartItem)
                        onDismiss()
                    }) {
                        Text("Remove from Panier", color = Color.Red)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

// Sample products for TodaysPicksSection
val sampleProducts = listOf(
    Product("Apple", "$2.99/kg", R.drawable.ic_apple),
    Product("Banana", "$1.99/kg", R.drawable.ic_banana),
    Product("orange()", "$3.49/kg", R.drawable.ic_orange),
    Product("Strawberry", "$4.99/kg", R.drawable.ic_strawberry),
    Product("Tomato", "$2.49/kg", R.drawable.ic_tomato),
    Product("Carrot", "$1.79/kg", R.drawable.ic_carrot),
    Product("Broccoli", "$3.99/kg", R.drawable.ic_broccoli),
    Product("Lettuce", "$2.29/kg", R.drawable.ic_lettuce),
    Product("Pepper", "$3.79/kg", R.drawable.ic_pepper),
    Product("Cucumber", "$1.99/kg", R.drawable.ic_cucumber)
)


@Composable
fun OfferBannersSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OfferBanner(
            title = "20% OFF",
            subtitle = "on all fruits!",
            color = orange(),
            modifier = Modifier.weight(1f)
        )
        OfferBanner(
            title = "100% Fresh",
            subtitle = "& Organic",
            color = activeGreen(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun OfferBanner(title: String, subtitle: String, color: Color, modifier: Modifier = Modifier) {
    val bannerColor = if (title.contains("20%")) orange() else activeGreen()
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bannerColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, color = Color.White, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Shop Now >", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}

@Composable
fun DebugLogsButton(orders: List<Order>, onRefreshOrders: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    var showLogs by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf("") }
    val userId = auth.currentUser?.uid // Moved userId declaration here
    
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Button(
            onClick = {
                scope.launch {
                    val localOrders = DataStore.loadOrders(context, userId)
                    val cloudOrders = OrderRepository.loadOrders(userId)
                    
                    logs = buildString {
                        appendLine("=== DEBUG LOGS ===")
                        appendLine("User ID: ${userId ?: "NOT LOGGED IN"}")
                        appendLine("Local Orders: ${localOrders.size}")
                        localOrders.forEach { order ->
                            appendLine("  - ${order.orderId} (${order.items.size} items)")
                        }
                        appendLine("Cloud Orders: ${cloudOrders.size}")
                        cloudOrders.forEach { order ->
                            appendLine("  - ${order.orderId} (${order.items.size} items)")
                        }
                        appendLine("=================")
                    }
                    android.util.Log.d("DebugLogs", logs)
                    showLogs = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) {
            Icon(Icons.Filled.BugReport, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("View Debug Logs")
        }
        
        if (showLogs) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = logs,
                        color = Color.Green,
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Manual Sync Button
                    Button(
                        onClick = {
                            scope.launch {
                                // Sync current orders in memory (not just local storage)
                                var syncCount = 0
                                orders.forEach { order ->
                                    val success = OrderRepository.saveOrder(order)
                                    if (success) syncCount++
                                }
                                logs += "\n\nSync to Cloud: $syncCount/${orders.size} orders synced"
                                android.util.Log.d("DebugLogs", "Sync to cloud completed: $syncCount orders")
                                
                                // Refresh logs
                                val cloudOrders = OrderRepository.loadOrders(userId)
                                logs += "\nCloud Orders Now: ${cloudOrders.size}"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = activeGreen())
                    ) {
                        Text("Sync to Cloud")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            scope.launch {
                                val cloudOrders = OrderRepository.loadOrders(userId)
                                DataStore.saveOrders(context, userId, cloudOrders)
                                onRefreshOrders() // Refresh UI
                                logs += "\n\nSync from Cloud: ${cloudOrders.size} orders downloaded"
                                android.util.Log.d("DebugLogs", "Sync from cloud completed: ${cloudOrders.size} orders")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                    ) {
                        Text("Sync from Cloud")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            scope.launch {
                                val success = DatabaseCleaner.clearAllOrders(context)
                                if (success) {
                                    onRefreshOrders()
                                    logs += "\n\n✅ All orders cleared from Firebase and local storage"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Orders")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showLogs = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

// --- Other Screens ---
@Composable
fun CategoriesScreen(cartItems: List<CartItem>, onAddToCart: (Product, Double) -> Unit, onModifyItem: (CartItem, Double) -> Unit) {
    var showQuantityPopup by remember { mutableStateOf(false) }
    var selectedProductForQuantity by remember { mutableStateOf<Product?>(null) }
    var selectedCartItemForModification by remember { mutableStateOf<CartItem?>(null) }
    var selectedFilter by remember { mutableStateOf("All") }

    val onProductActionClick: (Product, CartItem?) -> Unit = { product, cartItem ->
        if (cartItem != null) {
            selectedCartItemForModification = cartItem
        } else {
            selectedProductForQuantity = product
        }
        showQuantityPopup = true
    }
    
    val allProducts = remember { sampleCategories.flatMap { it.products }.distinctBy { it.name } }
    
    val filteredProducts = remember(selectedFilter) {
        when (selectedFilter) {
            "All" -> allProducts
            "Fruits" -> sampleCategories.find { it.name == "Fruits" }?.products ?: emptyList()
            "Vegetables" -> sampleCategories.find { it.name == "Vegetables" }?.products ?: emptyList()
            "Organic" -> sampleCategories.find { it.name == "Organic" }?.products ?: emptyList()
            "Seasonal" -> sampleCategories.find { it.name == "Seasonal" }?.products ?: emptyList()
            else -> allProducts
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.8f
        )
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = cardBackground(),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Browse Products",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = darkText()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val filters = listOf("All", "Fruits", "Vegetables", "Organic", "Seasonal")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filters.size) { index ->
                            FilterChip(
                                label = filters[index],
                                isSelected = selectedFilter == filters[index],
                                onClick = { selectedFilter = filters[index] }
                            )
                        }
                    }
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "header") {
                    Text(
                        text = "${filteredProducts.size} Products",
                        fontSize = 14.sp,
                        color = inactiveGray(),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(
                    count = filteredProducts.size,
                    key = { index -> filteredProducts[index].name }
                ) { index ->
                    val product = filteredProducts[index]
                    val cartItem = cartItems.find { it.product.isSameAs(product) }
                    CompactProductCard(
                        product = product,
                        isInCart = cartItem != null,
                        cartItem = cartItem,
                        onProductActionClick = onProductActionClick
                    )
                }
            }
        }

        if (showQuantityPopup) {
            if (selectedProductForQuantity != null) {
                QuantitySelectionPopup(
                    product = selectedProductForQuantity!!,
                    initialQuantity = 1.0,
                    onConfirmAddToCart = onAddToCart,
                    onDismiss = { showQuantityPopup = false; selectedProductForQuantity = null }
                )
            } else if (selectedCartItemForModification != null) {
                QuantitySelectionPopup(
                    product = selectedCartItemForModification!!.product,
                    initialQuantity = selectedCartItemForModification!!.quantity,
                    existingCartItem = selectedCartItemForModification,
                    onConfirmAddToCart = { product, newQuantity ->
                        onModifyItem(selectedCartItemForModification!!, newQuantity)
                        showQuantityPopup = false
                        selectedCartItemForModification = null
                    },
                    onDismiss = { showQuantityPopup = false; selectedCartItemForModification = null }
                )
            }
        }
    }
}

@Composable
fun CompactProductCard(
    product: Product,
    isInCart: Boolean = false,
    cartItem: CartItem? = null,
    onProductActionClick: (Product, CartItem?) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground()),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = product.imageResId),
                contentDescription = null,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkText(),
                    maxLines = 1
                )
                Text(
                    text = product.price,
                    fontSize = 14.sp,
                    color = activeGreen(),
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Button(
                onClick = { onProductActionClick(product, cartItem) },
                modifier = Modifier
                    .width(80.dp)
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isInCart) orange() else activeGreen()
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = if (isInCart) Icons.Filled.Edit else Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryCard(category: Category, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground()),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkText()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${category.products.size} products",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = activeGreen().copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "View All →",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = activeGreen(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
            
            // Category Icon
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = when (category.name) {
                    "Fruits" -> orange().copy(alpha = 0.2f)
                    "Vegetables" -> activeGreen().copy(alpha = 0.2f)
                    "Organic" -> Color(0xFF8BC34A).copy(alpha = 0.2f)
                    "Seasonal" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                    else -> lightGreen()
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (category.name) {
                            "Fruits" -> Icons.Filled.LocalFlorist
                            "Vegetables" -> Icons.Filled.Spa
                            "Organic" -> Icons.Filled.Eco
                            "Seasonal" -> Icons.Filled.WbSunny
                            else -> Icons.Filled.ShoppingBasket
                        },
                        contentDescription = category.name,
                        modifier = Modifier.size(40.dp),
                        tint = when (category.name) {
                            "Fruits" -> orange()
                            "Vegetables" -> activeGreen()
                            "Organic" -> Color(0xFF8BC34A)
                            "Seasonal" -> Color(0xFFFF9800)
                            else -> activeGreen()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryRow(
    category: Category,
    cartItems: List<CartItem>, // New parameter
    onProductActionClick: (Product, CartItem?) -> Unit // New parameter
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(category.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = darkText())
            Text("See All >", color = activeGreen(), fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(category.products.size) { index ->
                val product = category.products[index]
                val cartItem = cartItems.find { it.product.isSameAs(product) }
                ProductCard(
                    product = product,
                    isInCart = cartItem != null,
                    cartItem = cartItem,
                    onProductActionClick = onProductActionClick
                )
            }
        }
    }
}

data class Category(
    val name: String,
    val products: List<Product>
)

data class Product(
    val name: String,
    val price: String,
    val imageResId: Int = 0, // Local drawable resource (0 if using URL)
    val imageUrl: String = "" // Web image URL
)

data class CartItem(val product: Product, var quantity: Double) // New CartItem data class

// --- Panier Screen Components ---

@Composable
fun PanierHeader(
    totalPrice: Double,
    totalWeight: Double,
    productCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = lightGreen(),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Product count badge only (title removed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = activeGreen(),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = productCount.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Statistics Cards (no icons, smaller labels and values)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = null,
                    label = "Total Price",
                    value = "$${String.format("%.2f", totalPrice)}",
                    iconColor = activeGreen(),
                    backgroundColor = Color.White
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = null,
                    label = "Total Weight",
                    value = "${String.format("%.1f", totalWeight)}kg",
                    iconColor = orange(),
                    backgroundColor = Color.White
                )
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    label: String,
    value: String,
    iconColor: Color,
    backgroundColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = darkText()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = inactiveGray()
            )
        }
    }
}

@Composable
fun CartItemRow(
    cartItem: CartItem,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product Image
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(60.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Image(
                painter = painterResource(id = cartItem.product.imageResId),
                contentDescription = cartItem.product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Product Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = cartItem.product.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = darkText()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = cartItem.product.price,
                color = inactiveGray(),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = lightGreen()
            ) {
                Text(
                    text = "${String.format("%.1f", cartItem.quantity)} kg",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = activeGreen(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Action Buttons
        Row {
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Modify",
                    tint = orange(),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

val sampleCategories = listOf(
    Category(
        name = "Fruits",
        products = listOf(
            Product("Apple", "$2.99/kg", R.drawable.ic_apple),
            Product("Banana", "$1.99/kg", R.drawable.ic_banana),
            Product("orange()", "$3.49/kg", R.drawable.ic_orange),
            Product("Strawberry", "$4.99/kg", R.drawable.ic_strawberry)
        )
    ),
    Category(
        name = "Vegetables",
        products = listOf(
            Product("Tomato", "$2.49/kg", R.drawable.ic_tomato),
            Product("Carrot", "$1.79/kg", R.drawable.ic_carrot),
            Product("Broccoli", "$3.99/kg", R.drawable.ic_broccoli),
            Product("Lettuce", "$2.29/kg", R.drawable.ic_lettuce),
            Product("Pepper", "$3.79/kg", R.drawable.ic_pepper),
            Product("Cucumber", "$1.99/kg", R.drawable.ic_cucumber)
        )
    ),
    Category(
        name = "Organic",
        products = listOf(
            Product("Organic Apple", "$4.99/kg", R.drawable.ic_apple),
            Product("Organic Carrot", "$3.49/kg", R.drawable.ic_carrot),
            Product("Organic Broccoli", "$5.99/kg", R.drawable.ic_broccoli)
        )
    ),
    Category(
        name = "Seasonal",
        products = listOf(
            Product("Fresh Strawberry", "$4.99/kg", R.drawable.ic_strawberry),
            Product("Summer Tomato", "$2.49/kg", R.drawable.ic_tomato),
            Product("Spring Lettuce", "$2.29/kg", R.drawable.ic_lettuce)
        )
    )
)

@SuppressLint("MissingPermission")
private fun getLastLocation(context: Context): Pair<Double, Double>? {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            LocationManager.GPS_PROVIDER
        } else {
            LocationManager.NETWORK_PROVIDER
        }
        @Suppress("DEPRECATION")
        val location = locationManager.getLastKnownLocation(provider)
        return location?.let { it.latitude to it.longitude }
    } catch (e: SecurityException) {
        android.util.Log.e("Location", "Permission denied: ${e.message}")
        return null
    }
}

@Composable
fun PanierScreen(
    cartItems: List<CartItem>,
    onDeleteItem: (CartItem) -> Unit,
    onModifyItem: (CartItem, Double) -> Unit,
    onCheckoutWithLocation: (latitude: Double, longitude: Double) -> Unit,
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    var showQuantityPopup by remember { mutableStateOf(false) }
    var selectedCartItemForQuantity by remember { mutableStateOf<CartItem?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingCheckoutAfterPermission by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (pendingCheckoutAfterPermission) {
            pendingCheckoutAfterPermission = false
            val location = if (isGranted) getLastLocation(context) else null
            val (lat, lng) = location ?: (0.0 to 0.0) // Default location if permission denied
            onCheckoutWithLocation(lat, lng)
        }
    }

    fun doCheckout() {
        if (authViewModel.currentUser.value == null) {
            navController.navigate(Screen.Auth.route)
            return
        }

        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                val location = getLastLocation(context)
                val (lat, lng) = location ?: (0.0 to 0.0) // Default location
                onCheckoutWithLocation(lat, lng)
            }
            else -> {
                pendingCheckoutAfterPermission = true
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    val totalWeight = cartItems.sumOf { it.quantity }
    val totalPrice = cartItems.sumOf { cartItem ->
        val pricePerKg = cartItem.product.price
            .replace("$", "")
            .replace("/kg", "")
            .trim()
            .toDoubleOrNull() ?: 0.0
        pricePerKg * cartItem.quantity
    }
    val productCount = cartItems.size

    Box(modifier = Modifier.fillMaxSize()) {
        if (cartItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(lightGray())
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingCart,
                    contentDescription = "Empty Cart",
                    modifier = Modifier.size(100.dp),
                    tint = inactiveGray()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Your cart is empty!",
                    color = darkText(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add some fresh products to get started",
                    color = inactiveGray(),
                    fontSize = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(lightGray())
            ) {
                // Checkout Button at Top
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shadowElevation = 4.dp,
                    color = cardBackground(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Button(
                        onClick = { doCheckout() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = activeGreen()),
                        shape = RoundedCornerShape(16.dp)
                    ) {
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
                                        text = productCount.toString(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.ShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Proceed to Checkout",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(36.dp))
                        }
                    }
                }
                
                // Summary Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground()),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Price",
                                    fontSize = 14.sp,
                                    color = inactiveGray()
                                )
                                Text(
                                    text = "$${String.format("%.2f", totalPrice)}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeGreen()
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Weight",
                                    fontSize = 14.sp,
                                    color = inactiveGray()
                                )
                                Text(
                                    text = "${String.format("%.1f", totalWeight)} kg",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = darkText()
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Items List
                Box(modifier = Modifier.weight(1f)) {
                    val listState = rememberLazyListState()
                    val showScrollIndicator = remember {
                        derivedStateOf {
                            listState.canScrollForward
                        }
                    }
                    
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cartItems.size) { index ->
                            val cartItem = cartItems[index]
                            ModernCartItemCard(
                                cartItem = cartItem,
                                onEditClick = {
                                    selectedCartItemForQuantity = cartItem
                                    showQuantityPopup = true
                                },
                                onDeleteClick = { onDeleteItem(cartItem) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                    
                    // Scroll indicator
                    if (showScrollIndicator.value) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Scroll down",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    if (showQuantityPopup && selectedCartItemForQuantity != null) {
        QuantitySelectionPopup(
            product = selectedCartItemForQuantity!!.product,
            initialQuantity = selectedCartItemForQuantity!!.quantity,
            existingCartItem = selectedCartItemForQuantity,
            onConfirmAddToCart = { product, newQuantity ->
                onModifyItem(selectedCartItemForQuantity!!, newQuantity)
                showQuantityPopup = false
                selectedCartItemForQuantity = null
            },
            onDismiss = { showQuantityPopup = false; selectedCartItemForQuantity = null },
            onRemoveFromCart = { cartItem ->
                onDeleteItem(cartItem)
                showQuantityPopup = false
                selectedCartItemForQuantity = null
            }
        )
    }
}

@Composable
fun ModernCartItemCard(
    cartItem: CartItem,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground()),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image - Optimized
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(70.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = cartItem.product.imageResId),
                        contentDescription = cartItem.product.name,
                        modifier = Modifier.size(60.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartItem.product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = darkText()
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Quantity",
                            fontSize = 11.sp,
                            color = inactiveGray()
                        )
                        Text(
                            text = "${String.format("%.1f", cartItem.quantity)} kg",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = darkText()
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Price",
                            fontSize = 11.sp,
                            color = inactiveGray()
                        )
                        val pricePerKg = cartItem.product.price
                            .replace("$", "")
                            .replace("/kg", "")
                            .trim()
                            .toDoubleOrNull() ?: 0.0
                        val itemTotal = pricePerKg * cartItem.quantity
                        Text(
                            text = "$${String.format("%.2f", itemTotal)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeGreen()
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = orange().copy(alpha = 0.1f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = orange(),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFFEBEE),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground()),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Order Icon
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(70.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(order.getStatusColor().copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = order.getStatusIcon(),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = order.getStatusColor()
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Order #${order.orderId.takeLast(8)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = darkText()
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = order.getFormattedDate(),
                            fontSize = 11.sp,
                            color = inactiveGray()
                        )
                        Text(
                            text = "${order.items.size} items",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = darkText()
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = order.getStatusColor().copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = order.getStatusText(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = order.getStatusColor(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$${String.format("%.2f", order.totalAmount)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeGreen()
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.size(36.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = activeGreen().copy(alpha = 0.1f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = activeGreen(),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        
        // Expanded content
        if (expanded) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
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
        }
    }
}


@Composable
fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            color = darkText(),
            fontWeight = FontWeight.Medium
        )
    }
}

@SuppressLint("HardwareIds", "MissingPermission")
fun getPhoneNumbers(context: Context): List<String> {
    val phoneNumbers = mutableListOf<String>()
    
    try {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // Try to get phone number from TelephonyManager
            val number = telephonyManager.line1Number
            if (!number.isNullOrEmpty()) {
                phoneNumbers.add(number)
            }
            
            // For dual SIM support (Android 5.1+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as android.telephony.SubscriptionManager
                val subscriptionInfoList = subscriptionManager.activeSubscriptionInfoList
                
                subscriptionInfoList?.forEach { subscriptionInfo ->
                    val simNumber = subscriptionInfo.number
                    if (!simNumber.isNullOrEmpty() && !phoneNumbers.contains(simNumber)) {
                        phoneNumbers.add(simNumber)
                    }
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("ProfileScreen", "Error getting phone numbers", e)
    }
    
    return phoneNumbers
}

// --- Bottom Navigation Bar ---
@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isDark = isSystemInDarkTheme()
    val barColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
        ) {
            // Bottom bar with wave cutout
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .align(Alignment.BottomCenter)
            ) {
                val path = Path().apply {
                    moveTo(0f, 60f)
                    lineTo(size.width * 0.3f, 60f)
                    
                    cubicTo(
                        size.width * 0.35f, 60f,
                        size.width * 0.4f, 0f,
                        size.width * 0.5f, 0f
                    )
                    
                    cubicTo(
                        size.width * 0.6f, 0f,
                        size.width * 0.65f, 60f,
                        size.width * 0.7f, 60f
                    )
                    
                    lineTo(size.width, 60f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                
                drawPath(
                    path = path,
                    color = Color.Black.copy(alpha = 0.05f),
                    style = Fill
                )
                
                drawPath(
                    path = path,
                    color = barColor,
                    style = Fill
                )
            }
            
            // Navigation items
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Home
                NavBarItem(
                    icon = if (currentRoute == Screen.Home.route) Icons.Filled.Home else Icons.Outlined.Home,
                    label = "Home",
                    selected = currentRoute == Screen.Home.route,
                    onClick = { navController.navigate(Screen.Home.route) { launchSingleTop = true } }
                )
                
                // Category
                NavBarItem(
                    icon = if (currentRoute == Screen.Categories.route) Icons.Filled.Apps else Icons.Outlined.Apps,
                    label = "Category",
                    selected = currentRoute == Screen.Categories.route,
                    onClick = { navController.navigate(Screen.Categories.route) { launchSingleTop = true } }
                )
                
                // Panier - just text, button is above
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Panier",
                        fontSize = 11.sp,
                        color = if (currentRoute == Screen.Panier.route) Color(0xFFE91E63) else Color.Gray
                    )
                }
                
                // Orders
                NavBarItem(
                    icon = if (currentRoute == Screen.Orders.route) Icons.Filled.Receipt else Icons.Outlined.Receipt,
                    label = "Orders",
                    selected = currentRoute == Screen.Orders.route,
                    onClick = { navController.navigate(Screen.Orders.route) { launchSingleTop = true } }
                )
                
                // Profile
                NavBarItem(
                    icon = if (currentRoute == Screen.Profile.route) Icons.Filled.Person else Icons.Outlined.Person,
                    label = "Profile",
                    selected = currentRoute == Screen.Profile.route,
                    onClick = { navController.navigate(Screen.Profile.route) { launchSingleTop = true } }
                )
            }
            
            // Elevated Center Button (Panier)
            FloatingActionButton(
                onClick = { navController.navigate(Screen.Panier.route) { launchSingleTop = true } },
                modifier = Modifier
                    .size(65.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 0.dp),
                containerColor = activeGreen(),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ShoppingBag,
                    contentDescription = "Panier",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun NavBarItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) activeGreen() else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (selected) activeGreen() else Color.Gray,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(initialRoute: String? = null) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val cartItems = remember { mutableStateListOf<CartItem>() }
    val orders = remember { mutableStateListOf<Order>() }
    val notifications = remember { mutableStateListOf<AppNotification>() }
    var notificationCount by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var deliveryLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    val auth = FirebaseAuth.getInstance()
    var currentUserId by remember { mutableStateOf(auth.currentUser?.uid) }
    var lastUserId by remember { mutableStateOf<String?>(null) }
    
    // Navigate to initial route if provided
    LaunchedEffect(initialRoute) {
        if (initialRoute != null) {
            navController.navigate(initialRoute)
        }
    }
    
    // Update currentUserId when auth state changes
    LaunchedEffect(Unit) {
        try {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                val newUserId = firebaseAuth.currentUser?.uid
                if (newUserId != currentUserId) {
                    android.util.Log.d("MainScreen", "🔐 Auth state changed: $newUserId")
                    currentUserId = newUserId
                }
            }
            auth.addAuthStateListener(listener)
        } catch (e: Exception) {
            android.util.Log.e("MainScreen", "❌ Auth listener error", e)
        }
    }

    // Handle user change - clear everything and sync from Firebase
    LaunchedEffect(currentUserId) {
        android.util.Log.d("MainScreen", "🔄 LaunchedEffect triggered")
        
        if (currentUserId != lastUserId) {
            android.util.Log.d("MainScreen", "🔄 User changed")
            
            // Clear everything
            cartItems.clear()
            orders.clear()
            notifications.clear()
            notificationCount = 0
            
            val userId = currentUserId
            if (userId != null) {
                try {
                    // Load from cache FIRST (instant)
                    val cachedOrders = DataStore.loadOrders(context, userId)
                    val cachedCart = DataStore.loadCartItems(context, userId)
                    
                    orders.addAll(cachedOrders)
                    cartItems.addAll(cachedCart)
                    android.util.Log.d("MainScreen", "⚡ Loaded from cache: ${orders.size} orders, ${cartItems.size} cart items")
                    
                    // Skip Firebase sync on login - let real-time listener handle it
                    android.util.Log.d("MainScreen", "✅ Using cache, listener will sync in background")
                } catch (e: Exception) {
                    android.util.Log.e("MainScreen", "❌ Cache load error", e)
                }
            } else {
                // User logged out - clear cache
                android.util.Log.d("MainScreen", "🚪 User logged out - clearing cache")
                try {
                    DataStore.saveOrders(context, null, emptyList())
                    DataStore.saveCartItems(context, null, emptyList())
                } catch (e: Exception) {
                    android.util.Log.e("MainScreen", "❌ Cache clear error", e)
                }
            }
            
            lastUserId = currentUserId
        }
    }

    // Real-time listener for orders
    LaunchedEffect(currentUserId) {
        val userId = currentUserId
        if (userId != null) {
            try {
                android.util.Log.d("MainScreen", "🎧 Starting real-time listener for user: $userId")
                OrderRepository.observeOrders(userId).collect { liveOrders ->
                    android.util.Log.d("MainScreen", "📡 Received ${liveOrders.size} orders from Firebase")
                    
                    // Log each order status for debugging
                    liveOrders.forEach { order ->
                        android.util.Log.d("MainScreen", "  Order ${order.orderId}: ${order.status}")
                    }
                    
                    // Merge orders intelligently to avoid duplicates
                    val tempOrders = orders.filter { it.orderId.startsWith("TEMP") }
                    val realOrderIds = liveOrders.map { it.orderId }.toSet()
                    
                    // Keep temp orders that haven't been replaced yet
                    val ordersToKeep = tempOrders.filter { !realOrderIds.contains(it.orderId) }
                    
                    // IMPORTANT: Create a new list to trigger recomposition
                    val newOrdersList = mutableListOf<Order>()
                    newOrdersList.addAll(liveOrders)
                    newOrdersList.addAll(ordersToKeep)
                    
                    // Update orders list
                    orders.clear()
                    orders.addAll(newOrdersList)
                    
                    DataStore.saveOrders(context, userId, liveOrders)
                    android.util.Log.d("MainScreen", "✅ Orders synced: ${liveOrders.size} from Firebase, ${ordersToKeep.size} temp")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainScreen", "❌ Real-time listener error: ${e.message}", e)
            }
        } else {
            android.util.Log.d("MainScreen", "⚠️ No user ID - skipping real-time listener")
        }
    }

    // Real-time listener for notifications
    LaunchedEffect(currentUserId) {
        val userId = currentUserId
        if (userId != null) {
            // Load from cache first
            val cached = DataStore.loadNotifications(context, userId)
            notifications.clear()
            notifications.addAll(cached)
            notificationCount = cached.count { !it.isRead }
            
            android.util.Log.d("MainScreen", "🔔 Starting notification listener")
            val listenerRegistration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("MainScreen", "Notification listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        android.util.Log.d("MainScreen", "🔔 Received ${snapshot.documents.size} notification documents")
                        
                        val liveNotifications = snapshot.documents.mapNotNull { doc ->
                            try {
                                val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time 
                                    ?: doc.getLong("timestamp") 
                                    ?: System.currentTimeMillis()
                                
                                AppNotification(
                                    id = doc.id,
                                    title = doc.getString("title") ?: "",
                                    body = doc.getString("body") ?: "",
                                    type = doc.getString("type") ?: "info",
                                    orderId = doc.getString("orderId"),
                                    timestamp = timestamp,
                                    isRead = doc.getBoolean("isRead") ?: false
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("MainScreen", "Failed to parse notification: ${e.message}")
                                null
                            }
                        }.sortedByDescending { it.timestamp }
                        
                        notifications.clear()
                        notifications.addAll(liveNotifications)
                        notificationCount = liveNotifications.count { !it.isRead }
                        DataStore.saveNotifications(context, userId, liveNotifications)
                        android.util.Log.d("MainScreen", "🔔 Notifications updated: ${liveNotifications.size}, unread: $notificationCount")
                    }
                }
            
            // Cleanup: Remove listener when LaunchedEffect is cancelled
            try {
                awaitCancellation()
            } finally {
                listenerRegistration.remove()
                android.util.Log.d("MainScreen", "🔔 Notification listener removed")
            }
        }
    }

    val onAddToCart: (Product, Double) -> Unit = { product, quantity ->
        val existingItem = cartItems.find { it.product.isSameAs(product) }
        if (existingItem != null) {
            existingItem.quantity = quantity
        } else {
            cartItems.add(CartItem(product, quantity))
        }
        DataStore.saveCartItems(context, currentUserId, cartItems.toList())
    }

    val onDeleteItem: (CartItem) -> Unit = { cartItem ->
        cartItems.remove(cartItem)
        DataStore.saveCartItems(context, currentUserId, cartItems.toList())
    }
    
    val onModifyItem: (CartItem, Double) -> Unit = { cartItem, newQuantity ->
        val index = cartItems.indexOfFirst { it.product.name == cartItem.product.name }
        if (index != -1) {
            cartItems[index] = cartItem.copy(quantity = newQuantity)
            DataStore.saveCartItems(context, currentUserId, cartItems.toList())
        }
    }
    
    val onClearCart: () -> Unit = {
        cartItems.clear()
        DataStore.saveCartItems(context, currentUserId, emptyList())
    }

    val onCheckoutWithLocation: (Double, Double) -> Unit = { lat, lng ->
        deliveryLocation = Pair(lat, lng)
        navController.navigate(Screen.Checkout.route)
    }
    
    val onPlaceOrder: suspend (Order) -> String? = { order ->
        android.util.Log.d("MainScreen", "🛒 Placing order...")
        var realOrderId: String? = null
        
        if (currentUserId != null) {
            try {
                // Add order to UI immediately with TEMP ID for instant feedback
                orders.add(0, order)
                android.util.Log.d("MainScreen", "⚡ Order added to UI immediately: ${order.orderId}")
                
                // Submit to API to get real order ID
                android.util.Log.d("MainScreen", "📤 Submitting to API...")
                val apiResult = ApiService.submitOrder(order, currentUserId!!)
                
                apiResult.onSuccess { response ->
                    android.util.Log.d("MainScreen", "✅ API response: $response")
                    
                    // Parse response to get real order ID
                    try {
                        val json = org.json.JSONObject(response)
                        realOrderId = json.getString("orderId")
                        
                        // Replace temp order with real order ID
                        val tempIndex = orders.indexOfFirst { it.orderId == order.orderId }
                        if (tempIndex != -1) {
                            val realOrder = order.copy(orderId = realOrderId!!)
                            orders[tempIndex] = realOrder
                            DataStore.saveOrders(context, currentUserId, orders.toList())
                            android.util.Log.d("MainScreen", "🔄 Replaced temp order with real ID: $realOrderId")
                        }
                        
                        // API already saved to Firestore - real-time listener will sync
                    } catch (e: Exception) {
                        android.util.Log.e("MainScreen", "Failed to parse API response", e)
                        // Fallback: save to Firebase directly
                        OrderRepository.saveOrder(order)
                        realOrderId = order.orderId
                    }
                }.onFailure { error ->
                    android.util.Log.e("MainScreen", "❌ API failed: ${error.message}")
                    // Fallback: save to Firebase directly
                    OrderRepository.saveOrder(order)
                    realOrderId = order.orderId
                }
                
            } catch (e: Exception) {
                android.util.Log.e("MainScreen", "💥 Error: ${e.message}", e)
                // Remove from UI and try Firebase
                orders.remove(order)
                OrderRepository.saveOrder(order)
                realOrderId = order.orderId
            }
        } else {
            android.util.Log.e("MainScreen", "❌ Not signed in")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Please sign in to place orders", Toast.LENGTH_LONG).show()
            }
        }
        
        realOrderId
    }
    
    val onCancelOrder: (Order) -> Unit = { order ->
        scope.launch {
            try {
                // Update status to CANCELLED in Firestore
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("orders")
                    .document(order.orderId)
                    .update("status", "CANCELLED")
                    .await()
                
                // Update local order status
                val index = orders.indexOfFirst { it.orderId == order.orderId }
                if (index != -1) {
                    orders[index] = order.copy(status = OrderStatus.CANCELLED)
                                            DataStore.saveOrders(context, currentUserId, orders.toList())
                }
                
                android.util.Log.d("MainScreen", "Order cancelled: ${order.orderId}")
            } catch (e: Exception) {
                android.util.Log.e("MainScreen", "Failed to cancel order", e)
            }
        }
    }
    
    val bgColor = lightGray()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = bgColor
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavigationGraph(
                navController = navController,
                modifier = Modifier.fillMaxSize(),
                context = context,
                cartItems = cartItems,
                orders = orders,
                notifications = notifications.toList(),
                notificationCount = notificationCount,
                deliveryLocation = deliveryLocation,
                onAddToCart = onAddToCart,
                onDeleteItem = onDeleteItem,
                onModifyItem = onModifyItem,
                onCheckoutWithLocation = onCheckoutWithLocation,
                onClearCart = onClearCart,
                onPlaceOrder = onPlaceOrder,
                onSignOut = {
                    orders.clear()
                    cartItems.clear()
                },
                onRefreshOrders = {
                    scope.launch {
                        val cloudOrders = OrderRepository.loadOrders(currentUserId)
                        orders.clear()
                        orders.addAll(cloudOrders)
                        DataStore.saveOrders(context, currentUserId, cloudOrders)
                    }
                },
                onCancelOrder = onCancelOrder,
                onMarkNotificationAsRead = { notificationId ->
                    val index = notifications.indexOfFirst { it.id == notificationId }
                    if (index != -1) {
                        notifications[index] = notifications[index].copy(isRead = true)
                        notificationCount = notifications.count { !it.isRead }
                        DataStore.saveNotifications(context, currentUserId, notifications.toList())
                        
                        // Update in Firestore
                        if (currentUserId != null) {
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("notifications")
                                .document(notificationId)
                                .update("isRead", true)
                        }
                    }
                },
                onClearAllNotifications = {
                    notifications.clear()
                    notificationCount = 0
                    DataStore.saveNotifications(context, currentUserId, emptyList())
                    
                    // Delete from Firestore
                    if (currentUserId != null) {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("notifications")
                            .whereEqualTo("userId", currentUserId)
                            .get()
                            .addOnSuccessListener { documents ->
                                for (document in documents) {
                                    document.reference.delete()
                                }
                            }
                    }
                },
                onSyncOrders = {
                    scope.launch {
                        // Always sync FROM cloud (source of truth)
                        val cloudOrders = OrderRepository.loadOrders(currentUserId)
                        orders.clear()
                        orders.addAll(cloudOrders)
                        DataStore.saveOrders(context, currentUserId, cloudOrders)
                        android.util.Log.d("OrdersScreen", "Refreshed ${cloudOrders.size} orders from cloud")
                    }
                }
            )
        }
    }
}

@Composable
fun NotificationsScreen(
    navController: NavHostController, 
    context: Context,
    notifications: List<AppNotification>,
    onMarkAsRead: (String) -> Unit,
    onClearAll: () -> Unit
) {
    android.util.Log.d("NotificationsScreen", "🔄 Recomposing with ${notifications.size} notifications")
    
    Column(modifier = Modifier.fillMaxSize().background(lightGray())) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().background(cardBackground()).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = activeGreen())
                Spacer(modifier = Modifier.width(8.dp))
                Text("Notifications", style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
            
            if (notifications.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("Clear All", color = Color(0xFFFF5252))
                }
            }
        }
        
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No notifications yet", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(notifications.size) { index ->
                    val notif = notifications[index]
                    NotificationItem(notif, onClick = {
                        android.util.Log.d("NotificationsScreen", "🔔 Clicked notification: ${notif.id}, type: ${notif.type}, orderId: ${notif.orderId}")
                        
                        // Navigate FIRST before marking as read
                        when (notif.type) {
                            "order" -> {
                                if (notif.orderId != null) {
                                    android.util.Log.d("NotificationsScreen", "📍 Navigating to order details: ${notif.orderId}")
                                    navController.navigate(Screen.OrderDetails.createRoute(notif.orderId))
                                } else {
                                    android.util.Log.d("NotificationsScreen", "📍 Navigating to orders list")
                                    navController.navigate(Screen.Orders.route)
                                }
                            }
                            else -> {
                                android.util.Log.d("NotificationsScreen", "⚠️ Unknown notification type: ${notif.type}")
                            }
                        }
                        
                        // Mark as read AFTER navigation
                        onMarkAsRead(notif.id)
                    })
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: AppNotification, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (notification.isRead) Color.White else Color(0xFFE8F5E9))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                when (notification.type) {
                    "order" -> Icons.Filled.Receipt
                    "promo" -> Icons.Filled.LocalOffer
                    else -> Icons.Filled.Info
                },
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(notification.title, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(notification.body, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(notification.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun OrderDetailsScreen(navController: NavHostController, orderId: String?, orders: List<Order>) {
    val order = orders.find { it.orderId == orderId }
    
    Column(modifier = Modifier.fillMaxSize().background(lightGray())) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Order Details", style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
        
        if (order == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Order not found", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Order ID", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Text(order.orderId)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Status", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Text(
                                    order.getStatusText(),
                                    color = order.getStatusColor(),
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Text("${order.totalAmount} DZD", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Color(0xFF4CAF50))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Date", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Text(java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(order.timestamp)))
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
                
                item {
                    Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                items(order.items.size) { index ->
                    val item = order.items[index]
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = item.product.imageResId),
                                contentDescription = null,
                                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.product.name, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Text("Qty: ${item.quantity} @ ${item.product.price}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
                
                item {
                    Button(
                        onClick = { /* TODO: Add English correction functionality */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Correct My English and Show Me the Error")
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    FRIZZLYTheme {
        MainScreen()
    }
}

