package com.example.f_rizzly

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object OrderRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        // Re-enable offline persistence (default is true)
        // Firestore's security rules and proper querying prevent user data mixing.
        // Disabling persistence can lead to ANRs or unexpected behavior.
    }

    // Real-time orders listener
    fun observeOrders(userId: String?): Flow<List<Order>> = callbackFlow {
        android.util.Log.d("OrderRepository", "🎧 observeOrders called for user: $userId")
        
        if (userId == null) {
            android.util.Log.w("OrderRepository", "⚠️ userId is null, returning empty")
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        android.util.Log.d("OrderRepository", "📡 Setting up Firestore listener...")
        val listener = firestore.collection("orders")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                android.util.Log.d("OrderRepository", "🔔 Snapshot listener triggered!")
                
                if (error != null) {
                    android.util.Log.e("OrderRepository", "❌ Listen failed: ${error.message}")
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    android.util.Log.d("OrderRepository", "📦 Snapshot received: ${snapshot.documents.size} documents")
                    
                    val orders = snapshot.documents.mapNotNull { doc ->
                        try {
                            val orderId = doc.getString("orderId") ?: return@mapNotNull null
                            val totalAmount = doc.getDouble("totalAmount") ?: 0.0
                            
                            // Handle both Long and Timestamp types
                            val timestamp = try {
                                doc.getLong("timestamp") ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                try {
                                    doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                                } catch (e2: Exception) {
                                    System.currentTimeMillis()
                                }
                            }
                            
                            val statusStr = doc.getString("status") ?: "PENDING"
                            val status = try {
                                OrderStatus.valueOf(statusStr)
                            } catch (e: Exception) {
                                OrderStatus.PENDING
                            }
                            
                            val itemsList = doc.get("items") as? List<HashMap<String, Any>> ?: emptyList()
                            android.util.Log.d("OrderRepository", "Order ${orderId}: Found ${itemsList.size} items in Firestore")
                            
                            val items = itemsList.mapNotNull { itemMap ->
                                try {
                                    val productName = itemMap["productName"] as? String ?: return@mapNotNull null
                                    val productPrice = itemMap["productPrice"] as? String ?: return@mapNotNull null
                                    val productImageResId = (itemMap["productImageResId"] as? Long)?.toInt() ?: android.R.drawable.ic_menu_gallery
                                    val quantity = when (val q = itemMap["quantity"]) {
                                        is Double -> q
                                        is Long -> q.toDouble()
                                        is Int -> q.toDouble()
                                        else -> return@mapNotNull null
                                    }
                                    
                                    CartItem(
                                        product = Product(productName, productPrice, productImageResId),
                                        quantity = quantity
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("OrderRepository", "Failed to parse item: ${e.message}")
                                    null
                                }
                            }
                            
                            val locationMap = doc.get("deliveryLocation") as? HashMap<String, Any>
                            val deliveryLocation = locationMap?.let {
                                val lat = it["latitude"] as? Double ?: return@let null
                                val lng = it["longitude"] as? Double ?: return@let null
                                Pair(lat, lng)
                            }
                            
                            android.util.Log.d("OrderRepository", "Order ${orderId}: Parsed ${items.size} items successfully")
                            
                            Order(
                                orderId = orderId,
                                items = items,
                                totalAmount = totalAmount,
                                deliveryLocation = deliveryLocation,
                                timestamp = timestamp,
                                status = status
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("OrderRepository", "Failed to parse order: ${e.message}")
                            null
                        }
                    }.sortedByDescending { it.timestamp }
                    
                    android.util.Log.d("OrderRepository", "✅ Parsed ${orders.size} orders, sending to Flow...")
                    val result = trySend(orders)
                    android.util.Log.d("OrderRepository", "📤 trySend result: ${result.isSuccess}, ${result.isFailure}, ${result.isClosed}")
                } else {
                    android.util.Log.w("OrderRepository", "⚠️ Snapshot is null")
                }
            }
        
        android.util.Log.d("OrderRepository", "✅ Listener registered, waiting for updates...")
        awaitClose { 
            android.util.Log.d("OrderRepository", "🔌 Listener closed")
            listener.remove() 
        }
    }
    
    // Save order to Firestore
    suspend fun saveOrder(order: Order): Boolean {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            android.util.Log.e("OrderRepository", "❌ Cannot save order - user not logged in")
            return false
        }
        
        return try {
            android.util.Log.d("OrderRepository", "💾 Attempting to save order")
            
            val orderData = hashMapOf(
                "orderId" to order.orderId,
                "items" to order.items.map { item ->
                    hashMapOf(
                        "productName" to item.product.name,
                        "productPrice" to item.product.price,
                        "productImageResId" to item.product.imageResId,
                        "quantity" to item.quantity
                    )
                },
                "totalAmount" to order.totalAmount,
                "deliveryLocation" to order.deliveryLocation?.let {
                    hashMapOf("latitude" to it.first, "longitude" to it.second)
                },
                "timestamp" to order.timestamp,
                "status" to order.status.name,
                "userId" to userId
            )
            
            firestore.collection("orders")
                .document(order.orderId)
                .set(orderData)
                .await()
            
            android.util.Log.d("OrderRepository", "Successfully saved order ${order.orderId}")
            true
        } catch (e: Exception) {
            android.util.Log.e("OrderRepository", "Failed to save order ${order.orderId}: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }
    
    // Load orders from Firestore
    suspend fun loadOrders(userId: String?): List<Order> {
        if (userId == null) {
            android.util.Log.e("OrderRepository", "Cannot load orders - user not logged in")
            return emptyList()
        }
        
        return try {
            android.util.Log.d("OrderRepository", "Loading orders for user")
            
            // Remove orderBy to avoid index requirement - sort in app instead
            val snapshot = firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            android.util.Log.d("OrderRepository", "Found ${snapshot.documents.size} orders in Firestore")
            
            val orders = snapshot.documents.mapNotNull { doc ->
                try {
                    val orderId = doc.getString("orderId") ?: return@mapNotNull null
                    val totalAmount = doc.getDouble("totalAmount") ?: 0.0
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val statusStr = doc.getString("status") ?: "PENDING"
                    val status = try {
                        OrderStatus.valueOf(statusStr)
                    } catch (e: Exception) {
                        OrderStatus.PENDING
                    }
                    
                    val itemsList = doc.get("items") as? List<HashMap<String, Any>> ?: emptyList()
                    val items = itemsList.mapNotNull { itemMap ->
                        try {
                            val productName = itemMap["productName"] as? String ?: return@mapNotNull null
                            val productPrice = itemMap["productPrice"] as? String ?: return@mapNotNull null
                            val productImageResId = (itemMap["productImageResId"] as? Long)?.toInt() ?: return@mapNotNull null
                            val quantity = (itemMap["quantity"] as? Double) ?: return@mapNotNull null
                            
                            CartItem(
                                product = Product(productName, productPrice, productImageResId),
                                quantity = quantity
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    val locationMap = doc.get("deliveryLocation") as? HashMap<String, Any>
                    val deliveryLocation = locationMap?.let {
                        val lat = it["latitude"] as? Double ?: return@let null
                        val lng = it["longitude"] as? Double ?: return@let null
                        Pair(lat, lng)
                    }
                    
                    Order(
                        orderId = orderId,
                        items = items,
                        totalAmount = totalAmount,
                        deliveryLocation = deliveryLocation,
                        timestamp = timestamp,
                        status = status
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            
            // Sort by timestamp in app (newest first)
            orders.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            android.util.Log.e("OrderRepository", "Failed to load orders: ${e.message}", e)
            e.printStackTrace()
            emptyList()
        }
    }
    
    // Update order status
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Boolean {
        return try {
            firestore.collection("orders")
                .document(orderId)
                .update("status", status.name)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
